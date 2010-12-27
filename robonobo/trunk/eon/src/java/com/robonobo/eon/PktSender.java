package com.robonobo.eon;

import static java.lang.System.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.robonobo.common.concurrent.CatchingRunnable;
import com.robonobo.common.exceptions.SeekInnerCalmException;

public class PktSender extends CatchingRunnable {
	/** millisecs */
	private static final long WAIT_TIME = 100;
	/** millisecs - the max number of ms data credit we can have */
	private static final int MAX_CREDIT_TIME = 1000;
	Log log = LogFactory.getLog(getClass());
	private DatagramChannel chan;
	ByteBuffer sendBuf = ByteBuffer.allocate(EONManager.MAX_PKT_SIZE);
	private int maxBps = -1;
	/** This represents the number of bytes we can send now */
	private int bytesCredit;
	/** The last time we calculated how much credit we have */
	private long lastCreditTime;
	/** If we are out of credit, the next time we can send */
	private long nextSendTime = 0;
	private boolean stopping = false;
	private Thread t;
	private ArrayList<EONPacket> unThrottledPkts = new ArrayList<EONPacket>();
	private LinkedList<EONConnection> readyConns = new LinkedList<EONConnection>();
	private ReentrantLock lock;
	private Condition canSend;
	private Visitor vis = new Visitor();

	PktSender(DatagramChannel chan) {
		this.chan = chan;
		lock = new ReentrantLock();
		canSend = lock.newCondition();
	}

	void start() {
		t = new Thread(this);
		t.setName("EON-Send");
		t.start();
	}

	void stop() {
		stopping = true;
		if (t != null)
			t.interrupt();
	}

	void sendPktImmediate(EONPacket pkt) {
		lock.lock();
		try {
			unThrottledPkts.add(pkt);
			canSend.signal();
		} finally {
			lock.unlock();
		}
	}

	void haveDataToSend(EONConnection conn) {
		lock.lock();
		try {
			insertReadyConn(conn);
			// If we're not waiting to send due to our credit limit, signal the thread
			if (currentTimeMillis() >= nextSendTime)
				canSend.signal();
		} finally {
			lock.unlock();
		}
	}

	private void insertReadyConn(EONConnection conn) {
		// Annoying that one can't 'reset' an iterator, which means we're doing this object creation every time
		ListIterator<EONConnection> it = readyConns.listIterator();
		// Start at the beginning of the list and go up until we find a conn with a gamma >= to this one, then
		// insert it before (so that first-notifying conns get to send pkts first)
		while (it.hasNext()) {
			EONConnection testConn = it.next();
			if (testConn.getGamma() >= conn.getGamma()) {
				// Rewind so we insert before this one
				it.previous();
				break;
			}
		}
		it.add(conn);
	}

	@Override
	public void doRun() throws Exception {
		while (true) {
			if (stopping)
				return;
			long nowTime = currentTimeMillis();
			lock.lock();
			try {
				// If we've run out of credit, wait until the specified time, otherwise wait until signalled
				if (unThrottledPkts.size() == 0 && nextSendTime > nowTime) {
					if (log.isDebugEnabled())
						log.debug("Pausing pktSend: throttling - " + readyConns.size() + " ready conns");
					canSend.await((nextSendTime - nowTime), TimeUnit.MILLISECONDS);
				}
				while (unThrottledPkts.size() == 0 && readyConns.size() == 0) {
					if (log.isDebugEnabled())
						log.debug("Pausing pktSend: no more data");
					canSend.await();
				}
				nowTime = currentTimeMillis();
				if (maxBps >= 0) {
					bytesCredit += (nowTime - lastCreditTime) * (float) (maxBps / 1000);
					if (bytesCredit > maxBytesCredit())
						bytesCredit = maxBytesCredit();
				}
				lastCreditTime = nowTime;
				if (log.isDebugEnabled())
					log.debug("pktSend running, send credit " + bytesCredit + "B");
				// Any unthrottled pkts we have, send them now
				for (EONPacket pkt : unThrottledPkts) {
					if (log.isDebugEnabled())
						log.debug("Sending unthrottled pkt: " + pkt);
					sendPkt(pkt);
				}
				unThrottledPkts.clear();
			} finally {
				lock.unlock();
			}
			// Come out of the lock before we call conn.acceptVisitor() to remove deadlock possibilities
			// Send data from our ready conns until we're out of credit
			while (readyConns.size() > 0) {
				if (maxBps >= 0)
					vis.reset(bytesCredit);
				else
					vis.reset(Integer.MAX_VALUE);
				lock.lock();
				EONConnection conn;
				try {
					conn = readyConns.removeLast();
				} finally {
					lock.unlock();
				}
				// Collect our pkts in the visitor, then send them here so that we are out of the conn's lock - TODO probably not needed now that acceptVisitor is outside PktSender lock
				boolean haveMore = conn.acceptVisitor(vis);
				for (EONPacket pkt : vis.pkts) {
					if (log.isDebugEnabled())
						log.debug("Sending throttled pkt: " + pkt);
					if (maxBps >= 0)
						bytesCredit -= pkt.getPayloadSize();
					sendPkt(pkt);
				}
				if (haveMore) {
					lock.lock();
					try {
						// We're out of credit
						// Re-insert this guy behind any other waiting conns with the same gamma, otherwise one guy with
						// a 1MB send holds everyone up
						insertReadyConn(conn);
						// Wait to allow our credit to accumulate
						nextSendTime = nowTime + WAIT_TIME;
					} finally {
						lock.unlock();
					}
					break;
				}
			}
		}
	}

	private void sendPkt(EONPacket pkt) throws IOException {
		sendBuf.clear();
		pkt.toByteBuffer(sendBuf);
		sendBuf.flip();
		chan.send(sendBuf, pkt.getDestSocketAddress().getInetSocketAddress());
		if (log.isDebugEnabled())
			log.debug("s " + pkt.toString());
	}

	private int maxBytesCredit() {
		return MAX_CREDIT_TIME * (maxBps / 1000);
	}

	/**
	 * Pass maxBps < 0 to indicate no limit
	 */
	public void setMaxBps(int maxBps) {
		lock.lock();
		try {
			this.maxBps = maxBps;
			bytesCredit = (int) (maxBps * (float) (MAX_CREDIT_TIME / 1000));
			lastCreditTime = currentTimeMillis();
		} finally {
			lock.unlock();
		}
	}

	public int getMaxBps() {
		return maxBps;
	}

	class Visitor implements PktSendVisitor {
		int bytesAvailable;
		ArrayList<EONPacket> pkts = new ArrayList<EONPacket>();

		void reset(int bytesAvailable) {
			this.bytesAvailable = bytesAvailable;
			pkts.clear();
		}

		@Override
		public int bytesAvailable() {
			return bytesAvailable;
		}

		@Override
		public void sendPkt(EONPacket pkt) {
			if (bytesAvailable < pkt.getPayloadSize())
				throw new SeekInnerCalmException();
			pkts.add(pkt);
			if (log.isDebugEnabled())
				log.debug("PktSender accepting pkt for send: " + pkt);
			if (bytesAvailable != Integer.MAX_VALUE)
				bytesAvailable -= pkt.getPayloadSize();
		}

	}
}
