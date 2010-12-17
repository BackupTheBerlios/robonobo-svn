package com.robonobo.gui.components.base;

import java.awt.Font;

import javax.swing.JTextPane;
import javax.swing.text.StyledDocument;

import com.robonobo.gui.RoboFont;

public class RTextPane extends JTextPane {
	
	public RTextPane() {
		super();
		setupFont();
	}

	public RTextPane(StyledDocument doc) {
		super(doc);
		setupFont();
	}

	protected void setupFont() {
		Font font = getRFont();
		if(font != null)	
			setFont(font);
	}

	protected Font getRFont() {
		return RoboFont.getFont(12, false);
	}

}
