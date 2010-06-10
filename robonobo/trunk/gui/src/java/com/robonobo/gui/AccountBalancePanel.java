package com.robonobo.gui;

import info.clearthought.layout.TableLayout;

import java.awt.Color;

import javax.swing.JLabel;
import javax.swing.JPanel;

public class AccountBalancePanel extends JPanel {
	public AccountBalancePanel() {
		double[][] cellSizen = { 
				{ 10, 20, 50, TableLayout.FILL },
				{ TableLayout.FILL } 
			};
		setLayout(new TableLayout(cellSizen));
		// TODO: Replace this with real symbol
		add(new JLabel("W"), "1,0");
		JLabel balanceLbl = new JLabel("0.00");
		balanceLbl.setForeground(Color.RED);
		add(balanceLbl, "2,0");
	}
}
