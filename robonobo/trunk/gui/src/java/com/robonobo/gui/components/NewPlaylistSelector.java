package com.robonobo.gui.components;

import com.robonobo.gui.frames.RobonoboFrame;
import com.robonobo.gui.panels.LeftSidebar;
import static com.robonobo.gui.GUIUtils.*;

public class NewPlaylistSelector extends LeftSidebarSelector {
	public NewPlaylistSelector(LeftSidebar sideBar, RobonoboFrame frame) {
		super(sideBar, frame, "New Playlist", false, createImageIcon("/img/icon/new_playlist.png", null), "newplaylist");
	}
}
