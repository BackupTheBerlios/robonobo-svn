package com.robonobo.gui.laf;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

@SuppressWarnings("serial")
public class WangBalancePanel extends JPanel {
	public WangBalancePanel() {
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		add(new JLabel(GuiUtil.createImageIcon("/img/wangsymbol.png", "Wang balance")));
		add(new JLabel("345.00"));
		add(new JLabel("?"));
	}
}
