package com.robonobo.gui.laf;

import java.net.URL;

import javax.swing.ImageIcon;

public class GuiUtil {
	public static ImageIcon createImageIcon(String path, String description) {
		URL imgUrl = GuiUtil.class.getResource(path);
		if (imgUrl == null)
			return null;
		return new ImageIcon(imgUrl, description);
	}
}
