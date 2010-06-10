package com.robonobo.gui.laf;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

@SuppressWarnings("serial")
public class SidebarUserItem extends JPanel {
	String userName;
	JLabel myLbl;
	
	public SidebarUserItem(String userName, boolean expanded) {
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		this.userName = userName;
		if(expanded)
			myLbl = new JLabel(userName, GuiUtil.createImageIcon("/img/sidebaruser-expanded.png", userName), JLabel.LEFT);
		else
			myLbl = new JLabel(userName, GuiUtil.createImageIcon("/img/sidebaruser-collapsed.png", userName), JLabel.LEFT);
		add(myLbl);
	}
	
	public void setExpanded(boolean expanded) {
		if(expanded)
			myLbl.setIcon(GuiUtil.createImageIcon("/img/sidebaruser-expanded.png", userName));
		else
			myLbl.setIcon(GuiUtil.createImageIcon("/img/sidebaruser-collapsed.png", userName));
	}	
}
