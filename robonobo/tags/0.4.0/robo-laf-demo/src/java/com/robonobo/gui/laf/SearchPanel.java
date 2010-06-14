package com.robonobo.gui.laf;

import info.clearthought.layout.TableLayout;

import javax.swing.JPanel;
import javax.swing.JTextField;

@SuppressWarnings("serial")
public class SearchPanel extends JPanel {
	public SearchPanel() {
		double[][] cellSizen = { { TableLayout.FILL, 260, 5, 60 }, { TableLayout.FILL } };
		setLayout(new TableLayout(cellSizen));
		add(new JTextField(), "1,0");
		add(new SearchPanelButton("Search"), "3,0");
	}
}
