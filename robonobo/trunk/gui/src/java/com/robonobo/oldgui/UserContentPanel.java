package com.robonobo.oldgui;

import javax.swing.JScrollPane;

import org.debian.tablelayout.TableLayout;

import com.robonobo.core.api.model.User;

public class UserContentPanel extends ContentPanel {
	private UserDetailsPanel userDetailsPanel;
	private PlaylistListPanel playlistPanel;

	public UserContentPanel(RobonoboFrame frame, User user, boolean isMe) {
		super(frame);
		double[][] cellSizen = { {TableLayout.FILL}, {300, 200, TableLayout.FILL} };
		setLayout(new TableLayout(cellSizen));
		userDetailsPanel = new UserDetailsPanel(frame.getController(), user, isMe);
		add(userDetailsPanel, "0,0");
		playlistPanel = new PlaylistListPanel(frame, user, isMe);
		add(new JScrollPane(playlistPanel), "0,1");
		
	}

	@Override
	public void focus() {
	}

}
