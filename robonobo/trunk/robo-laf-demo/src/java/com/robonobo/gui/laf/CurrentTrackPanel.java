package com.robonobo.gui.laf;

import info.clearthought.layout.TableLayout;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

@SuppressWarnings("serial")
public class CurrentTrackPanel extends JPanel {
	
	public CurrentTrackPanel() {
		double[][] cellSizen = { {10, TableLayout.FILL, 10, 305, 10}, {5, TableLayout.FILL, 5} };
		setLayout(new TableLayout(cellSizen));
		add(new DetailsPanel(), "1,1");
		add(new ControlsPanel(), "3,1");
	}
	
	class DetailsPanel extends JPanel {
		public DetailsPanel() {
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			add(Box.createVerticalStrut(5));
			add(new JLabel("Buenas Tardes Amigo (5:32)"));
			add(Box.createVerticalStrut(5));
			add(new JLabel("<html><bold>Ween</bold> / Don't Shit Where You Eat</html>"));
		}
	}
	
	class ControlsPanel extends JPanel {
		public ControlsPanel() {
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			add(new PlaybackProgressBar());
			add(new PlaybackControlsPanel());
		}
	}
}
