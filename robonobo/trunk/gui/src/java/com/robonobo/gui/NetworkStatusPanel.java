package com.robonobo.gui;

import java.awt.Color;
import java.awt.Font;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.debian.tablelayout.TableLayout;

import com.robonobo.common.concurrent.CatchingRunnable;
import com.robonobo.core.RobonoboController;
import com.robonobo.core.api.RobonoboStatus;
import com.robonobo.core.api.RobonoboStatusListener;

public class NetworkStatusPanel extends JPanel implements RobonoboStatusListener {
	private JLabel statusLbl;
	private RobonoboController controller;
	private Log log = LogFactory.getLog(getClass());

	public NetworkStatusPanel(RobonoboController controller) {
		this.controller = controller;
		double[][] cellSizen = { 
				{ 10, TableLayout.FILL },
				{ 5, TableLayout.FILL } 
		};
		setLayout(new TableLayout(cellSizen));
		statusLbl = new JLabel("");
		statusLbl.setFont(new Font("sans-serif", Font.PLAIN, 10));
		add(statusLbl, "1,1");
		controller.addRobonoboStatusListener(this);
		statusChanged();
	}

	public void statusChanged() {
		final RobonoboStatus status = controller.getStatus();
		log.warn("NetworkStatusPanel: "+status);
		SwingUtilities.invokeLater(new CatchingRunnable() {
			@Override
			public void doRun() throws Exception {
				if(status == RobonoboStatus.Connected) {
					statusLbl.setText("Connected");
					statusLbl.setForeground(Color.GREEN);
				} else {
					statusLbl.setText("Not connected");
					statusLbl.setForeground(Color.RED);
				}
			}
		});
	}
}
