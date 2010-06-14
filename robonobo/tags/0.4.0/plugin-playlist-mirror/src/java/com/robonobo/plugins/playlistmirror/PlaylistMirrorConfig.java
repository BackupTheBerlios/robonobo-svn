package com.robonobo.plugins.playlistmirror;

import java.io.Serializable;

public class PlaylistMirrorConfig implements Serializable {
	String connectionDriverClass;
	String connectionDialect;
	String connectionUrl;
	String connectionUsername;
	String connectionPassword;
	String playlistIdRegex = "^.*$";
	/** Secs */
	int checkFrequency = 60;
	/**
	 * For monitoring: should be of the form <streamid>|<delaysecs>[,<streamid>|<delaysecs>...]
	 */
	String delayedDeleteShares;

	public String getConnectionDriverClass() {
		return connectionDriverClass;
	}

	public void setConnectionDriverClass(String connectionDriverClass) {
		this.connectionDriverClass = connectionDriverClass;
	}

	public String getConnectionDialect() {
		return connectionDialect;
	}

	public void setConnectionDialect(String connectionDialect) {
		this.connectionDialect = connectionDialect;
	}

	public String getConnectionUrl() {
		return connectionUrl;
	}

	public void setConnectionUrl(String connectionUrl) {
		this.connectionUrl = connectionUrl;
	}

	public String getConnectionUsername() {
		return connectionUsername;
	}

	public void setConnectionUsername(String connectionUsername) {
		this.connectionUsername = connectionUsername;
	}

	public String getConnectionPassword() {
		return connectionPassword;
	}

	public void setConnectionPassword(String connectionPassword) {
		this.connectionPassword = connectionPassword;
	}

	public String getPlaylistIdRegex() {
		return playlistIdRegex;
	}

	public void setPlaylistIdRegex(String playlistIdRegex) {
		this.playlistIdRegex = playlistIdRegex;
	}

	public int getCheckFrequency() {
		return checkFrequency;
	}

	public void setCheckFrequency(int checkFrequency) {
		this.checkFrequency = checkFrequency;
	}

	public String getDelayedDeleteShares() {
		return delayedDeleteShares;
	}

	public void setDelayedDeleteShares(String delayedDeleteShares) {
		this.delayedDeleteShares = delayedDeleteShares;
	}
}
