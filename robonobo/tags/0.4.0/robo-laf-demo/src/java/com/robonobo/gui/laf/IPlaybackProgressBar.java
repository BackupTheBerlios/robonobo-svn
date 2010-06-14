package com.robonobo.gui.laf;

public interface IPlaybackProgressBar {
	/** 0-100 */
	public int getProgress();
	public void setProgress(int progress);

	/** All these are "0:00" - "9:59:59" */
	public void setStartText(String startText);
	public void setEndText(String endText);
	public void setSliderText(String sliderText);
	
	/** Any listeners added will receive callbacks when the user drags the slider */
	public void addListener(IPlaybackProgressBar.Listener listener);
	public void removeListener(IPlaybackProgressBar.Listener listener);
	
	public interface Listener {
		/** Called when the user drags the slider to a new position */
		public void sliderMoved(int newProgress);
		/** Called when the user releases the mouse to stop dragging the slider */
		public void sliderFinishedMoving();
	}
}
