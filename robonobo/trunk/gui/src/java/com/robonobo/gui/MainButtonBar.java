package com.robonobo.gui;

import info.clearthought.layout.TableLayout;

import java.util.List;

import javax.swing.JPanel;

import com.robonobo.core.Platform;
import com.robonobo.core.RobonoboController;

/**
 * Includes the supplied list of buttons, plus a playback status panel on the right
 */
public class MainButtonBar extends JPanel {
	List<MainButton> buttons;
	RobonoboFrame frame;
	RobonoboController controller;

	public MainButtonBar(List<MainButton> buttons, RobonoboFrame frame, TrackListTablePanel tablePanel) {
		this.buttons = buttons;
		this.frame = frame;
		this.controller = frame.getController();
		// Build array containing widths for tablelayout
		// TODO Would be neater to do this with flowlayout
		double[] widths = new double[(buttons.size()+1) * 2];
		for(int i=0;i<buttons.size();i++) {
			widths[i*2] = 70; // Button width
			if(i == buttons.size() - 1)
				widths[i*2+1] = TableLayout.FILL; // Right padding after buttons
			else
				widths[i*2+1] = 5; // Padding between buttons
		}
		// Add the width for the playbackstatuspanel and padding after
		widths[widths.length-2] = 200;
		widths[widths.length-1] = 5;
		double[][] cellSizen = { widths, { TableLayout.FILL } };
		setLayout(new TableLayout(cellSizen));
		Platform.getPlatform().customizeMainbarButtons(buttons);
		// Add components
		for(int i=0;i<buttons.size();i++)
			add(buttons.get(i), String.valueOf(i*2)+",0");
		add(new PlaybackStatusPanel(frame, tablePanel), String.valueOf(widths.length-2)+",0");
	}


}
