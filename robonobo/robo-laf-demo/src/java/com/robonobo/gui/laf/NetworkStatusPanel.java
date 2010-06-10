package com.robonobo.gui.laf;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

@SuppressWarnings("serial")
public class NetworkStatusPanel extends JPanel {
	public NetworkStatusPanel() {
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		add(new JLabel(GuiUtil.createImageIcon("/img/connect-world.png", "Network status")));
		add(new JLabel("<html>22/108 Linked Connections<br/>25k up/ 5k down"));
	}
}
