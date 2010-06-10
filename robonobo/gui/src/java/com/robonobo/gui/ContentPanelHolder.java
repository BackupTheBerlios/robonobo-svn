package com.robonobo.gui;

import java.awt.CardLayout;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JPanel;

class ContentPanelHolder extends JPanel {
	private Map<String, ContentPanel> panels = new HashMap<String, ContentPanel>();
	String currentPanel;
	
	ContentPanelHolder(RobonoboFrame robonoboFrame) {
		setLayout(new CardLayout());
	}

	void addContentPanel(String name, ContentPanel panel) {
		add(panel, name);
		panels.put(name, panel);
		if(name.equals(currentPanel))
			bringPanelToFront(name);
	}

	ContentPanel getContentPanel(String name) {
		return panels.get(name);
	}
	
	void removeContentPanel(String name) {
		ContentPanel panel = panels.remove(name);
		if(panel != null)
			remove(panel);
	}
	
	void bringPanelToFront(String panelName) {
		CardLayout cl = (CardLayout) getLayout();
		cl.show(this, panelName);
		panels.get(panelName).focus();
		currentPanel = panelName;
	}
}
