package com.robonobo.gui.components;

import static com.robonobo.gui.RoboColor.*;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;

import com.robonobo.gui.RoboFont;
import com.robonobo.gui.frames.RobonoboFrame;
import com.robonobo.gui.model.ActiveSearchListModel;
import com.robonobo.gui.panels.LeftSidebar;

@SuppressWarnings("serial")
public class ActiveSearchList extends LeftSidebarList {
	/** Clicks with an X between mincloseclick and maxcloseclick we take to be on the close 'X' **/
	private static final int MIN_CLOSE_CLICK = 163;
	private static final int MAX_CLOSE_CLICK = 176;

	public ActiveSearchList(LeftSidebar sideBar, final RobonoboFrame frame) {
		super(sideBar, frame, new ActiveSearchListModel(frame.getController()));
		setCellRenderer(new CellRenderer());
		addMouseListener(new MouseListener());
	}

	private class MouseListener extends MouseAdapter {
		public void mouseClicked(MouseEvent e) {
			int clickIdx = locationToIndex(new Point(e.getX(), e.getY()));
			if(clickIdx < 0)
				return;
			if (e.getX() >= MIN_CLOSE_CLICK && e.getX() <= MAX_CLOSE_CLICK) {
				// They clicked on the close 'X'
				((ActiveSearchListModel) getModel()).removeElementAt(clickIdx);
				// TODO Bring another main panel to the front
				e.consume();
			} 
		}
	}

	class CellRenderer extends DefaultListCellRenderer {
		JLabel textLbl;
		JLabel closeLbl;
		JPanel pnl;

		public CellRenderer() {
			textLbl = new JLabel();
			textLbl.setOpaque(false);
			textLbl.setIcon(new ImageIcon(ActiveSearchList.class.getResource("/img/icon/magnifier_small.png")));
			textLbl.setMaximumSize(new Dimension(160, 65535));
			textLbl.setPreferredSize(new Dimension(160, 65535));
			textLbl.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
			textLbl.setFont(RoboFont.getFont(11, false));
			closeLbl = new JLabel(new ImageIcon(ActiveSearchList.class.getResource("/img/icon/red_x_small.png")));
			closeLbl.setOpaque(false);
			closeLbl.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
			pnl = new JPanel();
			pnl.setLayout(new BoxLayout(pnl, BoxLayout.X_AXIS));
			pnl.add(textLbl);
			pnl.add(closeLbl);
			pnl.setOpaque(true);
			pnl.setMaximumSize(new Dimension(65535, 65535));
		}

		public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
			String searchStr = (String) value;
			textLbl.setText(searchStr);
			if (index == getSelectedIndex()) {
				pnl.setBackground(LIGHT_GRAY);
				textLbl.setForeground(BLUE_GRAY);
			} else {
				pnl.setBackground(MID_GRAY);
				textLbl.setForeground(DARK_GRAY);
			}
			return pnl;
		}
	}
}
