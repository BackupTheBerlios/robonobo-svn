package com.robonobo.gui.preferences;

import java.awt.Dimension;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JRadioButton;

import org.debian.tablelayout.TableLayout;

import com.robonobo.gui.RobonoboFrame;

@SuppressWarnings("serial")
public class BoolPrefPanel extends PrefPanel {
	JRadioButton trueBut;
	JRadioButton falseBut;
	String propName;
	
	public BoolPrefPanel(RobonoboFrame frame, String propName, String description) {
		super(frame);
		this.propName = propName;
		double[][] cellSizen = { { 5, TableLayout.FILL, 140, 50, 5, 45, 5 }, { 25 } };
		setLayout(new TableLayout(cellSizen));
		add(new JLabel(description), "1,0");
		trueBut = new JRadioButton("Yes");
		falseBut = new JRadioButton("No");
		ButtonGroup butGr = new ButtonGroup();
		butGr.add(trueBut);
		butGr.add(falseBut);
		resetValue();
		add(trueBut, "3,0");
		add(falseBut, "5,0");
		setMaximumSize(new Dimension(478, 27));
	}

	public void resetValue() {
		boolean propVal = Boolean.parseBoolean(getProperty(propName));
		if(propVal)
			trueBut.setSelected(true);
		else
			falseBut.setSelected(true);
	}
	
	@Override
	public void applyChanges() {
		setProperty(propName, String.valueOf(trueBut.isSelected()));
	}

	@Override
	public boolean hasChanged() {
		return !(trueBut.isSelected() == Boolean.parseBoolean(getProperty(propName)));
	}
}
