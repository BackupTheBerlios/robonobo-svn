package com.robonobo.gui.model;

import com.robonobo.core.RobonoboController;
import com.robonobo.core.api.model.Playlist;

@SuppressWarnings("serial")
public class PlaylistListModel extends SortedListModel<Playlist> {
	RobonoboController control;

	public PlaylistListModel(RobonoboController control) {
		this.control = control;
	}

	@Override
	public Object getElementAt(int index) {
		String title = get(index).getTitle();
		return title;
	}

	public Playlist getPlaylistAt(int index) {
		return get(index);
	}

	public int getPlaylistIndex(Playlist p) {
		return list.indexOf(p);
	}
}
