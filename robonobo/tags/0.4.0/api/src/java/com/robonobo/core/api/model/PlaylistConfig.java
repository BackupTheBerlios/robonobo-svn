package com.robonobo.core.api.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Defines some node-specific details for a playlist, for example whether to autofetch it.  These details are held locally
 * @author mortw0
 *
 */
public class PlaylistConfig {
	private String playlistId;
	private Map<String, String> items = new HashMap<String, String>();
	
	public PlaylistConfig() {
	}

	public Map<String, String> getItems() {
		return items;
	}

	public void setItems(Map<String, String> items) {
		this.items = items;
	}

	public String getItem(String key) {
		return items.get(key);
	}
	
	public void setItem(String key, String value) {
		items.put(key, value);
	}
	
	public String getPlaylistId() {
		return playlistId;
	}

	public void setPlaylistId(String playlistId) {
		this.playlistId = playlistId;
	}
}
