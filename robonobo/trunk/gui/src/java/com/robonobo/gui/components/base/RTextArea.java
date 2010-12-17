package com.robonobo.gui.components.base;

import java.awt.Font;

import javax.swing.JTextArea;
import javax.swing.text.Document;

import com.robonobo.gui.RoboFont;

public class RTextArea extends JTextArea {
	public RTextArea() {
		super();
		setupFont();
	}

	public RTextArea(Document doc, String text, int rows, int columns) {
		super(doc, text, rows, columns);
		setupFont();
	}

	public RTextArea(Document doc) {
		super(doc);
		setupFont();
	}

	public RTextArea(int rows, int columns) {
		super(rows, columns);
		setupFont();
	}

	public RTextArea(String text, int rows, int columns) {
		super(text, rows, columns);
		setupFont();
	}

	public RTextArea(String text) {
		super(text);
		setupFont();
	}

	protected void setupFont() {
		Font font = getRFont();
		if(font != null)	
			setFont(font);
	}

	protected Font getRFont() {
		return RoboFont.getFont(11, false);
	}

}
