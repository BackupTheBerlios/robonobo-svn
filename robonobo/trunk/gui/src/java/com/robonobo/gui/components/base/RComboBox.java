package com.robonobo.gui.components.base;

import java.awt.Font;
import java.util.Vector;

import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;

import com.robonobo.gui.RoboFont;

public class RComboBox extends JComboBox {

	public RComboBox() {
		super();
		setupFont();
	}

	public RComboBox(ComboBoxModel aModel) {
		super(aModel);
		setupFont();
	}

	public RComboBox(Object[] items) {
		super(items);
		setupFont();
	}

	public RComboBox(Vector<?> items) {
		super(items);
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
