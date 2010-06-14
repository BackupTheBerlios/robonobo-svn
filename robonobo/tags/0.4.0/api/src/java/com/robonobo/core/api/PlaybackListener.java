package com.robonobo.core.api;


/**
 * Notified when audio playback is started/stopped/paused
 */
public interface PlaybackListener {
	public void playbackStarting();
	public void playbackStarted();
	public void playbackPaused();
	public void playbackStopped();
	public void playbackCompleted();
	public void playbackProgress(long microsecs);
	public void playbackError(String error);
}
