package com.robonobo.gui;

import java.awt.datatransfer.Transferable;

import javax.swing.tree.DefaultMutableTreeNode;

import com.robonobo.common.swing.SortableTreeNode;

public abstract class SelectableTreeNode extends SortableTreeNode {
	
	public SelectableTreeNode(Object val) {
		super(val);
	}
	
	/**
	 * Returns true if the node wants to remain selected, or false if it doesn't want to be selected (and the selection should remain at the old value)
	 */
	public boolean handleSelect() {
		// Default implementation does nothing and doesn't want selection
		return false;
	}
	
	/** For drag n drop */
	public boolean importData(Transferable t) {
		return false;
	}
}
