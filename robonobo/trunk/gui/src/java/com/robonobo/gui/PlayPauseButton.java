package com.robonobo.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.Icon;
import javax.swing.SwingUtilities;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.robonobo.common.concurrent.CatchingRunnable;
import com.robonobo.core.RobonoboController;
import com.robonobo.core.api.NextTrackListener;
import com.robonobo.core.api.PlaybackListener;
import com.robonobo.core.api.RobonoboException;
import com.robonobo.core.api.model.CloudTrack;
import com.robonobo.core.api.model.Track;

@SuppressWarnings("serial")
public class PlayPauseButton extends MainButton implements PlaybackListener {
	private static final String IMG_PAUSE = "/img/Pause.png";
	private static final String IMG_PLAY = "/img/Play.png";
	private final Log log = LogFactory.getLog(getClass());
	private String playDesc = "Play selected tracks";
	private String pauseDesc = "Pause playback";
	private Icon playIcon = GUIUtils.createImageIcon(IMG_PLAY, playDesc);
	private Icon pauseIcon = GUIUtils.createImageIcon(IMG_PAUSE, pauseDesc);
	private boolean enabledIfStopped = true;

	private enum PlayState {
		Stopped, Playing, Paused
	};

	private PlayState state = PlayState.Stopped;
	RobonoboController controller;
	TrackListTablePanel tablePanel;
	TrackListTableModel tableModel;
	private NextTrackListener finishListener;

	public PlayPauseButton(RobonoboController controller, TrackListTablePanel tablePanel,
			TrackListTableModel tableModel, NextTrackListener finishListener) {
		super(IMG_PLAY, "Play selected tracks");
		this.controller = controller;
		controller.addPlaybackListener(this);
		this.tablePanel = tablePanel;
		this.tableModel = tableModel;
		this.finishListener = finishListener;
		addActionListener(new PlayPauseListener());
	}

	/**
	 * Make sure you only call this from the swing gui thread, or Bad Things
	 * will happen!
	 */
	public void play(List<String> streamIds) {
		controller.clearPlayQueue();
		for (String streamId : streamIds) {
			controller.addToPlayQueue(streamId);
		}
		doPlay();
	}

	/**
	 * Make sure you only call this from the swing gui thread, or Bad Things
	 * will happen!
	 */
	public void play(String streamId) {
		controller.clearPlayQueue();
		controller.addToPlayQueue(streamId);
		doPlay();
	}

	private void doPlay() {
		if (state == PlayState.Playing || state == PlayState.Paused)
			controller.stopPlayback();
		controller.getExecutor().execute(new CatchingRunnable() {
			public void doRun() throws Exception {
				controller.play(finishListener);
			}
		});
		tablePanel.clearTableSelection();
	}

	public void playbackStarting() {
		// As far as we're concerned, starting and playing are the same thing
		playbackStarted();
	}
	
	public void playbackStarted() {
		state = PlayState.Playing;
		setIcon(pauseIcon);
		setToolTipText(pauseDesc);
		updateVisibility();
	}

	public void playbackPaused() {
		state = PlayState.Paused;
		setIcon(playIcon);
		setToolTipText(playDesc);
		updateVisibility();
	}

	public void playbackStopped() {
		state = PlayState.Stopped;
		setIcon(playIcon);
		setToolTipText(playDesc);
		updateVisibility();
	}

	public void playbackCompleted() {
		state = PlayState.Stopped;
		setIcon(playIcon);
		setToolTipText(playDesc);
		updateVisibility();
	}

	public void playbackProgress(long microsecs) {
		// Do nothing
	}

	public void playbackError(String error) {
		// Do nothing
	}
	
	private List<Track> downloadSelected() {
		List<Track> selected = tablePanel.getSelectedTracks();
		for (Track t : selected) {
			if(t instanceof CloudTrack) {
				try {
					controller.addDownload(t.getStream().getStreamId());
				} catch (RobonoboException ex) {
					log.error("Caught exception adding Download", ex);
				}
			}
		}
		return selected;
	}

	public void setEnabledIfStopped(boolean enabledIfStopped) {
		this.enabledIfStopped = enabledIfStopped;
		updateVisibility();
	}

	private void updateVisibility() {
		SwingUtilities.invokeLater(new CatchingRunnable() {
			@Override
			public void doRun() throws Exception {
				setEnabled(!(state == PlayState.Stopped && !enabledIfStopped));
			}
		});
	}

	class PlayPauseListener implements ActionListener {
		public void actionPerformed(final ActionEvent e) {
			switch (state) {
			case Stopped:
				List<String> selSids = tablePanel.getSelectedStreamIds();
				if (selSids.size() > 0) {
					tablePanel.clearTableSelection();
					controller.spawnNecessaryDownloads(selSids);
					controller.clearPlayQueue();
					controller.stopPlayback();
					for (String sid : selSids) {
						controller.addToPlayQueue(sid);
					}
					controller.play(finishListener);
				}
				break;
			case Playing:
				controller.pause();
				break;
			case Paused:
				controller.play(null);
				break;
			}
		}
	}

}
