package com.robonobo.gui.components;

import static com.robonobo.gui.GUIUtils.*;

import com.robonobo.gui.frames.RobonoboFrame;
import com.robonobo.gui.panels.LeftSidebar;

public class MyMusicSelector extends LeftSidebarSelector {
	public MyMusicSelector(LeftSidebar sideBar, RobonoboFrame frame) {
		super(sideBar, frame, "My Music Library", true, createImageIcon("/img/icon/home.png", null), "mymusiclibrary");
	}
}
