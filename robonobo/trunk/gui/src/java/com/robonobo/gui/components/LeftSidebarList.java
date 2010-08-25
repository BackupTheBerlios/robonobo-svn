package com.robonobo.gui.components;

import java.awt.Dimension;

import javax.swing.JList;
import javax.swing.ListModel;
import javax.swing.RepaintManager;

import com.robonobo.gui.RoboFont;
import com.robonobo.gui.frames.RobonoboFrame;
import com.robonobo.gui.panels.LeftSidebar;

@SuppressWarnings("serial")
public abstract class LeftSidebarList extends JList implements LeftSidebarComponent {
	RobonoboFrame frame;
	LeftSidebar sideBar;
	int selectedIndex = -1;
	
	public LeftSidebarList(LeftSidebar sideBar, RobonoboFrame frame, ListModel lm) {
		super(lm);
		this.sideBar = sideBar;
		this.frame = frame;
		setName("robonobo.playlist.list");
		setFont(RoboFont.getFont(11, false));
		setAlignmentX(0.0f);
		setMaximumSize(new Dimension(65535, 50));
	}
	
	public void relinquishSelection() {
		selectedIndex = -1;
		RepaintManager.currentManager(this).markCompletelyDirty(this);
	}
}
