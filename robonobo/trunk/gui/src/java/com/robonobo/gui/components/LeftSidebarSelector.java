package com.robonobo.gui.components;

import static com.robonobo.gui.RoboColor.*;

import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.*;

import com.robonobo.gui.RoboFont;
import com.robonobo.gui.frames.RobonoboFrame;
import com.robonobo.gui.panels.LeftSidebar;

/**
 * A simple label in the left sidebar that can be selected
 * @author macavity
 *
 */
public abstract class LeftSidebarSelector extends JPanel implements LeftSidebarComponent {
	protected LeftSidebar sideBar;
	protected RobonoboFrame frame;
	protected String contentPanelName;

	public LeftSidebarSelector(LeftSidebar sideBar, RobonoboFrame frame, String label, boolean lblBold, Icon icon, String contentPanelName) {
		this.sideBar = sideBar;
		this.frame = frame;
		this.contentPanelName = contentPanelName;
		setOpaque(true);
		setAlignmentX(0f);
		setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setPreferredSize(new Dimension(185, 19));
		setMinimumSize(new Dimension(185, 19));
		setMaximumSize(new Dimension(185, 19));
		JLabel textLbl = new JLabel(label, icon, JLabel.LEFT);
		textLbl.setFont(RoboFont.getFont(11, lblBold));
		textLbl.setOpaque(false);
		add(textLbl);
		addMouseListener(new MouseListener());

	}

	public void relinquishSelection() {
		setSelected(false);
	}
	
	public void setSelected(boolean isSelected) {
		if (isSelected) {
			frame.getMainPanel().selectContentPanel(contentPanelName);
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
