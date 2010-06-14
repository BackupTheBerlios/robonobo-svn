package com.robonobo.mina.network;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Semaphore;

import com.robonobo.common.exceptions.SeekInnerCalmException;
import com.robonobo.common.util.TimeUtil;
import com.robonobo.core.api.proto.CoreApi.EndPoint;
import com.robonobo.mina.external.buffer.PageInfo;
import com.robonobo.mina.instance.MinaInstance;
import com.robonobo.mina.message.MessageHolder;
import com.robonobo.mina.message.proto.MinaProtocol.ReqPage;
import com.robonobo.mina.stream.StreamMgr;
import com.robonobo.mina.util.MinaConnectionException;

/**
 * @syncpriority 120
 */
public class BCPair extends ConnectionPair {
	static final char GAMMA = 0x03b3;
	private BroadcastConnection bc;
	private boolean isClosed;
	private Semaphore reqPageSem = new Semaphore(1, true);

	/**
	 * @param pages
	 * @syncpriority 160
	 */
	public BCPair(MinaInstance mina, StreamMgr sm, ControlConnection cc, EndPoint listenEp, List<Long> pages) {
		super(sm, cc);
		try {
			bc = cc.getSCF().getBroadcastConnection(cc, listenEp);
			bc.setBCPair(this);
		} catch (MinaConnectionException e) {
			log.error("Error getting broadcast connection to talk to " + listenEp, e);
			die();
		}
		// This will set our gamma
		cc.addBCPair(this);
		log.info("Created broadcast conn to " + cc.getNodeId() + " for stream " + sm.getStreamId());
		requestPages(pages);
	}

	public boolean isClosed() {
		return isClosed;
	}

	@Override
	public int getFlowRate() {
		return bc.getFlowRate();
	}
	
	/**
	 * @syncpriority 120
	 */
	public synchronized void die() {
		if (isClosed)
			return;
		isClosed = true;
		log.info("Ceasing broadcast of " + sm.getStreamId() + " to " + cc.getNodeId());
		if (bc != null)
			bc.close();
		cc.removeBCPair(this);
		super.die();
	}

	/**
	 * @syncpriority 120
	 */
	public synchronized void abort() {
		if (bc != null)
			bc.close();
	}

	/**
	 * @syncpriority 120
	 */
	public void requestPages(List<Long> pages) {
		// Bug huntin
		if (bc == null)
			throw new SeekInnerCalmException();
		// We use a semaphore here rather than plain ol' synchronized as the
		// fairness flag means it guarantees requests are handled in the order
		// they are received - to make sure we don't handle reqpage requests out of
		// order
		boolean failedPage = false;
		try {
			reqPageSem.acquire();
			try {
				// Get the total amount of data we want to send
				long totalPageLen = 0;
				if (mina.getConfig().isAgoric()) {
					for(Iterator<Long> iter = pages.iterator();iter.hasNext();) {
						Long pn = iter.next();
						if (sm.getPageBuffer().haveGotPage(pn)) {
							PageInfo pi = sm.getPageBuffer().getPageInfo(pn);
							totalPageLen += pi.getLength();
						} else {
							iter.remove();
							failedPage = true;
							if (log.isDebugEnabled())
								log.debug(this + " requested page " + pn + " which I do not have");
						}
					}
				}
				// Make sure they have enough ends to get these (if their
				// balance is too low, this call will cause them to be told to
				// pay up)
				int auctStatIdx = (mina.getConfig().isAgoric()) ? mina.getSellMgr().requestAndCharge(cc.getNodeId(), totalPageLen)
						: 0;
				if (auctStatIdx < 0) {
					// Ask again when they've paid up
					ReqPage rp = ReqPage.newBuilder().setStreamId(sm.getStreamId()).addAllPage(pages).build();
					mina.getSellMgr().cmdPendingOpenAccount(new MessageHolder("ReqPage", rp, cc, TimeUtil.now()));
					return;
				} else {
					for (Long pn : pages) {
						bc.addPageToQ(pn, auctStatIdx);
					}
				}
			} finally {
				reqPageSem.release();
			}
			// If they asked us for a page we didn't have, tell them where we are in the stream
			if(failedPage) {
				cc.sendMessage("StreamStatus", sm.buildStreamStatus(cc.getNodeId()));
			}
		} catch (InterruptedException e) {
			throw new SeekInnerCalmException();
		}
	}

	public void setGamma(float gamma) {
		if(log.isDebugEnabled())
			log.debug(this+": setting "+GAMMA+" to "+gamma);
		bc.setGamma(gamma);
	}
	
	public boolean equals(Object obj) {
		if (obj instanceof BCPair)
			return (hashCode() == obj.hashCode());
		else
			return false;
	}

	public int hashCode() {
		return getClass().getName().hashCode() ^ cc.getNodeId().hashCode() ^ sm.getStreamId().hashCode();
	}

	public String toString() {
		return "BCP[node=" + cc.getNodeId() + ",stream=" + sm.getStreamId() + "]";
	}
}
