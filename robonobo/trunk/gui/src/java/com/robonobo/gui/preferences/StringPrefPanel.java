package com.robonobo.gui.preferences;

import java.awt.Dimension;

import javax.swing.JLabel;
import javax.swing.JTextField;

import org.debian.tablelayout.TableLayout;

import com.robonobo.gui.RobonoboFrame;

@SuppressWarnings("serial")
public class StringPrefPanel extends PrefPanel {
	JTextField textField;
	String propName;
	
	public StringPrefPanel(RobonoboFrame frame, String propName, String description) {
		super(frame);
		double[][] cellSizen = { { 5, TableLayout.FILL, 5, 230, 5 }, { 25 } };
		setLayout(new TableLayout(cellSizen));
		add(new JLabel(description), "1,0");
		textField = new JTextField(getProperty(propName));
		add(textField, "3,0");
		this.propName = propName;
		setMaximumSize(new Dimension(478, 27));
	}
	
	@Override
	public void applyChanges() {
		setProperty(propName, textField.getText());
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
