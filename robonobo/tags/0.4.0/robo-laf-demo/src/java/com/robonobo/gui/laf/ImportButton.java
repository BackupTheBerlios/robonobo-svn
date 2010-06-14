package com.robonobo.gui.laf;

import javax.swing.Icon;
import javax.swing.JButton;

@SuppressWarnings("serial")
public class ImportButton extends JButton {

	public ImportButton(Icon icon) {
		super(icon);
	}

	public ImportButton(String text, Icon icon) {
		super(text, icon);
	}

	public ImportButton(String text) {
		super(text);
	}
	
}
