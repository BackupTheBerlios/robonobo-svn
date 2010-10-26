package com.robonobo.gui.panels;

import java.awt.CardLayout;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.robonobo.common.concurrent.CatchingRunnable;

@SuppressWarnings("serial")
public class ContentPanelHolder extends JPanel {
	private Map<String, ContentPanel> panels = new HashMap<String, ContentPanel>();
	String currentPanel;
	
	ContentPanelHolder() {
		setLayout(new CardLayout());
	}

	void addContentPanel(String name, ContentPanel panel) {
		add(panel, name);
		panels.put(name, panel);
		if(name.equals(currentPanel))
			selectContentPanel(name);
	}

	ContentPanel getContentPanel(String name) {
		return panels.get(name);
	}
	
	ContentPanel currentContentPanel() {
		if(currentPanel == null)
			return null;
		return panels.get(currentPanel);
	}
	
	ContentPanel removeContentPanel(String name) {
		ContentPanel panel = panels.remove(name);
		if(panel != null)
			remove(panel);
		return panel;
	}
	
	void selectContentPanel(final String panelName) {
		final CardLayout cl = (CardLayout) getLayout();
		currentPanel = panelName;
		if(SwingUtilities.isEventDispatchThread())
			cl.show(this, panelName);
		else {
			SwingUtilities.invokeLater(new CatchingRunnable() {
				public void doRun() throws Exception {
					cl.show(ContentPanelHolder.this, panelName);
				}
			});
		}
	}
}
