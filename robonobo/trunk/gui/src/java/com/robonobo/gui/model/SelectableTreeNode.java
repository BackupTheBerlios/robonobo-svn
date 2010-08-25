package com.robonobo.gui.model;

import java.awt.datatransfer.Transferable;

import com.robonobo.common.swing.SortableTreeNode;

@SuppressWarnings("serial")
public class SelectableTreeNode extends SortableTreeNode {
	
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
	
	@Override
	public int compareTo(SortableTreeNode o) {
		return 0;
	}
}
