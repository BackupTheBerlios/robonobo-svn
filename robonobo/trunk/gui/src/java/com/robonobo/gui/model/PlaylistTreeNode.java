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
	private FriendTreeModel model;
	int numUnseenTracks;

	public PlaylistTreeNode(Playlist p, RobonoboFrame frame, FriendTreeModel model) {
		super(p.getTitle());
		this.playlist = p;
		this.frame = frame;
		this.model = model;
		numUnseenTracks = frame.getController().numUnseenTracks(p);
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
		numUnseenTracks = frame.getController().numUnseenTracks(playlist);
	}

	@Override
	public boolean wantSelect() {
		return true;
	}
	
	@Override
	public boolean handleSelect() {
		numUnseenTracks = 0;
		frame.getController().markAllAsSeen(playlist);
		frame.getMainPanel().selectContentPanel(contentPanelName());
		return true;
	}

	public int getNumUnseenTracks() {
		return numUnseenTracks;
	}
	
	protected String contentPanelName() {
		return "playlist/" + playlist.getPlaylistId();
	}

	@Override
	public boolean importData(Transferable t) {
		return false;
	}
	
	@Override
	public int compareTo(SortableTreeNode o) {
		PlaylistTreeNode other = (PlaylistTreeNode) o;
		return playlist.getTitle().compareTo(other.getPlaylist().getTitle());
	}
}