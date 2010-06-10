package com.robonobo.gui.laf;

import javax.swing.Icon;
import javax.swing.JButton;

@SuppressWarnings("serial")
public class DialogButton extends JButton {

	public DialogButton(Icon icon) {
		super(icon);
	}

	public DialogButton(String text, Icon icon) {
		super(text, icon);
	}

	public DialogButton(String text) {
		super(text);
	}
	
}
