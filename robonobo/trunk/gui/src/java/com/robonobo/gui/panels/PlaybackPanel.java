package com.robonobo.gui.panels;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.robonobo.gui.RobonoboFont;
import com.robonobo.gui.components.PlaybackProgressBar;
import com.robonobo.gui.frames.RobonoboFrame;

public class PlaybackPanel extends JPanel {
	public PlaybackPanel() {
		setLayout(new BorderLayout());
		
		setName("playback.background.panel");
		setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		setBackground(Color.LIGHT_GRAY);
		final JPanel titlesPanel = new JPanel();
//		titlesPanel.setBorder(BorderFactory.createLineBorder(Color.RED));
		titlesPanel.setLayout(new BoxLayout(titlesPanel, BoxLayout.PAGE_AXIS));
		titlesPanel.setOpaque(false);
		add(titlesPanel, BorderLayout.CENTER);
		final JLabel headTitleLabel = new JLabel("Buenas Tardes Amigo");
		headTitleLabel.setPreferredSize(new Dimension(450, 3));
		headTitleLabel.setMinimumSize(new Dimension(450, 35));
		headTitleLabel.setMaximumSize(new Dimension(450, 35));
		headTitleLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 0));
		headTitleLabel.setFont(RobonoboFont.getFont(24, false));
		headTitleLabel.setForeground(new Color(0x0, 0x44, 0x71));
		titlesPanel.add(headTitleLabel);
		final JLabel artistLabel = new JLabel("Ween");
		artistLabel.setPreferredSize(new Dimension(450, 20));
		artistLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 0, 0));
		artistLabel.setFont(RobonoboFont.getFont(18, true));
		titlesPanel.add(artistLabel);
		JLabel albumLabel = new JLabel("Don't Shit Where You Eat");
		albumLabel.setPreferredSize(new Dimension(450, 20));
		albumLabel.setBorder(BorderFactory.createEmptyBorder(2, 12, 0, 0));
		albumLabel.setFont(RobonoboFont.getFont(16, false));
		titlesPanel.add(albumLabel);
		final JPanel playerPanel = new JPanel(new BorderLayout(5, 5));
		add(playerPanel, BorderLayout.EAST);
		playerPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		final PlaybackProgressBar progressBar = new PlaybackProgressBar();
		progressBar.setPreferredSize(new Dimension(305, 24));
		progressBar.addListener(new PlaybackProgressBar.Listener() {
			public void sliderFinishedMoving() {
			}

			public void sliderMoved(int newProgress) {
				final int totalSec = progressBar.getValue();
				final int hours = totalSec / 3600;
				final int minutes = (totalSec % 3600) / 60;
				final int seconds = (totalSec % 60);
				if (hours > 0) {
					progressBar.setSliderText(String.format("%d:%02d:%02d", hours, minutes, seconds));
				} else {
					progressBar.setSliderText(String.format("%02d:%02d", minutes, seconds));
				}
			}
		});
//		progressBar.setMaximum(335);
//		progressBar.setValue(0);
		progressBar.setStartText("");
		playerPanel.add(progressBar, BorderLayout.NORTH);
		final JPanel playerCtrlPanel = new JPanel(new BorderLayout());
		playerPanel.add(playerCtrlPanel, BorderLayout.CENTER);
		playerCtrlPanel.setOpaque(false);
		final JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
		playerCtrlPanel.add(buttonsPanel, BorderLayout.WEST);
		buttonsPanel.setOpaque(false);
		final JButton backButton = new JButton();
		backButton.setName("robonobo.round.button");
		backButton.setIcon(new ImageIcon(RobonoboFrame.class.getResource("/img/icon/play_back.png")));
		backButton.setPreferredSize(new Dimension(50, 50));
		buttonsPanel.add(backButton);
		final JButton ejectButton = new JButton();
		ejectButton.setName("robonobo.round.button");
		ejectButton.setIcon(new ImageIcon(RobonoboFrame.class.getResource("/img/icon/play_eject.png")));
		ejectButton.setPreferredSize(new Dimension(50, 50));
		ejectButton.setEnabled(false);
		buttonsPanel.add(ejectButton);
		final JButton playButton = new JButton();
		playButton.setName("robonobo.round.button");
		playButton.setIcon(new ImageIcon(RobonoboFrame.class.getResource("/img/icon/play_play.png")));
		playButton.setPreferredSize(new Dimension(50, 50));
		buttonsPanel.add(playButton);
		final JButton nextButton = new JButton();
		nextButton.setName("robonobo.round.button");
		nextButton.setIcon(new ImageIcon(RobonoboFrame.class.getResource("/img/icon/play_next.png")));
		nextButton.setPreferredSize(new Dimension(50, 50));
		buttonsPanel.add(nextButton);
		buttonsPanel.add(Box.createHorizontalStrut(50));
		final JButton closeButton = new JButton();
		closeButton.setName("robonobo.exit.button");
		closeButton.setPreferredSize(new Dimension(40, 40));
		buttonsPanel.add(closeButton);
	}
}
