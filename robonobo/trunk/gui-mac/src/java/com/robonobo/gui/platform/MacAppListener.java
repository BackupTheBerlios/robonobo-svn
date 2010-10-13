package com.robonobo.gui.platform;

import com.apple.eawt.ApplicationEvent;
import com.apple.eawt.ApplicationListener;
import com.robonobo.gui.dialogs.AboutDialog;
import com.robonobo.gui.frames.RobonoboFrame;

public class MacAppListener implements ApplicationListener {
	private RobonoboFrame frame;
		
	public MacAppListener(RobonoboFrame frame) {
		this.frame = frame;
	}

	public void handleAbout(ApplicationEvent e) {
		AboutDialog dialog = new AboutDialog(frame);
		dialog.setVisible(true);
		e.setHandled(true);
	}

	public void handleOpenApplication(ApplicationEvent arg0) {
		// TODO Auto-generated method stub

	}

	public void handleOpenFile(ApplicationEvent arg0) {
		// TODO Auto-generated method stub

	}

	public void handlePreferences(ApplicationEvent e) {
		frame.showPreferences();
		e.setHandled(true);
	}

	public void handlePrintFile(ApplicationEvent arg0) {
		// TODO Auto-generated method stub

	}

	public void handleQuit(ApplicationEvent e) {
		frame.setVisible(false);
		frame.getController().shutdown();
		System.exit(0);
	}

	public void handleReOpenApplication(ApplicationEvent arg0) {
		// TODO Auto-generated method stub

	}

	
}
