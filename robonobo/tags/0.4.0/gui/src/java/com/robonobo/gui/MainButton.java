package com.robonobo.gui;

import javax.swing.JButton;

public class MainButton extends JButton {
	public MainButton(String pathToIconImg, String desc) {
		super(GUIUtils.createImageIcon(pathToIconImg, desc));
		setToolTipText(desc);
	}
}
