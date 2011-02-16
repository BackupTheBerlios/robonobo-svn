package com.robonobo.gui.sheets;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JButton;
import javax.swing.JPanel;

import com.robonobo.core.Platform;
import com.robonobo.gui.frames.RobonoboFrame;

public abstract class Sheet extends JPanel {
	protected RobonoboFrame frame;

	public Sheet(RobonoboFrame frame) {
		this.frame = frame;
	}
	
	/** Called after sheet is shown, on the gui thread */
	public abstract void onShow();
	
	/** This will be made the default button when the sheet is shown */
	public abstract JButton defaultButton();
	
	/** Make sure to call super.onUndim() if you override this! */
	public void onUndim() {
		super.setVisible(false);
	}
	
	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (!visible)
			frame.undim();
	}
}
