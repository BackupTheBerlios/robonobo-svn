package com.robonobo.gui.model;

import java.util.*;

import javax.swing.DefaultListModel;
import javax.swing.SwingUtilities;

import com.robonobo.common.concurrent.CatchingRunnable;
import com.robonobo.core.RobonoboController;
import com.robonobo.core.api.UserPlaylistListener;
import com.robonobo.core.api.model.Playlist;
import com.robonobo.core.api.model.User;

@SuppressWarnings("serial")
public class PlaylistListModel extends DefaultListModel implements UserPlaylistListener {
	List<Playlist> playlists = new ArrayList<Playlist>();
	
	RobonoboController control;

	public PlaylistListModel(RobonoboController control) {
		this.control = control;
		control.addUserPlaylistListener(this);
	}
	
	@Override
	public Object getElementAt(int index) {
		return playlists.get(index).getTitle();
	}

	@Override
	public int getSize() {
		return playlists.size();
	}
	
	public void removeElementAt(int index) {
		synchronized (this) {
			playlists.remove(index);
		}
		fireIntervalRemoved(this, index, index);
	}
	
	@Override
	public void loggedIn() {
		SwingUtilities.invokeLater(new CatchingRunnable() {
			public void doRun() throws Exception {
				synchronized (PlaylistListModel.this) {
					int oldSize = playlists.size();
					playlists.clear();
					if(oldSize > 0)
						fireIntervalRemoved(this, 0, oldSize-1);
				}
			}
		});
	}
	
	public Playlist getPlaylistAt(int index) {
		return playlists.get(index);
	}
	
	public synchronized int getPlaylistIndex(Playlist p) {
		// TODO Optimize me (?)
		return playlists.indexOf(p);
	}

	@Override
	public void userChanged(User u) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void playlistChanged(Playlist p) {
		// TODO Auto-generated method stub
		
	}
}
