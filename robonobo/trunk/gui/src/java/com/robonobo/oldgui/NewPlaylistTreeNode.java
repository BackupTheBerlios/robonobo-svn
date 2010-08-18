/**
 * 
 */
package com.robonobo.oldgui;

import java.awt.Font;

class NewPlaylistTreeNode extends PlaylistTreeNode {
	public NewPlaylistTreeNode(RobonoboFrame frame, Font font) {
		super("New Playlist", frame, font, MyPlaylistsTreeModel.MAX_MY_PLAYLIST_TITLE_WIDTH);
	}

	@Override
	protected String contentPanelName() {
		return "playlist-new";
	}

	public int compareTo(PlaylistTreeNode o) {
		// New Playlist is always first
		return -1;
	}
}