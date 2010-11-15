/**
 * 
 */
package com.robonobo.oldgui;

import java.awt.Font;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.SwingUtilities;
import javax.swing.tree.TreePath;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.robonobo.common.concurrent.CatchingRunnable;
import com.robonobo.common.swing.DefaultSortableTreeNode;
import com.robonobo.common.swing.SortableTreeNode;
import com.robonobo.common.swing.SortedTreeModel;
import com.robonobo.core.RobonoboController;
import com.robonobo.core.api.UserPlaylistListener;
import com.robonobo.core.api.model.*;
import com.robonobo.gui.model.FriendTreeNode;
import com.robonobo.gui.model.PlaylistTreeNode;

@SuppressWarnings("serial")
class FriendTreeModel extends SortedTreeModel implements UserPlaylistListener {
	static final int MAX_FRIEND_PLAYLIST_TITLE_WIDTH = 100;
	RobonoboFrame frame;
	RobonoboController controller;
	Font font;
	FriendsTree tree;
	Map<Long, FriendTreeNode> friendNodes = new HashMap<Long, FriendTreeNode>();
	Map<Long, Map<String, PlaylistTreeNode>> playlistNodes = new HashMap<Long, Map<String, PlaylistTreeNode>>();
	Log log = LogFactory.getLog(getClass());

	public FriendTreeModel(RobonoboFrame rFrame, Font font) {
		super(null);
		setRoot(new DefaultSortableTreeNode("Friends"));
		frame = rFrame;
		controller = frame.controller;
		this.font = font;
		controller.addUserPlaylistListener(this);
	}

	/** Have this fugly callback as we need to expand & resize the tree when we change data */
	public void setTree(FriendsTree tree) {
		this.tree = tree;
	}
	
	public void loggedIn() {
		SwingUtilities.invokeLater(new CatchingRunnable() {
			public void doRun() throws Exception {
				synchronized (FriendTreeModel.this) {
					getRoot().removeAllChildren();
					friendNodes.clear();
					playlistNodes.clear();
				}
				tree.updateMaxSize();
				nodeStructureChanged(getRoot());
			}
		});
	}

	public void userChanged(final User u) {
		// If it's me, check to see if any of my friends are no longer friends
		if (controller.getMyUser().equals(u)) {
			SwingUtilities.invokeLater(new CatchingRunnable() {
				public void doRun() throws Exception {
					synchronized (FriendTreeModel.this) {
						for (Long friendId : friendNodes.keySet()) {
							if (!u.getFriendIds().contains(friendId)) {
								removeNodeFromParent(friendNodes.get(friendId));
								friendNodes.remove(friendId);
								playlistNodes.remove(friendId);
							}
						}
					}
					tree.updateMaxSize();
				}
			});
		} else if (controller.getMyUser().getFriendIds().contains(u.getUserId())) {
			SwingUtilities.invokeLater(new CatchingRunnable() {
				public void doRun() throws Exception {
					synchronized (FriendTreeModel.this) {
						boolean wasEmpty = (friendNodes.size() == 0);
						if (friendNodes.containsKey(u.getUserId())) {
							FriendTreeNode ftn = friendNodes.get(u.getUserId());
							// Already have this friend - check to see if any playlists have been deleted
							Iterator<Entry<String, PlaylistTreeNode>> iter = playlistNodes.get(u.getUserId()).entrySet().iterator();
							while(iter.hasNext()) {
								Entry<String, PlaylistTreeNode> entry = iter.next();
								if(!u.getPlaylistIds().contains(entry.getKey())) {
									PlaylistTreeNode ptn = entry.getValue();
									removeNodeFromParent(ptn);
									iter.remove();
								}
							}
							// Friend might have changed friendly names, re-order if necessary
							ftn.setFriend(u);
							replaceNodeSorted(getRoot(), ftn);
						} else {
							// New friend node
							FriendTreeNode ftn = new FriendTreeNode(u);
							friendNodes.put(u.getUserId(), ftn);
							playlistNodes.put(u.getUserId(), new HashMap<String, PlaylistTreeNode>());
							insertNodeSorted(getRoot(), ftn);
						}
						// If we were empty but now we're not, expand the root to show friends
						if(wasEmpty && (friendNodes.size() > 0))
							tree.expandRow(0);
					}
					tree.updateMaxSize();
				}
			});
		}
	}

	public void playlistChanged(final Playlist p) {
		SwingUtilities.invokeLater(new CatchingRunnable() {
			public void doRun() throws Exception {
				synchronized (FriendTreeModel.this) {
					for (FriendTreeNode ftn : friendNodes.values()) {
						if(ftn.getFriend().getPlaylistIds().contains(p.getPlaylistId())) {
							// This user has this playlist - do we have a node for this yet?
							long friendId = ftn.getFriend().getUserId();
							PlaylistTreeNode ptn = playlistNodes.get(friendId).get(p.getPlaylistId());
							if(ptn == null) {
//								ptn = new PlaylistTreeNode(p, frame, font, MAX_FRIEND_PLAYLIST_TITLE_WIDTH);
								playlistNodes.get(friendId).put(p.getPlaylistId(), ptn);
								insertNodeSorted(ftn, ptn);
							} else {
								ptn.setPlaylist(p);
								replaceNodeSorted(ftn, ptn);
							}
						}
					}
				}
				tree.updateMaxSize();
			}
		});
	}

	@Override
	public void libraryUpdated(Library lib) {
		// Do nothing
	}
	public TreePath getPlaylistTreePath(String playlistId) {
		// NB If the playlist is in the tree more than once (eg shared
		// playlist), this will select the first instance only...
		synchronized (this) {
			for (Map<String, PlaylistTreeNode> ptns : playlistNodes.values()) {
				if(ptns.containsKey(playlistId))
					return new TreePath(getPathToRoot(ptns.get(playlistId)));
			}
		}
		return null;
	}
	
	@Override
	public SortableTreeNode getRoot() {
		return (SortableTreeNode) super.getRoot();
	}
}