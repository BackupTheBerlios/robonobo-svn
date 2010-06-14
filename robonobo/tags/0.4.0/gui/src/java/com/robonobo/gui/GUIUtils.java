package com.robonobo.gui;

import java.awt.Rectangle;
import java.awt.Window;
import java.net.URL;

import javax.swing.ImageIcon;

public class GUIUtils {
	public static final int DEFAULT_NUM_SHAKES = 10;
	public static final int DEFAULT_SHAKE_FORCE = 5;

	public static ImageIcon createImageIcon(String path, String description) {
		URL imgUrl = GUIUtils.class.getResource(path);
		if (imgUrl == null)
			return null;
		return new ImageIcon(imgUrl, description);
	}

	public static void shakeWindow(final Window win, int numShakes, int shakeForce) {
		final Rectangle origRect = win.getBounds();
		for (int i = 0; i < numShakes; i++) {
			int x;
			if (i % 2 == 0)
				x = shakeForce;
			else
				x = -shakeForce;
			win.setBounds(origRect.x + x, origRect.y, origRect.width, origRect.height);
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
			}
			win.setBounds(origRect); // Reset to original position
		}
	}

	public static void shakeWindow(Window win) {
		GUIUtils.shakeWindow(win, DEFAULT_NUM_SHAKES, DEFAULT_SHAKE_FORCE);
	}
}
