package com.robonobo.gui.components;

import static com.robonobo.gui.GUIUtils.createImageIcon;
import static com.robonobo.gui.RoboColor.BLUE_GRAY;
import static com.robonobo.gui.RoboColor.DARK_GRAY;
import static com.robonobo.gui.RoboColor.LIGHT_GRAY;
import static com.robonobo.gui.RoboColor.MID_GRAY;

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;

import org.jdesktop.swingx.renderer.DefaultListRenderer;

import com.robonobo.gui.RoboFont;
import com.robonobo.gui.frames.RobonoboFrame;
import com.robonobo.gui.model.PlaylistListModel;
import com.robonobo.gui.panels.LeftSidebar;

@SuppressWarnings("serial")
public class PlaylistList extends LeftSidebarList {
	private static final int MAX_LBL_WIDTH = 170;
	
	ImageIcon newPlaylistIcon;
	ImageIcon playlistIcon;
		
	public PlaylistList(LeftSidebar sideBar, RobonoboFrame frame) {
		super(sideBar, frame, new PlaylistListModel(frame.getController()));
		newPlaylistIcon = createImageIcon("/img/icon/new_playlist.png", null);
		playlistIcon = createImageIcon("/img/icon/playlist.png", null);
		setCellRenderer(new CellRenderer());
		setName("robonobo.playlist.list");
		setAlignmentX(0.0f);
		setMaximumSize(new Dimension(65535, 65535));
	}

	@Override
	protected void itemSelected(int index) {
		// TODO something
	}
	
	class CellRenderer extends DefaultListRenderer {
		JLabel lbl = new JLabel();

		public CellRenderer() {
			lbl = new JLabel();
			lbl.setOpaque(true);
			lbl.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
			lbl.setFont(RoboFont.getFont(11, false));
			lbl.setMaximumSize(new Dimension(MAX_LBL_WIDTH, 65535));
			lbl.setPreferredSize(new Dimension(MAX_LBL_WIDTH, 65535));
		}
		
		@Override
		public Component getListCellRendererComponent(JList list, Object value,
				int index, boolean isSelected, boolean cellHasFocus) {
			lbl.setText((String) value);
			if(index == 0)
				lbl.setIcon(newPlaylistIcon);
			else
				lbl.setIcon(playlistIcon);
			if(isSelected) {
				lbl.setBackground(LIGHT_GRAY);
				lbl.setForeground(BLUE_GRAY);
			} else {
				lbl.setBackground(MID_GRAY);
				lbl.setForeground(DARK_GRAY);
			}
			return lbl;
		}
	}
}