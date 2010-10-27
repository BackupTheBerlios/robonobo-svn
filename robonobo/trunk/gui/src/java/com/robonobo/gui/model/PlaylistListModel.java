package com.robonobo.gui.model;

import java.util.HashMap;
import java.util.Map;

import javax.swing.SwingUtilities;

import com.robonobo.common.concurrent.CatchingRunnable;
import com.robonobo.core.RobonoboController;
import com.robonobo.core.api.UserPlaylistListener;
import com.robonobo.core.api.model.Playlist;
import com.robonobo.core.api.model.User;

@SuppressWarnings("serial")
public class PlaylistListModel extends SortedListModel<Playlist> implements UserPlaylistListener {
	RobonoboController control;
	// Keep track of playlists here so we can remove them as necessary
	Map<String, Playlist> pMap = new HashMap<String, Playlist>();

	public PlaylistListModel(RobonoboController control) {
		this.control = control;
		control.addUserPlaylistListener(this);
	}

	@Override
	public Object getElementAt(int index) {
		return get(0).getTitle();
	}

	public Playlist getPlaylistAt(int index) {
		return get(index);
	}

	public synchronized int getPlaylistIndex(Playlist p) {
		// TODO Optimize me (?)
		return list.indexOf(p);
	}

	@Override
	public void loggedIn() {
		SwingUtilities.invokeLater(new CatchingRunnable() {
			public void doRun() throws Exception {
				synchronized (PlaylistListModel.this) {
					clear();
					pMap.clear();
				}
			}
		});
	}

	@Override
	public void userChanged(final User u) {
		SwingUtilities.invokeLater(new CatchingRunnable() {
			public void doRun() throws Exception {
				synchronized (PlaylistListModel.this) {
					if (u.getUserId() == control.getMyUser().getUserId()) {
						// Check for removed playlists
						for (String oldPlId : pMap.keySet()) {
							if (!u.getPlaylistIds().contains(oldPlId)) {
								Playlist p = pMap.get(oldPlId);
								if (p != null)
									remove(p);
							}
						}
						pMap.clear();
					}
				}
			}
		});
	}

	@Override
	public void playlistChanged(final Playlist p) {
		SwingUtilities.invokeLater(new CatchingRunnable() {
			public void doRun() throws Exception {
				synchronized (PlaylistListModel.this) {
					String plId = p.getPlaylistId();
					if (control.getMyUser().getPlaylistIds().contains(plId)) {
						if (pMap.containsKey(plId))
							remove(pMap.get(plId));
						insertSorted(p);
						pMap.put(plId, p);
					}
				}
			}
		});
	}
}
