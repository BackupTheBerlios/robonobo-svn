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

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import com.robonobo.common.async.PushDataProvider;
import com.robonobo.common.async.PushDataReceiver;
import com.robonobo.common.concurrent.CatchingRunnable;

public class DEONConnection extends EONConnection implements PushDataProvider {
	// currentState
	public static final int DEONConnectionState_Open = 1;
	public static final int DEONConnectionState_Closed = 2;
	EonSocketAddress localEP, remoteEP;
	private List<ByteBuffer> incomingDataBufs = new ArrayList<ByteBuffer>();
	private List<EonSocketAddress> incomingDataAddrs = new ArrayList<EonSocketAddress>();
	// FIXME: can we start with a closed currentState?
	int state = DEONConnectionState_Closed;
	PushDataReceiver dataReceiver;
	ReentrantLock receiveLock = new ReentrantLock();
	boolean dataReceiverRunning = false;

	// Don't allow this class to be instantiated directly - use
	// EONManager.GetDEONConnection()
	protected DEONConnection(EONManager mgr) {
		super(mgr);
	}

	public void bind() throws EONException, IllegalArgumentException {
		connect(-1, null);
	}

	public void bind(int localEONPort) throws EONException, IllegalArgumentException {
		connect(localEONPort, null);
	}

	public void connect(EonSocketAddress remoteEndPoint) throws EONException, IllegalArgumentException {
		connect(-1, remoteEndPoint);
	}

	public synchronized void connect(int localEONPort, EonSocketAddress remoteEndPoint) throws EONException,
	IllegalArgumentException {
		if(state == DEONConnectionState_Open) {
			throw new EONException("Connection is already open");
		}
		if(localEONPort != -1 && (localEONPort < 1 || localEONPort > 65535)) {
			throw new IllegalArgumentException("Port must be between 1 and 65535");
		}
		localEP = new EonSocketAddress(mgr.getLocalSocketAddress(), 1);
		// First, request our port
		if(localEONPort == -1) {
			// Unspecified port
			localEP.setEonPort(mgr.getPort(this));
			if(localEP.getEonPort() == -1) {
				throw new IllegalArgumentException("No EON ports available");
			}
		} else {
			localEP.setEonPort(localEONPort);
			mgr.requestPort(localEP.getEonPort(), this);
		}
		remoteEP = remoteEndPoint;
		state = DEONConnectionState_Open;
	}

	public void send(byte[] buffer) throws EONException, IllegalArgumentException {
		send(buffer, 0, buffer.length);
	}

	public void send(byte[] buffer, int startIndex, int numBytes) throws EONException, IllegalArgumentException {
		if(remoteEP == null) {
			throw new EONException("Remote EON Endpoint has not been specified");
		}
		sendTo(remoteEP, buffer, startIndex, numBytes);
	}

	public void sendTo(EonSocketAddress remoteEndPoint, byte[] buffer) throws EONException, IllegalArgumentException {
		sendTo(remoteEndPoint, buffer, 0, buffer.length);
	}

	public void sendTo(EonSocketAddress remoteEndPoint, byte[] buffer, int startIndex, int numBytes)
	throws EONException, IllegalArgumentException {
		if(startIndex + numBytes > buffer.length) {
			throw new IllegalArgumentException("Supplied indices do not fit in supplied buffer");
		}
		synchronized(this) {
			if(state == DEONConnectionState_Closed) {
				throw new EONException("Connection is closed");
			}
		}
		byte[] payloadArr = new byte[numBytes];
		System.arraycopy(buffer, startIndex, payloadArr, 0, numBytes);
		ByteBuffer payload = ByteBuffer.wrap(payloadArr);
		DEONPacket thisPacket = new DEONPacket(null, remoteEndPoint, payload);
		thisPacket.setSourceSocketAddress(localEP);
		// TODO - some way of specifying that some deonconnections ignore our throttling
		super.sendPacket(thisPacket, 1d, false);
	}

	public synchronized void close() {
		if(state == DEONConnectionState_Closed) {
			log.warn("Connection is closed");
		}
		try {
			mgr.returnPort(localEP.getEonPort(), this);
		} catch(Exception e) {
			// dont care, we are closing, but log anyway
			log.warn("Exception caught on close()", e);
		}
		state = DEONConnectionState_Closed;
		fireOnClose();
	}

	public void abort() {
		close();
	}

	public EonSocketAddress getLocalSocketAddress() {
		return localEP;
	}

	// This might be null
	public EonSocketAddress getRemoteSocketAddress() {
		return remoteEP;
	}

	public int getState() {
		return state;
	}

	/**
	 * @return 2 element array - bytebuffer at 0, eonsocketaddress at 1
	 */
	public synchronized Object[] read() throws EONException, InterruptedException {
		synchronized (receiveLock) {
			while(incomingDataBufs.size() == 0) {
				receiveLock.wait();
			}
			ByteBuffer buf = (ByteBuffer) incomingDataBufs.get(0);
			incomingDataBufs.remove(0);
			EonSocketAddress addr = (EonSocketAddress) incomingDataAddrs.get(0);
			Object[] result = new Object[2];
			result[0] = buf;
			result[1] = addr;
			return result;
		}
	}


	void receivePacket(DEONPacket packet) {
		ByteBuffer buf = ByteBuffer.allocate(packet.getPayload().limit());
		buf.put(packet.getPayload());
		EonSocketAddress addr = packet.getSourceSocketAddress();
		synchronized (receiveLock) {
			incomingDataBufs.add(buf);
			incomingDataAddrs.add(addr);
			if(dataReceiver == null)
				receiveLock.notifyAll();
			else {
				// Async read
				if(dataReceiverRunning) {
					// This will be picked up by the already-running receiver
					return;
				} else {
					fireAsyncReceiver();
				}
			}
		}
	}
	
	/** Must only be called from inside sync(receiveLock) block */
	private void fireAsyncReceiver() {
		dataReceiverRunning = true;
		// Fire off our async receiver
		mgr.getExecutor().execute(new CatchingRunnable() {
			public void doRun() throws Exception {
				while (true) {
					// Grab a copy of our datareceiver in case it's set to null
					PushDataReceiver dataRec = null;
					ByteBuffer buf = null;
					EonSocketAddress sockAddr = null;
					synchronized (receiveLock) {
						dataRec = dataReceiver;
						if (dataRec == null || incomingDataBufs.size() == 0) {
							dataReceiverRunning = false;
							return;
						}
						buf = incomingDataBufs.remove(0);
						sockAddr = incomingDataAddrs.remove(0);
					}
					dataRec.receiveData(buf, sockAddr);
				}
			}
		});
	}
	
	public void setDataReceiver(PushDataReceiver dataReceiver) {
		synchronized (receiveLock) {
			this.dataReceiver = dataReceiver;
			// If we've got incoming data blocks queued up, handle them now
			if(dataReceiver != null && incomingDataBufs.size() > 0 && !dataReceiverRunning)
				fireAsyncReceiver();
		}
	}
}