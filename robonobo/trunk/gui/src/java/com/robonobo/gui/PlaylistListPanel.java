package com.robonobo.gui;

import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JLabel;
import javax.swing.JPanel;

import com.robonobo.core.Platform;
import com.robonobo.core.api.model.Playlist;
import com.robonobo.core.api.model.User;

public class PlaylistListPanel extends JPanel {
	public PlaylistListPanel(final RobonoboFrame frame, final User user, final boolean isMe) {
		setLayout(new FlowLayout());
		JLabel titleLbl = new JLabel("Playlists");
		titleLbl.setFont(titleLbl.getFont().deriveFont(Font.BOLD));
		add(titleLbl);
		if(isMe) {
			JLabel newPlaylistLbl = new JLabel("New playlist...");
			newPlaylistLbl.setForeground(Platform.getPlatform().getLinkColor());
			newPlaylistLbl.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent e) {
					frame.getContentHolder().bringPanelToFront("playlist-new");
				}
			});
			add(newPlaylistLbl);
		}
		for (String playlistId : user.getPlaylistIds()) {
			// TODO This needs to be fixed, playlist might not be loaded yet
			final Playlist fp = frame.getController().getPlaylist(playlistId);
			JLabel plLbl = new JLabel(fp.getTitle());
			plLbl.setForeground(Platform.getPlatform().getLinkColor());
			plLbl.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent e) {
					frame.getContentHolder().bringPanelToFront("playlist-"+fp.getPlaylistId());
				}
			});
			add(plLbl);
		}
	}
	

}
