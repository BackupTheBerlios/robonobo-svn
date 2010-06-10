package com.robonobo.gui.laf;

import info.clearthought.layout.TableLayout;

import java.awt.Dimension;

import javax.swing.JFrame;

@SuppressWarnings("serial")
public class EditPlaylistFrame extends JFrame {
	private Dimension initSize = new Dimension(1024, 768);

	public EditPlaylistFrame() {
		setPreferredSize(initSize);
		setSize(initSize);
		setTitle("robonobo");
		double[][] cellSizen = { { 205, TableLayout.FILL }, { TableLayout.FILL } };
		setLayout(new TableLayout(cellSizen));

		setJMenuBar(new MenuBar());
		add(new SidebarContainerPanel(), "0,0");
		add(new EditPlaylistMainPanel(), "1,0");
	}
	
}
