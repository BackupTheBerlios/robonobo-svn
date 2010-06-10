package com.robonobo.gui.laf;

import java.awt.Component;
import java.util.List;

import javax.swing.AbstractListModel;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

@SuppressWarnings("serial")
public class SidebarPlaylistList extends JList {
	private ImageIcon newPlaylistIcon = GuiUtil.createImageIcon("/img/newplaylist.png", "New Playlist");
	private ImageIcon playlistIcon = GuiUtil.createImageIcon("/img/playlist.png", "");
	private boolean includeNewPlaylist;
	private List<String> playlistTitles;
	
	public SidebarPlaylistList(List<String> playlistTitles, boolean includeNewPlaylist) {
		this.playlistTitles = playlistTitles;
		this.includeNewPlaylist = includeNewPlaylist;
		setModel(new SidebarListModel());
		setCellRenderer(new SidebarListRenderer());
	}

	private class SidebarListRenderer extends JLabel implements ListCellRenderer {
		public Component getListCellRendererComponent(
				JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
	        if (isSelected) {
	            setBackground(list.getSelectionBackground());
	            setForeground(list.getSelectionForeground());
	        } else {
	            setBackground(list.getBackground());
	            setForeground(list.getForeground());
	        }
	        if(includeNewPlaylist && index == 0)
		        setIcon(newPlaylistIcon);
	        else
	        	setIcon(playlistIcon);
	        setText((String) list.getModel().getElementAt(index));
			return this;
		}
	}

	private class SidebarListModel extends AbstractListModel {
		@Override
		public int getSize() {
			if (includeNewPlaylist)
				return playlistTitles.size() + 1;
			return playlistTitles.size();
		}

		@Override
		public Object getElementAt(int index) {
			if (includeNewPlaylist) {
				if (index == 0)
					return "New Playlist";
				return playlistTitles.get(index - 1);
			}
			return playlistTitles.get(index);
		}
	}
}
