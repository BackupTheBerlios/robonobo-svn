package com.robonobo.gui.panels;

import static com.robonobo.gui.GUIUtils.*;
import static com.robonobo.gui.RoboColor.*;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.List;

import javax.swing.*;

import com.robonobo.common.concurrent.CatchingRunnable;
import com.robonobo.core.RobonoboController;
import com.robonobo.core.api.*;
import com.robonobo.core.api.model.*;
import com.robonobo.gui.RoboFont;
import com.robonobo.gui.components.PlaybackProgressBar;
import com.robonobo.gui.components.TrackList;
import com.robonobo.gui.frames.RobonoboFrame;

@SuppressWarnings("serial")
public class PlaybackPanel extends JPanel implements PlaybackListener, TrackListener, NextPrevListener {
	enum PlayState {
		Stopped, Playing, Paused
	};
	
	ImageIcon prevIcon = createImageIcon("/img/icon/play_back.png", "Previous track");
	ImageIcon nextIcon = createImageIcon("/img/icon/play_next.png", "Next track");
	ImageIcon dloadIcon = createImageIcon("/img/icon/play_next.png", "Download selected tracks");
	ImageIcon playIcon = createImageIcon("/img/icon/play_play.png", "Play selected tracks");
	ImageIcon pauseIcon = createImageIcon("/img/icon/play_pause.png", "Pause playback");
	
	RobonoboFrame frame;
	RobonoboController control;
	JLabel titleLbl, artistLbl, albumLbl;
	PlaybackProgressBar playbackProgress;
	Stream playingStream = null;
	PlayState state;
	JButton prevBtn, dloadBtn, playPauseBtn, nextBtn, delBtn;

	public PlaybackPanel(final RobonoboFrame frame) {
		this.frame = frame;
		control = frame.getController();
		setLayout(new BorderLayout());

		setName("playback.background.panel");
		setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		setBackground(MID_GRAY);
		JPanel titlesPanel = new JPanel();
		titlesPanel.setLayout(new BoxLayout(titlesPanel, BoxLayout.PAGE_AXIS));
		titlesPanel.setOpaque(false);
		add(titlesPanel, BorderLayout.CENTER);
		titleLbl = new JLabel("");
		titleLbl.setPreferredSize(new Dimension(450, 38));
		titleLbl.setMinimumSize(new Dimension(450, 38));
		titleLbl.setMaximumSize(new Dimension(450, 38));
		titleLbl.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 0));
		titleLbl.setFont(RoboFont.getFont(24, false));
		titleLbl.setForeground(BLUE_GRAY);
		titlesPanel.add(titleLbl);
		artistLbl = new JLabel("");
		artistLbl.setPreferredSize(new Dimension(450, 20));
		artistLbl.setBorder(BorderFactory.createEmptyBorder(1, 10, 0, 0));
		artistLbl.setFont(RoboFont.getFont(18, true));
		titlesPanel.add(artistLbl);
		albumLbl = new JLabel("");
		albumLbl.setPreferredSize(new Dimension(450, 20));
		albumLbl.setBorder(BorderFactory.createEmptyBorder(2, 12, 0, 0));
		albumLbl.setFont(RoboFont.getFont(16, false));
		titlesPanel.add(albumLbl);
		final JPanel playerPanel = new JPanel(new BorderLayout(5, 5));
		add(playerPanel, BorderLayout.EAST);
		playerPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

		playbackProgress = new PlaybackProgressBar(frame);
		playbackProgress.lock();
		playerPanel.add(playbackProgress, BorderLayout.NORTH);
		final JPanel playerCtrlPanel = new JPanel(new BorderLayout());
		playerPanel.add(playerCtrlPanel, BorderLayout.CENTER);
		playerCtrlPanel.setOpaque(false);
		final JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
		playerCtrlPanel.add(buttonsPanel, BorderLayout.WEST);
		buttonsPanel.setOpaque(false);
		
		prevBtn = new JButton();
		prevBtn.setName("robonobo.round.button");
		prevBtn.setIcon(prevIcon);
		prevBtn.setPreferredSize(new Dimension(50, 50));
		prevBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				frame.getController().previous();
			}
		});
		buttonsPanel.add(prevBtn);
		
		dloadBtn = new JButton();
		dloadBtn.setName("robonobo.round.button");
		dloadBtn.setIcon(dloadIcon);
		dloadBtn.setPreferredSize(new Dimension(50, 50));
		dloadBtn.setEnabled(false);
		buttonsPanel.add(dloadBtn);
		
		playPauseBtn = new JButton();
		playPauseBtn.setName("robonobo.round.button");
		playPauseBtn.setIcon(new ImageIcon(RobonoboFrame.class.getResource("/img/icon/play_play.png")));
		playPauseBtn.setPreferredSize(new Dimension(50, 50));
		playPauseBtn.addActionListener(new PlayPauseListener());
		buttonsPanel.add(playPauseBtn);
		
		nextBtn = new JButton();
		nextBtn.setName("robonobo.round.button");
		nextBtn.setIcon(nextIcon);
		nextBtn.setPreferredSize(new Dimension(50, 50));
		nextBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				frame.getController().next();
			}
		});
		buttonsPanel.add(nextBtn);
		
		buttonsPanel.add(Box.createHorizontalStrut(50));
		
		delBtn = new JButton();
		delBtn.setName("robonobo.exit.button");
		delBtn.setPreferredSize(new Dimension(40, 40));
		buttonsPanel.add(delBtn);
	}

	public void trackSelectionChanged() {
		checkButtonsEnabled();
		doRepaint();
	}
	
	@Override
	public void playbackStopped() {
		state = PlayState.Stopped;
		blank();
		playPauseBtn.setIcon(playIcon);
		checkButtonsEnabled();
		doRepaint();
	}

	@Override
	public void playbackStarting() {
		// As far as we're concerned, starting and playing are the same thing
		playbackStarted();
	}

	@Override
	public void playbackStarted() {
		state = PlayState.Playing;
		Stream s = frame.getController().currentPlayingStream();
		if (!s.equals(playingStream)) {
			synchronized (this) {
				playingStream = s;
				titleLbl.setText(s.getTitle());
				artistLbl.setText(s.getAttrValue("artist"));
				albumLbl.setText(s.getAttrValue("album"));
				playbackProgress.setTrackDuration(s.getDuration());
				playbackProgress.setTrackPosition(0);
				updateDataAvailable();
			}
		}
		playPauseBtn.setIcon(pauseIcon);
		checkButtonsEnabled();
		doRepaint();
	}

	@Override
	public synchronized void playbackProgress(long microsecs) {
		if (playingStream == null)
			return;
		playbackProgress.setTrackPosition(microsecs / 1000);
	}

	@Override
	public void playbackPaused() {
		state = PlayState.Paused;
		playPauseBtn.setIcon(playIcon);
		checkButtonsEnabled();
		doRepaint();
		// TODO Flash progress bar?
	}

	@Override
	public void playbackCompleted() {
		// For us, stopped and completed are the same thing
		// TODO Maybe for everybody...?
		playbackStopped();
	}

	@Override
	public void playbackError(String error) {
		frame.updateStatus("Playback Error", 10, 30);
	}

	@Override
	public void allTracksLoaded() {
		// Do nothing
	}

	@Override
	public synchronized void tracksUpdated(Collection<String> streamIds) {
		if (playingStream != null && streamIds.contains(playingStream.getStreamId()))
			updateDataAvailable();
	}

	@Override
	public synchronized void trackUpdated(String streamId) {
		if (playingStream != null && playingStream.getStreamId().equals(streamId))
			updateDataAvailable();
	}

	@Override
	public void updateNextPrev(final boolean canNext, final boolean canPrev) {
		SwingUtilities.invokeLater(new CatchingRunnable() {
			public void doRun() throws Exception {
				nextBtn.setEnabled(canNext);
				prevBtn.setEnabled(canPrev);
			}
		});
	}
	
	private void checkButtonsEnabled() {
		List<String> selStreamIds = frame.getMainPanel().currentContentPanel().getTrackList().getSelectedStreamIds();
		synchronized (this) {
			// Enable download button if there are any tracks selected
			dloadBtn.setEnabled(selStreamIds.size() > 0);
			// Enable play/pause button unless we are stopped and there are no tracks selected
			playPauseBtn.setEnabled(!(state == PlayState.Stopped && selStreamIds.size() == 0));
		}
	}
	
	private synchronized void updateDataAvailable() {
		Track t = frame.getController().getTrack(playingStream.getStreamId());
		float dataAvailable = 0;
		if (t instanceof SharedTrack)
			dataAvailable = 1f;
		else if (t instanceof DownloadingTrack) {
			DownloadingTrack dt = (DownloadingTrack) t;
			dataAvailable = (float) playingStream.getSize() / dt.getBytesDownloaded();
		} else
			dataAvailable = 0;
		playbackProgress.setDataAvailable(dataAvailable);
	}

	private void blank() {
		synchronized (this) {
			playingStream = null;
			titleLbl.setText(null);
			artistLbl.setText(null);
			albumLbl.setText(null);
			playbackProgress.lock();
		}
		doRepaint();
	}

	private void doRepaint() {
		RepaintManager.currentManager(this).markCompletelyDirty(this);
	}
	
	class PlayPauseListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			switch (state) {
			case Stopped:
				TrackList trackList = frame.getMainPanel().currentContentPanel().getTrackList();
				List<String> selSids = trackList.getSelectedStreamIds();
				if (selSids.size() > 0) {
					trackList.clearTableSelection();
					control.spawnNecessaryDownloads(selSids);
					control.clearPlayQueue();
					control.stopPlayback();
					for (String sid : selSids) {
						control.addToPlayQueue(sid);
					}
					control.play(trackList);
				}
				break;
			case Playing:
				control.pause();
				break;
			case Paused:
				control.play(null);
				break;		
			}
		}
	}
}
