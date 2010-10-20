/**
 * 
 */
package com.robonobo.gui.model;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.SwingUtilities;
import javax.swing.tree.TreePath;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.robonobo.common.concurrent.CatchingRunnable;
import com.robonobo.common.swing.SortableTreeNode;
import com.robonobo.common.swing.SortedTreeModel;
import com.robonobo.core.RobonoboController;
import com.robonobo.core.api.UserPlaylistListener;
import com.robonobo.core.api.model.Playlist;
import com.robonobo.core.api.model.User;
import com.robonobo.gui.frames.RobonoboFrame;

@SuppressWarnings("serial")
public class FriendTreeModel extends SortedTreeModel implements UserPlaylistListener {
	static final int MAX_FRIEND_PLAYLIST_TITLE_WIDTH = 100;
	RobonoboFrame frame;
	RobonoboController controller;
	Map<Long, FriendTreeNode> friendNodes = new HashMap<Long, FriendTreeNode>();
	Map<Long, Map<String, PlaylistTreeNode>> playlistNodes = new HashMap<Long, Map<String, PlaylistTreeNode>>();
	Log log = LogFactory.getLog(getClass());
	SelectableTreeNode myRoot;

	public FriendTreeModel(RobonoboFrame rFrame) {
		super(null);
		myRoot = new SelectableTreeNode("Friends");
		setRoot(myRoot);
		frame = rFrame;
		controller = frame.getController();
		controller.addUserPlaylistListener(this);
	}

	public void loggedIn() {
		SwingUtilities.invokeLater(new CatchingRunnable() {
			public void doRun() throws Exception {
				synchronized (FriendTreeModel.this) {
					getRoot().removeAllChildren();
					friendNodes.clear();
					playlistNodes.clear();
				}
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
				}
			});
		} else if (controller.getMyUser().getFriendIds().contains(u.getUserId())) {
			SwingUtilities.invokeLater(new CatchingRunnable() {
				public void doRun() throws Exception {
					synchronized (FriendTreeModel.this) {
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
					}
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
								ptn = new PlaylistTreeNode(p, frame);
								playlistNodes.get(friendId).put(p.getPlaylistId(), ptn);
								insertNodeSorted(ftn, ptn);
							} else {
								ptn.setPlaylist(p);
								replaceNodeSorted(ftn, ptn);
							}
						}
					}
				}
			}
		});
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