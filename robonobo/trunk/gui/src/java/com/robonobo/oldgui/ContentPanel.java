package com.robonobo.oldgui;

import java.awt.LayoutManager;

import javax.swing.JPanel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public abstract class ContentPanel extends JPanel {
	protected final Log log = LogFactory.getLog(getClass());
	protected RobonoboFrame frame;
	
	public ContentPanel(RobonoboFrame frame) {
		this.frame = frame;
		setTransferHandler(frame.getFileImportDropHandler());
	}

	public ContentPanel(LayoutManager arg0) {
		super(arg0);
		// TODO Auto-generated constructor stub
	}

	public ContentPanel(boolean arg0) {
		super(arg0);
		// TODO Auto-generated constructor stub
	}

	public ContentPanel(LayoutManager arg0, boolean arg1) {
		super(arg0, arg1);
		// TODO Auto-generated constructor stub
	}

	/**
	 * Focusses on the most important element on this panel (probably the search box)
	 */
	public abstract void focus();
	
	/** Default implementation returns null */
	public PlayPauseButton getPlayPauseButton() {
		return null;
	}
}
