package com.robonobo.gui.components;

import static com.robonobo.gui.GUIUtils.*;
import static com.robonobo.gui.RoboColor.*;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.*;

import com.robonobo.core.api.model.User;
import com.robonobo.gui.RoboFont;
import com.robonobo.gui.frames.RobonoboFrame;
import com.robonobo.gui.model.FriendTreeModel;
import com.robonobo.gui.model.FriendTreeNode;
import com.robonobo.gui.model.PlaylistTreeNode;
import com.robonobo.gui.model.SelectableTreeNode;
import com.robonobo.gui.panels.LeftSidebar;

@SuppressWarnings("serial")
public class FriendTree extends ExpandoTree implements LeftSidebarComponent {
	static final Dimension MAX_FRIEND_SZ = new Dimension(145, Integer.MAX_VALUE);
	static final Dimension MAX_PLAYLIST_SZ = new Dimension(135, Integer.MAX_VALUE);

	LeftSidebar sideBar;
	RobonoboFrame frame;
	ImageIcon rootIcon, friendIcon, playlistIcon;
	Font normalFont, boldFont;

	public FriendTree(final LeftSidebar sideBar, RobonoboFrame frame) {
		super(new FriendTreeModel(frame));
		this.sideBar = sideBar;
		this.frame = frame;

		normalFont = RoboFont.getFont(11, false);
		boldFont = RoboFont.getFont(11, true);
		setName("robonobo.playlist.tree");
		setAlignmentX(0.0f);
		setRootVisible(true);
		collapseRow(0);

		rootIcon = createImageIcon("/img/icon/friends.png", null);
		friendIcon = createImageIcon("/img/icon/friend.png", null);
		playlistIcon = createImageIcon("/img/icon/playlist.png", null);

		setCellRenderer(new CellRenderer());
		setSelectionModel(new SelectionModel());
		getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		addTreeSelectionListener(new TreeSelectionListener() {
			public void valueChanged(TreeSelectionEvent e) {
				TreePath tp = e.getNewLeadSelectionPath();
				if (tp == null)
					return;
				if (!(tp.getLastPathComponent() instanceof SelectableTreeNode))
					return;
				SelectableTreeNode stn = (SelectableTreeNode) tp.getLastPathComponent();
				if (stn.wantSelect()) {
					sideBar.clearSelectionExcept(FriendTree.this);
					if (stn.handleSelect())
						getModel().firePathToRootChanged(stn);
				} else
					setSelectionPath(e.getOldLeadSelectionPath());
			}
		});
	}

	@Override
	public FriendTreeModel getModel() {
		return (FriendTreeModel) super.getModel();
	}

	public void relinquishSelection() {
		((SelectionModel) getSelectionModel()).reallyClearSelection();
	}

	public void selectForPlaylist(String playlistId) {
		setSelectionPath(getModel().getPlaylistTreePath(playlistId));
	}

	private class CellRenderer extends DefaultTreeCellRenderer {
		public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded,
				boolean leaf, int row, boolean hasFocus) {
			final JLabel lbl = (JLabel) super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row,
					hasFocus);
			final TreeNode node = (TreeNode) value;

			if (node instanceof PlaylistTreeNode) {
				lbl.setIcon(playlistIcon);
				lbl.setMaximumSize(MAX_PLAYLIST_SZ);
				lbl.setPreferredSize(MAX_PLAYLIST_SZ);
				PlaylistTreeNode ptn = (PlaylistTreeNode) node;
				int unseen = ptn.getNumUnseenTracks();
				if (unseen > 0) {
					lbl.setText("[" + unseen + "] " + lbl.getText());
					lbl.setFont(boldFont);
				} else
					lbl.setFont(normalFont);
			} else if (node instanceof FriendTreeNode) {
				lbl.setIcon(friendIcon);
				lbl.setMaximumSize(MAX_FRIEND_SZ);
				lbl.setPreferredSize(MAX_FRIEND_SZ);
				int unseen = getTotalUnseen(node);
				if (unseen > 0) {
					lbl.setText("[" + unseen + "] " + lbl.getText());
					lbl.setFont(boldFont);
				} else
					lbl.setFont(normalFont);
			} else if (node.getParent() == null) {
				lbl.setIcon(rootIcon);
				// Are there any unseen tracks at all?
				int unseen = getTotalUnseen(node);
				if (unseen > 0)
					lbl.setFont(boldFont);
				else
					lbl.setFont(normalFont);
			} else
				lbl.setFont(normalFont);

			if (getSelectionPath() != null && node.equals(getSelectionPath().getLastPathComponent())) {
				lbl.setForeground(BLUE_GRAY);
			} else {
				lbl.setForeground(DARK_GRAY);
			}
			return lbl;
		}

		public void paint(Graphics g) {
			paintComponent(g);
		}

		int getTotalUnseen(TreeNode n) {
			int unseen = 0;
			for (int i = 0; i < n.getChildCount(); i++) {
				TreeNode child = n.getChildAt(i);
				if(child instanceof PlaylistTreeNode)
					unseen += ((PlaylistTreeNode)child).getNumUnseenTracks();
				else
					unseen += getTotalUnseen(child);
			}
			return unseen;
		}
	}

	/**
	 * Stop Swing from deselecting us at its twisted whim
	 */
	class SelectionModel extends DefaultTreeSelectionModel {
		@Override
		public void clearSelection() {
			// Do nothing
		}

		public void reallyClearSelection() {
			super.clearSelection();
		}
	}
}
