package com.robonobo.oldgui;

import static com.robonobo.gui.GUIUtils.*;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.debian.tablelayout.TableLayout;

import com.robonobo.core.api.UserPlaylistListener;
import com.robonobo.core.api.model.*;
import com.robonobo.oldgui.GroupButtonGroup.SelectListener;

public class LeftSidebar extends JPanel implements UserPlaylistListener {
	protected final Log log = LogFactory.getLog(getClass());
	RobonoboFrame frame;
	GroupButtonGroup btnGroup;
	GroupButton allMusicBtn;
	GroupButton myMusicBtn;
	FriendsTree friendsTree;
	MyPlaylistsTree myPlaylistsTree;
	boolean treesReady = false;
	private JPanel innerPanel;
	private Font sidebarFont;

	public LeftSidebar(RobonoboFrame myFrame) {
		this.frame = myFrame;
		// Create a border of 10px all the way around, then create an inner
		// panel inside that which contains our stuff
		double[][] cellSizen = { { 10, TableLayout.FILL }, { 10, TableLayout.FILL, 10 } };
		setLayout(new TableLayout(cellSizen));
		innerPanel = new JPanel();
		add(innerPanel, "1,1");
		innerPanel.setLayout(new BoxLayout(innerPanel, BoxLayout.Y_AXIS));

		// Button group only allows one button to be selected at a time - add a
		// listener to deselect our trees when a button is selected
		btnGroup = new GroupButtonGroup();
		btnGroup.addListener(new SelectListener() {
			public void itemSelected() {
				if (friendsTree != null)
					friendsTree.clearSelection();
				if (myPlaylistsTree != null)
					myPlaylistsTree.clearSelection();
			}
		});

		// Everyone's Music button
		allMusicBtn = new GroupButton("Everyone's Music", createImageIcon("/img/ManyUsers.png", "All Music"));
		allMusicBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (((GroupButton) e.getSource()).isSelected())
					frame.getContentHolder().bringPanelToFront("allMusic");
			}
		});
		btnGroup.addButton(allMusicBtn);
		allMusicBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, allMusicBtn.getMaximumSize().height));

		// My Music button
		innerPanel.add(Box.createRigidArea(new Dimension(0, 10)));
		myMusicBtn = new GroupButton("My Music", createImageIcon("/img/OneUser.png", "My Music"));
		myMusicBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (((GroupButton) e.getSource()).isSelected())
					frame.getContentHolder().bringPanelToFront("myMusic");
			}
		});
		btnGroup.addButton(myMusicBtn);
		myMusicBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, myMusicBtn.getMaximumSize().height));

		frame.getController().addUserPlaylistListener(this);

		// Trees - add them here so that they receive user/playlist change
		// notifications
		myPlaylistsTree = new MyPlaylistsTree(frame, innerPanel.getBackground(), myMusicBtn.getFont());
		friendsTree = new FriendsTree(frame, innerPanel.getBackground(), myMusicBtn.getFont());
		// When one of the trees is selected, deselect the other one
		myPlaylistsTree.addTreeSelectionListener(new TreeSelectionListener() {
			public void valueChanged(TreeSelectionEvent e) {
				TreePath tp = e.getNewLeadSelectionPath();
				if (tp == null)
					return;
				friendsTree.clearSelection();
			}
		});
		friendsTree.addTreeSelectionListener(new TreeSelectionListener() {
			public void valueChanged(TreeSelectionEvent e) {
				TreePath tp = e.getNewLeadSelectionPath();
				if (tp == null)
					return;
				myPlaylistsTree.clearSelection();
			}
		});

		addComponents(false);
		
		// Might have been logged in already before we added ourselves as a listener
		if(frame.getController().getMyUser() != null)
			loggedIn();
	}

	private void addComponents(boolean validate) {
		innerPanel.removeAll();
		innerPanel.add(allMusicBtn);
		if (treesReady)
			innerPanel.add(friendsTree);
		innerPanel.add(myMusicBtn);
		if (treesReady)
			innerPanel.add(myPlaylistsTree);
		if (validate)
			innerPanel.validate();
	}

	public void selectAllMusic() {
		allMusicBtn.doClick();
	}

	public void selectMyMusic() {
		myMusicBtn.doClick();
	}

	public void selectNewPlaylist() {
		myPlaylistsTree.selectNewPlaylist();
	}

	public void selectMyPlaylist(Playlist p) {
		if (p.getPlaylistId() == null)
			selectNewPlaylist();
		else
			myPlaylistsTree.selectPlaylist(p.getPlaylistId());
	}

	public void selectFriendPlaylist(Playlist p) {
		friendsTree.selectPlaylist(p.getPlaylistId());
	}
	
	public void reopenMyPlaylistsTree() {
		if (myPlaylistsTree != null) {
			myPlaylistsTree.collapseRow(0);
			myPlaylistsTree.expandRow(0);
		}
	}

	public void reopenFriendsTree() {
		if(friendsTree != null) {
			friendsTree.collapseRow(0);
			friendsTree.expandRow(0);
		}
	}
	
	public void loggedIn() {
		// Grab the font from the buttons
		sidebarFont = allMusicBtn.getFont();
		myPlaylistsTree.setFont(sidebarFont);
		friendsTree.setFont(sidebarFont);
		treesReady = true;
		addComponents(true);
	}

	public void playlistChanged(Playlist p) {
		// Add panel for playlist if not already existent
		String panelName = "playlist-" + p.getPlaylistId();
		PlaylistConfig pc = frame.getController().getPlaylistConfig(p.getPlaylistId());
		ContentPanel pPanel = frame.getContentHolder().getContentPanel(panelName);
		long myUserId = frame.getController().getMyUser().getUserId();
		if (pPanel == null) {
			// Create playlist panel
			if (p.getOwnerIds().contains(myUserId))
				frame.getContentHolder().addContentPanel(panelName, new MyPlaylistContentPanel(frame, p, pc));
			else
				frame.getContentHolder().addContentPanel(panelName, new FriendPlaylistContentPanel(frame, p, pc));
		} else {
			// Playlist panel already exists - check to see if I'm now an owner and wasn't (or vice versa)
			if((pPanel instanceof MyPlaylistContentPanel) && !p.getOwnerIds().contains(myUserId)) {
				frame.getContentHolder().addContentPanel(panelName, new FriendPlaylistContentPanel(frame, p, pc));
			} else if((pPanel instanceof FriendPlaylistContentPanel) && p.getOwnerIds().contains(myUserId)) {
				frame.getContentHolder().addContentPanel(panelName, new MyPlaylistContentPanel(frame, p, pc));
			}
		}
	}

	public void userChanged(User u) {
		// Add panel for user if not already existent
		// if
		// (frame.getController().getMyUser().getFriendIds().contains(u.getUserId()))
		// {
		// String panelName = "friend-" + u.getEmail();
		// if (frame.getContentHolder().getContentPanel(panelName) == null)
		// frame.getContentHolder().addContentPanel(panelName, new
		// UserContentPanel(frame, u, false));
		// }
	}
	
	@Override
	public void libraryUpdated(Library lib) {
		// Do nothing
	}
	
	public GroupButtonGroup getBtnGroup() {
		return btnGroup;
	}
}
