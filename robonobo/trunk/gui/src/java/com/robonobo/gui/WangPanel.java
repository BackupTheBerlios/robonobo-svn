package com.robonobo.gui;

import java.awt.Color;
import java.text.NumberFormat;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.debian.tablelayout.TableLayout;

import com.robonobo.common.concurrent.CatchingRunnable;
import com.robonobo.core.RobonoboController;
import com.robonobo.core.wang.WangListener;

public class WangPanel extends JPanel implements WangListener {
	static double MIN_OK_BALANCE = 1d;
	
	NumberFormat nf;
	RobonoboController controller;
	JLabel balanceLbl;

	public WangPanel(RobonoboController controller) {
		this.controller = controller;
		double[][] cellSizen = { { 12, TableLayout.FILL }, { TableLayout.FILL } };
		setLayout(new TableLayout(cellSizen));
		add(new JLabel(GUIUtils.createImageIcon("/img/wang-small.png", null)), "0,0");
		balanceLbl = new JLabel("0.000");
		balanceLbl.setForeground(Color.RED);
		add(balanceLbl, "1,0");
		nf = NumberFormat.getNumberInstance();
		nf.setMaximumFractionDigits(3);
		nf.setMinimumFractionDigits(3);
		controller.addWangListener(this);
	}

	public void balanceChanged(final double newBalance) {
		SwingUtilities.invokeLater(new CatchingRunnable() {
			public void doRun() throws Exception {
				balanceLbl.setText(nf.format(newBalance));
				if(newBalance >= MIN_OK_BALANCE)
					balanceLbl.setForeground(Color.GREEN);
				else
					balanceLbl.setForeground(Color.RED);
			}
		});
	}
}
