package com.robonobo.mina.network;

import static com.robonobo.common.util.TextUtil.formatSizeInBytes;
import static com.robonobo.common.util.TimeUtil.now;
import static java.lang.System.*;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.robonobo.common.concurrent.Attempt;
import com.robonobo.common.concurrent.CatchingRunnable;
import com.robonobo.common.util.FileUtil;
import com.robonobo.mina.agoric.AuctionState;
import com.robonobo.mina.external.buffer.Page;
import com.robonobo.mina.message.proto.MinaProtocol.ReqPage;
import com.robonobo.mina.message.proto.MinaProtocol.SourceStatus;
import com.robonobo.mina.message.proto.MinaProtocol.StartSource;
import com.robonobo.mina.message.proto.MinaProtocol.StopSource;
import com.robonobo.mina.message.proto.MinaProtocol.StreamStatus;
import com.robonobo.mina.stream.StreamMgr;
import com.robonobo.mina.util.MinaConnectionException;

/**
 * @syncpriority 130
 */
public class LCPair extends ConnectionPair {
	private static final int MIN_PAGE_TIMEOUT = 60000;
	private ListenConnection lc;
	private boolean closing = false;
	private Map<Long, PageAttempt> reqdPages = new HashMap<Long, PageAttempt>();
	private boolean setupFinished = false;
	private SourceStatus lastSourceStat;
	private StreamStatus lastStreamStat;
	// These below are used to measure page timeouts, all in ms
	// TODO Revisit page timeouts when we have some real internetty data
	int srtt, rttvar;
	int rto;
	Future<?> usefulDataTimeout = null;

	public LCPair(StreamMgr sm, ControlConnection cc, SourceStatus ss) throws MinaConnectionException {
		super(sm, cc);
		rto = MIN_PAGE_TIMEOUT;
		lc = cc.getSCF().getListenConnection(cc);
		lc.setLCPair(this);
		setLastSourceStat(ss);
		cc.addLCPair(this);
		startListening();
	}

	@Override
	public int getFlowRate() {
		return lc.getFlowRate();
	}

	/**
	 * 
	 */
	protected void startListening() {
		synchronized (this) {
			if (setupFinished)
				return;
		}
		// If we don't yet have a confirmed bid, make one, and call us back when
		// they do
		// TODO: We might need to change our bid if this new stream demands a
		// higher rate
		final String nodeId = cc.getNodeId();
		if (mina.getConfig().isAgoric() && mina.getBuyMgr().getAgreedBidTo(nodeId) <= 0) {
			mina.getBuyMgr().gotAgorics(cc.getNodeId(), lastSourceStat.getAgorics());
			mina.getBuyMgr().newAuctionState(cc.getNodeId(), new AuctionState(lastSourceStat.getAuctionState()));
			// Set our timeout to 8 * command timeout, to allow time to open an
			// auction and for bids to go to and fro
			Attempt a = new Attempt(mina.getExecutor(), 8 * mina.getConfig().getMessageTimeout() * 1000, "lcp-" + sm.getStreamId()
					+ "-" + nodeId) {
				protected void onSuccess() {
					startListening();
				}

				protected void onTimeout() {
					log.error("Timeout attempting to set up connection to " + nodeId + " for stream "
							+ sm.getStreamId());
					mina.getSourceMgr().cachePossiblyDeadSource(lastSourceStat, lastStreamStat);
					die(false);
				}
			};
			a.start();
			mina.getBuyMgr().setupAccountAndBid(nodeId, sm.getBidStrategy().getOpeningBid(nodeId), a);
			return;
		}

		// Let's get this party started
		sm.getPRM().notifyStreamStatus(cc.getNodeId(), lastStreamStat);
		SortedSet<Long> newPages = sm.getPRM().getPagesToRequest(cc.getNodeId(), 1, reqdPages.keySet());
		StartSource ss = StartSource.newBuilder().setStreamId(sm.getStreamId()).setEp(lc.getEndPoint())
				.addAllPage(newPages).build();
		cc.sendMessage("StartSource", ss);
		for (Long pn : newPages) {
			int statusIdx = mina.getConfig().isAgoric() ? mina.getBuyMgr().getCurrentStatusIdx(cc.getNodeId()) : 0;
			PageAttempt rpa = new PageAttempt(rto, pn, currentTimeMillis(), statusIdx);
			reqdPages.put(pn, rpa);
			rpa.start();
		}
		synchronized (this) {
			setupFinished = true;
		}
	}

	public void notifySourceStatus(SourceStatus sourceStat) {
		setLastSourceStat(sourceStat);
		for (StreamStatus streamStat : sourceStat.getSsList()) {
			if (streamStat.getStreamId().equals(sm.getStreamId())) {
				notifyStreamStatus(streamStat);
				break;
			}
		}
	}

	/**
	 * @syncpriority 90
	 */
	public void notifyStreamStatus(StreamStatus streamStat) {
		this.lastStreamStat = streamStat;
		sm.getPRM().notifyStreamStatus(cc.getNodeId(), streamStat);
		sendReqPageIfNecessary();
	}

	public SourceStatus getLastSourceStatus() {
		return lastSourceStat;
	}

	public StreamStatus getLastStreamStatus() {
		return lastStreamStat;
	}

	private void setLastSourceStat(SourceStatus sourceStat) {
		this.lastSourceStat = sourceStat;
		for (StreamStatus streamStat : sourceStat.getSsList()) {
			if (streamStat.getStreamId().equals(sm.getStreamId()))
				this.lastStreamStat = streamStat;
		}
	}

	/**
	 * @syncpriority 130
	 */
	public synchronized void abort() {
		if (lc != null)
			lc.close();
	}

	public void die() {
		die(true);
	}

	/**
	 * @syncpriority 130
	 */
	public void die(boolean sendStopSource) {
		log.debug(this + " closing down");
		synchronized (this) {
			if (closing)
				return;
			closing = true;
			if (lc != null)
				lc.close();
		}
		for (PageAttempt rpa : reqdPages.values()) {
			rpa.failed();
		}
		if(usefulDataTimeout != null)
			usefulDataTimeout.cancel(false);
		if (sendStopSource) {
			try {
				sendMessage("StopSource", StopSource.newBuilder().setStreamId(sm.getStreamId()).build());
			} catch (MinaConnectionException ignore) {
			}
		}
		cc.removeLCPair(this);
		super.die();
	}

	public boolean equals(Object obj) {
		if (!(obj instanceof LCPair))
			return false;
		ConnectionPair other = (ConnectionPair) obj;
		return (other.getCC().getNodeId().equals(cc.getNodeId()) && other.getSM().getStreamId()
				.equals(sm.getStreamId()));
	}

	public int hashCode() {
		return getClass().hashCode() ^ cc.getNodeId().hashCode() ^ sm.getStreamId().hashCode();
	}

	public boolean isComplete() {
		return setupFinished;
	}

	public String toString() {
		return "LCP[node=" + cc.getNodeId() + ",stream=" + sm.getStreamId() + "]";
	}

	/**
	 * @syncpriority 200
	 */
	public void receivePage(Page p) {
		Long pn = new Long(p.getPageNumber());
		PageAttempt rpa;
		synchronized (this) {
			rpa = reqdPages.remove(pn);
		}
		if (rpa != null) {
			rpa.succeeded();
			updateRtt((int) (now().getTime() - rpa.startTime));
		} else {
			log.error(this + " received non-requested page " + p.getPageNumber());
			return;
		}

		if (log.isDebugEnabled()) {
			long bytesInFlight = sm.getPageBuffer().getAvgPageSize() * reqdPages.size();
			log.debug("Stream " + sm.getStreamId() + " / node " + cc.getNodeId() + ": got page " + pn + " - rate="
					+ formatSizeInBytes(getFlowRate()) + "/s, " + reqdPages.size() + " pages in flight ("
					+ FileUtil.humanReadableSize(bytesInFlight) + ")");
		}

		sm.receivePage(p);
		if (mina.getConfig().isAgoric()) {
			// Check that the auction state is legit (they might try to shaft us
			// by putting in a higher-bid state) - it must be equal to or
			// greater than (mod 64) the status when we requested it
			if (AuctionState.INDEX_MOD.lt(p.getAuctionStatus(), rpa.statusIdx)) {
				// TODO Something much more serious
				log.error("ERROR: " + this + " received page with statusIdx " + p.getAuctionStatus()
						+ " but I requested it with idx " + rpa.statusIdx);
				return;
			}
			mina.getBuyMgr().receivedPage(cc.getNodeId(), p.getAuctionStatus(), p.getLength());
		}

		sendReqPageIfNecessary();
	}

	/**
	 * @syncpriority 120
	 */
	private void sendReqPageIfNecessary() {
		if (closing || !sm.isReceiving() || mina.getCCM().isShuttingDown())
			return;
		// Check to see if we should req pages now (there might be a
		// higher-priority stream from this source)
		if (!cc.isHighestPriority(this)) {
			// Wait a second and try again
			mina.getExecutor().schedule(new CatchingRunnable() {
				public void doRun() throws Exception {
					sendReqPageIfNecessary();
				}
			}, 1, TimeUnit.SECONDS);
			return;
		}

		int statusIdx = (mina.getConfig().isAgoric()) ? mina.getBuyMgr().getCurrentStatusIdx(cc.getNodeId()) : 0;
		int pgWin = pageWindowSize();
		synchronized (this) {
			if (reqdPages.size() < pgWin) {
				int pagesToReq = pgWin - reqdPages.size();
				SortedSet<Long> newPages = sm.getPRM()
						.getPagesToRequest(cc.getNodeId(), pagesToReq, reqdPages.keySet());
				if (newPages.size() > 0) {
					if (usefulDataTimeout != null) {
						usefulDataTimeout.cancel(false);
						usefulDataTimeout = null;
					}
					try {
						sendMessage("ReqPage", ReqPage.newBuilder().setStreamId(sm.getStreamId()).addAllPage(newPages)
								.build());
					} catch (MinaConnectionException e) {
						// We'll die shortly - just return
						return;
					}
					for (long pn : newPages) {
						PageAttempt rpa = new PageAttempt(rto, pn, now().getTime(), statusIdx);
						reqdPages.put(pn, rpa);
						rpa.start();
					}
				} else if (reqdPages.size() == 0) {
					// We have no pages in flight, and we just got given an
					// empty set of pages to ask for, which means they have no
					// pages that are useful to us... Start a timeout to shut us
					// down if they don't offer any useful data within 2 mins
					if (usefulDataTimeout == null) {
						log.debug(this + " has no useful data - starting timeout of "
								+ mina.getConfig().getUsefulDataSourceTimeout() + "s");
						usefulDataTimeout = mina.getExecutor().schedule(new CatchingRunnable() {
							public void doRun() throws Exception {
								log.info(LCPair.this + " useful data timeout - closing (caching source)");
								mina.getSourceMgr().cacheSourceUntilDataAvailable(lastSourceStat, lastStreamStat);
								die();
							}
						}, mina.getConfig().getUsefulDataSourceTimeout(), TimeUnit.SECONDS);
					}
				}
			}
		}
	}

	private int pageWindowSize() {
		float windowSecs = mina.getConfig().getPageRequestLookAheadTime() / 1000;
		int result = (getFlowRate() == 0) ? 1
				: (int) (getFlowRate() * windowSecs / sm.getPageBuffer().getAvgPageSize());
		if (result < 1)
			result = 1;
		return result;
	}

	private void updateRtt(int newRtt) {
		// See rfc 2988
		if (srtt == 0) {
			srtt = newRtt;
			rttvar = newRtt / 2;
		} else {
			rttvar = (int) ((0.75 * rttvar) + (0.25 * Math.abs(srtt - newRtt)));
			srtt = (int) ((0.875 * srtt) + (0.125 * newRtt));
		}
		rto = srtt + 4 * rttvar;
		if (rto < MIN_PAGE_TIMEOUT)
			rto = MIN_PAGE_TIMEOUT;
	}

	class PageAttempt extends Attempt {
		long startTime;
		long pageNum;
		int statusIdx;

		public PageAttempt(int timeoutMs, long pageNum, long startTime, int statusIdx) {
			super(mina.getExecutor(), timeoutMs, "ReqPage-" + pageNum);
			this.pageNum = pageNum;
			this.startTime = startTime;
			this.statusIdx = statusIdx;
		}

		@Override
		protected void onFail() {
			sm.getPRM().notifyOverduePage(pageNum);
			if (log.isDebugEnabled())
				log.debug(LCPair.this + " marking overdue page " + pageNum);
		}

		@Override
		protected void onTimeout() {
			onFail();
		}
	}
}
