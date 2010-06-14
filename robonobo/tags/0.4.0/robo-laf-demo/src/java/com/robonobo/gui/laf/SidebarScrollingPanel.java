package com.robonobo.gui.laf;

import java.util.Arrays;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

@SuppressWarnings("serial")
public class SidebarScrollingPanel extends JScrollPane {
	public SidebarScrollingPanel() {
		setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_NEVER);
		setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_ALWAYS);
		setViewportView(new ListPanel());
	}
	
	class ListPanel extends JPanel {
		public ListPanel() {
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			SidebarTopLvlButton everyonesMusic = new SidebarTopLvlButton("Everyone's Music");
			everyonesMusic.setSelected(true);
			add(everyonesMusic);
			add(new SidebarUserItem("Geffen's", false));
			add(new SidebarUserItem("Will's", true));
			add(new SidebarPlaylistList(Arrays.asList("Playlist 001 [over]", "Playlist for That Night", "I Hate This One"), false));
			SidebarTopLvlButton myMusic = new SidebarTopLvlButton("My Music");
			myMusic.setSelected(false);
			add(myMusic);
			add(new SidebarPlaylistList(Arrays.asList("Playlist 001", "Playlist For That Night", "I Hate This One", "Dj Mix FIFE!~"), true));
		}
	}
}
