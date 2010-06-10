package com.robonobo.gui.laf;

import java.awt.GridLayout;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

public class MainTabsPanel extends JPanel {
	public MainTabsPanel(Map<String, JPanel> panels) {
		setBorder(BorderFactory.createEmptyBorder(0, 10, 20, 10));
		setLayout(new GridLayout(1, 0));
		JTabbedPane tabPane = new JTabbedPane();
		for (Entry<String, JPanel> panel : panels.entrySet()) {
			tabPane.addTab(panel.getKey(), panel.getValue());
		}
		add(tabPane, "0,0");
	}
}
