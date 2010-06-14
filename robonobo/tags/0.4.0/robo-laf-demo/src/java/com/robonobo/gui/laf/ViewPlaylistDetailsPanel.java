package com.robonobo.gui.laf;

import info.clearthought.layout.TableLayout;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

@SuppressWarnings("serial")
public class ViewPlaylistDetailsPanel extends JPanel {
	public ViewPlaylistDetailsPanel() {
		double[][] cellSizen = { { 15, TableLayout.FILL, 15, 330, 15 }, { 10, TableLayout.FILL } };
		setLayout(new TableLayout(cellSizen));
		add(new LeftPanel(), "1,1");
		add(new RightPanel(), "3,1");
	}

	class LeftPanel extends JPanel {
		public LeftPanel() {
			double[][] cellSizen = { { TableLayout.FILL }, { 25, 10, TableLayout.FILL, 10 } };
			setLayout(new TableLayout(cellSizen));
			add(new JLabel("<html><b>Vessels</b></html>"), "0,0");
			add(new JLabel("<html>Lorem ipsum dolor sit amet, consectetur adipiscing elit. Integer id urna sit amet erat mattis posuere. Pellentesque vestibulum mollis velit, eget dignissim neque bibendum eu. Nullam congue viverra nisl eget sagittis.</html>"), "0,2");
		}
	}

	class RightPanel extends JPanel {
		public RightPanel() {
			double[][] cellSizen = { { TableLayout.FILL }, { TableLayout.FILL, 20, 5, 20, 5, 35, 6 } };
			setLayout(new TableLayout(cellSizen));
			JCheckBox autoCb = new JCheckBox("Download new tracks automatically");
			add(autoCb, "0,3");
			add(new ButtonPanel(), "0,5");
		}
	}

	class ButtonPanel extends JPanel {
		public ButtonPanel() {
			setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
			add(new DialogButton("SAVE"));
		}
	}

}
