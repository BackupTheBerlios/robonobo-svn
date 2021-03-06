package com.robonobo.core.service;

import static com.robonobo.common.util.FileUtil.makeFileNameSafe;
import static com.robonobo.common.util.TimeUtil.now;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.robonobo.common.exceptions.SeekInnerCalmException;
import com.robonobo.core.api.RobonoboException;
import com.robonobo.core.api.StreamVelocity;
import com.robonobo.core.api.model.DownloadingTrack;
import com.robonobo.core.api.model.Stream;
import com.robonobo.core.api.model.DownloadingTrack.DownloadStatus;
import com.robonobo.core.storage.StorageService;
import com.robonobo.mina.external.ConnectedNode;
import com.robonobo.mina.external.MinaControl;
import com.robonobo.mina.external.MinaListener;
import com.robonobo.mina.external.buffer.PageBuffer;
import com.robonobo.mina.external.buffer.PageBufferListener;

/**
 * Responsible for translating mina Receptions to robonobo Downloads, and
 * keeping track of not-yet-started Downloads (which have no associated
 * Reception)
 * 
 * @author macavity
 */
@SuppressWarnings("unchecked")
public class DownloadService extends AbstractService implements MinaListener, PageBufferListener {
	static final int PRIORITY_CURRENT = Integer.MAX_VALUE;
	static final int PRIORITY_NEXT = Integer.MAX_VALUE - 1;
	Log log = LogFactory.getLog(getClass());
	private DbService db;
	private MinaControl mina;
	private MetadataService metadata;
	private StorageService storage;
	private ShareService share;
	private EventService event;
	private PlaybackService playback;
	private Set<String> downloadStreamIds;
	private Set<String> preFetchStreamIds;
	
	/**
	 * Saves downloads in the order they are added, to make sure they're
	 * downloaded in order
	 */
	private List<String> dPriority = new ArrayList<String>();
	/**
	 * Make sure that we don't add two downloads with the same start time, to
	 * ensure they're restored from the db in the right order. So, keep track of
	 * the last download time, and if it hasn't increased, add 1ms to ensure
	 * uniqueness
	 */
	long lastDlStartTime = 0;
	private File downloadsDir;

	public DownloadService() {
		addHardDependency("core.mina");
		addHardDependency("core.metadata");
		addHardDependency("core.storage");
		addHardDependency("core.shares");
	}

	@Override
	public void startup() throws Exception {
		db = rbnb.getDbService();
		mina = rbnb.getMina();
		mina.addMinaListener(this);
		metadata = rbnb.getMetadataService();
		storage = rbnb.getStorageService();
		share = rbnb.getShareService();
		event = rbnb.getEventService();
		playback = rbnb.getPlaybackService();
		downloadStreamIds = new HashSet<String>();
		downloadStreamIds.addAll(db.getDownloads());
		preFetchStreamIds = new HashSet<String>();
		downloadsDir = new File(rbnb.getHomeDir(), "in-progress");
		downloadsDir.mkdir();
		int numStarted = 0;
		for (String streamId : downloadStreamIds) {
			DownloadingTrack d = db.getDownload(streamId);
			if (d.getDownloadStatus() == DownloadStatus.Finished) {
				db.deleteDownload(streamId);
				continue;
			}
			PageBuffer pb = storage.loadPageBuf(d.getStream().getStreamId());
			d.setPageBuf(pb);
			// If we died or got kill-9d at the wrong point, we might be 100%
			// finished downloading - turn it into a share here, or it'll never
			// get added
			if(pb.isComplete()) {
				share.addShareFromCompletedDownload(d);
				db.deleteDownload(streamId);
				continue;
			}
			synchronized (dPriority) {
				dPriority.add(d.getStream().getStreamId());
			}
			if (numStarted < rbnb.getConfig().getMaxRunningDownloads()) {
				startDownload(d, pb);
				numStarted++;
			}
		}
		updatePriorities();
	}

	@Override
	public void shutdown() throws Exception {
		for (String streamId : db.getDownloads()) {
			DownloadingTrack dl = getDownload(streamId);
			stopDownload(dl);
		}
	}
	
	public void addDownload(String streamId) throws RobonoboException {
		File dataFile = new File(downloadsDir, makeFileNameSafe(streamId));
		log.info("Adding download for " + streamId);
		Stream s1 = metadata.getStream(streamId);
		DownloadingTrack d = new DownloadingTrack(s1, dataFile, DownloadStatus.Paused);
		long startTime = now().getTime();
		synchronized (this) {
			if (startTime == lastDlStartTime)
				startTime++;
			lastDlStartTime = startTime;
		}
		d.setDateAdded(new Date(startTime));
		try {
			PageBuffer pb = storage.createPageBufForDownload(s1, dataFile);
			if (numRunningDownloads() < rbnb.getConfig().getMaxRunningDownloads())
				startDownload(d, pb);
		} catch (Exception e) {
			log.error("Caught exception when starting download for " + s1.getStreamId(), e);
			throw new RobonoboException(e);
		} finally {
			if (d != null)
				db.putDownload(d);
		}
		synchronized (dPriority) {
			dPriority.add(s1.getStreamId());
		}
		updatePriorities();
		synchronized (this) {
			downloadStreamIds.add(s1.getStreamId());
		}
		event.fireTrackUpdated(s1.getStreamId());
	}
	
	public void deleteDownload(String streamId) throws RobonoboException {
		log.info("Deleting download for stream " + streamId);
		playback.stopIfCurrentlyPlaying(streamId);
		mina.stopReception(streamId);
		db.deleteDownload(streamId);
		// If we have started sharing this stream, don't nuke the pagebuf
		if (db.getShare(streamId) == null)
			storage.nukePageBuf(streamId);
		synchronized (dPriority) {
			dPriority.remove(streamId);
		}
		updatePriorities();
		synchronized (this) {
			downloadStreamIds.remove(streamId);
		}
		event.fireTrackUpdated(streamId);
		startMoreDownloads();
	}

	public void pauseDownload(String streamId) {
		log.debug("Pausing download for " + streamId);
		DownloadingTrack d = db.getDownload(streamId);
		try {
			stopDownload(d);
		} catch (Exception e) {
			log.error("Caught exception when pausing download for " + streamId, e);
		}
		d.setDownloadStatus(DownloadStatus.Paused);
		db.putDownload(d);
		event.fireTrackUpdated(streamId);
	}

	public void startDownload(String streamId) throws RobonoboException {
		log.debug("Starting download for " + streamId);
		DownloadingTrack d = db.getDownload(streamId);
		if (d == null)
			throw new SeekInnerCalmException();
		if (d.getDownloadStatus() == DownloadStatus.Finished) {
			log.debug("Not starting finished download " + streamId);
			return;
		}
		if(d.getDownloadStatus() == DownloadStatus.Downloading) {
			log.debug("Not starting already-running download "+streamId);
			return;
		}
		PageBuffer pb;
		try {
			pb = storage.loadPageBuf(streamId);
		} catch (IOException e) {
			throw new RobonoboException(e);
		}
		d.setPageBuf(pb);
		pb.addListener(this);
		startDownload(d, pb);
		d.setDownloadStatus(DownloadStatus.Downloading);
		db.putDownload(d);
		event.fireTrackUpdated(streamId);
	}

	private void startDownload(DownloadingTrack d, PageBuffer pb) throws RobonoboException {
		d.setPageBuf(pb);
		mina.startReception(d.getStream().getStreamId(), pb, StreamVelocity.LowestCost);
		if (d.getDownloadStatus() != DownloadStatus.Downloading) {
			d.setDownloadStatus(DownloadStatus.Downloading);
			db.putDownload(d);
		}
	}

	private void stopDownload(DownloadingTrack d) throws Exception {
		mina.stopReception(d.getStream().getStreamId());
		d.getPageBuf().close();
		event.fireTrackUpdated(d.getStream().getStreamId());
	}

	private int numRunningDownloads() {
		return db.numRunningDownloads();
	}

	public DownloadingTrack getDownload(String streamId) {
		synchronized (this) {
			if(!downloadStreamIds.contains(streamId))
				return null;
		}
		DownloadingTrack d = rbnb.getDbService().getDownload(streamId);
		if (d != null) {
			d.setNumSources(mina.numSources(streamId));
			try {
				d.setPageBuf(storage.loadPageBuf(streamId));
			} catch (IOException e) {
				log.error("Error loading page buffer for stream "+streamId, e);
				return null;
			}
		}
		return d;

	}

	public void receptionCompleted(String streamId) {
		Stream s = metadata.getStream(streamId);
		DownloadingTrack d = db.getDownload(streamId);
		if (d == null) {
			log.error("ERROR: no download for completed stream " + s.getStreamId());
			return;
		}
		try {
			d.setPageBuf(storage.loadPageBuf(streamId));
		} catch (Exception e) {
			log.error("Error stopping completed download", e);
		}
		d.setDownloadStatus(DownloadStatus.Finished);
		synchronized (dPriority) {
			dPriority.remove(streamId);
			preFetchStreamIds.remove(streamId);
		}
		updatePriorities();
		synchronized (this) {
			downloadStreamIds.remove(streamId);
		}
		try {
			// Start sharing this one
			share.addShareFromCompletedDownload(d);
		} catch (Exception e) {
			log.error("Error pausing download or starting share", e);
		}
		db.deleteDownload(streamId);
		startMoreDownloads();
	}

	private void startMoreDownloads() {
		int activeDls = 0;
		List<DownloadingTrack> dls = new ArrayList<DownloadingTrack>();
		synchronized (dPriority) {
			for (String streamId : dPriority) {
				DownloadingTrack d = getDownload(streamId);
				dls.add(d);
				if (d.getDownloadStatus() == DownloadStatus.Downloading && d.getDownloadRate() > 0)
					activeDls++;
			}
		}
		int numToStart = rbnb.getConfig().getMaxRunningDownloads() - activeDls;
		if (numToStart > 0) {
			for (DownloadingTrack d : dls) {
				if (d.getDownloadStatus() == DownloadStatus.Paused) {
					try {
						startDownload(d.getStream().getStreamId());
						numToStart--;
					} catch (RobonoboException e) {
						log.error("Error starting download", e);
					}
				}
				if (numToStart == 0)
					break;
			}
		}
	}

	public void receptionConnsChanged(String streamId) {
		// Do nothing
	}

	public void preFetch(String streamId) {
		DownloadingTrack d = getDownload(streamId);
		if(d == null)
			return;
		try {
		if(d.getDownloadStatus() == DownloadStatus.Paused)
			startDownload(streamId);
		} catch(RobonoboException e) {
			log.error("Caught exception starting pre-fetch download for stream "+streamId, e);
			return;
		}
		synchronized (dPriority) {
			preFetchStreamIds.add(streamId);
		}
		updatePriorities();
	}
	
	public void updatePriorities() {
		mina.clearStreamPriorities();
		// Currently playing/paused stream has priority
		String curStreamId = playback.getCurrentStreamId();
		synchronized (dPriority) {
			for (int i = 0; i < dPriority.size(); i++) {
				String streamId = dPriority.get(i);
				int newPri = dPriority.size() - i;
				if (preFetchStreamIds.contains(streamId))
					newPri = PRIORITY_NEXT;
				if (curStreamId != null && curStreamId.equals(streamId))
					newPri = PRIORITY_CURRENT;
				mina.setStreamPriority(streamId, newPri);
			}
		}
	}

	public void receptionStarted(String streamId) {
		// Do nothing
	}

	public void receptionStopped(String streamId) {
		// Do nothing
	}

	public String getName() {
		return "Download Service";
	}

	public String getProvides() {
		return "core.downloads";
	}

	public void broadcastStarted(String streamId) {
	}

	public void broadcastStopped(String streamId) {
	}

	public void minaStarted(MinaControl mina) {
	}

	public void minaStopped(MinaControl mina) {
	}

	public void nodeConnected(ConnectedNode node) {
	}

	public void nodeDisconnected(ConnectedNode node) {
	}

	@Override
	public void advisedOfTotalPages(PageBuffer pb) {
	}

	@Override
	public void gotPage(PageBuffer pb, long pageNum) {
		event.fireTrackUpdated(pb.getStreamId());
	}

}
