package com.robonobo.gui.components;

import static com.robonobo.gui.RoboColor.*;
import static com.robonobo.gui.GUIUtils.*;

import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.RepaintManager;

import com.robonobo.gui.RoboFont;
import com.robonobo.gui.frames.RobonoboFrame;
import com.robonobo.gui.panels.LeftSidebar;

public class MyMusicSelector extends JPanel implements LeftSidebarComponent {
	private LeftSidebar sideBar;
	private RobonoboFrame frame;

	boolean isSelected;

	public MyMusicSelector(LeftSidebar sideBar, RobonoboFrame frame) {
		this.sideBar = sideBar;
		this.frame = frame;
		setOpaque(true);
		setAlignmentX(0f);
		setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setPreferredSize(new Dimension(185, 19));
		setMinimumSize(new Dimension(185, 19));
		setMaximumSize(new Dimension(185, 19));
		JLabel myMusicLbl = new JLabel("My Music Library", createImageIcon("/img/icon/home.png", null), JLabel.LEFT);
		myMusicLbl.setFont(RoboFont.getFont(11, true));
		myMusicLbl.setOpaque(false);
		add(myMusicLbl);
		setSelected(true);
		addMouseListener(new MouseListener());
	}

	public void relinquishSelection() {
		setSelected(false);
	}

	private void setSelected(boolean isSelected) {
		if (isSelected) {
			setBackground(LIGHT_GRAY);
			setForeground(BLUE_GRAY);
			sideBar.clearSelectionExcept(this);
		} else {
			setBackground(MID_GRAY);
			setForeground(DARK_GRAY);
		}
		RepaintManager.currentManager(this).markCompletelyDirty(this);
	}

	class MouseListener extends MouseAdapter {
		@Override
		public void mouseClicked(MouseEvent e) {
			setSelected(true);
			e.consume();
		}
	}
}
