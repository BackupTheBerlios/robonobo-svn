package com.robonobo.oldgui;

import java.awt.Color;
import java.awt.Font;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

@SuppressWarnings("serial")
public class FriendsTree extends LeftSidebarTree {
	Log log = LogFactory.getLog(getClass());

	public FriendsTree(RobonoboFrame rFrame, Color bgColor, Font font) {
		super(new FriendTreeModel(rFrame, font), rFrame, bgColor, font, false);
		getModel().setTree(this);
		// If we were already logged in before we got here, the treemodel's
		// listener method won't be called, so do it here
		if(rFrame.getController().getMyUser() != null)
			getModel().loggedIn();
	}

	@Override
	public FriendTreeModel getModel() {
		return (FriendTreeModel) super.getModel();
	}

	public void selectPlaylist(String playlistId) {
		setSelectionPath(getModel().getPlaylistTreePath(playlistId));
	}
}
