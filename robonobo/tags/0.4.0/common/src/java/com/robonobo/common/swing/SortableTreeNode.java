package com.robonobo.common.swing;

import javax.swing.tree.DefaultMutableTreeNode;

public abstract class SortableTreeNode extends DefaultMutableTreeNode implements Comparable<SortableTreeNode> {
	public SortableTreeNode(Object userObject) {
		super(userObject);
	}

	public abstract int compareTo(SortableTreeNode o);
}
