package com.robonobo.gui.laf;

import javax.swing.JProgressBar;

public class PlaybackProgressBar extends JProgressBar {
	public PlaybackProgressBar() {
		setMinimum(0);
		setMaximum(100);
		setValue(50);
		setString("2:30");
		setStringPainted(true);
	}
}
