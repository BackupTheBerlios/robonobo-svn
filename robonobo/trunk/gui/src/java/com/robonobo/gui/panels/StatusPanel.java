package com.robonobo.gui.panels;

import static com.robonobo.gui.RoboColor.*;

import java.awt.Color;
import java.awt.Dimension;
import java.text.NumberFormat;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.debian.tablelayout.TableLayout;

import com.robonobo.common.concurrent.CatchingRunnable;
import com.robonobo.common.util.FileUtil;
import com.robonobo.common.util.TextUtil;
import com.robonobo.core.RobonoboController;
import com.robonobo.core.api.*;
import com.robonobo.core.wang.WangListener;
import com.robonobo.gui.RoboFont;
import com.robonobo.gui.frames.RobonoboFrame;
import com.robonobo.mina.external.ConnectedNode;

@SuppressWarnings("serial")
public class StatusPanel extends JPanel implements WangListener, RobonoboStatusListener, TransferSpeedListener {
	Log log = LogFactory.getLog(getClass());

	RobonoboController control;
	JLabel balanceLabel;
	NumberFormat balanceFormat;
	private ImageIcon connFailImg;
	private ImageIcon connOkImg;
	private JLabel networkStatusIcon;
	private JLabel numConnsLbl;

	private JLabel bandwidthLbl;
	
	public StatusPanel(RobonoboFrame frame) {
		this.control = frame.getController();
		setPreferredSize(new Dimension(200, 85));
		setMaximumSize(new Dimension(200, 85));
		double[][] cellSizen = { { 10, 32, 5, 100, 10, TableLayout.FILL, 5 }, { 10, 30, 5, 15, 15, 5, TableLayout.FILL} };
		setLayout(new TableLayout(cellSizen));
		setName("robonobo.status.panel");
		setOpaque(true);
		
		balanceLabel = new JLabel(new ImageIcon(RobonoboFrame.class.getResource("/img/icon/wang_symbol.png")));
		balanceLabel.setForeground(ORANGE);
		balanceLabel.setFont(RoboFont.getFont(22, false));
		add(balanceLabel, "2,1,3,1,LEFT,CENTER");
		balanceFormat = NumberFormat.getInstance();
		balanceFormat.setMaximumFractionDigits(2);
		balanceFormat.setMinimumFractionDigits(2);

		JLabel queryLabel = new JLabel("?");
		queryLabel.setForeground(ORANGE);
		queryLabel.setFont(RoboFont.getFont(12, false));
		add(queryLabel, "4,1,LEFT,TOP");

		connOkImg = new ImageIcon(RobonoboFrame.class.getResource("/img/icon/connection_ok.png"));
		connFailImg = new ImageIcon(RobonoboFrame.class.getResource("/img/icon/connection_fail.png"));
		networkStatusIcon = new JLabel(connFailImg);
		add(networkStatusIcon, "1,3,1,5");
		
		numConnsLbl = new JLabel("Starting...");
		numConnsLbl.setFont(RoboFont.getFont(9, false));
		numConnsLbl.setForeground(Color.WHITE);
		add(numConnsLbl, "3,3,5,3,LEFT,BOTTOM");

		bandwidthLbl = new JLabel("");
		bandwidthLbl.setFont(RoboFont.getFont(9, false));
		bandwidthLbl.setForeground(Color.WHITE);
		add(bandwidthLbl, "3,4,5,4,LEFT,BOTTOM");

		control.addRobonoboStatusListener(this);
		control.addWangListener(this);
		setBalance(0d);
		updateConnStatus();		
	}

	@Override
	public void balanceChanged(final double newBalance) {
		setBalance(newBalance);
	}
	
	@Override
	public void roboStatusChanged() {
		updateConnStatus();
	}
	
	@Override
	public void connectionAdded(ConnectedNode node) {
		updateConnStatus();
	}
	
	@Override
	public void connectionLost(ConnectedNode node) {
		updateConnStatus();
	}

	private void setBalance(final double newBalance) {
		SwingUtilities.invokeLater(new CatchingRunnable() {
			public void doRun() throws Exception {
				synchronized (StatusPanel.this) {
					// TODO Make it red if they have no ends
					String balanceTxt = balanceFormat.format(newBalance);
					balanceLabel.setText(balanceTxt);
				}
			}
		});
	}
	
	private void updateConnStatus() {
		SwingUtilities.invokeLater(new CatchingRunnable() {
			public void doRun() throws Exception {
				synchronized (StatusPanel.this) {
					RobonoboStatus status = control.getStatus();
					switch(status) {
					case Starting:
						numConnsLbl.setText("Starting...");
						networkStatusIcon.setIcon(connFailImg);
						break;
					case NotConnected:
						numConnsLbl.setText("No Connections");
						networkStatusIcon.setIcon(connFailImg);
						break;
					case Connected:
						List<ConnectedNode> nodes = control.getConnectedNodes();
						if(nodes.size() > 0)
							networkStatusIcon.setIcon(connOkImg);
						else
							networkStatusIcon.setIcon(connFailImg);
						numConnsLbl.setText(TextUtil.numItems(nodes, "Connection"));
						break;
					case Stopping:
						numConnsLbl.setText("Stopping...");
					}
				}
			}
		});
	}
	
	@Override
	public void newTransferSpeeds(Map<String, TransferSpeed> speedsByStream, Map<String, TransferSpeed> speedsByNode) {
		int totalDown = 0;
		int totalUp = 0;
		for (TransferSpeed ts : speedsByNode.values()) {
			totalDown += ts.download;
			totalUp += ts.upload;
		}
		updateSpeeds(totalDown, totalUp);
	}
	
	private void updateSpeeds(final int downloadBps, final int uploadBps) {
		SwingUtilities.invokeLater(new CatchingRunnable() {
			public void doRun() throws Exception {
				StringBuffer sb = new StringBuffer();
				sb.append(FileUtil.humanReadableSize(uploadBps)).append("/s");
				sb.append(" up - ");
				sb.append(FileUtil.humanReadableSize(downloadBps)).append("/s");
				sb.append(" down");
				synchronized (StatusPanel.this) {
					bandwidthLbl.setText(sb.toString());
				}
			}
		});
	}
}
