package com.robonobo.gui.laf;

import info.clearthought.layout.TableLayout;

import java.util.HashMap;
import java.util.Map;

import javax.swing.JPanel;

@SuppressWarnings("serial")
public class ViewPlaylistMainPanel extends JPanel {
	public ViewPlaylistMainPanel() {
		double[][] cellSizen = { {10, TableLayout.FILL, 10}, {10, 80, 10, 20, 10, TableLayout.FILL, 15, 170} };
		setLayout(new TableLayout(cellSizen));
		add(new SearchPanel(), "1,3");
		add(new CurrentTrackPanel(), "1,1");
		add(new TrackListPanel(), "1,5");
		Map<String, JPanel> tabPanels = new HashMap<String, JPanel>();
		tabPanels.put("playlist", new ViewPlaylistDetailsPanel());
		add(new MainTabsPanel(tabPanels), "1,7");
	}

}
