package com.robonobo.gui;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.robonobo.common.swing.SortableTreeNode;
import com.robonobo.core.api.model.User;

public class FriendTreeNode extends SelectableTreeNode {
	private User friend;
	Log log = LogFactory.getLog(getClass());

	public FriendTreeNode(User friend) {
		super(friend.getFriendlyName());
		this.friend = friend;
	}

	public User getFriend() {
		return friend;
	}

	public void setFriend(User friend) {
		this.friend = friend;
		setUserObject(friend.getFriendlyName());
	}

	@Override
	public boolean handleSelect() {
		// frame.getContentHolder().bringPanelToFront("friend-"+friend.getEmail());
		// return true;
		return false;
	}

	@Override
	public PlaylistTreeNode getChildAt(int index) {
		return (PlaylistTreeNode) super.getChildAt(index);
	}

	@Override
	public int compareTo(SortableTreeNode o) {
		FriendTreeNode other = (FriendTreeNode) o;
		return friend.getFriendlyName().compareTo(other.getFriend().getFriendlyName());
	}
	
}
