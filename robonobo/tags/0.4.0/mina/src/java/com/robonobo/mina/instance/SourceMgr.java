package com.robonobo.mina.instance;

import static com.robonobo.common.util.TimeUtil.now;
import static com.robonobo.common.util.TimeUtil.timeInFuture;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;

import com.robonobo.common.concurrent.Batcher;
import com.robonobo.common.concurrent.CatchingRunnable;
import com.robonobo.common.exceptions.SeekInnerCalmException;
import com.robonobo.common.util.TimeUtil;
import com.robonobo.core.api.proto.CoreApi.Node;
import com.robonobo.mina.message.proto.MinaProtocol.DontWantSource;
import com.robonobo.mina.message.proto.MinaProtocol.ReqSourceStatus;
import com.robonobo.mina.message.proto.MinaProtocol.SourceStatus;
import com.robonobo.mina.message.proto.MinaProtocol.StreamStatus;
import com.robonobo.mina.message.proto.MinaProtocol.WantSource;
import com.robonobo.mina.message.proto.MinaProtocol.SourceStatus.Builder;
import com.robonobo.mina.network.ControlConnection;
import com.robonobo.mina.stream.StreamMgr;

/**
 * Handles requesting and caching of source info
 * 
 * @author macavity
 * 
 */
public class SourceMgr {
	static final int SOURCE_CHECK_FREQ = 30; // Secs
	private MinaInstance mina;
	Log log;
	/** Map<StreamId, Map<NodeId, SourceStatus>> sources that are ready to go */
	private Map<String, Map<String, SourceStatus>> readySources = Collections.synchronizedMap(new HashMap<String, Map<String, SourceStatus>>());
	/** Stream IDs that want sources */
	private Set<String> wantSources = Collections.synchronizedSet(new HashSet<String>());
	private SortedSet<WaitingSourceStatus> waitSourceSet = new TreeSet<WaitingSourceStatus>();
	private Map<String, WaitingSourceStatus> waitSourceMap = new HashMap<String, WaitingSourceStatus>();
	/** Batch up source requests, to avoid repeated requests to the same nodes */
	private WantSourceBatcher wsBatch;
	private Map<String, ReqSourceStatusBatcher> rssBatchers = new HashMap<String, ReqSourceStatusBatcher>();

	private ScheduledFuture<?> queryTask;

	public SourceMgr(MinaInstance mina) {
		this.mina = mina;
		log = mina.getLogger(getClass());
		wsBatch = new WantSourceBatcher();
	}

	public void start() {
		queryTask = mina.getExecutor().scheduleAtFixedRate(new CatchingRunnable() {
			public void doRun() throws Exception {
				queryWaitingSources();
			}
		}, SOURCE_CHECK_FREQ, SOURCE_CHECK_FREQ, TimeUnit.SECONDS);
	}

	public void stop() {
		queryTask.cancel(true);
	}

	/**
	 * Tells the network we want sources
	 * 
	 * @param tolerateDelay
	 *            false to send the request for sources out immediately
	 *            (otherwise waits <5sec to batch requests together)
	 */
	public void wantSources(String streamId, boolean tolerateDelay) {
		if (wantSources.contains(streamId))
			return;
		wantSources.add(streamId);
		if (tolerateDelay)
			wsBatch.add(streamId);
		else {
			WantSource ws = WantSource.newBuilder().addStreamId(streamId).build();
			mina.getCCM().sendMessageToNetwork("WantSource", ws);
		}
	}

	public boolean wantsSource(String streamId) {
		return wantSources.contains(streamId);
	}

	public void dontWantSources(String streamId) {
		if (!wantSources.contains(streamId))
			return;
		wantSources.remove(streamId);
		synchronized (readySources) {
			readySources.remove(streamId);
		}
		// We don't send DontWantSources on shutdown, so don't bother batching
		DontWantSource dws = DontWantSource.newBuilder().addStreamId(streamId).build();
		mina.getCCM().sendMessageToNetwork("DontWantSource", dws);
	}

	public void gotSource(String streamId, Node source) {
		if (!wantSources.contains(streamId))
			return;
		if (source.getId().equals(mina.getMyNodeId().toString()))
			return;
		if (mina.getBadNodeList().checkBadNode(source.getId())) {
			log.debug("Ignoring Bad source " + source.getId());
			return;
		}
		if (!mina.getNetMgr().canConnectTo(source)) {
			log.debug("Ignoring source " + source + " - cannot connect");
			return;
		}
		StreamMgr sm = mina.getSmRegister().getSM(streamId);
		if (sm == null)
			throw new SeekInnerCalmException();
		queryStatus(source, streamId, sm.tolerateDelay());
	}

	public void gotSourceStatus(SourceStatus sourceStat) {
		// Remove it from our list of waiting sources - sm.foundSource() might add it again
		synchronized (waitSourceSet) {
			WaitingSourceStatus wss = waitSourceMap.remove(sourceStat.getFromNode().getId());
			if(wss != null)
				waitSourceSet.remove(wss);
		}
		for (StreamStatus streamStat : sourceStat.getSsList()) {
			if (!wantSources.contains(streamStat.getStreamId()))
				continue;
			StreamMgr sm = mina.getSmRegister().getSM(streamStat.getStreamId());
			if (sm != null)
				sm.foundSource(sourceStat, streamStat);
		}
	}

	/**
	 * Called when this source does not have a listener slot open, or else one
	 * is too expensive
	 */
	public void cacheSourceUntilAgoricsAcceptable(SourceStatus sourceStat, StreamStatus streamStat) {
		cacheSourceUntil(sourceStat, streamStat, 1000 * mina.getConfig().getSourceAgoricsFailWaitTime());
	}

	/** Called when this source does not enough data to serve us */
	public void cacheSourceUntilDataAvailable(SourceStatus sourceStat, StreamStatus streamStat) {
		cacheSourceUntil(sourceStat, streamStat, 1000 * mina.getConfig().getSourceDataFailWaitTime());
	}

	/**
	 * When a connection to a node dies unexpectedly, it might be network
	 * randomness between us and them, so wait for a while then retry them
	 */
	public void cachePossiblyDeadSource(SourceStatus sourceStat, StreamStatus streamStat) {
		cacheSourceUntil(sourceStat, streamStat, 1000 * mina.getConfig().getDeadSourceQueryTime());
	}

	private void cacheSourceUntil(SourceStatus sourceStat, StreamStatus streamStat, int waitMs) {
		Date waitUntil = timeInFuture(waitMs);
		log.debug("Caching source "+sourceStat.getFromNode().getId()+" for stream "+streamStat.getStreamId()+" until "+TimeUtil.getTimeFormat().format(waitUntil));
		synchronized (waitSourceSet) {
			WaitingSourceStatus wss;
			if (waitSourceMap.containsKey(sourceStat.getFromNode().getId())) {
				wss = waitSourceMap.get(sourceStat.getFromNode().getId());
				// Make sure we're waiting on this stream
				if (!wss.ssb.getSsList().contains(streamStat)) {
					wss.ssb.addSs(streamStat);
					waitSourceSet.remove(wss);
				}
				// See if we should check sooner
				if (wss.nextQuery.after(waitUntil))
					waitSourceSet.remove(wss);
				wss.nextQuery = waitUntil;
				// If this wasn't previously removed, this will do nothing
				waitSourceSet.add(wss);
				waitSourceMap.put(sourceStat.getFromNode().getId(), wss);
			} else {
				wss = new WaitingSourceStatus(waitUntil, waitMs*2, SourceStatus.newBuilder(sourceStat));
				// Make sure we're waiting on this stream
				if (!wss.ssb.getSsList().contains(streamStat))
					wss.ssb.addSs(streamStat);
				waitSourceSet.add(wss);
				waitSourceMap.put(sourceStat.getFromNode().getId(), wss);
			}
		}
	}

	/** Query all sources whose time has come */
	private void queryWaitingSources() {
		synchronized (waitSourceSet) {
			while (true) {
				if (waitSourceSet.size() == 0)
					return;
				WaitingSourceStatus wss = waitSourceSet.first();
				if (wss.nextQuery.after(now()))
					return;
				waitSourceSet.remove(wss);
				Node source = wss.ssb.getFromNode();
				waitSourceMap.remove(source.getId());
				// Check that we still want sources for all these streams
				boolean wantIt = false;
				for (StreamStatus ss : wss.ssb.getSsList()) {
					if(wantSources.contains(ss.getStreamId())) {
						wantIt = true;
						break;
					}
				}
				if(!wantIt)
					continue;
				if(wss.retries < mina.getConfig().getSourceQueryRetries()) {
					// Re-add it again in case it doesn't answer - if it does, it'll get removed
					wss.retries = wss.retries+1;
					wss.nextQuery = timeInFuture(wss.retryAfterMs);
					wss.retryAfterMs = wss.retryAfterMs * 2;
					waitSourceSet.add(wss);
					waitSourceMap.put(source.getId(), wss);
				}

				List<String> rssList = new ArrayList<String>();
				for (StreamStatus ss : wss.ssb.getSsList()) {
					rssList.add(ss.getStreamId());
				}
				queryStatus(source, rssList);
			}
		}
	}

	private void queryStatus(Node source, List<String> streamIdList) {
		ReqSourceStatusBatcher rssb;
		synchronized (rssBatchers) {
			if (rssBatchers.containsKey(source.getId()))
				rssb = rssBatchers.get(source.getId());
			else {
				rssb = new ReqSourceStatusBatcher(source);
				rssBatchers.put(source.getId(), rssb);
			}
		}
		for (String streamId : streamIdList) {
			rssb.add(streamId);
		}
	}

	private void queryStatus(Node source, String streamId, boolean tolerateDelay) {
		if (tolerateDelay) {
			ReqSourceStatusBatcher rssb;
			synchronized (rssBatchers) {
				if (rssBatchers.containsKey(source.getId()))
					rssb = rssBatchers.get(source.getId());
				else {
					rssb = new ReqSourceStatusBatcher(source);
					rssBatchers.put(source.getId(), rssb);
				}
			}
			rssb.add(streamId);
		} else {
			ReqSourceStatus.Builder rssb = ReqSourceStatus.newBuilder();
			rssb.addStreamId(streamId);
			sendReqSourceStatus(source, rssb);
		}
	}

	/**
	 * Called when this source is good to service us, but we are not ready or
	 * able to handle it
	 */
	public void cacheSourceUntilReady(SourceStatus sourceStat, StreamStatus streamStat) {
		synchronized (readySources) {
			if (!readySources.containsKey(streamStat.getStreamId()))
				readySources.put(streamStat.getStreamId(), new HashMap<String, SourceStatus>());
			readySources.get(streamStat.getStreamId()).put(sourceStat.getFromNode().getId(), sourceStat);
		}
	}

	/**
	 * Returns the set of ready sources, and removes trace of them - if you want
	 * to cache them, add them again
	 */
	public Set<SourceStatus> getReadySources(String streamId) {
		Set<SourceStatus> result = new HashSet<SourceStatus>();
		synchronized (readySources) {
			if (readySources.containsKey(streamId))
				result.addAll(readySources.remove(streamId).values());
		}
		return result;
	}

	/**
	 * Returns the set of ready nodes, but doesn't remove trace of them
	 */
	public Set<Node> getReadyNodes(String streamId) {
		Set<Node> result = new HashSet<Node>();
		synchronized (readySources) {
			for (SourceStatus ss : readySources.get(streamId).values()) {
				result.add(ss.getFromNode());
			}
		}
		return result;
	}

	/**
	 * Returns the set of ready nodes, but doesn't remove trace of them
	 */
	public Set<String> getReadyNodeIds(String streamId) {
		Set<String> result = new HashSet<String>();
		synchronized (readySources) {
			if (readySources.containsKey(streamId)) {
				for (SourceStatus ss : readySources.get(streamId).values()) {
					result.add(ss.getFromNode().getId());
				}
			}
		}
		return result;
	}

	public int numReadySources(String streamId) {
		synchronized (readySources) {
			if (!readySources.containsKey(streamId))
				return 0;
			return readySources.get(streamId).size();
		}		
	}

	private void sendReqSourceStatus(Node source, ReqSourceStatus.Builder sourceBldr) {
		ControlConnection cc = mina.getCCM().getCCWithId(source.getId());
		// Use the right descriptor depending on whether they're a
		// local conn or not
		if (cc != null) {
			cc.sendMessage("ReqSourceStatus", sourceBldr.build());
		} else {
			sourceBldr.setFromNode(mina.getNetMgr().getDescriptorForTalkingTo(source, false));
			sourceBldr.setToNodeId(source.getId());
			mina.getCCM().sendMessageToSupernodes("ReqSourceStatus", sourceBldr.build());
		}
	}

	/**
	 * A source that we do not need now, but which we might need in a bit
	 */
	class WaitingSourceStatus implements Comparable<WaitingSourceStatus> {
		Date nextQuery;
		SourceStatus.Builder ssb;
		int retryAfterMs;
		int retries = 0;

		public WaitingSourceStatus(Date nextQuery, int retryAfterMs, Builder ssb) {
			this.nextQuery = nextQuery;
			this.ssb = ssb;
			this.retryAfterMs = retryAfterMs;
		}

		public int compareTo(WaitingSourceStatus o) {
			return nextQuery.compareTo(o.nextQuery);
		}

		@Override
		public int hashCode() {
			return ssb.getFromNode().getId().hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof WaitingSourceStatus))
				return false;
			WaitingSourceStatus o = (WaitingSourceStatus) obj;
			return ssb.getFromNode().getId().equals(o.ssb.getFromNode().getId());
		}
	}

	class WantSourceBatcher extends Batcher<String> {
		WantSourceBatcher() {
			super(mina.getConfig().getSourceRequestBatchTime(), mina.getExecutor());
		}

		@Override
		protected void runBatch(Collection<String> streamIds) {
			WantSource ws = WantSource.newBuilder().addAllStreamId(streamIds).build();
			mina.getCCM().sendMessageToNetwork("WantSource", ws);
		}
	}

	class ReqSourceStatusBatcher extends Batcher<String> {
		Node source;

		ReqSourceStatusBatcher(Node source) {
			super(mina.getConfig().getSourceRequestBatchTime(), mina.getExecutor());
			this.source = source;
		}

		@Override
		protected void runBatch(Collection<String> streamIdCol) {
			synchronized (rssBatchers) {
				rssBatchers.remove(source.getId());
			}
			ReqSourceStatus.Builder rssb = ReqSourceStatus.newBuilder();
			rssb.addAllStreamId(streamIdCol);
			sendReqSourceStatus(source, rssb);
		}
	}

}
