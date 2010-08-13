package com.robonobo.gui.preferences;

import java.awt.Dimension;

import javax.swing.JLabel;
import javax.swing.JTextField;

import org.debian.tablelayout.TableLayout;

import com.robonobo.common.swing.IntegerTextField;
import com.robonobo.gui.RobonoboFrame;

@SuppressWarnings("serial")
public class IntPrefPanel extends PrefPanel {
	JTextField textField;
	String propName;
	
	public IntPrefPanel(RobonoboFrame frame, String propName, String description, boolean allowNegative) {
		super(frame);
		double[][] cellSizen = { { 5, TableLayout.FILL, 185, 50, 5 }, { 25 } };
		setLayout(new TableLayout(cellSizen));
		add(new JLabel(description), "1,0");
		textField = new IntegerTextField(Integer.parseInt(getProperty(propName)), allowNegative);
		add(textField, "3,0");
		this.propName = propName;
		setMaximumSize(new Dimension(478, 27));
	}
	
	@Override
	public void applyChanges() {
		String text = textField.getText();
		Integer.parseInt(text);
		setProperty(propName, text);
	}

	@Override
	public boolean hasChanged() {
		return !(textField.getText().equals(getProperty(propName)));
	}	
	
	@Override
	public void resetValue() {
		textField.setText(getProperty(propName));
	}
}
