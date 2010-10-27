package com.robonobo.gui.panels;

import javax.swing.JPanel;

import org.debian.tablelayout.TableLayout;

import com.robonobo.gui.frames.RobonoboFrame;

@SuppressWarnings("serial")
public class MainPanel extends JPanel {
	private PlaybackPanel playbackPanel;
	private ContentPanelHolder cpHolder;

	public MainPanel(RobonoboFrame frame) {
		double[][] cellSizen = { { TableLayout.FILL }, { 100, 10, TableLayout.FILL } };
		setLayout(new TableLayout(cellSizen));
		playbackPanel = new PlaybackPanel(frame);
		add(playbackPanel, "0,0");
		cpHolder = new ContentPanelHolder();
		add(cpHolder, "0,2");
		addContentPanel("mymusiclibrary", new MyMusicLibraryContentPanel(frame));
		selectContentPanel("mymusiclibrary");
	}
	
	public void addContentPanel(String name, ContentPanel panel) {
		cpHolder.addContentPanel(name, panel);
	}

	public ContentPanel currentContentPanel() {
		return cpHolder.currentContentPanel();
	}
	
	public ContentPanel getContentPanel(String name) {
		return cpHolder.getContentPanel(name);
	}
	
	public void selectContentPanel(String name) {
		cpHolder.selectContentPanel(name);
	}

	public ContentPanel removeContentPanel(String panelName) {
		return cpHolder.removeContentPanel(panelName);
	}
	
	public PlaybackPanel getPlaybackPanel() {
		return playbackPanel;
	}
}
