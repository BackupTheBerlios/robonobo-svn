package com.robonobo.gui.components;

import java.awt.Dimension;

import javax.swing.JTree;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.TreeModel;

/**
 * Whenever part of the tree is expanded or collapsed, it updates its max size so that the layout reflows properly
 * @author macavity
 *
 */
public class ExpandoTree extends JTree {
	public ExpandoTree(TreeModel newModel) {
		super(newModel);
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
	}
	
	void updateMaxSize() {
		// Fill width, but not height
		setMaximumSize(new Dimension(Integer.MAX_VALUE, getPreferredSize().height));
	}
}
