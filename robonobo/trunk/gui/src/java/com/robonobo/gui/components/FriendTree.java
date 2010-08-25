package com.robonobo.gui.components;

import static com.robonobo.gui.GUIUtils.*;
import static com.robonobo.gui.RoboColor.*;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import com.robonobo.gui.RoboFont;
import com.robonobo.gui.frames.RobonoboFrame;
import com.robonobo.gui.model.FriendTreeModel;
import com.robonobo.gui.model.SelectableTreeNode;
import com.robonobo.gui.panels.LeftSidebar;

@SuppressWarnings("serial")
public class FriendTree extends ExpandoTree implements LeftSidebarComponent {
	static final Dimension MAX_FRIEND_SZ = new Dimension(145, Integer.MAX_VALUE);
	static final Dimension MAX_PLAYLIST_SZ = new Dimension(135, Integer.MAX_VALUE);

	LeftSidebar sideBar;
	RobonoboFrame frame;
	ImageIcon rootIcon, friendIcon, playlistIcon;

	public FriendTree(final LeftSidebar sideBar, RobonoboFrame frame) {
		super(new FriendTreeModel(frame));
		this.sideBar = sideBar;
		this.frame = frame;

		setFont(RoboFont.getFont(11, false));
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
		// Detect selection and pass it to the treenode to handle
		addTreeSelectionListener(new TreeSelectionListener() {
			public void valueChanged(TreeSelectionEvent e) {
				TreePath tp = e.getNewLeadSelectionPath();
				if (tp == null)
					return;
				if (!(tp.getLastPathComponent() instanceof SelectableTreeNode))
					return;
				SelectableTreeNode stn = (SelectableTreeNode) tp.getLastPathComponent();
				boolean wantSelect = stn.handleSelect();
				if (wantSelect) {
					sideBar.clearSelectionExcept(FriendTree.this);
					// TODO Tell the frame to select the correct main panel
				} else
					setSelectionPath(e.getOldLeadSelectionPath());
			}
		});
	}

	public void relinquishSelection() {
		((SelectionModel)getSelectionModel()).reallyClearSelection();
	}

	private class CellRenderer extends DefaultTreeCellRenderer {
		public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
			final JLabel lbl = (JLabel) super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
			final TreeNode node = (TreeNode) value;

			if (node.getParent() == null)
				lbl.setIcon(rootIcon);
			else if (!node.isLeaf()) {
				lbl.setIcon(friendIcon);
				lbl.setMaximumSize(MAX_FRIEND_SZ);
				lbl.setPreferredSize(MAX_FRIEND_SZ);
			} else {
				lbl.setIcon(playlistIcon);
				lbl.setMaximumSize(MAX_PLAYLIST_SZ);
				lbl.setPreferredSize(MAX_PLAYLIST_SZ);
			}

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
	}

	/**
	 * Stop Swing from deselecting us at its twisted whim
	 * 
	 * @author macavity
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
