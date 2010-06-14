package com.robonobo.gui.laf;

import info.clearthought.layout.TableLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;

@SuppressWarnings("serial")
public class SidebarContainerPanel extends JPanel {

	public SidebarContainerPanel() {
		double[][] cellSizen = { { 5, TableLayout.FILL, 5 }, { 5, 20, 5, TableLayout.FILL, 5, 40, 5, 10, 5, 50, 5 } };
		
		setLayout(new TableLayout(cellSizen));

		add(new TopLabelPanel(), "1,1");
		add(new SidebarScrollingPanel(), "1,3");
		add(new WangBalancePanel(), "1,5");
		add(new JLabel("<html>Status: <bold>Connected</bold></html"), "1,7");
		add(new NetworkStatusPanel(), "1,9");
	}

	// Top panel has 'music library' and a clickable link
	class TopLabelPanel extends JPanel {
		public TopLabelPanel() {
			double[][] cellSizen = { { TableLayout.FILL, 20 }, { TableLayout.FILL } };
			setLayout(new TableLayout(cellSizen));
			add(new JLabel("Music Library:"), "0,0");
			add(new JLabel("<html><a href=\"\">[+]</a></html>"), "1,0");
		}
	}
}
