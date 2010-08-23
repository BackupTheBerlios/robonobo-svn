package com.robonobo.gui.panels;

import java.awt.Color;
import java.awt.Dimension;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.debian.tablelayout.TableLayout;

import com.robonobo.gui.RobonoboFont;
import com.robonobo.gui.frames.RobonoboFrame;

@SuppressWarnings("serial")
public class StatusPanel extends JPanel {
	static final Color DARK_BG = new Color(28, 28, 28);
	static final Color BALANCE_FG = new Color(0xfe, 0xd2, 0x05);

	public StatusPanel() {
		setPreferredSize(new Dimension(200, 85));
		setMaximumSize(new Dimension(200, 85));
		double[][] cellSizen = { { 10, 32, 5, 100, 10, TableLayout.FILL, 5 }, { 10, 30, 5, 15, 15, 5, TableLayout.FILL} };
		setLayout(new TableLayout(cellSizen));
		setName("robonobo.status.panel");
		setOpaque(true);
		
		JLabel balanceLabel = new JLabel(new ImageIcon(RobonoboFrame.class.getResource("/img/icon/wang_symbol.png")));
		balanceLabel.setText("345.00");
		balanceLabel.setForeground(BALANCE_FG);
		balanceLabel.setFont(RobonoboFont.getFont(22, false));
		add(balanceLabel, "2,1,3,1,LEFT,CENTER");
		
		JLabel queryLabel = new JLabel(" ?");
		queryLabel.setForeground(BALANCE_FG);
		queryLabel.setFont(RobonoboFont.getFont(12, false));
		add(queryLabel, "4,1,LEFT,TOP");

		JLabel networkStatusIcon = new JLabel(new ImageIcon(RobonoboFrame.class.getResource("/img/icon/connection_ok.png")));
		add(networkStatusIcon, "1,3,1,5");
		
		JLabel numConnsLbl = new JLabel("4 Connections");
		numConnsLbl.setFont(RobonoboFont.getFont(9, false));
		numConnsLbl.setForeground(Color.WHITE);
		add(numConnsLbl, "3,3,5,3,LEFT,BOTTOM");
		
		JLabel bwLbl = new JLabel("25 KB/s up - 5 KB/s down");
		bwLbl.setFont(RobonoboFont.getFont(9, false));
		bwLbl.setForeground(Color.WHITE);
		add(bwLbl, "3,4,5,4,LEFT,BOTTOM");
	}
}
