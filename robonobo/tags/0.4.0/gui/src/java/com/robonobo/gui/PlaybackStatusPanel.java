package com.robonobo.gui;

import info.clearthought.layout.TableLayout;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.robonobo.common.concurrent.CatchingRunnable;
import com.robonobo.common.util.TimeUtil;
import com.robonobo.core.Platform;
import com.robonobo.core.RobonoboController;
import com.robonobo.core.api.PlaybackListener;
import com.robonobo.core.api.model.Stream;

@SuppressWarnings("serial")
public class PlaybackStatusPanel extends JPanel implements PlaybackListener {
	private RobonoboFrame frame;
	private RobonoboController controller;
	private MarqueeLabel titleLbl, artistLbl;
	private JLabel playPauseLbl, timeLbl;
	private ImageIcon playingIcon = GUIUtils.createImageIcon("/img/table/play.png", null);
	private ImageIcon pausedIcon = GUIUtils.createImageIcon("/img/table/pause.png", null);
	private String playingStreamId = null;
	private Object lastProgressStr;
	private String trackLength;

	public PlaybackStatusPanel(RobonoboFrame frame, final TrackListTablePanel tablePanel) {
		this.frame = frame;
		this.controller = frame.getController();

		int progressLblWidth = Platform.getPlatform().getTrackProgressLabelWidth();
		int artistLblWidth = 175 - progressLblWidth;
		double[][] cellSizen = { { 10, artistLblWidth, 5, progressLblWidth, 10 }, { 0, 30, 15, 25, 10 } };
		setLayout(new TableLayout(cellSizen));
		setBackground(Color.WHITE);
		setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
		titleLbl = new MarqueeLabel("", controller.getExecutor());
		titleLbl.setFont(new Font("serif", Font.PLAIN, 16));
		add(titleLbl, "1,1,3,1");
		artistLbl = new MarqueeLabel("", controller.getExecutor());
		artistLbl.setFont(new Font("serif", Font.PLAIN, 12));
		add(artistLbl, "1, 3");
		playPauseLbl = new JLabel();
		playPauseLbl.setHorizontalAlignment(JLabel.RIGHT);
		add(playPauseLbl, "3, 2");
		timeLbl = new JLabel("");
		timeLbl.setFont(new Font("serif", Font.PLAIN, 16));
		timeLbl.setHorizontalAlignment(JLabel.RIGHT);
		add(timeLbl, "3, 3");

		controller.addPlaybackListener(this);
		Stream s = controller.currentPlayingStream();
		if (s != null) {
			titleLbl.setText(s.getTitle());
			artistLbl.setText(s.getAttrValue("artist"));
			playPauseLbl.setIcon(playingIcon);
			trackLength = TimeUtil.minsSecsFromMs(s.getDuration());
			timeLbl.setText("0:00/" + trackLength);
			playingStreamId = s.getStreamId();
		}

		// Double clicking on me makes the tablepanel scroll to the
		// currently-playing track
		addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2)
					if (playingStreamId != null)
						tablePanel.scrollTableToStream(playingStreamId);
				super.mouseClicked(e);
			}
		});
	}

	public void playbackCompleted() {
		blank();
	}

	public void playbackPaused() {
		playPauseLbl.setIcon(pausedIcon);
	}

	public void playbackStarting() {
		// For now do the same thing as started, but we might want to do something different - flash the 0:00 time maybe?
		playbackStarted();
	}
	
	public void playbackStarted() {
		Stream s = controller.currentPlayingStream();
		titleLbl.setText(s.getTitle());
		artistLbl.setText(s.getAttrValue("artist"));
		playPauseLbl.setIcon(playingIcon);
		if (!s.getStreamId().equals(playingStreamId)) {
			trackLength = TimeUtil.minsSecsFromMs(s.getDuration());
			timeLbl.setText("0:00/" + trackLength);
			playingStreamId = s.getStreamId();
			doRepaint();
		}
	}

	public void playbackStopped() {
		blank();
	}

	public void playbackProgress(long microsecs) {
		if (playingStreamId == null)
			return;
		String progressStr = TimeUtil.minsSecsFromMs(microsecs / 1000);
		if (!progressStr.equals(lastProgressStr)) {
			lastProgressStr = progressStr;
			timeLbl.setText(progressStr + "/" + trackLength);
			doRepaint();
		}
	}

	public void playbackError(String error) {
		frame.updateStatus(error, 5, 30);
	}

	private void blank() {
		titleLbl.setText(null);
		artistLbl.setText(null);
		playPauseLbl.setIcon(null);
		timeLbl.setText(null);
		playingStreamId = null;
		doRepaint();
	}

	private void doRepaint() {
		SwingUtilities.invokeLater(new CatchingRunnable() {
			public void doRun() throws Exception {
				repaint();
			}
		});
	}
}
