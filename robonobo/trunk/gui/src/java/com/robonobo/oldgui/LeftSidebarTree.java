package com.robonobo.oldgui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.TransferHandler;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import com.robonobo.core.RobonoboController;

public class LeftSidebarTree extends JTree {
	RobonoboFrame frame;
	RobonoboController controller;
	public LeftSidebarTree(TreeModel tm, RobonoboFrame rFrame, final Color bgColor, Font font, boolean setupDnD) {
		super(tm);
		frame = rFrame;
		controller = frame.getController();
		
		// Setup colors - some l&f's fuck this up
		// TODO When we have our own l&f, remove this...
		setBackground(null);
		setOpaque(false);
		setFont(font);
		setCellRenderer(new DefaultTreeCellRenderer() {
			public Component getTreeCellRendererComponent(
					JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
				Component cmp = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
				if (!sel)
					((DefaultTreeCellRenderer) cmp).setBackgroundNonSelectionColor(bgColor);
				return cmp;
			}
		});
		
		// Fill up width, but not height
		setMaximumSize(new Dimension(Integer.MAX_VALUE, getMaximumSize().height));
		addTreeExpansionListener(new TreeExpansionListener() {
			public void treeExpanded(TreeExpansionEvent event) {
				updateMaxSize();
			}
			public void treeCollapsed(TreeExpansionEvent event) {
				updateMaxSize();
			}
		});
		
		// Detect selection and pass it to the treenode to handle
		getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		addTreeSelectionListener(new TreeSelectionListener() {
			public void valueChanged(TreeSelectionEvent e) {
				TreePath tp = e.getNewLeadSelectionPath();
				if (tp == null)
					return;
				if(!(tp.getLastPathComponent() instanceof SelectableTreeNode))
					return;
				SelectableTreeNode stn = (SelectableTreeNode) tp.getLastPathComponent();
				boolean wantSelect = stn.handleSelect();
				if (wantSelect)
					frame.getLeftSidebar().getBtnGroup().deselectAll();
				else
					setSelectionPath(e.getOldLeadSelectionPath());
			}
		});
		
		// Play/pause on spacebar
		KeyAdapter keyListener = new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				// Spacebar == play/pause
				if (e.getKeyChar() == KeyEvent.VK_SPACE) {
					ContentPanel cp = frame.getContentHolder().getContentPanel(frame.getContentHolder().currentPanel);
					PlayPauseButton btn = cp.getPlayPauseButton();
					if (btn != null)
						btn.doClick();
					e.consume();
				}
			}
		};
		addKeyListener(keyListener);

		if(setupDnD)
			setupDnD();
	}

	private void setupDnD() {
		// For drag and drop, figure out which treenode is under the point,
		// and delegate to them
		setTransferHandler(new TransferHandler() {
			@Override
			public boolean canImport(JComponent c, DataFlavor[] flavors) {
				return true;
			}

			@Override
			public boolean importData(JComponent c, Transferable t) {
				TreePath path = getSelectionPath();
				SelectableTreeNode node = (SelectableTreeNode) path.getLastPathComponent();
				if (node == null)
					return false;
				return node.importData(t);
			}
		});
	}

	/**
	 * The BoxLayout used in the LeftSidebar works with maximum sizes, so
	 * whenever we add/remove/collapse/expand nodes, we have to update it
	 */
	void updateMaxSize() {
		// Fill width, but not height
		setMaximumSize(new Dimension(Integer.MAX_VALUE, getPreferredSize().height));
	}
}
