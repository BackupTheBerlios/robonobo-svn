package com.robonobo.oldgui;

import java.awt.Color;
import java.awt.Font;

public class MyPlaylistsTree extends LeftSidebarTree {
	public MyPlaylistsTree(RobonoboFrame rFrame, Color bgColor, Font font) {
		super(new MyPlaylistsTreeModel(rFrame, font), rFrame, bgColor, font, true);
		getModel().setTree(this);
		// If we were already logged in before we got here, the treemodel's
		// listener method won't be called, so do it here
		if(rFrame.getController().getMyUser() != null)
			getModel().loggedIn();
	}

	public MyPlaylistsTreeModel getModel() {
		return (MyPlaylistsTreeModel) super.getModel();
	}

	public void selectNewPlaylist() {
		setSelectionPath(getModel().getNewPlaylistTreePath());
	}

	public void selectPlaylist(String playlistId) {
		setSelectionPath(getModel().getPlaylistTreePath(playlistId));
	}
}
