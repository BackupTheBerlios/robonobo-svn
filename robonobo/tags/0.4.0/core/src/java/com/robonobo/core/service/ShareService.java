package com.robonobo.core.service;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.robonobo.common.concurrent.CatchingRunnable;
import com.robonobo.common.exceptions.SeekInnerCalmException;
import com.robonobo.common.util.FileUtil;
import com.robonobo.core.api.RobonoboException;
import com.robonobo.core.api.model.DownloadingTrack;
import com.robonobo.core.api.model.SharedTrack;
import com.robonobo.core.api.model.Stream;
import com.robonobo.core.api.model.DownloadingTrack.DownloadStatus;
import com.robonobo.core.api.model.SharedTrack.ShareStatus;
import com.robonobo.core.storage.StorageService;
import com.robonobo.mina.external.MinaControl;
import com.robonobo.mina.external.buffer.PageBuffer;
import com.robonobo.spi.FormatSupportProvider;

/**
 * Responsible for translating mina Broadcasts to robonobo Shares
 * 
 * @author macavity
 */
@SuppressWarnings("unchecked")
public class ShareService extends AbstractRuntimeServiceProvider {
	/** Seconds */
	// public static final int WATCHDIR_CHECK_FREQ = 60 * 10;
	public static final int WATCHDIR_CHECK_FREQ = 30;
	public static final int WATCHDIR_INITIAL_WAIT = 30;

	Log log = LogFactory.getLog(getClass());
	DbService db;
	EventService event;
	UserService users;
	StorageService storage;
	MetadataService metadata;
	PlaybackService playback;
	MinaControl mina;
	private Set<String> shareStreamIds;
	private ScheduledFuture<?> watchDirTask;
	private boolean watchDirRunning = false;

	public ShareService() {
		addHardDependency("core.mina");
		addHardDependency("core.metadata");
		addHardDependency("core.storage");
	}

	@Override
	public void startup() throws Exception {
		db = robonobo.getDbService();
		event = robonobo.getEventService();
		users = robonobo.getUsersService();
		storage = robonobo.getStorageService();
		metadata = robonobo.getMetadataService();
		playback = robonobo.getPlaybackService();
		mina = robonobo.getMina();
		// Keep track of our stream ids, everything else loaded on-demand from the db
		shareStreamIds = db.getShares();
		watchDirTask = getRobonobo().getExecutor().scheduleWithFixedDelay(new WatchDirChecker(), WATCHDIR_INITIAL_WAIT, WATCHDIR_CHECK_FREQ, TimeUnit.SECONDS);
	}

	@Override
	public void shutdown() {
		// Don't specifically stop our shares, the mina shutdown will stop them
		watchDirTask.cancel(true);
	}

	public void addShare(String streamId, File dataFile) throws RobonoboException {
		log.info("Adding share for id " + streamId + " at " + dataFile.getAbsolutePath());
		Stream s = metadata.getStream(streamId);
		SharedTrack sh = db.getShare(streamId);
		if (sh != null) {
			throw new RobonoboException("Share already added");
		}
		sh = new SharedTrack(s, dataFile, ShareStatus.Sharing);
		PageBuffer pb;
		try {
			pb = storage.createPageBufForBroadcast(sh.getStream(), sh.getFile());
			FormatSupportProvider fsp = robonobo.getFormatService().getFormatSupportProvider(s.getMimeType());
			if (fsp == null)
				throw new IOException("No FSP available for the mimeType " + s.getMimeType());
			log.debug("Paginating " + dataFile.getAbsolutePath());
			fsp.paginate(dataFile, pb);
		} catch (IOException e) {
			throw new RobonoboException(e);
		}
		db.putShare(sh);
		synchronized (this) {
			shareStreamIds.add(s.getStreamId());
		}
		startShare(streamId, pb);
		event.fireTrackUpdated(s.getStreamId());
		users.checkPlaylistsForNewShare(sh);
	}

	public void addShareFromCompletedDownload(DownloadingTrack d) throws RobonoboException {
		log.debug("Adding share for completed download " + d.getStream().getStreamId());
		if (d.getDownloadStatus() != DownloadStatus.Finished) {
			throw new SeekInnerCalmException();
		}
		SharedTrack sh = new SharedTrack(d.getStream(), d.getFile(), ShareStatus.Sharing);
		db.putShare(sh);
		synchronized (this) {
			shareStreamIds.add(d.getStream().getStreamId());
		}
		startShare(d.getStream().getStreamId(), d.getPageBuf());
		event.fireTrackUpdated(d.getStream().getStreamId());
		users.checkPlaylistsForNewShare(sh);
	}

	public void deleteShare(String streamId) {
		log.info("Deleting share for stream " + streamId);
		playback.stopIfCurrentlyPlaying(streamId);
		SharedTrack share = db.getShare(streamId);
		if(share == null)
			return;
		stopShare(streamId);
		db.deleteShare(streamId);
		storage.nukePageBuf(streamId);
		synchronized (this) {
			shareStreamIds.remove(streamId);
		}
		event.fireTrackUpdated(streamId);
	}

	private void startShare(String streamId, PageBuffer pb) throws RobonoboException {
		Stream s = db.getStream(streamId);
		if (s == null)
			metadata.putStream(s);
		mina.startBroadcast(s.getStreamId(), pb);
	}

	private void stopShare(String streamId) {
		mina.stopBroadcast(streamId);
	}

	public String getName() {
		return "Share service";
	}

	public String getProvides() {
		return "core.shares";
	}

	public SharedTrack getShare(String streamId) {
		synchronized (this) {
			if(!shareStreamIds.contains(streamId))
				return null;
		}
		return db.getShare(streamId);
	}
	
	public Collection<SharedTrack> getSharesByPattern(String searchPattern) {
		Collection<SharedTrack> shares = db.getSharesByPattern(searchPattern);
		return shares;
	}

	public SharedTrack getShareByFilePath(String filePath) {
		return db.getShareByFilePath(filePath);
	}

	public void checkWatchDir(File watchDir) throws RobonoboException {
		synchronized (this) {
			if (watchDirRunning)
				return;
			watchDirRunning = true;
		}
		try {
			log.debug("Checking watch dir " + watchDir.getAbsolutePath());
			for (File itemInWatchDir : watchDir.listFiles()) {
				if (!db.haveCheckedFile(itemInWatchDir)) {
					List<File> mp3z = FileUtil.getFilesWithinPath(itemInWatchDir, "mp3");
					for (File mp3File : mp3z) {
						log.debug("Adding share from file " + mp3File.getAbsolutePath());
						SharedTrack sh = getShareByFilePath(mp3File.getAbsolutePath());
						if (sh == null) {
							Stream s;
							try {
								s = getRobonobo().getFormatService().getStreamForFile(mp3File);
								metadata.putStream(s);
								addShare(s.getStreamId(), mp3File);
							} catch (Exception e) {
								log.error("Error adding file " + mp3File.getAbsolutePath() + ": " + e.getMessage());
								continue;
							}
						}
					}
					db.notifyFileChecked(itemInWatchDir);
				}
			}
		} finally {
			synchronized (this) {
				watchDirRunning = false;
			}
		}
	}

	void startAllShares() throws IOException, RobonoboException {
		log.debug("Start Share thread running");
		int i=0;
		// Copy out our stream ids so we can iterate while adding new shares
		String[] arr;
		synchronized (this) {
			arr = new String[shareStreamIds.size()];
			shareStreamIds.toArray(arr);
		}
		for(String streamId : arr) {
			PageBuffer pb = storage.loadPageBuf(streamId);
			if(pb == null) {
				// Errot
				log.error("Found null pagebuf when starting share for "+streamId+" - deleting share");
				db.deleteShare(streamId);
				continue;
			}
			startShare(streamId, pb);
			i++;
		}
		log.debug("Start Share thread finished: started "+i+" shares");
	}

	private class WatchDirChecker extends CatchingRunnable {
		public WatchDirChecker() {
		}

		public void doRun() throws Exception {
			List<File> watchDirs = db.getWatchDirs();
			for (File watchDir : watchDirs) {
				checkWatchDir(watchDir);
			}
		}
	}
}
