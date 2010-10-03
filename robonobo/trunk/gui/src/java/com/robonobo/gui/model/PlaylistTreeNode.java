package com.robonobo.gui.model;

import java.awt.datatransfer.Transferable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.robonobo.common.swing.SortableTreeNode;
import com.robonobo.core.api.model.Playlist;
import com.robonobo.gui.frames.RobonoboFrame;

@SuppressWarnings("serial")
public class PlaylistTreeNode extends SelectableTreeNode  {
	Log log = LogFactory.getLog(getClass());
	RobonoboFrame frame;
	private Playlist playlist;

	public PlaylistTreeNode(Playlist p, RobonoboFrame frame) {
		super(p.getTitle());
		this.playlist = p;
		this.frame = frame;
	}

	public PlaylistTreeNode(String title, RobonoboFrame frame) {
		super(title);
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
		setUserObject(playlist.getTitle());
	}

	@Override
	public boolean handleSelect() {
//		frame.getContentHolder().bringPanelToFront(contentPanelName());
		return true;
	}

	public int unseenTracks() {
		// DEBUG - get this info from somewhere
		if(playlist != null && playlist.getTitle().equalsIgnoreCase("playlist 001"))
			return 5;
		return 0;
	}
	
	protected String contentPanelName() {
		return "playlist-" + playlist.getPlaylistId();
	}

	@Override
	public boolean importData(Transferable t) {
		return false;
		// For drag n drop, we delegate everything to the playlist's track list
		// table... DRY and all that
//		MyPlaylistContentPanel panel = (MyPlaylistContentPanel) frame.getContentHolder().getContentPanel(contentPanelName());
//		TransferHandler th = panel.tablePanel.table.getTransferHandler();
//		try {
//			return th.importData(null, t);
//		} catch (Exception e) {
//			log.error("Caught exception importing data", e);
//			return false;
//		}
	}
	
	@Override
	public int compareTo(SortableTreeNode o) {
		PlaylistTreeNode other = (PlaylistTreeNode) o;
		return playlist.getTitle().compareTo(other.getPlaylist().getTitle());
	}
}