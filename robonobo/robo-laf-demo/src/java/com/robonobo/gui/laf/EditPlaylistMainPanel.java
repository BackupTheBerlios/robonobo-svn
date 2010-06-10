package com.robonobo.gui.laf;

import java.util.HashMap;
import java.util.Map;

import info.clearthought.layout.TableLayout;

import javax.swing.JPanel;

@SuppressWarnings("serial")
public class EditPlaylistMainPanel extends JPanel {

	public EditPlaylistMainPanel() {
		double[][] cellSizen = { {10, TableLayout.FILL, 10}, {10, 80, 10, 20, 10, TableLayout.FILL, 15, 170} };
		setLayout(new TableLayout(cellSizen));
		add(new SearchPanel(), "1,3");
		add(new CurrentTrackPanel(), "1,1");
		add(new TrackListPanel(), "1,5");
		Map<String, JPanel> tabPanels = new HashMap<String, JPanel>();
		tabPanels.put("edit playlist", new EditPlaylistDetailsPanel());
		add(new MainTabsPanel(tabPanels), "1,7");
	}
	
}
