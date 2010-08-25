package com.robonobo.oldgui;

import javax.swing.JButton;

import com.robonobo.gui.GUIUtils;

public class MainButton extends JButton {
	public MainButton(String pathToIconImg, String desc) {
		super(GUIUtils.createImageIcon(pathToIconImg, desc));
		setToolTipText(desc);
	}
}
