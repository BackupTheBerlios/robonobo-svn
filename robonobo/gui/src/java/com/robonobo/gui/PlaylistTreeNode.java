package com.robonobo.gui;

import java.awt.Font;
import java.awt.datatransfer.Transferable;

import javax.swing.TransferHandler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.robonobo.common.swing.SortableTreeNode;
import com.robonobo.core.api.model.Playlist;
import static com.robonobo.common.util.TextUtil.*;

class PlaylistTreeNode extends SelectableTreeNode  {
	Log log = LogFactory.getLog(getClass());
	RobonoboFrame frame;
	private Playlist playlist;
	private Font font;
	private int maxTitleWidth;

	public PlaylistTreeNode(Playlist p, RobonoboFrame frame, Font font, int maxTitleWidth) {
		super(limitWithEllipsis(p.getTitle(), font, maxTitleWidth, frame));
		this.playlist = p;
		this.font = font;
		this.maxTitleWidth = maxTitleWidth;
		this.frame = frame;
	}

	public PlaylistTreeNode(String title, RobonoboFrame frame, Font font, int maxTitleWidth) {
		super(limitWithEllipsis(title, font, maxTitleWidth, frame));
		this.font = font;
		this.maxTitleWidth = maxTitleWidth;
		this.frame = frame;
	}

	public int compareTo(PlaylistTreeNode o) {
		return playlist.getTitle().compareTo(o.getPlaylist().getTitle());
	}
	
	public Playlist getPlaylist() {
		return playlist;
	}

	public void setPlaylist(Playlist playlist) {
		this.playlist = playlist;
		setUserObject(limitWithEllipsis(playlist.getTitle(), font, maxTitleWidth, frame));
	}

	@Override
	public boolean handleSelect() {
		frame.getContentHolder().bringPanelToFront(contentPanelName());
		return true;
	}

	protected String contentPanelName() {
		return "playlist-" + playlist.getPlaylistId();
	}

	@Override
	public boolean importData(Transferable t) {
		// For drag n drop, we delegate everything to the playlist's track list
		// table... DRY and all that
		MyPlaylistContentPanel panel = (MyPlaylistContentPanel) frame.getContentHolder().getContentPanel(contentPanelName());
		TransferHandler th = panel.tablePanel.table.getTransferHandler();
		try {
			return th.importData(null, t);
		} catch (Exception e) {
			log.error("Caught exception importing data", e);
			return false;
		}
	}
	
	@Override
	public int compareTo(SortableTreeNode o) {
		// I'm always after the new playlist node
		if(o instanceof NewPlaylistTreeNode)
			return 1;
		PlaylistTreeNode other = (PlaylistTreeNode) o;
		return playlist.getTitle().compareTo(other.getPlaylist().getTitle());
	}
}