package com.robonobo.gui.laf;

import info.clearthought.layout.TableLayout;

import java.awt.Dimension;

import javax.swing.JFrame;
import javax.swing.JLabel;

@SuppressWarnings("serial")
public class ImportFrame extends JFrame {
	private Dimension initSize = new Dimension(1024, 768);

	public ImportFrame() {
		setPreferredSize(initSize);
		setSize(initSize);
		setTitle("robonobo import");
		double[][] cellSizen = { { TableLayout.FILL, 250, TableLayout.FILL }, {270, 30, 30, 35, 10, 35, TableLayout.FILL, 30, 20, 50} };
		setLayout(new TableLayout(cellSizen));
		add(new JLabel("Your Library is empty.", null, JLabel.CENTER), "1,1");
		add(new ImportButton("Import files to your library"), "1,3");
		add(new ImportButton("Do it later."), "1,5");
		add(new JLabel("<html>Status:<b>Connected</b></html>", null, JLabel.CENTER), "1,7");
		add(new NetworkStatusPanel(), "1,9");
	}
}
