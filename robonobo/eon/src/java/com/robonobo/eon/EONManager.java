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

import java.io.IOException;
import java.net.ConnectException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.PortUnreachableException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.robonobo.common.concurrent.StartStopable;
import com.robonobo.common.util.Modulo;

public class EONManager implements StartStopable {
	private static final int SOCKET_BUFFER_SIZE = 64 * 1024;
	public static final int MAX_PKT_SIZE = 8192;
	Log log;
	String instanceName;
	InetSocketAddress localEP;
	DatagramChannel chan;
	boolean sockOK;
	ReceiverThread recvThread;
	ConnectionHolder conns;
	ScheduledThreadPoolExecutor executor;
	Modulo mod;
	Date uploadCheckLastBase = new Date();
	int bytesUploadedSinceBase = 0;
	@SuppressWarnings("unchecked")
	List backlogPkts;
	int backlogPktBytes = 0;
	ByteBuffer sendBuf = ByteBuffer.allocate(MAX_PKT_SIZE);
	SenderThread sndThread;
	ReentrantLock sendLock = new ReentrantLock();
	List<EONPacket> pktsToSend = new LinkedList<EONPacket>();

	public static InetAddress getWildcardAddress() {
		try {
			return Inet4Address.getByName("0.0.0.0");
		} catch (UnknownHostException e) {
			// Can't happen
			throw new RuntimeException(e);
		}
	}

	public EONManager(String instanceName, ScheduledThreadPoolExecutor executor, int port) throws EONException {
		this(instanceName, executor, new InetSocketAddress(port));
	}

	public EONManager(String instanceName, ScheduledThreadPoolExecutor executor) throws EONException {
		this(instanceName, executor, null);
	}

	public EONManager(String instanceName, ScheduledThreadPoolExecutor executor, InetSocketAddress localEP)
			throws EONException {
		this.instanceName = instanceName;
		log = getLogger(getClass());
		this.executor = executor;
		try {
			chan = DatagramChannel.open();
			chan.socket().setReceiveBufferSize(SOCKET_BUFFER_SIZE);
			chan.socket().setSendBufferSize(SOCKET_BUFFER_SIZE);
			chan.socket().bind(localEP);
			// Re-grab the value to see what we ended up on
			this.localEP = (InetSocketAddress) chan.socket().getLocalSocketAddress();
		} catch (IOException e) {
			throw new EONException("Unable to construct udp socket on " + localEP.toString(), e);
		}
		sockOK = true;
		recvThread = null;
		conns = new ConnectionHolder(this);
		mod = new Modulo((long) Integer.MAX_VALUE + 1);
		log.debug("EONManager created on endpoint " + this.localEP.getAddress().getHostAddress()+":"+this.localEP.getPort());
	}

	public boolean isRunning() {
		return sockOK;
	}

	public void start() throws EONException {
		recvThread = new ReceiverThread();
		sndThread = new SenderThread();
		recvThread.start();
		sndThread.start();
	}

	public void stop() {
		log.debug("Stopping EONManager");
		conns.closeAllConns(30000);
		sndThread.terminate();
		recvThread.terminate();
		try {
			chan.close();
		} catch (Exception ignore) {
		}
		sockOK = false;
	}

	public Log getLogger(Class callerClass) {
		return LogFactory.getLog(callerClass.getName() + ".INSTANCE." + instanceName);
	}

	public DEONConnection createDEONConnection() {
		return new DEONConnection(this);
	}

	public SEONConnection createSEONConnection() throws EONException {
		return new SEONConnection(this);
	}

	public void sendNATSeed(InetSocketAddress remoteEP) throws EONException {
		DEONPacket seed = new DEONPacket(new EonSocketAddress(localEP, 0), new EonSocketAddress(remoteEP, 0), null);
		sendPacket(seed);
	}

	public ScheduledThreadPoolExecutor getExecutor() {
		return executor;
	}

	public boolean requestPort(int localEONPort, EONConnection thisConn) throws EONException, IllegalArgumentException {
		return requestPort(localEONPort, new EonSocketAddress(getWildcardAddress(), 1, 1), thisConn);
	}

	public boolean requestPort(int localEONPort, EonSocketAddress addressMask, EONConnection thisConn)
			throws EONException {
		if (!sockOK)
			throw new EONException("No socket available");
		return conns.requestPort(localEONPort, addressMask, thisConn);
	}

	public int getPort(EONConnection thisConn) throws IllegalArgumentException, EONException {
		return getPort(new EonSocketAddress(getWildcardAddress(), 1, 1), thisConn);
	}

	public int getPort(EonSocketAddress addressMask, EONConnection thisConn)
			throws IllegalArgumentException, EONException {
		if (!sockOK)
			throw new EONException("No socket available");
		return conns.getPort(addressMask, thisConn);
	}

	public void returnPort(int localEONPort, EONConnection thisConn) throws EONException, IllegalArgumentException {
		returnPort(localEONPort, new EonSocketAddress(getWildcardAddress(), 1, 1), thisConn);
	}

	public void returnPort(int localEONPort, EonSocketAddress addressMask, EONConnection thisConn)
			throws EONException, IllegalArgumentException {
		conns.returnPort(localEONPort, addressMask, thisConn);
	}

	private void sendBackloggedPackets() throws EONException {
		while (backlogPkts.size() > 0) {
			EONPacket pkt = (EONPacket) backlogPkts.get(0);
			sendPacket(pkt);
			if (backlogPkts.size() == 0) // This method might have been called
				// again through recursion
				return;

			// Taken out this simulated conditions thing
			// if(numBytes > 0) {
			// backlogPkts.remove(0);
			// backlogPktBytes -= numBytes;
			// } else
			// // Whoops, we've breached our upload restriction
			// return;
		}
	}

	public void sendPacket(EONPacket pkt) throws EONException {
		synchronized (sendLock) {
			pktsToSend.add(pkt);
			sendLock.notify();
		}
	}

	private void reallySendPacket(EONPacket pkt) throws IOException {
		if (!sockOK)
			throw new IOException("SendPacket() called, but we have no socket");
		sendBuf.clear();
		pkt.toByteBuffer(sendBuf);
		sendBuf.flip();
		chan.send(sendBuf, pkt.getDestSocketAddress().getInetSocketAddress());
		if (log.isDebugEnabled())
			log.debug("s " + pkt.toString());
	}

	public int getLowestMaxObservedRtt(SEONConnection exceptConn) {
		return conns.getLowestMaxObservedRtt(exceptConn);
	}

	// Note: If we have allowed the system to choose our IP address and/or
	// port, this will
	// not be set until Start() is called
	// FIXME: (Ray) changed this to return a InetSocketAddress from localEP,
	// otherwise we end up with 0.0.0.0
	public InetSocketAddress getLocalSocketAddress() throws EONException {
		if (sockOK)
			return localEP;
		// return (InetSocketAddress) udpSock.getLocalSocketAddress();
		else
			throw new EONException("Underlying socket not ready");
	}

	// Send everything using a single thread, some platforms get crappy
	// performance when too many threads write to a socket
	private class SenderThread extends Thread {
		private boolean terminated = false;

		public SenderThread() {
			setName("EONMgr-SenderThread");
		}
		
		@Override
		public void run() {
			try {
				while (true) {
					EONPacket pkt;
					try {
						synchronized (sendLock) {
							while (pktsToSend.size() == 0) {
								if (terminated)
									return;
								sendLock.wait();
							}
							pkt = pktsToSend.remove(0);
						}
						reallySendPacket(pkt);
					} catch (InterruptedException e) {
						log.debug("EONMgr sender thread caught InterruptedException");
					}
				}
			} catch (Exception e) {
				if (!terminated)
					log.error("EonMgr sender thread caught exception", e);
			}
		}

		public void terminate() {
			terminated = true;
			interrupt();
		}
	}

	private class ReceiverThread extends Thread {
		boolean terminated = false;

		public ReceiverThread() {
			super();
			setName("EonManagerThread");
		}

		public void run() {
			EONPacket thisPacket;
			log.debug("EONManagerThread running");
			// Loop, picking up packets and passing them off to connections as
			// necessary
			while (true) {
				InetSocketAddress remoteEP = null;
				try {
					// Allocate a new buffer every time, as it means we don't
					// have to copy it when handling gets passed to another
					// thread, and it's cheaper to keep allocating same-sized
					// buffers
					ByteBuffer recvBuf = ByteBuffer.allocate(MAX_PKT_SIZE);
					// Pick up next packet (will block here if no packets)
					remoteEP = (InetSocketAddress) chan.receive(recvBuf);
					recvBuf.flip();
					thisPacket = EONPacket.parse(recvBuf);
					if (thisPacket == null) {
						// Duff packet. Discard and move on.
						log.error("Error parsing packet from " + remoteEP.getAddress().toString() + ":"
								+ remoteEP.getPort());
						continue;
					}
					// Set the IP data on the packet
					thisPacket.getSourceSocketAddress().setAddress(remoteEP.getAddress());
					thisPacket.getSourceSocketAddress().setUdpPort(remoteEP.getPort());
					thisPacket.getDestSocketAddress().setAddress(((InetSocketAddress) getLocalSocketAddress()).getAddress());
					thisPacket.getDestSocketAddress().setUdpPort(((InetSocketAddress) getLocalSocketAddress()).getPort());
					if (log.isDebugEnabled()) {
						String logStr = "r " + thisPacket.toString();
						log.debug(logStr);
					}
					handlePacket(thisPacket);
				} catch (EONException e) {
					if (terminated)
						return;
					log.error("Caught EONException in Eon Mgr thread", e);
					continue;
				} catch (ConnectException e) {
					// We're exiting
					return;
				} catch (PortUnreachableException e) {
					conns.killAllConnsAssociatedWithAddress(remoteEP);
					continue;
				} catch (SocketException e) {
					if (!terminated)
						log.error("Caught SocketException when waiting for packets", e);
					return;
				} catch (IOException e) {
					if (!terminated)
						log.error("IOException when attempting to receive UDP Packet", e);
					return;
				} catch (Exception e) {
					log.error("EONMgr caught " + e.getClass().getName(), e);
					continue;
				}
			}
		}

		public void terminate() {
			// set the terminated flag
			terminated = true;

			// close socket, forces receive() to stop blocking
			try {
				chan.close();
			} catch (IOException ignore) {
			}

			// interrupt the thread
			interrupt();
		}

		public void handlePacket(EONPacket thisPacket) throws EONException {
			// If this is sent to EON port 0, it is a NATseed - discard
			if (thisPacket.getDestSocketAddress().getEonPort() == 0)
				return;
			EONConnection thisConn = conns.getLocalConnForIncoming(thisPacket.getDestSocketAddress(), thisPacket.getSourceSocketAddress());
			if (thisConn != null) {
				// Check it's the right protocol type
				if (thisPacket.getProtocol() == EONPacket.EON_PROTOCOL_SEON) {
					SEONPacket sPkt = (SEONPacket) thisPacket;
					if (thisConn instanceof SEONConnection) {
						((SEONConnection) thisConn).receivePacket(sPkt);
						return;
					} else {
						// Send a RST (unless the packet contains a RST, in
						// which case do nothing)
						if (!sPkt.isRST()) {
							SEONPacket rstPacket = new SEONPacket(sPkt.getDestSocketAddress(), sPkt.getSourceSocketAddress(), null);
							rstPacket.setRST(true);
							if (sPkt.isACK())
								rstPacket.setSequenceNumber(sPkt.getAckNumber());
							else {
								try {
									rstPacket.setSequenceNumber(0);
									// FIXME: I think that this should be 1 if
									// data is null or length is 0
									rstPacket.setAckNumber(mod.add(sPkt.getSequenceNumber(), (long) ((sPkt.getPayload() == null) ? 1
											: sPkt.getPayload().remaining())));
									rstPacket.setACK(true);
								} catch (IllegalArgumentException e) {
									log.error("Unable to perform modulo operation", e);
								}
							}
							sendPacket(rstPacket);
						}
					}
				} else if (thisPacket.getProtocol() == EONPacket.EON_PROTOCOL_DEON) {
					if (thisConn instanceof DEONConnection) {
						((DEONConnection) thisConn).receivePacket((DEONPacket) thisPacket);
						return;
					}
				}
			} else {
				// No connection to take this packet
				if (thisPacket.getProtocol() == EONPacket.EON_PROTOCOL_SEON) {
					SEONPacket sPkt = (SEONPacket) thisPacket;
					// If it's a S-EON packet, send a RST unless it contains a
					// RST (in which case do nothing)
					if (!sPkt.isRST()) {
						SEONPacket rstPacket = new SEONPacket(sPkt.getDestSocketAddress(), sPkt.getSourceSocketAddress(), null);
						rstPacket.setRST(true);
						if (sPkt.isACK())
							rstPacket.setSequenceNumber(sPkt.getAckNumber());
						else {
							try {
								rstPacket.setSequenceNumber(0);
								rstPacket.setAckNumber(mod.add(sPkt.getSequenceNumber(), (long) ((sPkt.getPayload() == null) ? 0
										: sPkt.getPayload().remaining())));
								rstPacket.setACK(true);
							} catch (IllegalArgumentException e) {
								log.error("Unable to perform modulo operation", e);
							}
						}
						sendPacket(rstPacket);
					}
				}
				// Just drop the packet and move on
				log.warn("EON Packet dropped: " + thisPacket);
			}
		}
	}
}
