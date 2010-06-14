package com.robonobo.gui.preferences;

import info.clearthought.layout.TableLayout;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import com.robonobo.common.swing.IntegerTextField;
import com.robonobo.core.api.config.RobonoboConfig;
import com.robonobo.gui.RobonoboFrame;

@SuppressWarnings("serial")
public class GatewayPrefPanel extends PrefPanel {
	JRadioButton autoBut, neverBut, manualBut;
	JTextField manualPort;
	RobonoboConfig roboCfg;
	
	public GatewayPrefPanel(RobonoboFrame frame) {
		super(frame);
		double[][] cellSizen = { { 5, TableLayout.FILL, 5, 180, 5, 45, 5 }, { 25, 5, 25, 5, 25 } };
		setLayout(new TableLayout(cellSizen));
		add(new JLabel("Router IP address and port"), "1,0");

		roboCfg = frame.getController().getConfig();
		ButtonGroup butGr = new ButtonGroup();
		ActionListener radLis = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				manualPort.setEnabled(manualBut.isSelected());
			}
		};
		
		autoBut = new JRadioButton("Automatically detect");
		autoBut.addActionListener(radLis);
		butGr.add(autoBut);
		add(autoBut, "3,0,5,0");
		
		neverBut = new JRadioButton("Disable (reduces performance)");
		neverBut.addActionListener(radLis);
		butGr.add(neverBut);
		add(neverBut, "3,2,5,2");
		
		manualBut = new JRadioButton("Manual: use router port");
		manualBut.addActionListener(radLis);
		butGr.add(manualBut);
		add(manualBut, "3,4");
		manualPort = new IntegerTextField(null, false);
		add(manualPort, "5,4");
		
		resetValue();
		setMaximumSize(new Dimension(478, 87));
	}

	public void resetValue() {
		manualPort.setText(null);
		String cfgMode = roboCfg.getGatewayCfgMode();
		if(cfgMode.equals("auto"))
			autoBut.doClick();
		else if(cfgMode.equals("off"))
			neverBut.doClick();
		else {
			manualBut.doClick();
			int port = Integer.parseInt(cfgMode);
			manualPort.setText(String.valueOf(port));
		}
	}
	
	@Override
	public void applyChanges() {
		roboCfg.setGatewayCfgMode(getCfgMode());
	}

	@Override
	public boolean hasChanged() {
		return !(roboCfg.getGatewayCfgMode().equals(getCfgMode()));
	}

	private String getCfgMode() {
		if(autoBut.isSelected())
			return "auto";
		if(neverBut.isSelected())
			return "off";
		return String.valueOf(manualPort.getText());
	}
}
