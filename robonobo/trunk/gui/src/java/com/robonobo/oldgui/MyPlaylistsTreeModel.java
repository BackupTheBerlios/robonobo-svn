package com.robonobo.oldgui;

import java.awt.Font;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.SwingUtilities;
import javax.swing.tree.TreePath;

import com.robonobo.common.concurrent.CatchingRunnable;
import com.robonobo.common.swing.DefaultSortableTreeNode;
import com.robonobo.common.swing.SortableTreeNode;
import com.robonobo.common.swing.SortedTreeModel;
import com.robonobo.core.RobonoboController;
import com.robonobo.core.api.UserPlaylistListener;
import com.robonobo.core.api.model.Playlist;
import com.robonobo.core.api.model.User;

public class MyPlaylistsTreeModel extends SortedTreeModel implements UserPlaylistListener {
	static final int MAX_MY_PLAYLIST_TITLE_WIDTH = 120;
	/**
	 * Same playlist might be at several points in the tree, if it's shared by
	 * more than one of our friends
	 */
	private Map<String, PlaylistTreeNode> playlistNodes = new HashMap<String, PlaylistTreeNode>();
	private NewPlaylistTreeNode newPlaylistNode;
	private RobonoboFrame frame;
	private RobonoboController controller;
	private Font font;
	private MyPlaylistsTree tree;
	
	public MyPlaylistsTreeModel(RobonoboFrame frame, Font font) {
		super(null);
		setRoot(new DefaultSortableTreeNode("My Playlists"));
		this.frame = frame;
		controller = frame.controller;
		this.font = font;
		controller.addUserPlaylistListener(this);
	}

	/** Have this fugly callback as we need to expand & resize the tree when we change data */
	public void setTree(MyPlaylistsTree tree) {
		this.tree = tree;
	}
	
	public void loggedIn() {
		SwingUtilities.invokeLater(new CatchingRunnable() {
			public void doRun() throws Exception {
				synchronized (MyPlaylistsTreeModel.this) {
					getRoot().removeAllChildren();
					playlistNodes.clear();
					newPlaylistNode = new NewPlaylistTreeNode(frame, font);
					insertNodeSorted(getRoot(), newPlaylistNode);
					tree.expandRow(0);
				}
				tree.updateMaxSize();
				nodeStructureChanged(getRoot());
			}
		});
	}

	public void userChanged(final User u) {
		if(controller.getMyUser().equals(u)) {
			// Check to see if any playlists have been deleted
			SwingUtilities.invokeLater(new CatchingRunnable() {
				public void doRun() throws Exception {
					synchronized (MyPlaylistsTreeModel.this) {
						Iterator<Entry<String, PlaylistTreeNode>> iter = playlistNodes.entrySet().iterator();
						while(iter.hasNext()) {
							Entry<String, PlaylistTreeNode> entry = iter.next();
							if(!u.getPlaylistIds().contains(entry.getKey())) {
								removeNodeFromParent(entry.getValue());
								iter.remove();
							}
						}
					}
					tree.updateMaxSize();
				}
			});
		}
	}
	
	public void playlistChanged(final Playlist p) {
		if(controller.getMyUser().getPlaylistIds().contains(p.getPlaylistId())) {
			SwingUtilities.invokeLater(new CatchingRunnable() {
				public void doRun() throws Exception {
					synchronized (MyPlaylistsTreeModel.this) {
						PlaylistTreeNode ptn = playlistNodes.get(p.getPlaylistId());
						if(ptn == null) {
							// New playlist
							ptn = new PlaylistTreeNode(p, frame, font, MAX_MY_PLAYLIST_TITLE_WIDTH);
							playlistNodes.put(p.getPlaylistId(), ptn);
							insertNodeSorted(getRoot(), ptn);
						} else {
							// Updated playlist
							ptn.setPlaylist(p);
							replaceNodeSorted(getRoot(), ptn);
						}
					}
					tree.updateMaxSize();
				}
			});
		}
	}
	
	@Override
	public SortableTreeNode getRoot() {
		return (SortableTreeNode) super.getRoot();
	}
	
	public TreePath getPlaylistTreePath(String playlistId) {
		return new TreePath(getPathToRoot(playlistNodes.get(playlistId)));
	}
	
	public TreePath getNewPlaylistTreePath() {
		return new TreePath(getPathToRoot(newPlaylistNode));
	}
}
