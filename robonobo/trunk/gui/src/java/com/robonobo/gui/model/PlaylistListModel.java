package com.robonobo.gui.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.DefaultListModel;

import com.robonobo.core.RobonoboController;

/**
 * TODO Make this actually do something
 * @author macavity
 *
 */
public class PlaylistListModel extends DefaultListModel {
	List<String> flarp = new ArrayList<String>(Arrays.asList("New playlist", "Playlist 001", "A really really really long playlist name", "Playlist For That Night", "I Hate This One", "DJ Mix FIFE!~"));
	RobonoboController control;

	public PlaylistListModel(RobonoboController control) {
		this.control = control;
	}
	
	@Override
	public Object getElementAt(int index) {
		return flarp.get(index);
	}

	@Override
	public int getSize() {
		return flarp.size();
	}
	
	public void removeElementAt(int index) {
		flarp.remove(index);
		fireIntervalRemoved(this, index, index);
	}
}
