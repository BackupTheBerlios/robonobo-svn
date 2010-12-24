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
	private LinkedList<EONPacket> throttledPkts = new LinkedList<EONPacket>();
	private ReentrantLock lock;
	private Condition canSend;

	PktSender(DatagramChannel chan) {
		this.chan = chan;
		lock = new ReentrantLock();
		canSend = lock.newCondition();
	}

	void start() {
		t = new Thread(this);
		t.start();
	}

	void stop() {
		stopping = true;
		if (t != null)
			t.interrupt();
	}

	void addPkt(EONPacket pkt, boolean noDelay) {
		lock.lock();
		try {
			if (noDelay || maxBps < 0) {
				unThrottledPkts.add(pkt);
				canSend.signal();
			} else {
				// Annoying that one can't 'reset' an iterator, which means we're doing this object creation every time
				ListIterator<EONPacket> it = throttledPkts.listIterator();
				// Start at the beginning of our list and go up until we find a pkt with a gamma >= to this one, then
				// insert it before
				while (it.hasNext()) {
					EONPacket testPkt = it.next();
					if (testPkt.getGamma() >= pkt.getGamma()) {
						// Rewind so we insert our pkt before this one
						it.previous();
						break;
					}
				}
				it.add(pkt);
				long nowTime = currentTimeMillis();
				// If we're not waiting to send due to our credit limit, signal the thread
				if(nowTime >= nextSendTime)
					canSend.signal();
			}
		} finally {
			lock.unlock();
		}
	}
	
	@Override
	public void doRun() throws Exception {
		lock.lock();
		try {
			while (true) {
				if (stopping)
					return;
				long nowTime = currentTimeMillis();
				// If we've run out of credit, wait until the specified time, otherwise wait until signalled
				if(unThrottledPkts.size() == 0 && nextSendTime > nowTime) {
					if(log.isDebugEnabled())
						log.debug("Pausing data send due to throttling");
					canSend.await((nextSendTime - nowTime), TimeUnit.MILLISECONDS);
				}
				while (unThrottledPkts.size() == 0 && throttledPkts.size() == 0) {
					canSend.await();
				}
				nowTime = currentTimeMillis();
				bytesCredit += (nowTime - lastCreditTime) * (float) (maxBps / 1000);
				if(bytesCredit > maxBytesCredit())
					bytesCredit = maxBytesCredit();
				lastCreditTime = nowTime;
				// Any unthrottled pkts we have, send them now
				for (EONPacket pkt : unThrottledPkts) {
					sendPkt(pkt);
				}
				unThrottledPkts.clear();
				// Send our throttled pkts until we run out of credit - end of list first
				while (throttledPkts.size() > 0 && bytesCredit >= getPayloadSize(throttledPkts.getLast())) {
					EONPacket pkt = throttledPkts.removeLast();
					bytesCredit -= getPayloadSize(pkt);
					sendPkt(pkt);
				}
				// If we're out of credit, make us wait for a while
				if(throttledPkts.size() > 0)
					nextSendTime = nowTime + WAIT_TIME;
			}
		} finally {
			lock.unlock();
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
	
	private int getPayloadSize(EONPacket pkt) {
		if (pkt.getPayload() == null)
			return 0;
		return pkt.getPayload().limit();
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
	
	class TestyPkt extends EONPacket {
		int pktNum;
		@Override
		public EonSocketAddress getDestSocketAddress() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public EonSocketAddress getSourceSocketAddress() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void setDestSocketAddress(EonSocketAddress endPoint) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void setSourceSocketAddress(EonSocketAddress endPoint) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void toByteBuffer(ByteBuffer buf) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public int getProtocol() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public ByteBuffer getPayload() {
			// TODO Auto-generated method stub
			return null;
		}
		
	}
}
