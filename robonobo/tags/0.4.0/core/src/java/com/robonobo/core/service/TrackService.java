package com.robonobo.core.service;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.robonobo.common.concurrent.CatchingRunnable;
import com.robonobo.core.api.TransferSpeed;
import com.robonobo.core.api.AudioPlayer.Status;
import com.robonobo.core.api.model.CloudTrack;
import com.robonobo.core.api.model.Stream;
import com.robonobo.core.api.model.Track;
import com.robonobo.core.api.model.Track.PlaybackStatus;
import com.robonobo.mina.external.MinaControl;

/**
 * First point of call to get information about a track, whether we're sharing it, downloading it or it just exists in the cloud.
 * 
 * Get shares and downloads from here rather than Share/Download service as they will then include info on whether they're playing, and their transfer speeds
 * 
 * @author macavity
 * 
 */
public class TrackService extends AbstractRuntimeServiceProvider {
	Log log = LogFactory.getLog(getClass());
	private ShareService share;
	private DownloadService download;
	private MetadataService metadata;
	private PlaybackService playback;
	private EventService event;
	private MinaControl mina;
	private ScheduledFuture<?> transferSpeedsTask;
	protected Map<String, TransferSpeed> transferSpeeds = null;
	private String currentPlayingStreamId = null;
	private boolean allSharesStarted;

	public TrackService() {
		addHardDependency("core.shares");
		addHardDependency("core.downloads");
	}

	public String getName() {
		return "Track Service";
	}

	public String getProvides() {
		return "core.tracks";
	}

	@Override
	public void startup() throws Exception {
		share = robonobo.getShareService();
		download = robonobo.getDownloadService();
		metadata = robonobo.getMetadataService();
		playback = robonobo.getPlaybackService();
		event = robonobo.getEventService();
		mina = robonobo.getMina();
		// Fork off a thread to start shares, we might have a lot... do this here rather than in shareservice as we start after them and we need to be present
		// to fire allSharesStarted
		log.debug("Spawning thread to start shares");
		getRobonobo().getExecutor().execute(new CatchingRunnable() {
			@Override
			public void doRun() throws Exception {
				share.startAllShares();
				allSharesStarted = true;
				event.fireAllTracksLoaded();
			}
		});
		transferSpeedsTask = getRobonobo().getExecutor().scheduleAtFixedRate(new CatchingRunnable() {
			public void doRun() throws Exception {
				// We fire the updated event for all streams that were in the
				// previous set of speeds and this set, so they get reset to 0
				Set<String> changedStreamIds = new HashSet<String>();
				synchronized (TrackService.this) {
					if (transferSpeeds != null)
						changedStreamIds.addAll(transferSpeeds.keySet());
					transferSpeeds = mina.getTransferSpeeds();
					changedStreamIds.addAll(transferSpeeds.keySet());
				}
				event.fireTracksUpdated(changedStreamIds);
			}
		}, 1, 1, TimeUnit.SECONDS);
	}

	@Override
	public void shutdown() throws Exception {
		transferSpeedsTask.cancel(true);
	}

	/**
	 * Don't hang onto the object you get returned from here - implement TrackListener, and look it up every time instead. That way we keep ram usage down and
	 * you always have the correct status (tracks start off as CloudTracks, then become DownloadingTracks, then SharedTracks)
	 */
	public Track getTrack(String streamId) {
		Track result;
		// Are we sharing this track?
		result = share.getShare(streamId);
		if (result == null) {
			// Are we downloading this track?
			result = download.getDownload(streamId);
			if (result == null) {
				// Nope - it's in the cloud
				Stream s = metadata.getStream(streamId);
				if (s == null)
					return null;
				result = new CloudTrack(s, mina.numSources(streamId));
			}
		}
		// Set playback status (might be already there if downloading, if necessary override it)
		String playingSid = playback.getCurrentStreamId();
		if (playingSid != null && playingSid.equals(streamId)) {
			switch(playback.getStatus()) {
			case Starting:
				result.setPlaybackStatus(PlaybackStatus.Starting);
				break;
			case Playing:
				result.setPlaybackStatus(PlaybackStatus.Playing);
				break;
			case Paused:
				result.setPlaybackStatus(PlaybackStatus.Paused);
				break;
			}
		}
		// Set transfer speeds
		synchronized (this) {
			if (transferSpeeds != null && transferSpeeds.containsKey(streamId)) {
				TransferSpeed ts = transferSpeeds.get(streamId);
				result.setRates(ts.getDownload(), ts.getUpload());
			}
		}
		return result;
	}

	public void notifyPlayingTrackChange(String newPlayingStreamId) {
		if (currentPlayingStreamId != null)
			event.fireTrackUpdated(currentPlayingStreamId);
		if (newPlayingStreamId != null)
			event.fireTrackUpdated(newPlayingStreamId);
		currentPlayingStreamId = newPlayingStreamId;
	}

	public boolean haveAllSharesStarted() {
		return allSharesStarted;
	}
}
