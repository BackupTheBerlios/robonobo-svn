package com.robonobo.eon;

/*
 * Eye-Of-Needle
 * Copyright (C) 2008 Will Morton (macavity@well.com) & Ray Hilton (ray@wirestorm.net)

 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
import static com.robonobo.common.util.TimeUtil.*;
import static java.lang.Math.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import com.robonobo.common.async.PullDataProvider;
import com.robonobo.common.async.PullDataReceiver;
import com.robonobo.common.async.PushDataChannel;
import com.robonobo.common.async.PushDataReceiver;
import com.robonobo.common.concurrent.CatchingRunnable;
import com.robonobo.common.concurrent.Timeout;
import com.robonobo.common.exceptions.SeekInnerCalmException;
import com.robonobo.common.io.ByteBufferInputStream;
import com.robonobo.common.util.ByteUtil;
import com.robonobo.common.util.FlowRateIndicator;
import com.robonobo.common.util.Modulo;
import com.robonobo.common.util.TimeUtil;
import com.robonobo.eon.SEONConnection.State;

/**
 * The class that maintains one end of a S-EON (TCP-like) connection. For algorithm details, see RFC 4614 for general
 * TCP overview, RFC 2581 for congestion control, RFC 2018 for SACK and
 * http://citeseer.ist.psu.edu/fall96simulationbased.html for some algorithm comparison and examples.
 */
public class SEONConnection extends EONConnection implements PullDataReceiver, PushDataChannel {
	enum State {
		Closed, Listen, SynSent, SynReceived, Established, FinWait, LastAck
	};

	/**
	 * Logs details of all data received - ultra spammy, do not enable unless debugging!
	 */
	private static final boolean DEBUG_LOG_BUFS = false;

	private State state;
	/** 1492 (MTU) - 20 (ip hdr) - 8 (udp hdr) - 38 (max seon hdr size) */
	public static final int MSS = 1426;
	// DEBUG - use 1000 to make reading log files easier
	// public static final int MSS = 1000;
	static final int MAX_RTO = 30000;
	static final int MIN_RTO = 1000;
	static final int INIT_RTO = 5000;
	// Initial ssthresh of Integer.MAX_VALUE means that first time around, we
	// slow-start until we hit pkt loss
	static final int INITIAL_SSTHRESH = Integer.MAX_VALUE;
	/**
	 * Max pkts to be sent in response to a single ack, suggested value given in
	 * http://citeseer.ist.psu.edu/fall96simulationbased.html
	 */
	static final int MAX_BURST_PKTS = 4;
	static final float BPS_LIMIT_MULTIPLIER = 1.2f;
	// /** If srtt > (minRtt + gamma(maxRtt - minRtt)), then we have
	// congestion and we cut our window. See TCP-LP. */
	float gamma = 1.0f;
	static final float CONGESTION_THRESH = 0.15f;
	static NumberFormat nf;
	Modulo mod;
	EonSocketAddress localEP, remoteEP;
	Timeout retransTimeout, responseTimeout;
	LinkedList<ByteBuffer> incomingDataBufs = new LinkedList<ByteBuffer>();
	ByteBufferInputStream outgoing = new ByteBufferInputStream();
	LinkedList<SEONPacket> recvdPkts = new LinkedList<SEONPacket>();
	/** Packets that have been sent but not yet acknowledged */
	LinkedList<SEONPacket> retransQ = new LinkedList<SEONPacket>();
	/**
	 * Packets that have definitely been lost, and should be retransmitted before any new data
	 */
	LinkedList<SEONPacket> lostQ = new LinkedList<SEONPacket>();
	boolean needToRetransmitFirstPkt = false;
	/** Map<Long, Long> (seqnum, msSinceEpoch) */
	Map<Long, Long> transmissionTimes = new HashMap<Long, Long>();
	boolean inConnect = false; // Are we going through the connection process
	// after a Connect() call?
	long sendNext, sendUna, iss, recvNext;
	int numDupAcks = 0;
	/** multiple of MSS, not bytes */
	int ssThresh = INITIAL_SSTHRESH;
	/** multiple of MSS, not bytes */
	int sendWindow = 2;
	long bytesAckedSinceWindowInc = 0;
	/**
	 * Don't increase or decrease the window until sendUna is >= fastRecoveryUntil
	 */
	long fastRecoveryUntil = -1;
	/**
	 * When we get pkt loss during SS, we've probably lost a load of pkts; wait an RTT to allow the other end to ACK
	 * whatever it's got, then assume everything else is lost
	 */
	boolean needToHardRetransmit = false;
	Date hardRetransmitTime = null;
	int responseTimeoutNormal = 60000;
	int responseTimeoutLow = 30000; // Used during connection and destruction.
	// ms - default value
	int rto = INIT_RTO, srtt = 0, rttvar = 0; // ms

	boolean shouldSendFin = false;
	boolean shouldSendFinAck = false;

	// Congestion prioritization algorithm parameters
	int minObservedRtt = Integer.MAX_VALUE, maxObservedRtt = -1; // ms

	// If gamma<1, we may never lose packets after
	// our initial SS due to the congestion algorithm, and this may lead to our
	// maxObservedRtt becoming out of date, and hence our congestion algorithm
	// becoming bogus.
	// So, we update our maxObservedRtt every N seconds. However, if gamma<1
	// this will result in our maxObservedRtt getting
	// continually smaller. So, we never reduce our maxObservedRtt through this
	// continuous-update method below the floor of the maxObservedRtt values for
	// all connections in this priority pool. In contrast, maxObservedRtt values
	// taken due to packet loss may record any value.
	// TODO This is bollocks, rethink everything around updating maxRtt
	static int MAX_OBSERVED_RTT_MONITOR_PERIOD = 10000; // ms
	private int provMaxObservedRtt = 0;
	private Date lastChangedMaxObsRtt = new Date(0);

	Random rand;
	boolean noDelay;
	/** React to congestion indicators at most once per RTT */
	Date ignoreCongestionUntil = new Date(0);
	/**
	 * After reacting to congestion, monitor for 3*rtt and cut window to 1 if more congestion observed
	 */
	Date interferenceTimeout = new Date(0);
	PullDataProvider dataProvider;
	PushDataReceiver dataReceiver;
	boolean dataReceiverRunning = false;
	boolean waitingForVisitor = false;
	/** Used to make sure the datareceiver is called in order */
	ReentrantLock receiveLock = new ReentrantLock(true);
	Condition haveData;
	private CatchingRunnable asyncReceiver;

	public SEONConnection(EONManager mgr) throws EONException {
		super(mgr);
		state = State.Closed;
		// All sequence numbers are mod 2^32
		mod = new Modulo((long) Math.pow(2, 32));
		rand = new Random();
		// Use Nagle's algorithm by default
		noDelay = false;
		initTimeouts();
		asyncReceiver = new AsyncReceiver();
		haveData = receiveLock.newCondition();
	}

	SEONConnection(EONManager manager, SEONPacket synPacket) throws EONException {
		this(manager);
		localEP = new EonSocketAddress(mgr.getLocalSocketAddress(), synPacket.getDestSocketAddress().getEonPort());
		remoteEP = synPacket.getSourceSocketAddress();
		mgr.requestPort(localEP.getEonPort(), synPacket.getSourceSocketAddress(), this);
		recvNext = mod.add(synPacket.getSequenceNumber(), 1);
		selectISS();
		sendNext = mod.add(iss, 1);
		sendUna = iss;
		if (log.isDebugEnabled())
			log.debug("SEONConnection created for incoming connection from packet " + synPacket.toString());
		SEONPacket synAckPacket = new SEONPacket(localEP, remoteEP, null);
		synAckPacket.setSequenceNumber(iss);
		synAckPacket.setAckNumber(recvNext);
		synAckPacket.setSYN(true);
		synAckPacket.setACK(true);
		state = State.SynReceived;
		sendPktImmediate(synAckPacket, rto, responseTimeoutNormal);
	}

	static {
		nf = NumberFormat.getInstance();
		nf.setMaximumFractionDigits(2);
	}

	void initTimeouts() {
		// We do all this fucking about with NameGetters as the connection's
		// name changes after it connects
		retransTimeout = new Timeout(mgr.getExecutor(), new CatchingRunnable() {
			public void doRun() throws Exception {
				rtoTimeout();
			}
		}, new Timeout.NameGetter() {
			public String getName() {
				return SEONConnection.this + "[retrans]";
			}
		});
		responseTimeout = new Timeout(mgr.getExecutor(), new CatchingRunnable() {
			public void doRun() throws Exception {
				responseTimeout();
			}
		}, new Timeout.NameGetter() {
			public String getName() {
				return SEONConnection.this + "[response]";
			}
		});
	}

	public synchronized void cancelTimeouts() {
		retransTimeout.cancel();
		responseTimeout.cancel();
	}

	public synchronized void abort() {
		cancelTimeouts();
		if (state == State.Closed)
			return;
		if (state == State.Listen)
			close();
		SEONPacket rstPacket = new SEONPacket(localEP, remoteEP, null);
		rstPacket.setSequenceNumber(sendNext);
		rstPacket.setRST(true);
		sendPktImmediate(rstPacket, 0, responseTimeoutNormal);
		closeConnection();
	}

	public synchronized void bind() throws EONException {
		int port = mgr.getPort(this);
		localEP = new EonSocketAddress(mgr.getLocalSocketAddress(), port);
		setState(State.Listen);
	}

	public synchronized void bind(int localEONPort) throws EONException, IllegalArgumentException {
		if (mgr.requestPort(localEONPort, this)) {
			localEP = new EonSocketAddress(mgr.getLocalSocketAddress(), localEONPort);
			setState(State.Listen);
		} else
			throw new EONException("Port " + localEONPort + " not available");
	}

	public void providerClosed() {
		close();
	}

	public synchronized void close() {
		if (state == State.Closed || state == State.FinWait || state == State.LastAck)
			return;
		if (log.isDebugEnabled())
			log.debug(this + " closing");
		if (state == State.Listen) {
			closeConnection();
			return;
		}
		if (state == State.SynSent) {
			abort();
			return;
		}
		// SynReceived or Established
		// NB RFC 793 p60 is WRONG! We must set ACK as well as
		// FIN or it won't get processed
		noDelay = true;
		shouldSendFin = true;
		sendDataIfNecessary();
	}

	public void connect(EonSocketAddress remoteEP) throws EONException, IllegalArgumentException {
		connect(remoteEP, -1);
	}

	public synchronized void connect(EonSocketAddress remoteEndPoint, int localEONPort) throws EONException,
			IllegalArgumentException {
		if (state != State.Closed)
			throw new IllegalStateException("Connection already being attempted");
		if (remoteEndPoint == null)
			throw new EONException("remote end point cannot be null");
		// [Will 2006/5/06] We should allow connection to self, but I think it's
		// messing some things up
		if (remoteEndPoint.getInetSocketAddress().equals(mgr.getLocalSocketAddress()))
			throw new EONException("Can't connect to self yet, sorry");
		recvdPkts.clear();
		retransQ.clear();
		remoteEP = remoteEndPoint;
		// Grab our local port
		if (localEONPort == -1) {
			// Don't care which local port we use
			localEP = new EonSocketAddress(mgr.getLocalSocketAddress(), mgr.getPort(remoteEP, this));
		} else {
			mgr.requestPort(localEONPort, remoteEP, this);
			localEP = new EonSocketAddress(mgr.getLocalSocketAddress(), localEONPort);
		}
		selectISS();
		sendUna = iss;
		sendNext = mod.add(iss, 1);
		if (log.isDebugEnabled())
			log.debug("SEON Connection sending SYN packet");
		SEONPacket synPacket = new SEONPacket(localEP, remoteEP, null);
		synPacket.setSequenceNumber(iss);
		synPacket.setSYN(true);
		sendPktImmediate(synPacket, rto, responseTimeoutLow);
		inConnect = true;
		setState(State.SynSent);
	}

	public long getBytesInSendBuffer() {
		return outgoing.available();
	}

	/**
	 * Blocks until there is data to read
	 * 
	 * @return A byte buffer with the incoming data
	 */
	public void read(ByteBuffer buf) throws EONException {
		receiveLock.lock();
		try {
			while (true) {
				while (incomingDataBufs.size() == 0) {
					if (state == State.Closed)
						return;
					try {
						haveData.await();
					} catch (InterruptedException e) {
						throw new EONException(e);
					}
				}
				ByteBuffer incoming = (ByteBuffer) incomingDataBufs.getFirst();
				if (buf.remaining() >= incoming.remaining())
					buf.put(incoming);
				else {
					int remain = buf.remaining();
					buf.put(incoming.array(), incoming.position(), remain);
					incoming.position(incoming.position() + remain);
				}
				if (incoming.remaining() == 0)
					incomingDataBufs.removeFirst();
				if (buf.remaining() == 0)
					return;
				if (incomingDataBufs.size() == 0)
					return;
			}
		} finally {
			receiveLock.unlock();
		}
	}

	void gotIncomingData(SEONPacket pkt) {
		ByteBuffer buf = pkt.getPayload();
		buf.position(0);
		if (DEBUG_LOG_BUFS) {
			StringBuffer sb = new StringBuffer(this.toString()).append(" receiving data: ");
			ByteUtil.printBuf(buf, sb);
			log.debug(sb);
		}
		receiveLock.lock();
		try {
			incomingDataBufs.add(buf);
			inFlowRate.notifyData(buf.limit());
			if (dataReceiver == null) {
				// Synchronous receives
				haveData.signalAll();
			} else {
				// Async receives
				if (dataReceiverRunning) {
					// The receiver will pick up this pkt when it's finished
					return;
				} else
					fireAsyncReceiver();
			}
		} finally {
			receiveLock.unlock();
		}
	}

	/** Must only be called from inside sync(receiveLock) block */
	private void fireAsyncReceiver() {
		dataReceiverRunning = true;
		mgr.getExecutor().execute(asyncReceiver);
	}

	/**
	 * Milliseconds
	 */
	public int getCurrentRTO() {
		return rto;
	}

	public EonSocketAddress getLocalSocketAddress() {
		return localEP;
	}

	public boolean getNoDelay() {
		return noDelay;
	}

	/** This might be null */
	public EonSocketAddress getRemoteSocketAddress() {
		return remoteEP;
	}

	public boolean isOpen() {
		return state != State.Closed;
	}

	/**
	 * Milliseconds
	 */
	public int getResponseTimeout() {
		return responseTimeoutNormal;
	}

	public int getSynTimeout() {
		return responseTimeoutLow;
	}

	/**
	 * Set a dataprovider to do asynchronous sending: it will be queried for more data whenever we run out. Pass null to
	 * unset a previously-set dataprovider and go back to synchronous sending. If the dataprovider returns null, no data
	 * will be sent; after this has happened, to send more data you will need to call send() or setDataProvider() again,
	 * after which the dataprovider will be queried again as necessary.
	 */
	public void setDataProvider(PullDataProvider dataProvider) {
		this.dataProvider = dataProvider;
		if (log.isDebugEnabled())
			log.debug(this + " set dataprovider: " + dataProvider);
		if (dataProvider != null)
			sendDataIfNecessary();
	}

	/**
	 * Set a datareceiver to to async receiving instead of calling read(): receiveData() will be called with every
	 * bytebuffer that is received (and null as the second argument). receiveData() will not be called again until the
	 * previous call has returned, and the calls are guaranteed to be made in order of delivery
	 */
	public void setDataReceiver(PushDataReceiver dataReceiver) {
		receiveLock.lock();
		try {
			this.dataReceiver = dataReceiver;
			// If we've got incoming data blocks queued up, handle them now
			if (dataReceiver != null && incomingDataBufs.size() > 0 && !dataReceiverRunning)
				fireAsyncReceiver();
		} finally {
			receiveLock.unlock();
		}
	}

	void fetchMoreData() {
		ByteBuffer buf = dataProvider.getMoreData();
		if (buf == null) {
			if (log.isDebugEnabled())
				log.debug(this + " fetching more data: returned null");
		} else {
			if (log.isDebugEnabled())
				log.debug(this + " fetching more data: returned " + buf.remaining() + " bytes");
			outgoing.addBuffer(buf);
		}
	}

	public void send(ByteBuffer buf) throws EONException {
		if (state == State.Closed || state == State.Listen || state == State.FinWait || state == State.LastAck)
			throw new EONException("Cannot send in current state");
		outgoing.addBuffer(buf);
		if (retransQ.size() == 0)
			sendDataIfNecessary();
	}

	public void receiveData(ByteBuffer data, Object ignore) throws IOException {
		try {
			send(data);
		} catch (EONException e) {
			// Irritatingly, java <6 doesn't allow nested exceptions within IOE
			throw new IOException("Caught EONException: " + e.getMessage());
		}
	}

	public void setNoDelay(boolean noDelay) {
		this.noDelay = noDelay;
	}

	public void setResponseTimeout(int timeout) {
		responseTimeoutNormal = timeout;
	}

	public void setSynTimeout(int value) {
		responseTimeoutLow = value;
	}

	public String toString() {
		EonSocketAddress myEp = remoteEP;
		if (myEp != null)
			return "SEONConnection-" + remoteEP.toString();
		return "SEONConnection-UNCONNECTED";
	}

	public synchronized void receivePacket(SEONPacket pkt) throws EONException {
		if (state == State.Closed)
			throw new EONException("Connection is closed");
		stopResponseTimer();
		if (state == State.SynSent) {
			receivePacketInSynSent(pkt);
			return;
		}
		if (state == State.Listen) {
			receivePacketInListen(pkt);
			return;
		}
		// SynReceived, Established, FinWait, LastAck states
		long firstSeqNum = pkt.getSequenceNumber();
		long lastSeqNum;
		if (pkt.getPayload() != null && pkt.getPayload().limit() > 1)
			lastSeqNum = mod.add(firstSeqNum, (long) pkt.getPayload().limit() - 1);
		else
			lastSeqNum = firstSeqNum;

		if (mod.lt(lastSeqNum, recvNext)) {
			if (log.isDebugEnabled())
				log.debug(this + " dropping duplicate pkt " + pkt);
			sendBareAck();
			return;
		}
		if (mod.gte(recvNext, firstSeqNum) && mod.lte(recvNext, lastSeqNum))
			processPktNow(pkt);
		else {
			storePktForLater(pkt);
			// Let the sender know about this out-of-order reception by
			// sending a duplicate ack (if this pkt has data or is a FIN)
			if (pkt.isFIN() || (pkt.getPayload() != null && pkt.getPayload().limit() > 0))
				sendDupAck(pkt.getSequenceNumber());
		}
	}

	private void processPktNow(SEONPacket pkt) throws EONException {
		// Only send one ack, we might be processing a lot of pkts here
		boolean shouldSendBareAck = false;
		while (pkt != null) {
			if (pkt.isRST()) {
				closeConnection();
				return;
			}
			if (pkt.isSYN()) {
				SEONPacket rstPacket = new SEONPacket(localEP, remoteEP, null);
				rstPacket.setSequenceNumber(pkt.getAckNumber());
				rstPacket.setRST(true);
				sendPktImmediate(rstPacket, 0, 0);
				closeConnection();
				return;
			}
			if (!pkt.isACK()) {
				// Drop packet
				return;
			}
			if (state == State.LastAck) {
				if (pkt.getAckNumber() == sendNext) {
					// Our FinAck is acked, we're outta here
					closeConnection();
					return;
				}
			}
			// Does this pkt acknowledge any new data?
			boolean newDataAcked = false;
			if (mod.gt(pkt.getAckNumber(), sendUna) && mod.lte(pkt.getAckNumber(), sendNext)) {
				// Ack number has increased
				if (state == State.SynReceived) {
					setState(State.Established);
					inConnect = false;
				}
				newDataAcked = true;
				numDupAcks = 0;
				sendUna = pkt.getAckNumber();
				long bytesAckedByThisPkt = trimRetransmissionQueues(pkt);
				updateSendWindow(bytesAckedByThisPkt);
			} else if (mod.gt(pkt.getAckNumber(), sendNext)) {
				// Er, what? Haven't sent that one yet, dude!
				SEONPacket ackPacket = new SEONPacket(localEP, remoteEP, null);
				ackPacket.setSequenceNumber(sendNext);
				ackPacket.setAckNumber(recvNext);
				ackPacket.setACK(true);
				sendPktImmediate(ackPacket, 0, 0);
				return;
			} else if (pkt.getPayload() == null || pkt.getPayload().limit() == 0)
				newDataAcked = handleDupAck(pkt);
			// If we get here and we're still in SynReceived, something has
			// gone wrong
			if (state == State.SynReceived) {
				SEONPacket rstPacket = new SEONPacket(localEP, remoteEP, null);
				rstPacket.setSequenceNumber(pkt.getAckNumber());
				rstPacket.setRST(true);
				sendPktImmediate(pkt, 0, 0);
				closeConnection();
				return;
			}
			if (newDataAcked && getGamma() < 1.0f)
				checkCongestion();

			if (pkt.getPayload() != null && pkt.getPayload().limit() > 0) {
				gotIncomingData(pkt);
				recvNext = mod.add(pkt.getSequenceNumber(), pkt.getPayload().limit());
				boolean sentData = sendDataIfNecessary();
				shouldSendBareAck = !sentData;
			} else {
				if (log.isDebugEnabled() && outgoing.available() == 0)
					log.debug(this + ": outgoing buffer empty");
				sendDataIfNecessary();
			}
			// wikipedia suggests that we should restart retrans timer
			// whenever new data is acked, regardless of whether we've just
			// sent pkts or not, so we'll try it...
			// if (newDataAcked && pktsSent > 0)
			if (newDataAcked && retransQ.size() > 0)
				restartRetransmissionTimer(rto);
			if (pkt.isFIN()) {
				if (state == State.LastAck) {
					// Increment RecvNext if we didn't get any data
					if (pkt.getPayload() == null || pkt.getPayload().limit() == 0)
						recvNext = mod.add(pkt.getSequenceNumber(), 1);
					shouldSendBareAck = true;
				} else if (state == State.FinWait) {
					// Increment RecvNext if we didn't get any data
					if (pkt.getPayload() == null || pkt.getPayload().limit() == 0)
						recvNext = mod.add(pkt.getSequenceNumber(), 1);
					if (pkt.getAckNumber() == sendNext) {
						// Our FIN is acked
						sendBareAck();
						closeConnection();
						return;
					} else {
						// Simultaneous FINs
						SEONPacket finAckPacket = new SEONPacket(localEP, remoteEP, null);
						finAckPacket.setSequenceNumber(sendNext);
						sendNext = mod.add(sendNext, 1);
						finAckPacket.setAckNumber(recvNext);
						finAckPacket.setFIN(true);
						finAckPacket.setACK(true);
						sendPktImmediate(finAckPacket, rto, responseTimeoutLow);
						setState(State.LastAck);
						restartRetransmissionTimer(rto);
					}
				} else {
					noDelay = true;
					recvNext = mod.add(pkt.getSequenceNumber(), 1);
					shouldSendFinAck = true;
					sendDataIfNecessary();
				}
			}
			// Check if we can now process more packets
			if (recvdPkts.size() == 0) {
				pkt = null;
			} else {
				SEONPacket nextPacket = recvdPkts.getFirst();
				long pktDataLength = nextPacket.getPayload() == null ? 0 : nextPacket.getPayload().limit();
				if (mod.gte(recvNext, nextPacket.getSequenceNumber())
						&& mod.lte(recvNext, mod.add(nextPacket.getSequenceNumber(), pktDataLength))) {
					recvdPkts.remove();
					pkt = nextPacket;
				} else {
					// No more packets to process now
					pkt = null;
				}
			}
		}
		if (shouldSendBareAck)
			sendBareAck();
	}

	private void storePktForLater(SEONPacket pkt) throws EONException {
		// If the packet is after our last one, put it at the end, otherwise
		// search where to put it
		if (recvdPkts.size() == 0)
			recvdPkts.add(pkt);
		else if (mod.gt(pkt.getSequenceNumber(), recvdPkts.getLast().getSequenceNumber()))
			recvdPkts.addLast(pkt);
		else {
			// TODO If this is still too slow, we could cut out the iterator
			// creation by creating a custom list class with a resettable
			// iterator...
			ListIterator<SEONPacket> iter = recvdPkts.listIterator();
			while (iter.hasNext()) {
				SEONPacket itPkt = iter.next();
				if (pkt.getSequenceNumber() == itPkt.getSequenceNumber()) {
					// Duplicate pkt - ignore
					break;
				} else if (mod.lt(pkt.getSequenceNumber(), itPkt.getSequenceNumber())) {
					// Insert the pkt *before* the current pkt
					iter.previous();
					iter.add(pkt);
					break;
				}
			}
		}
	}

	private void receivePacketInListen(SEONPacket pkt) throws EONException {
		// Check to see that everything's kosher with the first packet, then
		// create a connection
		if (pkt.isRST()) {
			return;
		}
		if (pkt.isACK()) {
			SEONPacket rstPacket = new SEONPacket(localEP, pkt.getSourceSocketAddress(), null);
			rstPacket.setRST(true);
			rstPacket.setSequenceNumber(pkt.getAckNumber());
			sendPktImmediate(rstPacket, 0, 0);
			return;
		}
		if (pkt.isSYN()) {
			SEONConnection newConn = new SEONConnection(mgr, pkt);
			fireOnNewConnection(newConn);
		}
	}

	private void receivePacketInSynSent(SEONPacket packet) throws EONException {
		if (packet.isACK()) {
			if (mod.lte(packet.getAckNumber(), iss) || mod.gt(packet.getAckNumber(), sendNext)) {
				// Acknowledgement number unacceptable
				if (!packet.isRST()) {
					SEONPacket rstPacket = new SEONPacket(localEP, remoteEP, null);
					rstPacket.setRST(true);
					rstPacket.setSequenceNumber(packet.getAckNumber());
					sendPktImmediate(rstPacket, 0, 0);
				}
				return;
			} else {
				// Acknowledgement number acceptable
				if (packet.isRST()) {
					// The other end is resetting this connection
					closeConnection();
				}
			}
		} else {
			if (packet.isRST()) {
				// Ignore
				return;
			}
		}
		if (packet.isSYN()) {
			recvNext = mod.add(packet.getSequenceNumber(), 1);
			if (packet.isACK()) {
				sendUna = packet.getAckNumber();
				trimRetransmissionQueues(packet);
				if (retransQ.size() > 0)
					restartRetransmissionTimer(rto);
			}
			if (mod.gt(sendUna, iss)) {
				// Our SYN has been ACKd
				SEONPacket ackPacket = new SEONPacket(localEP, remoteEP, null);
				ackPacket.setSequenceNumber(sendNext);
				ackPacket.setAckNumber(recvNext);
				ackPacket.setACK(true);

				// If we are asynchronously sending and we need more data, get
				// some
				if (dataProvider != null && outgoing.available() < SEONConnection.MSS)
					fetchMoreData();

				// Add any data if we've got it
				if (outgoing.available() > 0) {
					int numBytes = (outgoing.available() < SEONConnection.MSS) ? outgoing.available()
							: SEONConnection.MSS;
					byte[] payloadArr = new byte[numBytes];
					try {
						outgoing.read(payloadArr, 0, numBytes);
						ByteBuffer payload = ByteBuffer.wrap(payloadArr);
						ackPacket.setPayload(payload);
						sendNext = mod.add(sendNext, numBytes);
					} catch (IOException e) {
						throw new EONException("Something happened to the pipe buffer", e);
					}
				}
				sendPktImmediate(ackPacket, rto, responseTimeoutNormal);
				inConnect = false; // We're connected
				setState(State.Established);
				// If there's any backlogged data, send it now
				sendDataIfNecessary();
				return;
			} else {
				// Our SYN has not been ACKd - simultaneous SYNs
				// from both sockets
				SEONPacket synAckPacket = new SEONPacket(localEP, remoteEP, null);
				synAckPacket.setSequenceNumber(iss);
				synAckPacket.setAckNumber(recvNext);
				synAckPacket.setSYN(true);
				synAckPacket.setACK(true);
				sendPktImmediate(synAckPacket, rto, responseTimeoutNormal);
				setState(State.SynReceived);
				return;
			}
		} else {
			// Neither SYN nor RST is set - ignore
			return;
		}
	}

	private void updateSendWindow(long bytesAckedByThisPkt) {
		// If no bytes acked, don't update window
		if (bytesAckedByThisPkt == 0) {
			if (log.isDebugEnabled())
				log.debug(this + " not updating window, no bytes acked by this pkt");
			return;
		}
		// If we are pausing before retransmitting, don't update window
		if (needToHardRetransmit) {
			if (log.isDebugEnabled())
				log.debug(this + " not updating window, still pausing before hard retransmit");
			return;
		}
		// If we are within our interference timeout, don't update window
		if (now().before(interferenceTimeout)) {
			if (log.isDebugEnabled())
				log.debug(this + " not updating window until interference timeout ("
						+ TimeUtil.getTimeFormat().format(interferenceTimeout) + ")");
			return;
		}
		// Don't extend our window above the global max bps (plus a bit of wiggle room)
		int globMaxBps = mgr.getMaxOutboundBps();
		if(globMaxBps >= 0) {
			int myBps = (int) ((float)(1000 / srtt) * sendWindow * MSS);
			int bpsLim = (int) Math.max(globMaxBps * BPS_LIMIT_MULTIPLIER, globMaxBps + MSS);
			if(myBps > bpsLim) {
				if(log.isDebugEnabled())
					log.debug(this+" not updating window - above global max bps ("+myBps+" > "+bpsLim+")");
				return;
			}
		}
		// If we are still in fast recovery, don't update the window
		if (fastRecoveryUntil >= 0 && mod.lt(sendUna, fastRecoveryUntil)) {
			if (log.isDebugEnabled())
				log.debug(this + " in fast recovery - not updating window until sendUna >= " + fastRecoveryUntil);
			return;
		}
		fastRecoveryUntil = -1;
		// Adjust our window depending on which algorithm we're using
		if (sendWindow < ssThresh) {
			// Slow start
			sendWindow++;
			if (log.isDebugEnabled())
				log.debug(this + " slow start, incrementing window to " + sendWindow);
		} else {
			// Congestion avoidance
			if (sendWindow == ssThresh && bytesAckedSinceWindowInc == 0)
				log.debug(this + " leaving slow start, entering congestion avoidance");
			bytesAckedSinceWindowInc += bytesAckedByThisPkt;
			if (bytesAckedSinceWindowInc >= windowInBytes()) {
				sendWindow++;
				bytesAckedSinceWindowInc = 0;
				if (log.isDebugEnabled())
					log.debug(this + " congestion avoidance, incrementing window to " + sendWindow);
			}
		}
		// Log window measurements
		if (log.isDebugEnabled())
			log.debug(this + ", WINSTATS, " + TimeUtil.now().getTime() + ", " + sendWindow);
	}

	/**
	 * Returns true if new data was acknowledged, false otherwise
	 */
	private boolean handleDupAck(SEONPacket pkt) throws EONException {
		numDupAcks++;
		if (numDupAcks == 3) {
			if (sendWindow < ssThresh) {
				// If we are slow-starting, we've just lost a load of pkts as
				// we've ramped to 2*safe window - wait for time = RTO to let
				// the other end ack what it's got, then assume everything
				// else is lost
				if (!needToHardRetransmit)
					signalHardRetransmit();
			} else {
				// Congestion avoidance - cut the window, wait
				// for SACKs to reduce the
				// retransmission queue, then retransmit the
				// first pkt and continue CA
				fastRetransmit();
			}
		}
		// Trim the queue, dupack may contain a SACK block
		int bytesTrimmed = trimRetransmissionQueues(pkt);
		return (bytesTrimmed > 0);
	}

	private void checkCongestion() {
		// Don't measure congestion if we are on our initial slow start
		// (ssThresh == int.maxvalue) as we don't yet have a meaningful measure
		// of maxRtt
		if (ssThresh == INITIAL_SSTHRESH) {
			if (log.isDebugEnabled())
				log.debug(this + " not checking congestion - still in initial SS");
			return;
		}
		if (!isCongested())
			return;
		if (TimeUtil.now().before(interferenceTimeout)) {
			// We are still within our congestion timer, cut window to 1
			sendWindow = 1;
			fastRecoveryUntil = -1;
			if (log.isDebugEnabled())
				log.debug(this + " repeat congestion observed - cutting window to 1");
		} else {
			// Cut window in half and monitor for congestion for 3 RTTs
			if (sendWindow >= 2)
				sendWindow /= 2;
			fastRecoveryUntil = -1;
			if (log.isDebugEnabled())
				log.debug(this + " congestion observed - cutting window to " + sendWindow);
			interferenceTimeout = TimeUtil.timeInFuture(3 * srtt);
		}
		// Don't react to more than one congestion event per RTT
		ignoreCongestionUntil = TimeUtil.timeInFuture(srtt);
	}

	synchronized void fireOnNewConnection(SEONConnection conn) {
		EONConnectionListener myListener = listener;
		if (myListener == null)
			return;
		EONConnectionEvent event = new EONConnectionEvent(conn);
		if (myListener instanceof SEONConnectionListener)
			((SEONConnectionListener) myListener).onNewSEONConnection(event);
	}

	void sendBareAck() throws EONException {
		SEONPacket pkt = new SEONPacket(localEP, remoteEP, null);
		pkt.setSequenceNumber(sendNext);
		pkt.setACK(true);
		pkt.setAckNumber(recvNext);
		sendPktImmediate(pkt, 0, 0);
	}

	synchronized void restartRetransmissionTimer(int rto) {
		if (log.isDebugEnabled())
			log.debug(this + ": Starting retransmission timer: " + rto + " ms");
		retransTimeout.set(rto);
	}

	synchronized void stopRetransmissionTimer() {
		if (log.isDebugEnabled())
			log.debug(this + ": Stopping retransmission timer");
		retransTimeout.clear();
	}

	@SuppressWarnings("static-access")
	void closeConnection() {
		// Take note if we're listening, but close before we call returnPort()
		// as the connholder might be waiting on us to close
		boolean wasListening = (state == state.Listen);
		setState(State.Closed);
		cancelTimeouts();
		try {
			if (wasListening)
				mgr.returnPort(localEP.getEonPort(), this);
			else
				mgr.returnPort(localEP.getEonPort(), remoteEP, this);
		} catch (EONException e) {
			log.error("Caught Exception while trying to close connection", e);
		}
		synchronized (this) {
			notifyAll();
		}
		fireOnClose();
	}

	/**
	 * Called when we get 3 dupacks during congestion avoidance. Cut the window and wait for SACKs to reduce the retrans
	 * queue, then when it's reduced to the window size, retransmit the first pkt
	 */
	void fastRetransmit() {
		StringBuffer sb = null;
		// Mark first pkt as being lost
		SEONPacket pkt = retransQ.peek();
		if (pkt == null) {
			// Shouldn't happen - bug huntin
			log.error("ERROR: fast retransmit, but no pkts in retrans queue");
			return;
		}
		needToRetransmitFirstPkt = true;
		if (log.isDebugEnabled())
			sb = new StringBuffer(toString()).append(": marking pkt " + (pkt).getSequenceNumber()
					+ " for fast retransmit");
		// Don't use lost pkts for rtt calculations
		transmissionTimes.remove(new Long(pkt.getSequenceNumber()));
		// Enter fast recovery state - unless we're already there
		if (fastRecoveryUntil < 0 || mod.gte(sendUna, fastRecoveryUntil)) {
			changeParamsForLossage();
			sendWindow = ssThresh;
			if (log.isDebugEnabled()) {
				sb.append(" - setting ssThresh, window to ").append(ssThresh);
				log.debug(sb);
				// Log window measurements
				log.debug(this + ", WINSTATS, " + TimeUtil.now().getTime() + ", " + sendWindow);
			}
			// Stay in fast recovery state until all outstanding pkts are clear
			SEONPacket lastSentPkt = retransQ.getLast();
			// Payload might be null if the pkt is a FIN
			fastRecoveryUntil = mod.add(lastSentPkt.getSequenceNumber(), (lastSentPkt.getPayload() == null) ? 1
					: lastSentPkt.getPayload().limit());
		}
		if (log.isDebugEnabled())
			log.debug(sb.toString());
	}

	void signalHardRetransmit() {
		needToHardRetransmit = true;
		int waitTime = rto;
		hardRetransmitTime = TimeUtil.timeInFuture(waitTime);
		if (log.isDebugEnabled())
			log.debug(this + " hard retransmit - waiting for " + waitTime + " ms (RTO)");
		changeParamsForLossage();
		// Slow start again
		sendWindow = 2;
		if (log.isDebugEnabled())
			log.debug(this + ", WINSTATS, " + TimeUtil.now().getTime() + ", " + sendWindow);
		fastRecoveryUntil = -1;
		// Cancel retransmission timeout, otherwise it'll fire at the same time
		// as our hard retransmit, and bugger things up
		retransTimeout.clear();
		mgr.getExecutor().schedule(new CatchingRunnable() {
			public void doRun() throws Exception {
				synchronized (SEONConnection.this) {
					if (needToHardRetransmit)
						sendDataIfNecessary();
				}
			}
		}, waitTime, TimeUnit.MILLISECONDS);
	}

	synchronized void rtoTimeout() {
		if (!isOpen())
			return;
		if (retransQ.size() == 0)
			return;
		if (log.isDebugEnabled())
			log.debug(this + " - RTO TIMEOUT");
		// Back off timer and reset timings
		rto *= 2;
		if (rto > MAX_RTO)
			rto = MAX_RTO;
		changeParamsForLossage();
		resetRtt();
		sendWindow = 1;
		fastRecoveryUntil = -1;
		markInTransitPktsAsLost();
		needToRetransmitFirstPkt = false;
		needToHardRetransmit = false;
		sendDataIfNecessary();
	}

	/**
	 * When we lose a pkt, change our window/congestion parameters
	 */
	void changeParamsForLossage() {
		synchronized (this) {
			if (retransQ.size() > 4)
				ssThresh = retransQ.size() / 2;
			else
				ssThresh = 2;
			if (log.isDebugEnabled())
				log.debug(this + " reducing ssThresh to " + ssThresh);
		}
		// Update our maxObservedRtt value (used by the congestion algorithm) to
		// that observed just before lossage
		maxObservedRtt = srtt;
		bytesAckedSinceWindowInc = 0;
	}

	public synchronized void constrainSSThresh(int maxSSThresh) {
		if (ssThresh > maxSSThresh)
			ssThresh = maxSSThresh;
	}

	synchronized void responseTimeout() {
		if (isOpen()) {
			if (log.isDebugEnabled())
				log.debug(SEONConnection.this + " - response timeout expired - exiting");
			recvdPkts.clear();
			closeConnection();
		}
	}

	/** All packets in the retrans queue are regarded as lost */
	void markInTransitPktsAsLost() {
		// Add everything in retransQ to lostQ, inserting at the correct places
		// -
		// both lists are already sorted so this isn't too bad
		ListIterator<SEONPacket> lostIter = lostQ.listIterator();
		SEONPacket curLostPkt = lostIter.hasNext() ? lostIter.next() : null;
		while (retransQ.size() > 0) {
			SEONPacket newPkt = retransQ.removeFirst();
			while (true) {
				if (curLostPkt == null) {
					lostQ.addLast(newPkt);
					break;
				}
				// If our newly-added packet comes before this one, add it
				// before
				if (mod.lt(newPkt.getSequenceNumber(), curLostPkt.getSequenceNumber())) {
					lostIter.previous();
					lostIter.add(newPkt);
					lostIter.next();
					break;
				}
				curLostPkt = lostIter.hasNext() ? lostIter.next() : null;
			}
		}
	}

	/**
	 * Generate a new sequence number, we want something between 1 and 2^32-1
	 */
	void selectISS() {
		// to make sure we get a value within the modulus, mod add 0
		Random r = new Random();
		iss = mod.add(Math.abs(r.nextInt()), 0);
		// DEBUG this makes log files easier to read
		// iss = 999;
	}

	protected boolean sendDataIfNecessary() {
		if (needToHardRetransmit && !now().before(hardRetransmitTime))
			runHardRetransmit();
		if (waitingForVisitor)
			return false;
		boolean result = haveDataToSend();
		// Tell the mgr that we have data to send, the sender thread will call acceptVisitor() to send our pkts
		if (result) {
			mgr.haveDataToSend(this);
			waitingForVisitor = true;
		}
		return result;
	}

	// synchronized int sendDataAsNecessary(boolean flushQueue) throws EONException {
	// if (state == State.Closed)
	// throw new EONException("Connection is closed");
	// if (state == State.Listen) {
	// // Can't happen
	// throw new SeekInnerCalmException();
	// }
	// // Don't send data in SynSent/SynReceived - but allow ourselves to
	// // retransmit
	// if (lostQ.size() == 0 && (state == State.SynSent || state == State.SynReceived)) {
	// if (log.isDebugEnabled())
	// log.debug(this + " not sending data, state is " + state);
	// return 0;
	// }
	// if (gamma == 0f)
	// return 0;
	//
	// int pktsSent = 0;
	// // Are we waiting after a pkt loss during SS?
	// if (needToHardRetransmit) {
	// if (TimeUtil.now().before(hardRetransmitTime)) {
	// return 0;
	// } else {
	// // TODO Move this out of this method, we should check before this
	// markInTransitPktsAsLost();
	// needToRetransmitFirstPkt = false;
	// if (log.isDebugEnabled())
	// log.debug(this + " hard retransmit - marking " + lostQ.size() + " pkts as lost");
	// resetRtt();
	// needToHardRetransmit = false;
	// }
	// }
	// try {
	// while (needToRetransmitFirstPkt || lostQ.size() > 0 || outgoing.available() > 0 || dataProvider != null) {
	// if (retransQ.size() >= sendWindow) {
	// if (log.isDebugEnabled()) {
	// StringBuffer sb = new StringBuffer(toString());
	// sb.append(" not sending data now due to window size (win=");
	// sb.append(sendWindow).append(",pkts=").append(retransQ.size());
	// sb.append(")");
	// log.debug(sb);
	// }
	// return pktsSent;
	// }
	// if (pktsSent >= MAX_BURST_PKTS) {
	// if (log.isDebugEnabled())
	// log.debug(this + " not sending data now due to burst limit");
	// return pktsSent;
	// }
	// // If we have any lost packets, send them before we send new
	// // data
	// if (lostQ.size() > 0) {
	// SEONPacket pkt = (SEONPacket) lostQ.removeFirst();
	// sendPacket(pkt, false);
	// pktsSent++;
	// continue;
	// }
	// // If we need to resend our pkt, do it now
	// if (needToRetransmitFirstPkt) {
	// SEONPacket pkt = (SEONPacket) retransQ.peek();
	// // Send it via the mgr directly to avoid adding it to
	// // the retransmission queue
	// mgr.sendPacket(pkt, getGamma(), false);
	// pktsSent++;
	// needToRetransmitFirstPkt = false;
	// continue;
	// }
	// // If we are asynchronously sending and we need more data, get
	// // some
	// if (dataProvider != null && outgoing.available() <= MSS) {
	// fetchMoreData();
	// // If we have no data in our queue after fetching more data,
	// // there is no more to send
	// if (outgoing.available() == 0)
	// return pktsSent;
	// }
	// if (noDelay || flushQueue) {
	// int numBytes = (outgoing.available() < MSS) ? (int) outgoing.available() : MSS;
	// byte[] payloadArr = new byte[numBytes];
	// try {
	// outgoing.read(payloadArr, 0, payloadArr.length);
	// } catch (IOException e) {
	// throw new EONException("Failed to read from the pipestream", e);
	// }
	// ByteBuffer payload = ByteBuffer.wrap(payloadArr);
	// SEONPacket pkt = new SEONPacket(localEP, remoteEP, payload);
	// pkt.setSequenceNumber(sendNext);
	// sendNext = mod.add(sendNext, numBytes);
	// pkt.setACK(true);
	// pkt.setAckNumber(recvNext);
	// sendPacket(pkt, false);
	// pktsSent++;
	// } else {
	// // Nagle's algorithm
	// if (outgoing.available() >= MSS) {
	// byte[] payloadArr = new byte[MSS];
	// try {
	// outgoing.read(payloadArr, 0, payloadArr.length);
	// } catch (IOException e) {
	// throw new EONException("Failed to read from the pipestream", e);
	// }
	// ByteBuffer payload = ByteBuffer.wrap(payloadArr);
	// SEONPacket pkt = new SEONPacket(localEP, remoteEP, payload);
	// pkt.setSequenceNumber(sendNext);
	// sendNext = mod.add(sendNext, MSS);
	// pkt.setACK(true);
	// pkt.setAckNumber(recvNext);
	// sendPacket(pkt, false);
	// pktsSent++;
	// } else {
	// // Partial packet - only send if we have no
	// // unconfirmed data
	// if (retransQ.size() == 0) {
	// byte[] payloadArr = new byte[(int) outgoing.available()];
	// try {
	// outgoing.read(payloadArr, 0, payloadArr.length);
	// } catch (IOException e) {
	// throw new EONException("Failed to read from the pipestream", e);
	// }
	// ByteBuffer payload = ByteBuffer.wrap(payloadArr);
	// SEONPacket pkt = new SEONPacket(localEP, remoteEP, payload);
	// pkt.setSequenceNumber(sendNext);
	// sendNext = mod.add(sendNext, payloadArr.length);
	// pkt.setACK(true);
	// pkt.setAckNumber(recvNext);
	// sendPacket(pkt, false);
	// pktsSent++;
	// } else {
	// if (log.isDebugEnabled())
	// log.debug(this + " not sending " + outgoing.available()
	// + " bytes due to Nagle's Algorithm");
	// return pktsSent;
	// }
	// }
	// }
	// }
	// } finally {
	// if (pktsSent > 0)
	// startResponseTimer(responseTimeoutNormal);
	// }
	// return pktsSent;
	//
	// }

	protected synchronized void runHardRetransmit() {
		markInTransitPktsAsLost();
		needToRetransmitFirstPkt = false;
		if (log.isDebugEnabled())
			log.debug(this + " hard retransmit - marking " + lostQ.size() + " pkts as lost");
		resetRtt();
		needToHardRetransmit = false;
	}

	protected synchronized boolean haveDataToSend() {
		if (state == State.Closed || state == state.Listen)
			return false;
		// Don't send data in SynSent/SynReceived - but allow ourselves to
		// retransmit
		if (lostQ.size() == 0 && (state == State.SynSent || state == State.SynReceived))
			return false;
		// Don't send if we've been silected
		if (gamma == 0f)
			return false;
		// Are we waiting after a pkt loss during SS?
		if (needToHardRetransmit && now().before(hardRetransmitTime))
			return false;
		if (shouldSendFin || shouldSendFinAck)
			return true;
		if (needToRetransmitFirstPkt || lostQ.size() > 0 || outgoing.available() > 0)
			return true;
		// If we are asynchronously sending and we need more data, get
		// some
		if (dataProvider != null && outgoing.available() <= MSS) {
			fetchMoreData();
			// If we have no data in our queue after fetching more data,
			// there is no more to send
			return (outgoing.available() > 0);
		}
		return false;
	}

	/** Return true if we have more data to send (but have stopped due to limit), false if no more data */
	@Override
	synchronized boolean acceptVisitor(PktSendVisitor vis) throws EONException {
		waitingForVisitor = false;
		if (state == State.Closed)
			throw new EONException("Connection is closed");
		if (state == State.Listen) {
			// Can't happen
			throw new SeekInnerCalmException();
		}
		// Don't send data in SynSent/SynReceived - but allow ourselves to
		// retransmit
		if (lostQ.size() == 0 && (state == State.SynSent || state == State.SynReceived)) {
			if (log.isDebugEnabled())
				log.debug(this + " not sending data, state is " + state);
			return false;
		}
		if (gamma == 0f)
			return false;

		// Are we waiting after a pkt loss during SS?
		if (needToHardRetransmit && now().before(hardRetransmitTime))
			return false;

		int pktsSent = 0;
		try {
			while (needToRetransmitFirstPkt || lostQ.size() > 0 || outgoing.available() > 0 || dataProvider != null) {
				if (retransQ.size() >= sendWindow) {
					if (log.isDebugEnabled()) {
						StringBuffer sb = new StringBuffer(toString());
						sb.append(" not sending data now due to window size (win=");
						sb.append(sendWindow).append(",pkts=").append(retransQ.size());
						sb.append(")");
						log.debug(sb);
					}
					return false;
				}
				if (pktsSent >= MAX_BURST_PKTS) {
					if (log.isDebugEnabled())
						log.debug(this + " not sending data now due to burst limit");
					return false;
				}
				// If we have any lost packets, send them before we send new
				// data
				if (lostQ.size() > 0) {
					if (lostQ.getFirst().getPayloadSize() > vis.bytesAvailable())
						return true;
					SEONPacket pkt = lostQ.removeFirst();
					addToRetransQ(pkt);
					outFlowRate.notifyData(pkt.getPayloadSize());
					vis.sendPkt(pkt);
					pktsSent++;
					continue;
				}
				// If we need to resend our pkt, do it now
				if (needToRetransmitFirstPkt) {
					SEONPacket pkt = (SEONPacket) retransQ.peek();
					if (pkt.getPayloadSize() > vis.bytesAvailable())
						return true;
					// Don't add this pkt to the retrans q
					outFlowRate.notifyData(pkt.getPayloadSize());
					vis.sendPkt(pkt);
					pktsSent++;
					needToRetransmitFirstPkt = false;
					continue;
				}
				// If we need to close the conn, do it now
				if (shouldSendFinAck) {
					SEONPacket finAckPacket = new SEONPacket(localEP, remoteEP, null);
					finAckPacket.setSequenceNumber(sendNext);
					sendNext = mod.add(sendNext, 1);
					finAckPacket.setAckNumber(recvNext);
					finAckPacket.setFIN(true);
					finAckPacket.setACK(true);
					addToRetransQ(finAckPacket);
					startResponseTimer(responseTimeoutLow);
					vis.sendPkt(finAckPacket);
					pktsSent++;
					setState(State.LastAck);
					shouldSendFinAck = false;
					return false;
				}
				if (shouldSendFin) {
					SEONPacket finPacket = new SEONPacket(localEP, remoteEP, null);
					finPacket.setSequenceNumber(sendNext);
					finPacket.setAckNumber(recvNext);
					finPacket.setFIN(true);
					finPacket.setACK(true);
					sendNext = mod.add(sendNext, 1);
					addToRetransQ(finPacket);
					startResponseTimer(responseTimeoutLow);
					vis.sendPkt(finPacket);
					pktsSent++;
					setState(State.FinWait);
					shouldSendFin = false;
					return false;
				}
				// Don't send any more data if we're waiting to close
				if (state == State.FinWait || state == State.LastAck)
					return false;
				// If we are asynchronously sending and we need more data, get
				// some
				if (dataProvider != null && outgoing.available() <= MSS)
					fetchMoreData();
				if (outgoing.available() == 0)
					return false;
				// Send fresh data
				if (noDelay) {
					int numBytes = min(outgoing.available(), MSS);
					if (numBytes > vis.bytesAvailable())
						return true;
					byte[] payloadArr = new byte[numBytes];
					try {
						outgoing.read(payloadArr, 0, payloadArr.length);
					} catch (IOException e) {
						throw new EONException("Failed to read from the pipestream", e);
					}
					ByteBuffer payload = ByteBuffer.wrap(payloadArr);
					SEONPacket pkt = new SEONPacket(localEP, remoteEP, payload);
					pkt.setSequenceNumber(sendNext);
					sendNext = mod.add(sendNext, numBytes);
					pkt.setACK(true);
					pkt.setAckNumber(recvNext);
					addToRetransQ(pkt);
					outFlowRate.notifyData(pkt.getPayloadSize());
					vis.sendPkt(pkt);
					pktsSent++;
				} else {
					// Nagle's algorithm
					if (outgoing.available() >= MSS) {
						if (MSS > vis.bytesAvailable())
							return true;
						byte[] payloadArr = new byte[MSS];
						try {
							outgoing.read(payloadArr, 0, payloadArr.length);
						} catch (IOException e) {
							throw new EONException("Failed to read from the pipestream", e);
						}
						ByteBuffer payload = ByteBuffer.wrap(payloadArr);
						SEONPacket pkt = new SEONPacket(localEP, remoteEP, payload);
						pkt.setSequenceNumber(sendNext);
						sendNext = mod.add(sendNext, MSS);
						pkt.setACK(true);
						pkt.setAckNumber(recvNext);
						addToRetransQ(pkt);
						outFlowRate.notifyData(pkt.getPayloadSize());
						vis.sendPkt(pkt);
						pktsSent++;
					} else {
						// Partial packet - only send if we have no
						// unconfirmed data
						if (retransQ.size() == 0) {
							int bytesToSend = (int) outgoing.available();
							if (bytesToSend > vis.bytesAvailable())
								return true;
							byte[] payloadArr = new byte[bytesToSend];
							try {
								outgoing.read(payloadArr, 0, payloadArr.length);
							} catch (IOException e) {
								throw new EONException("Failed to read from the pipestream", e);
							}
							ByteBuffer payload = ByteBuffer.wrap(payloadArr);
							SEONPacket pkt = new SEONPacket(localEP, remoteEP, payload);
							pkt.setSequenceNumber(sendNext);
							sendNext = mod.add(sendNext, payloadArr.length);
							pkt.setACK(true);
							pkt.setAckNumber(recvNext);
							outFlowRate.notifyData(pkt.getPayloadSize());
							vis.sendPkt(pkt);
							pktsSent++;
						} else {
							if (log.isDebugEnabled())
								log.debug(this + " not sending " + outgoing.available()
										+ " bytes due to Nagle's Algorithm");
							return false;
						}
					}
				}
			}
			return false;
		} finally {
			if (pktsSent > 0)
				startResponseTimer(responseTimeoutNormal);
		}
	}

	/**
	 * @param seqNumOfCausingPkt
	 *            The sequence number of the out-of-order packet that we've just received, causing us to send this dup
	 *            ack
	 */
	void sendDupAck(long seqNumOfCausingPkt) throws EONException {
		SEONPacket pkt = new SEONPacket(localEP, remoteEP, null);
		pkt.setSequenceNumber(sendNext);
		pkt.setACK(true);
		pkt.setAckNumber(recvNext);
		setSackBlocksOnDupAck(seqNumOfCausingPkt, pkt);
		sendPktImmediate(pkt, 0, 0);
	}

	/**
	 * The next seqnum to be expected after this pkt
	 */
	private long seqNumAfterPkt(SEONPacket pkt) {
		long result = pkt.getSequenceNumber();
		// If this is a bare FIN, the next seqnum will be seqnum+1 (which will
		// never appear)
		if (pkt.getPayload() != null && pkt.getPayload().limit() > 0)
			result = mod.add(result, pkt.getPayload().limit());
		else if (pkt.isFIN())
			result = mod.add(result, 1);
		return result;
	}

	/**
	 * @param seqNumOfCausingPkt
	 *            The sequence number of the out-of-order packet that we've just received, causing us to send this dup
	 *            ack
	 * @param dupAckPkt
	 *            The dupack that we're sending
	 */
	private void setSackBlocksOnDupAck(long seqNumOfCausingPkt, SEONPacket dupAckPkt) {
		// NOTE: RFC 2018 states that the first SACK block should be the one
		// that
		// includes the 'data' we've just received. However, if the pkt is a FIN
		// with no data, it is not clear what should be done, as the FIN
		// requires ACKing but contains no data. I am assuming that in this
		// case, the end of the sack block should be the seqnum of the FIN + 1,
		// to ACK the FIN

		// Work out what blocks of out-of-order arrived data we have
		// TODO Using a list like this means that all the longs will be
		// autoboxed into Longs... if this turns out to be a big performance hit
		// we'll need to replace these arraylists with a custom class that uses
		// primitive longs
		List<Long> begins = new ArrayList<Long>();
		List<Long> ends = new ArrayList<Long>();
		long blockStart = -1;
		long blockEnd = -1;
		for (SEONPacket recvdPkt : recvdPkts) {
			// Ignore packets with no payload unless they contain a FIN
			if (!recvdPkt.isFIN() && (recvdPkt.getPayload() == null || recvdPkt.getPayload().limit() == 0))
				continue;
			long firstSeqNum = recvdPkt.getSequenceNumber();
			// The sack block end marker represents the next byte after the last
			// one we have
			if (blockStart < 0) {
				// First block
				blockStart = firstSeqNum;
				blockEnd = seqNumAfterPkt(recvdPkt);
			} else if (firstSeqNum == blockEnd) {
				// Continuing current block
				blockEnd = seqNumAfterPkt(recvdPkt);
			} else {
				// New block
				begins.add(blockStart);
				ends.add(blockEnd);
				blockStart = firstSeqNum;
				blockEnd = seqNumAfterPkt(recvdPkt);
			}
		}
		// One at the end
		if (blockStart >= 0) {
			begins.add(blockStart);
			ends.add(blockEnd);
		}
		if (begins.size() == 0)
			return;

		// The sack block containing the pkt that caused this dupack must be
		// first (RFC 2018)
		int firstBlockIdx = -1;
		for (int i = 0; i < begins.size(); i++) {
			if (mod.lte(begins.get(i), seqNumOfCausingPkt) && mod.gt(ends.get(i), seqNumOfCausingPkt)) {
				firstBlockIdx = i;
				break;
			}
		}

		if (firstBlockIdx < 0) {
			// Can't happen (but does) - bug huntin
			StringBuffer sb = new StringBuffer("SackError: pkts: [");
			for (SEONPacket pkt : recvdPkts) {
				sb.append(pkt).append(" ");
			}
			sb.append("], sack blocks: [");
			for (int i = 0; i < begins.size(); i++) {
				sb.append(begins.get(i)).append("-").append(ends.get(i)).append(" ");
			}
			sb.append("], causing pkt: ").append(seqNumOfCausingPkt);
			log.error(sb);
			return;
		}

		if (firstBlockIdx != 0) {
			// Swap the blocks to make the right one first
			long flarp = begins.get(firstBlockIdx);
			begins.set(firstBlockIdx, begins.get(0));
			begins.set(0, flarp);
			flarp = ends.get(firstBlockIdx);
			ends.set(firstBlockIdx, ends.get(0));
			ends.set(0, flarp);
		}
		dupAckPkt.setSackBlocks(begins, ends);
	}

	synchronized void addToRetransQ(SEONPacket pkt) {
		retransQ.addLast(pkt);
		transmissionTimes.put(new Long(pkt.getSequenceNumber()), new Long(new Date().getTime()));
		restartRetransmissionTimer(rto);
	}

	synchronized void sendPktImmediate(SEONPacket pkt, int retransTimeout, int responseTimeout) {
		if (pkt.getPayloadSize() > 0)
			throw new SeekInnerCalmException();
		if (retransTimeout > 0 && (pkt.isSYN() || pkt.isFIN())) {
			addToRetransQ(pkt);
			if (responseTimeout > 0)
				startResponseTimer(responseTimeout);
		}
		mgr.sendPktImmediate(pkt);
	}

	synchronized void setState(State state) {
		if (!this.state.equals(state)) {
			if (log.isDebugEnabled())
				log.debug(this + " state change: " + this.state.toString() + " => " + state.toString());
			this.state = state;
			notifyAll();
		}
	}

	/**
	 * We use a lower value for timeout for the first SYN, as the host might not be there or might not be listening
	 * (we're using UDP so we won't get RSTs)
	 */
	synchronized void startResponseTimer(int timeoutMs) {
		if (!responseTimeout.isTaskIsScheduled()) {
			if (log.isDebugEnabled())
				log.debug(this + " - Starting response timer - " + timeoutMs + "ms");
			responseTimeout.set(timeoutMs);
		}
	}

	synchronized void stopResponseTimer() {
		if (log.isDebugEnabled())
			log.debug(this + " - Stopping response timer");
		responseTimeout.clear();
	}

	/**
	 * @param recvdPkt
	 *            The just-received packet
	 * @return The number of bytes trimmed
	 */
	int trimRetransmissionQueues(SEONPacket recvdPkt) throws EONException {
		int bytesAcked = trimQueue(lostQ, recvdPkt);
		bytesAcked += trimQueue(retransQ, recvdPkt);
		if (lostQ.size() == 0 && retransQ.size() == 0) {
			if (log.isDebugEnabled())
				log.debug(toString() + ": all data acknowledged - stopping retransmission timer");
			stopRetransmissionTimer();
		}
		return bytesAcked;
	}

	int trimQueue(LinkedList<SEONPacket> q, SEONPacket recvdPkt) {
		if (q.size() == 0)
			return 0;

		int bytesAcked = 0;
		long[] sackBegins = recvdPkt.getSackBegins();
		long[] sackEnds = recvdPkt.getSackEnds();

		// Take note of the highest num we're acknowledging, so we don't have to
		// process the entire list every time
		long maxSackEnd = -1;
		if (sackBegins != null) {
			for (int i = 0; i < sackEnds.length; i++) {
				if (maxSackEnd == -1 || mod.gt(sackEnds[i], maxSackEnd))
					maxSackEnd = sackEnds[i];
			}
		}

		// Iterate over our queue, and remove it if it has been acknowledged,
		// either through the normal ACK number or else through SACK blocks
		for (Iterator<SEONPacket> iter = q.iterator(); iter.hasNext();) {
			SEONPacket pkt = iter.next();
			long firstSeqNum = pkt.getSequenceNumber();
			long lastSeqNum;
			if (pkt.getPayload() != null && pkt.getPayload().limit() > 1)
				lastSeqNum = mod.add(firstSeqNum, pkt.getPayload().limit() - 1);
			else
				lastSeqNum = firstSeqNum;
			boolean pktIsAcked = false;
			if (lastSeqNum < sendUna) {
				// This pkt has been acknowledged through the normal ack number
				pktIsAcked = true;
			} else {
				// Check to see if this pkt has been acknowledged through sack
				// blocks
				if (sackBegins != null) {
					for (int i = 0; i < sackBegins.length; i++) {
						if (mod.gte(firstSeqNum, sackBegins[i]) && mod.lt(lastSeqNum, sackEnds[i])) {
							pktIsAcked = true;
							break;
						}
					}
				}
			}
			if (pktIsAcked) {
				if (log.isDebugEnabled())
					log.debug(this + ": pkt " + pkt + " has been acknowledged");
				if (pkt.getPayload() != null)
					bytesAcked += pkt.getPayload().limit();
				iter.remove();
				Long thisSN = new Long(firstSeqNum);
				if (transmissionTimes.containsKey(thisSN)) {
					int rtt = (int) (TimeUtil.now().getTime() - ((Long) transmissionTimes.get(thisSN)).longValue());
					updateRTO(rtt);
					transmissionTimes.remove(thisSN);
				}
			} else {
				// Check to see if we can stop processing the list
				if (maxSackEnd < 0 || mod.gt(pkt.getSequenceNumber(), maxSackEnd))
					break;
			}
		}
		return bytesAcked;
	}

	void resetRtt() {
		srtt = 0;
		rttvar = 0;
	}

	void updateRTO(int rtt) {
		// If we're on localhost rtt might be 0, just make it 1 here so we don't have to add checks everywhere...
		if(rtt == 0)
			rtt = 1;
		if (rtt < minObservedRtt)
			minObservedRtt = rtt;
		if (rtt > maxObservedRtt)
			maxObservedRtt = rtt;

		// Estimate our maxObservedRtt - if we are a non-1.0-gamma connection,
		// we might never get an accurate value of this after the initial SS, as
		// we may never lose a pkt
		// TODO: Need to look at this algorithm, seems a bit rubbish
		if (rtt > provMaxObservedRtt)
			provMaxObservedRtt = rtt;
		if (TimeUtil.msElapsedSince(lastChangedMaxObsRtt) > MAX_OBSERVED_RTT_MONITOR_PERIOD) {
			if (log.isDebugEnabled())
				log.debug(this + " checking provisional maxRtt, prov=" + provMaxObservedRtt + ", current max="
						+ maxObservedRtt);
			if (provMaxObservedRtt < maxObservedRtt) {
				// Never reduce our maxObservedRtt below the lowest value
				// obtained accurately (through pkt loss) - this prevents our
				// value from shrinking continuously
				int lowestMaxObservedRtt = mgr.getLowestMaxObservedRtt(this);
				if (lowestMaxObservedRtt > 0) {
					if (provMaxObservedRtt >= lowestMaxObservedRtt)
						maxObservedRtt = provMaxObservedRtt;
					else
						maxObservedRtt = lowestMaxObservedRtt;
					if (log.isDebugEnabled())
						log.debug(this + " lowering maxObservedRtt via provisional mechanism to " + maxObservedRtt
								+ "ms");
				}
			}
			provMaxObservedRtt = -1;
			lastChangedMaxObsRtt = TimeUtil.now();
		}
		// See RFC 2988
		synchronized (this) {
			if (srtt == 0 && rttvar == 0) {
				// Initialising RTO
				srtt = rtt;
				rttvar = rtt / 2;
				rto = srtt + (4 * rttvar);
			} else {
				rttvar = (int) ((0.75 * rttvar) + (0.25 * Math.abs(srtt - rtt)));
				srtt = (int) ((0.875 * srtt) + (0.125 * rtt));
				rto = srtt + (4 * rttvar);
			}
			if (rto < MIN_RTO)
				rto = MIN_RTO;
			if (rto > MAX_RTO)
				rto = MAX_RTO;
		}
		if (log.isDebugEnabled())
			log.debug(this + ": Updating RTO with rtt " + rtt + " - srtt=" + srtt + ", rto=" + rto);
	}

	long windowInBytes() {
		return sendWindow * MSS;
	}

	public int getSsThresh() {
		return ssThresh;
	}

	public boolean isConnected() {
		return state == State.Established;
	}

	public boolean isCongested() {
		if (gamma == 1.0f)
			return false;
		if (TimeUtil.now().before(ignoreCongestionUntil)) {
			if (log.isDebugEnabled())
				log.debug(this + " not congested - ignoring congestion for another "
						+ TimeUtil.msUntil(ignoreCongestionUntil) + "ms");
			return false;
		}
		// See tcp-lp
		int congThreshRtt = (int) (minObservedRtt + (gamma * (maxObservedRtt - minObservedRtt)));
		boolean congested = (srtt > congThreshRtt);
		if (log.isDebugEnabled())
			log.debug(this + " checking congestion (srtt=" + srtt + ",minRtt=" + minObservedRtt + ",maxRtt="
					+ maxObservedRtt + "): " + (congested ? "yes" : "no"));
		return congested;
	}

	public int getMaxObservedRtt() {
		return maxObservedRtt;
	}

	/**
	 * Congestion threshold for this connection, 0<gamma<=1 If gamma < 1 && srtt > (minRtt + gamma(maxRtt - minRtt)),
	 * then we have congestion and we cut our window. See TCP-LP.
	 */
	public float getGamma() {
		return gamma;
	}

	public void setGamma(float gamma) {
		if (gamma < 0f || gamma > 1f)
			throw new IllegalArgumentException("Illegal val " + gamma);
		// If our gamma was 0 and now it isn't, check to see if we can send data
		float oldG = this.gamma;
		this.gamma = gamma;
		if (oldG == 0f && gamma != 0f)
			sendDataIfNecessary();
	}

	private final class AsyncReceiver extends CatchingRunnable {
		public void doRun() throws Exception {
			while (true) {
				// Grab a copy of our datareceiver in case it's set to null
				PushDataReceiver dataRec = null;
				ByteBuffer buf = null;
				receiveLock.lock();
				try {
					dataRec = dataReceiver;
					if (dataRec == null || incomingDataBufs.size() == 0) {
						dataReceiverRunning = false;
						return;
					}
					buf = incomingDataBufs.removeFirst();
				} finally {
					receiveLock.unlock();
				}
				try {
					dataRec.receiveData(buf, null);
				} catch (IOException e) {
					close();
				} catch (Exception e) {
					log.error(SEONConnection.this + " caught " + e.getClass().getSimpleName(), e);
				}
			}
		}
	}
}
