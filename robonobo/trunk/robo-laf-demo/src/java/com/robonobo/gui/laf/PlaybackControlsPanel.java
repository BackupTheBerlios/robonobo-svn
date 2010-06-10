package com.robonobo.gui.laf;

import info.clearthought.layout.TableLayout;

import javax.swing.JPanel;

public class PlaybackControlsPanel extends JPanel {
	public PlaybackControlsPanel() {
		double[][] cellSizen = { {10, 42, 5, 42, 5, 42, 5, 42, TableLayout.FILL, 42, 5}, {TableLayout.FILL} };
		setLayout(new TableLayout(cellSizen));
		
		PlaybackRoundButton prevBtn = new PlaybackRoundButton(GuiUtil.createImageIcon("/img/buttobn/previous.png", "Previous"));
		PlaybackRoundButton dloadBtn = new PlaybackRoundButton(GuiUtil.createImageIcon("/img/button/download.png", "Download"));
		PlaybackRoundButton playBtn = new PlaybackRoundButton(GuiUtil.createImageIcon("/img/button/play.png", "Play"));
		PlaybackRoundButton nextBtn = new PlaybackRoundButton(GuiUtil.createImageIcon("/img/button/next.png", "Play"));
		PlaybackSquareButton delBtn = new PlaybackSquareButton(GuiUtil.createImageIcon("/img/button/del.png", "Delete Track"));
		delBtn.setEnabled(false);
		
		add(prevBtn, "1,0");
		add(dloadBtn, "3,0");
		add(playBtn, "5,0");
		add(nextBtn, "7,0");
		add(delBtn, "9,0");
	}
}
