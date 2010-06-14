package com.robonobo.core.service;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.robonobo.common.exceptions.SeekInnerCalmException;
import com.robonobo.common.util.TimeUtil;
import com.robonobo.core.api.AudioPlayer;
import com.robonobo.core.api.AudioPlayerListener;
import com.robonobo.core.api.NextTrackListener;
import com.robonobo.core.api.RobonoboException;
import com.robonobo.core.api.StreamVelocity;
import com.robonobo.core.api.AudioPlayer.Status;
import com.robonobo.core.api.model.CloudTrack;
import com.robonobo.core.api.model.DownloadingTrack;
import com.robonobo.core.api.model.Stream;
import com.robonobo.core.api.model.Track;
import com.robonobo.core.api.model.DownloadingTrack.DownloadStatus;
import com.robonobo.mina.external.MinaControl;
import com.robonobo.mina.external.buffer.PageBuffer;
import com.robonobo.mina.external.buffer.PageBufferListener;
import com.robonobo.mina.external.buffer.PageInfo;

public class PlaybackService extends AbstractRuntimeServiceProvider implements AudioPlayerListener, PageBufferListener {
	private AudioPlayer.Status status = Status.Stopped;

	/**
	 * If we're within this time (secs) after the start of a track, calling
	 * prev() goes to the previous track (otherwise, returns to the start of the
	 * current one)
	 */
	public static final int PREV_TRACK_GRACE_PERIOD = 5;
	/**
	 * How much data do we need before we start playing?
	 */
	public static final int BYTES_BUFFERED_DATA = 256000;
	private AudioPlayer player;
	private List<String> playQueue = new LinkedList<String>();
	private int playQueuePosition = -1;
	private Date playStartTime;
	private NextTrackListener finishListener = null;
	private final Log log = LogFactory.getLog(getClass());
	private EventService event;
	private TrackService tracks;
	private DownloadService download;
	private MinaControl mina;
	String currentStreamId;
	String nextStreamId;

	public PlaybackService() {
		addHardDependency("core.tracks");
	}

	@Override
	public void startup() throws Exception {
		event = robonobo.getEventService();
		tracks = robonobo.getTrackService();
		download = robonobo.getDownloadService();
		mina = robonobo.getMina();
	}

	@Override
	public void shutdown() throws Exception {
		stop();
	}

	public synchronized void addToPlayQueue(String streamId) {
		playQueue.add(streamId);
	}

	public synchronized void addToPlayQueue(Collection<String> streamIds) {
		playQueue.addAll(streamIds);
	}

	public synchronized void clearQueue() {
		// This only clears the queue from our current position forwards -
		// history is preserved
		while (playQueue.size() > playQueuePosition + 1)
			playQueue.remove(playQueuePosition + 1);
	}

	public Status getStatus() {
		return status;
	}

	/**
	 * 
	 */
	public synchronized void play() {
		// If we have an existent audio player, just set it playing
		if (player != null) {
			try {
				player.play();
				status = Status.Playing;
			} catch (IOException e) {
				log.error("Caught exception restarting playback", e);
			}
			tracks.notifyPlayingTrackChange(currentStreamId);
			event.firePlaybackStarted();
			return;
		}
		// If we are paused but with no player, we never got enough data to
		// start last time - don't move to the next stream, try this one again
		if (status != Status.Paused) {
			playQueuePosition++;
			if (playQueuePosition >= playQueue.size()) {
				playQueuePosition--;
				currentStreamId = null;
				return;
			}
			// Tell our listeners if we can now do next or previous
			boolean canNext = playQueuePosition < playQueue.size() - 1;
			boolean canPrev = playQueuePosition > 0;
			event.fireNextPrevChanged(canNext, canPrev);
		}
		currentStreamId = playQueue.get(playQueuePosition);
		Track t = tracks.getTrack(currentStreamId);
		if (t instanceof CloudTrack) {
			// Whoops, we're not sharing/downloading this stream...
			// download it!
			try {
				download.addDownload(currentStreamId);
			} catch (RobonoboException e) {
				log.error("Error adding download", e);
				return;
			}
		}
		// If this is a download, make sure it's running and is highest
		// priority/velocity
		if (t instanceof DownloadingTrack) {
			DownloadingTrack d = (DownloadingTrack) t;
			if (d.getDownloadStatus() == DownloadStatus.Paused) {
				try {
					download.startDownload(currentStreamId);
				} catch (RobonoboException e) {
					log.error("Error adding download", e);
					return;
				}
			}
			// Make sure the playing stream is the highest priority
			download.updatePriorities();
			mina.setStreamVelocity(currentStreamId, StreamVelocity.MaxRate);
			mina.setAllStreamVelocitiesExcept(currentStreamId, StreamVelocity.LowestCost);
		}
		PageBuffer pb = mina.getPageBuffer(currentStreamId);
		if (pb == null)
			throw new SeekInnerCalmException();
		// If we already have some of this stream, start playing it straight
		// away, otherwise ask it to notify us when it gets data, and start
		// playing
		Stream s = robonobo.getMetadataService().getStream(currentStreamId);
		boolean bufferedEnough = bufferedEnough(s, pb);
		if (bufferedEnough) {
			startPlaying(s, pb);
		} else {
			status = Status.Starting;
			event.firePlaybackStarting();
			tracks.notifyPlayingTrackChange(currentStreamId);
			pb.addListener(this);
		}
	}

	public String getNextStreamId() {
		return nextStreamId;
	}

	public void advisedOfTotalPages(PageBuffer pb) {
		// Do nothing
	}

	/**
	 * Called by the pagebuffer when it receives a page - check to see if we
	 * have enough data, and start playing if so
	 */
	public void gotPage(final PageBuffer pb, long pageNum) {
		Stream s = robonobo.getMetadataService().getStream(currentStreamId);
		if (currentStreamId.equals(pb.getStreamId())) {
			if (bufferedEnough(s, pb)) {
				pb.removeListener(this);
				startPlaying(s, pb);
			}
		} else {
			// We're playing another stream now, i don't want to hear from this
			// guy any more
			pb.removeListener(this);
		}
	}

	private void startPlaying(Stream s, PageBuffer pb) {
		synchronized (this) {
			player = getRobonobo().getFormatService().getFormatSupportProvider(s.getMimeType()).getAudioPlayer(s, pb, getRobonobo().getExecutor());
			player.addListener(this);
			try {
				player.play();
			} catch (IOException e) {
				log.error("Caught exception starting playback for " + s, e);
				player = null;
				status = Status.Stopped;
				return;
			}
		}
		status = Status.Playing;
		playStartTime = TimeUtil.now();
		log.info("Started playback for " + s);
		tracks.notifyPlayingTrackChange(currentStreamId);
		event.firePlaybackStarted();
	}

	// Do we have enough buffered data to start playing?
	private boolean bufferedEnough(Stream s, PageBuffer pb) {
		if (pb.isComplete())
			return true;
		int bytesData = 0;
		for (long pn = 0; pn < Integer.MAX_VALUE; pn++) {
			PageInfo pi = getRobonobo().getStorageService().getPageInfo(s.getStreamId(), pn);
			if (pi == null)
				return false;
			bytesData += pi.getLength();
			if (bytesData >= BYTES_BUFFERED_DATA)
				return true;
		}
		throw new SeekInnerCalmException();
	}

	/**
	 * Returns the current stream that is playing/paused, or null if none
	 */
	public String getCurrentStreamId() {
		return currentStreamId;
	}

	public synchronized void pause() {
		synchronized (this) {
			if (status == Status.Starting) {
				// We don't have a player yet, we're waiting for feedback to be
				// buffered - remove ourselves as a listener
				mina.getPageBuffer(currentStreamId).removeListener(this);
			}
			if (player != null) {
				try {
					player.pause();
				} catch (IOException e) {
					log.error("Error pausing", e);
					stop();
					return;
				}
			}
			status = Status.Paused;
		}
		tracks.notifyPlayingTrackChange(currentStreamId);
		event.firePlaybackPaused();
	}

	public synchronized void seek(long ms) {
		if (player != null) {
			try {
				player.seek(ms);
			} catch (IOException e) {
				log.error("Error seeking", e);
				stop();
				return;
			}
		}
	}

	/** If we are playing, pause. If we are paused, play. Otherwise, do nothing */
	public synchronized void togglePlayPause() {
		if (player == null)
			return;
		switch (status) {
		case Paused:
			play();
			break;
		case Playing:
			pause();
			break;
		}
	}

	public void stop() {
		String stoppedStreamId;
		synchronized (this) {
			if (player != null) {
				player.stop();
			}
			player = null;
			status = Status.Stopped;
			stoppedStreamId = currentStreamId;
			currentStreamId = null;
		}
		tracks.notifyPlayingTrackChange(stoppedStreamId);
		event.firePlaybackStopped();
	}

	public synchronized void next() {
		stop();
		play();
	}

	public synchronized void previous() {
		stop();
		if (TimeUtil.timeInPast(PREV_TRACK_GRACE_PERIOD * 1000).before(playStartTime)) {
			// Prev track
			if (playQueue.size() > 1)
				playQueuePosition -= 2;
			else
				throw new SeekInnerCalmException();
		} else
			// Restart current track
			playQueuePosition--;
		play();
	}

	public void onCompletion() {
		synchronized (this) {
			player = null;
			status = Status.Stopped;
		}
		log.debug("Finished playback");
		String justFinStreamId = currentStreamId;
		tracks.notifyPlayingTrackChange(justFinStreamId);
		event.firePlaybackCompleted();
		synchronized (this) {
			currentStreamId = null;
			if (playQueue.size() == playQueuePosition + 1) {
				if (finishListener != null) {
					String nextSid = finishListener.getNextTrack(justFinStreamId);
					if (nextSid != null) {
						addToPlayQueue(nextSid);
						play();
					}
				}
			} else
				play();
		}
	}

	public void onError(String error) {
		String errStreamId = currentStreamId;
		String myErr = "Got playback error while playing " + errStreamId + ": " + error;
		log.debug(myErr);
		event.firePlaybackError(myErr);
		// If we are downloading this, stop at once, it's fux0red
		DownloadingTrack d = robonobo.getDbService().getDownload(errStreamId);
		if (d != null) {
			try {
				download.deleteDownload(errStreamId);
			} catch (RobonoboException e) {
				log.error("Caught exception while stopping download", e);
			}
		}
		player = null;
		synchronized (this) {
			currentStreamId = null;
			if (playQueue.size() == playQueuePosition + 1) {
				if (finishListener != null) {
					String nextSid = finishListener.getNextTrack(errStreamId);
					if (nextSid != null) {
						addToPlayQueue(nextSid);
						play();
					}
				}
			} else
				play();
		}
	}

	public synchronized void stopIfCurrentlyPlaying(String streamId) {
		if (player != null && playQueue.get(playQueuePosition).equals(streamId))
			stop();
	}

	public String getName() {
		return "Playback service";
	}

	public String getProvides() {
		return "core.playback";
	}

	public void onProgress(long microsecs) {
		// The player calls this with microsecs=0 when we pause, just ignore it
		if (microsecs == 0)
			return;
		event.firePlaybackProgress(microsecs);
		// Cue up the next track if necessary
		Stream currentStream = robonobo.getMetadataService().getStream(currentStreamId);
		// This might be null if we are a left-over thread, just exit
		if (currentStream == null)
			return;
		long msLeft = currentStream.getDuration() - (microsecs / 1000);
		if (msLeft < (getRobonobo().getConfig().getDownloadCacheTime() * 1000)) {
			nextStreamId = finishListener.getNextTrack(currentStream.getStreamId());
			if (nextStreamId != null) {
				// If we don't have this track, better fetch it
				Track t = tracks.getTrack(nextStreamId);
				if (t instanceof CloudTrack)
					preFetchTrack(t.getStream());
				// If we're downloading this track, move it up in priority so we
				// get it faster
				else if (t instanceof DownloadingTrack) {
					DownloadingTrack d = (DownloadingTrack) t;
					if (d.getDownloadStatus() == DownloadStatus.Paused) {
						try {
							download.startDownload(nextStreamId);
						} catch (RobonoboException e) {
							log.error("Error starting pre-fetch download", e);
						}
					}
					download.updatePriorities();
					mina.setStreamVelocity(nextStreamId, StreamVelocity.MaxRate);
				}
			}
		}
	}

	private void preFetchTrack(Stream s) {
		log.info("Pre-fetching next track '" + s.getTitle() + "'");
		try {
			download.addDownload(s.getStreamId());
			DownloadingTrack d = download.getDownload(s.getStreamId());
			if (d.getDownloadStatus() == DownloadStatus.Paused)
				download.startDownload(s.getStreamId());
		} catch (RobonoboException e) {
			log.error("Caught exception pre-caching download", e);
		}
	}

	public void setFinishListener(NextTrackListener finishListener) {
		this.finishListener = finishListener;
	}

	public AudioPlayer.Status playbackStatus() {
		if (player == null)
			return Status.Stopped;
		return player.getStatus();
	}
}
