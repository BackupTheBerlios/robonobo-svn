package com.robonobo.gui.panels;

import javax.swing.JPanel;

import org.debian.tablelayout.TableLayout;

@SuppressWarnings("serial")
public class MainPanel extends JPanel {
	public MainPanel() {
		double[][] cellSizen = { { TableLayout.FILL }, { 100, 10, TableLayout.FILL, 5, 175 } };
		setLayout(new TableLayout(cellSizen));
		add(new PlaybackPanel(), "0,0");
		add(new TrackTablePanel(), "0,2");
		add(new DetailsTabPanel(), "0,4");
	}	
}
