package com.robonobo.gui.laf;

import javax.swing.JMenu;
import javax.swing.JMenuBar;

@SuppressWarnings("serial")
public class MenuBar extends JMenuBar {
	public MenuBar() {
		add(new JMenu("File"));
		add(new JMenu("Network"));
		add(new JMenu("Options"));
		add(new JMenu("Debug"));
		add(new JMenu("Help"));
	}
}
