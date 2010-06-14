package com.robonobo.core.api;

import com.robonobo.core.api.model.Playlist;
import com.robonobo.core.api.model.User;

/**
 * Notifies when users or playlists change
 */
public interface UserPlaylistListener {
	public void loggedIn();
	public void userChanged(User u);
	public void playlistChanged(Playlist p);
}
