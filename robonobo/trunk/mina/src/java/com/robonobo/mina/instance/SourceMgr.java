package com.robonobo.mina.instance;

import static com.robonobo.common.util.TimeUtil.*;

import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;

import com.robonobo.common.concurrent.*;
import com.robonobo.common.exceptions.SeekInnerCalmException;
import com.robonobo.core.api.proto.CoreApi.Node;
import com.robonobo.mina.message.proto.MinaProtocol.DontWantSource;
import com.robonobo.mina.message.proto.MinaProtocol.ReqSourceStatus;
import com.robonobo.mina.message.proto.MinaProtocol.SourceStatus;
import com.robonobo.mina.message.proto.MinaProtocol.StreamStatus;
import com.robonobo.mina.message.proto.MinaProtocol.WantSource;
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
	private Map<String, Map<String, SourceStatus>> readySources = new HashMap<String, Map<String, SourceStatus>>();
	/** Stream IDs that want sources */
	private Set<String> wantSources = new HashSet<String>();
	private SortedSet<SourceDetails> orderedSources = new TreeSet<SourceDetails>();
	private Map<String, SourceDetails> sourcesById = new HashMap<String, SourceDetails>();
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
				querySources();
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
	 *            false to send the request for sources out immediately (otherwise waits <5sec to batch requests
	 *            together)
	 */
	public void wantSources(String streamId, boolean tolerateDelay) {
		synchronized (this) {
			if (wantSources.contains(streamId))
				return;
			wantSources.add(streamId);
		}
		if (tolerateDelay)
			wsBatch.add(streamId);
		else {
			WantSource ws = WantSource.newBuilder().addStreamId(streamId).build();
			mina.getCCM().sendMessageToNetwork("WantSource", ws);
		}
	}

	public synchronized boolean wantsSource(String streamId) {
		return wantSources.contains(streamId);
	}

	public void dontWantSources(String streamId) {
		synchronized (this) {
			if (!wantSources.contains(streamId))
				return;
			wantSources.remove(streamId);
			readySources.remove(streamId);
		}
		// We don't send DontWantSources on shutdown, so don't bother batching
		DontWantSource dws = DontWantSource.newBuilder().addStreamId(streamId).build();
		mina.getCCM().sendMessageToNetwork("DontWantSource", dws);
	}

	public void gotSource(String streamId, Node source) {
		synchronized (this) {
			if (!wantSources.contains(streamId))
				return;
		}
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
			return;
		cacheSourceInitially(source, streamId);
		SourceDetails sd;
		synchronized (this) {
			sd = sourcesById.get(source.getId());
		}
		queryStatus(sd, sm.tolerateDelay());
	}

	public void gotSourceStatus(SourceStatus sourceStat) {
		// Remove it from our list of waiting sources - sm.foundSource() might add it again
		synchronized (this) {
			String sourceId = sourceStat.getFromNode().getId();
			SourceDetails sd = sourcesById.get(sourceId);
			if (sd != null) {
				for (StreamStatus ss : sourceStat.getSsList()) {
					sd.streamIds.remove(ss.getStreamId());
				}
				if (sd.streamIds.size() == 0) {
					sourcesById.remove(sourceId);
					orderedSources.remove(sd);
				}
			}
		}
		for (StreamStatus streamStat : sourceStat.getSsList()) {
			synchronized (this) {
				if (!wantSources.contains(streamStat.getStreamId()))
					continue;
			}
			StreamMgr sm = mina.getSmRegister().getSM(streamStat.getStreamId());
			if (sm != null)
				sm.foundSource(sourceStat, streamStat);
		}
	}

	/**
	 * Called when this source does not have a listener slot open, or else one is too expensive
	 */
	public void cacheSourceUntilAgoricsAcceptable(Node node, String streamId) {
		cacheSourceUntil(node, streamId, 1000 * mina.getConfig().getSourceAgoricsFailWaitTime(), "agorics unacceptable");
	}

	/** Called when this source does not enough data to serve us */
	public void cacheSourceUntilDataAvailable(Node node, String streamId) {
		cacheSourceUntil(node, streamId, 1000 * mina.getConfig().getSourceDataFailWaitTime(), "no useful data");
	}

	/**
	 * When a connection to a node dies unexpectedly, it might be network randomness between us and them, so wait for a
	 * while then retry them
	 */
	public void cachePossiblyDeadSource(Node node, String streamId) {
		cacheSourceUntil(node, streamId, 1000 * mina.getConfig().getDeadSourceQueryTime(), "network issue");
	}

	private void cacheSourceInitially(Node node, String streamId) {
		cacheSourceUntil(node, streamId, 1000 * mina.getConfig().getInitialSourceQueryTime(), "initial query");
	}

	private synchronized void cacheSourceUntil(Node node, String streamId, int waitMs, String reason) {
		Date waitUntil = timeInFuture(waitMs);
		SourceDetails sourceDetails;
		if (sourcesById.containsKey(node.getId())) {
			sourceDetails = sourcesById.get(node.getId());
			// Make sure we're waiting on this stream
			if (!sourceDetails.streamIds.contains(streamId)) {
				sourceDetails.streamIds.add(streamId);
				orderedSources.remove(sourceDetails);
			}
			// See if we should check sooner
			if (sourceDetails.nextQuery.after(waitUntil))
				orderedSources.remove(sourceDetails);
			sourceDetails.nextQuery = waitUntil;
			// If this wasn't previously removed, this will do nothing
			orderedSources.add(sourceDetails);
			sourcesById.put(node.getId(), sourceDetails);
		} else {
			sourceDetails = new SourceDetails(node, waitUntil, waitMs * 2);
			sourceDetails.streamIds.add(streamId);
			orderedSources.add(sourceDetails);
			sourcesById.put(node.getId(), sourceDetails);
		}
		// DEBUG
		log.debug("Caching source " + node.getId() + " for stream " + streamId + " (" + reason + ") until "
				+ getTimeFormat().format(waitUntil) + " - now have " + sourceDetails.streamIds.size() + " ss count");
	}

	/** Query all sources whose time has come */
	private void querySources() {
		// DEBUG - all the logging in this method should be removed
		log.debug("Querying sources");
		while (true) {
			SourceDetails sourceDetails;
			synchronized (this) {
				StringBuffer sb = new StringBuffer("Inspecting sources for query: ");
				for (SourceDetails sd : orderedSources) {
					sb.append(sd).append(" ");
				}
				log.debug(sb);

				if (orderedSources.size() == 0)
					return;
				sourceDetails = orderedSources.first();
				if (sourceDetails.nextQuery.after(now()))
					return;

				// DEBUG - are we removing these damn things?
				int szBefore = orderedSources.size();
				orderedSources.remove(sourceDetails);
				if (orderedSources.size() == szBefore)
					throw new SeekInnerCalmException("flarp!");

				Node source = sourceDetails.node;
				sourcesById.remove(source.getId());

				log.debug("Examining source " + sourceDetails);

				// Check that we still want sources for all these streams
				boolean wantIt = false;
				for (String sid : sourceDetails.streamIds) {
					if (wantSources.contains(sid)) {
						wantIt = true;
						break;
					}
				}
				if (!wantIt)
					continue;

				log.debug("Approved source " + sourceDetails);
				if (sourceDetails.retries < mina.getConfig().getSourceQueryRetries()) {
					// Re-add it again in case it doesn't answer - if it does, it'll get removed
					sourceDetails.retries = sourceDetails.retries + 1;
					sourceDetails.nextQuery = timeInFuture(sourceDetails.retryAfterMs);
					sourceDetails.retryAfterMs = sourceDetails.retryAfterMs * 2;
					log.debug("Re-adding source " + sourceDetails);
					orderedSources.add(sourceDetails);
					sourcesById.put(source.getId(), sourceDetails);
				}

				List<String> sidList = new ArrayList<String>();
				for (String sid : sourceDetails.streamIds) {
					sidList.add(sid);
				}
				log.debug("Querying source " + sourceDetails);
			}
			queryStatus(sourceDetails, true);
		}
	}

	private void queryStatus(SourceDetails sd, boolean tolerateDelay) {
		if (tolerateDelay) {
			ReqSourceStatusBatcher rssb;
			synchronized (this) {
				if (rssBatchers.containsKey(sd.node.getId()))
					rssb = rssBatchers.get(sd.node.getId());
				else {
					rssb = new ReqSourceStatusBatcher(sd.node);
					rssBatchers.put(sd.node.getId(), rssb);
				}
			}
			rssb.addAll(sd.streamIds);
		} else {
			ReqSourceStatus.Builder rssb = ReqSourceStatus.newBuilder();
			rssb.addAllStreamId(sd.streamIds);
			sendReqSourceStatus(sd.node, rssb);
		}
	}

	/**
	 * Called when this source is good to service us, but we are not ready or able to handle it
	 */
	public synchronized void cacheSourceUntilReady(SourceStatus sourceStat, StreamStatus streamStat) {
		if (!readySources.containsKey(streamStat.getStreamId()))
			readySources.put(streamStat.getStreamId(), new HashMap<String, SourceStatus>());
		readySources.get(streamStat.getStreamId()).put(sourceStat.getFromNode().getId(), sourceStat);
	}

	/**
	 * Returns the set of ready sources, and removes trace of them - if you want to cache them, add them again
	 */
	public synchronized Set<SourceStatus> getReadySources(String streamId) {
		Set<SourceStatus> result = new HashSet<SourceStatus>();
		if (readySources.containsKey(streamId))
			result.addAll(readySources.remove(streamId).values());
		return result;
	}

	/**
	 * Returns the set of ready nodes, but doesn't remove trace of them
	 */
	public synchronized Set<Node> getReadyNodes(String streamId) {
		Set<Node> result = new HashSet<Node>();
		for (SourceStatus ss : readySources.get(streamId).values()) {
			result.add(ss.getFromNode());
		}
		return result;
	}

	/**
	 * Returns the set of ready nodes, but doesn't remove trace of them
	 */
	public synchronized Set<String> getReadyNodeIds(String streamId) {
		Set<String> result = new HashSet<String>();
		if (readySources.containsKey(streamId)) {
			for (SourceStatus ss : readySources.get(streamId).values()) {
				result.add(ss.getFromNode().getId());
			}
		}
		return result;
	}

	public synchronized int numReadySources(String streamId) {
		if (!readySources.containsKey(streamId))
			return 0;
		return readySources.get(streamId).size();
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
	class SourceDetails implements Comparable<SourceDetails> {
		Node node;
		Set<String> streamIds = new HashSet<String>();
		Date nextQuery;
		int retryAfterMs;
		int retries = 0;

		public SourceDetails(Node node, Date nextQuery, int retryAfterMs) {
			this.node = node;
			this.nextQuery = nextQuery;
			this.retryAfterMs = retryAfterMs;
		}

		public int compareTo(SourceDetails o) {
			return nextQuery.compareTo(o.nextQuery);
		}

		@Override
		public int hashCode() {
			return node.getId().hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof SourceDetails))
				return false;
			SourceDetails o = (SourceDetails) obj;
			return node.getId().equals(o.node.getId());
		}

		@Override
		public String toString() {
			return "SD[" + node.getId() + ",nextq=" + getTimeFormat().format(nextQuery) + ",retries=" + retries
					+ ",retA=" + retryAfterMs + ",sids=" + streamIds + "]";
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

	/**
	 * Use UniqueBatcher here as if we get multiple GotSources for the same node in quick succession, we'll have duplicate stream ids
	 * @author macavity
	 *
	 */
	class ReqSourceStatusBatcher extends UniqueBatcher<String> {
		Node source;

		ReqSourceStatusBatcher(Node source) {
			super(mina.getConfig().getSourceRequestBatchTime(), mina.getExecutor());
			this.source = source;
		}

		@Override
		protected void runBatch(Collection<String> streamIdCol) {
			synchronized (SourceMgr.this) {
				rssBatchers.remove(source.getId());
			}
			ReqSourceStatus.Builder rssb = ReqSourceStatus.newBuilder();
			rssb.addAllStreamId(streamIdCol);
			sendReqSourceStatus(source, rssb);
		}
	}

}
