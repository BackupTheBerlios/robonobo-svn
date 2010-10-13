package com.robonobo.gui.panels;

import static com.robonobo.gui.RoboColor.*;

import java.awt.Color;
import java.awt.Dimension;
import java.text.NumberFormat;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.debian.tablelayout.TableLayout;

import com.robonobo.common.concurrent.CatchingRunnable;
import com.robonobo.core.RobonoboController;
import com.robonobo.core.wang.WangListener;
import com.robonobo.gui.RoboFont;
import com.robonobo.gui.frames.RobonoboFrame;

@SuppressWarnings("serial")
public class StatusPanel extends JPanel implements WangListener {
	RobonoboController control;
	JLabel balanceLabel;
	NumberFormat balanceFormat;
	
	public StatusPanel(RobonoboFrame frame) {
		this.control = frame.getController();
		setPreferredSize(new Dimension(200, 85));
		setMaximumSize(new Dimension(200, 85));
		double[][] cellSizen = { { 10, 32, 5, 100, 10, TableLayout.FILL, 5 }, { 10, 30, 5, 15, 15, 5, TableLayout.FILL} };
		setLayout(new TableLayout(cellSizen));
		setName("robonobo.status.panel");
		setOpaque(true);
		
		balanceLabel = new JLabel(new ImageIcon(RobonoboFrame.class.getResource("/img/icon/wang_symbol.png")));
		balanceLabel.setText("345.00");
		balanceLabel.setForeground(ORANGE);
		balanceLabel.setFont(RoboFont.getFont(22, false));
		add(balanceLabel, "2,1,3,1,LEFT,CENTER");
		
		JLabel queryLabel = new JLabel(" ?");
		queryLabel.setForeground(ORANGE);
		queryLabel.setFont(RoboFont.getFont(12, false));
		add(queryLabel, "4,1,LEFT,TOP");

		JLabel networkStatusIcon = new JLabel(new ImageIcon(RobonoboFrame.class.getResource("/img/icon/connection_ok.png")));
		add(networkStatusIcon, "1,3,1,5");
		
		JLabel numConnsLbl = new JLabel("4 Connections");
		numConnsLbl.setFont(RoboFont.getFont(9, false));
		numConnsLbl.setForeground(Color.WHITE);
		add(numConnsLbl, "3,3,5,3,LEFT,BOTTOM");
		
		JLabel bwLbl = new JLabel("25 KB/s up - 5 KB/s down");
		bwLbl.setFont(RoboFont.getFont(9, false));
		bwLbl.setForeground(Color.WHITE);
		add(bwLbl, "3,4,5,4,LEFT,BOTTOM");
		
//		balanceFormat = new NumberFormat();
//		control.addWangListener(this);
	}

	@Override
	public void balanceChanged(final double newBalance) {
//		SwingUtilities.invokeLater(new CatchingRunnable() {
//			public void doRun() throws Exception {
//				numberfor
//				balanceLabel.setText(TOOL_TIP_TEXT_KEY)
//			}
//		})
	}
}
