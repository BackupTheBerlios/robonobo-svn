package com.robonobo.gui.components;

import java.awt.Dimension;

import javax.swing.DefaultListSelectionModel;
import javax.swing.JList;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.robonobo.gui.RoboFont;
import com.robonobo.gui.frames.RobonoboFrame;
import com.robonobo.gui.panels.LeftSidebar;

@SuppressWarnings("serial")
public abstract class LeftSidebarList extends JList implements LeftSidebarComponent {
	protected RobonoboFrame frame;
	protected LeftSidebar sideBar;

	public LeftSidebarList(final LeftSidebar sideBar, RobonoboFrame frame, ListModel lm) {
		super(lm);
		this.sideBar = sideBar;
		this.frame = frame;
		setName("robonobo.playlist.list");
		setFont(RoboFont.getFont(11, false));
		setAlignmentX(0.0f);
		setMaximumSize(new Dimension(65535, 50));
		setSelectionModel(new SelectionModel());
		setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				if(!e.getValueIsAdjusting())
					return;
				sideBar.clearSelectionExcept(LeftSidebarList.this);
				itemSelected(e.getFirstIndex());
			}
		});
	}

	public void relinquishSelection() {
		((SelectionModel)getSelectionModel()).reallyClearSelection();
	}

	/**
	 * The BoxLayout used in the LeftSidebar works with maximum sizes, so
	 * whenever we add/remove/collapse/expand nodes, we have to update it
	 */
//	protected void updateMaxSize() {
//		// Fill width, but not height
//		setMaximumSize(new Dimension(Integer.MAX_VALUE, getPreferredSize().height));
//	}

	/**
	 * The user has clicked on this item
	 */
	abstract protected void itemSelected(int index);
	
	/**
	 * Stop Swing from deselecting us at its twisted whim
	 * @author macavity
	 */
	class SelectionModel extends DefaultListSelectionModel {
		@Override
		public void clearSelection() {
			// Do nothing - only clear it using reallyClearSelection()
		}

		public void reallyClearSelection() {
			super.clearSelection();
		}
	}
}
