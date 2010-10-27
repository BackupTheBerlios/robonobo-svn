package com.robonobo.gui.panels;

import static com.robonobo.gui.RoboColor.*;

import java.awt.Color;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;

import com.robonobo.core.api.UserPlaylistListener;
import com.robonobo.core.api.model.*;
import com.robonobo.gui.components.*;
import com.robonobo.gui.frames.RobonoboFrame;
import com.robonobo.gui.model.ActiveSearchListModel;
import com.robonobo.gui.model.SearchResultTableModel;

@SuppressWarnings("serial")
public class LeftSidebar extends JPanel implements UserPlaylistListener {
	List<LeftSidebarComponent> sideBarComps = new ArrayList<LeftSidebarComponent>();
	RobonoboFrame frame;
	private ActiveSearchList activeSearchList;
	private MyMusicSelector myMusic;
	private NewPlaylistSelector newPlaylist;
	private PlaylistList playlistList;

	public LeftSidebar(RobonoboFrame frame) {
		this.frame = frame;
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));

		final JPanel sideBarPanel = new JPanel();
		final JScrollPane treeListScroller = new JScrollPane(sideBarPanel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		add(treeListScroller);
		treeListScroller.getViewport().getView().setBackground(Color.WHITE);
		sideBarPanel.setLayout(new BoxLayout(sideBarPanel, BoxLayout.Y_AXIS));
		sideBarPanel.setBackground(MID_GRAY);

		SearchField searchField = new SearchField(this);
		sideBarPanel.add(searchField);

		activeSearchList = new ActiveSearchList(this, frame);
		activeSearchList.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
		sideBarPanel.add(activeSearchList);
		sideBarComps.add(activeSearchList);

		FriendTree fTree = new FriendTree(this, frame);
		fTree.setBorder(BorderFactory.createEmptyBorder(5, 10, 3, 10));
		sideBarPanel.add(fTree);
		sideBarComps.add(fTree);

		myMusic = new MyMusicSelector(this, frame);
		sideBarPanel.add(myMusic);
		sideBarComps.add(myMusic);

		newPlaylist = new NewPlaylistSelector(this, frame);
		sideBarPanel.add(newPlaylist);
		sideBarComps.add(newPlaylist);

		playlistList = new PlaylistList(this, frame);
		sideBarPanel.add(playlistList);
		sideBarComps.add(playlistList);

		JPanel spacerPanel = new JPanel();
		spacerPanel.setLayout(new BoxLayout(spacerPanel, BoxLayout.X_AXIS));
		spacerPanel.setPreferredSize(new Dimension(200, 5));
		spacerPanel.setOpaque(false);
		add(spacerPanel);
		add(new StatusPanel(frame));
	}

	public void searchAdded(String query) {
		ActiveSearchListModel model = (ActiveSearchListModel) activeSearchList.getModel();
		SearchResultTableModel srtm = model.addSearch(query);
		if (srtm != null) {
			SearchResultContentPanel srcm = new SearchResultContentPanel(frame, srtm);
			frame.getMainPanel().addContentPanel("search/" + query, srcm);
		}
		activeSearchList.setSelectedIndex(model.indexOfQuery(query));
		clearSelectionExcept(activeSearchList);
		frame.getMainPanel().selectContentPanel("search/" + query);
	}

	public void selectMyMusic() {
		myMusic.setSelected(true);
	}

	public void selectMyPlaylist(Playlist p) {
		playlistList.selectPlaylist(p);
	}

	public void clearSelectionExcept(LeftSidebarComponent selCmp) {
		for (LeftSidebarComponent cmp : sideBarComps) {
			if (cmp != selCmp)
				cmp.relinquishSelection();
		}
	}

	@Override
	public void loggedIn() {
		// Do nothing
	}

	@Override
	public void userChanged(User u) {
		// Do nothing
	}

	@Override
	public void playlistChanged(Playlist p) {
		// I'm not sure if this is the best place to create the panels, but this is the lowest common ancestor of the
		// playlistlist and the friendtree, and we need to think about both
		String panelName = "playlist/" + p.getPlaylistId();
		PlaylistConfig pc = frame.getController().getPlaylistConfig(p.getPlaylistId());
		ContentPanel pPanel = frame.getMainPanel().getContentPanel(panelName);
		long myUserId = frame.getController().getMyUser().getUserId();
		if (pPanel == null) {
			// Create playlist panel
			if (p.getOwnerIds().contains(myUserId))
				frame.getMainPanel().addContentPanel(panelName, new MyPlaylistContentPanel(frame, p, pc));
			else
				frame.getMainPanel().addContentPanel(panelName, new FriendPlaylistContentPanel(frame, p, pc));
		} else {
			// Playlist panel already exists - check to see if I'm now an owner and wasn't (or vice versa)
			if((pPanel instanceof MyPlaylistContentPanel) && !p.getOwnerIds().contains(myUserId)) {
				frame.getMainPanel().addContentPanel(panelName, new FriendPlaylistContentPanel(frame, p, pc));
			} else if((pPanel instanceof FriendPlaylistContentPanel) && p.getOwnerIds().contains(myUserId)) {
				frame.getMainPanel().addContentPanel(panelName, new MyPlaylistContentPanel(frame, p, pc));
			}
		}

	}
}
