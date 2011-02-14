package com.robonobo.gui.sheets;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JPanel;

import com.robonobo.core.Platform;
import com.robonobo.gui.frames.RobonoboFrame;

public abstract class Sheet extends JPanel implements KeyListener {
	protected RobonoboFrame frame;

	public Sheet(RobonoboFrame frame) {
		this.frame = frame;
		addKeyListener(this);
	}
	
	public void keyPressed(KeyEvent e) {
		int code = e.getKeyCode();
		int modifiers = e.getModifiers();
		if (code == KeyEvent.VK_ESCAPE)
			setVisible(false);
		if (code == KeyEvent.VK_Q && modifiers == Platform.getPlatform().getCommandModifierMask())
			frame.shutdown();
	}

	/** Called after sheet is shown, on the gui thread */
	public abstract void onShow();
	
	@Override
	public void keyTyped(KeyEvent e) {
		// Do nothing
	}

	@Override
	public void keyReleased(KeyEvent e) {
		// Do nothing
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (!visible)
			frame.undim();
	}
}
