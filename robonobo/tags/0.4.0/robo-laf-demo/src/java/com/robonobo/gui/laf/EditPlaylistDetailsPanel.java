package com.robonobo.gui.laf;

import info.clearthought.layout.TableLayout;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

@SuppressWarnings("serial")
class EditPlaylistDetailsPanel extends JPanel {
	public EditPlaylistDetailsPanel() {
		double[][] cellSizen = { { 15, TableLayout.FILL, 15, 330, 15 }, { 10, TableLayout.FILL } };
		setLayout(new TableLayout(cellSizen));
		add(new LeftPanel(), "1,1");
		add(new RightPanel(), "3,1");
	}

	class LeftPanel extends JPanel {
		public LeftPanel() {
			double[][] cellSizen = { { 90, 10, TableLayout.FILL }, { 25, 10, TableLayout.FILL, 10 } };
			setLayout(new TableLayout(cellSizen));
			add(new JLabel("Title:"), "0,0");
			add(new JTextField(), "2,0");
			add(new JLabel("Description:"), "0,2,LEADING,TOP");
			add(new JScrollPane(new JTextArea("")), "2,2");
		}
	}

	class RightPanel extends JPanel {
		public RightPanel() {
			double[][] cellSizen = { { TableLayout.FILL }, { TableLayout.FILL, 20, 5, 20, 5, 35, 6 } };
			setLayout(new TableLayout(cellSizen));
			JCheckBox friendCb = new JCheckBox("Let friends see this playlist");
			friendCb.setSelected(true);
			add(friendCb, "0,1");
			JCheckBox autoCb = new JCheckBox("Download new tracks automatically");
			autoCb.setEnabled(false);
			add(autoCb, "0,3");
			add(new ButtonPanel(), "0,5");
		}
	}

	class ButtonPanel extends JPanel {
		public ButtonPanel() {
			setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
			add(new DialogButton("SAVE"));
			add(Box.createHorizontalStrut(5));
			add(new DialogButton("SHARE"));
			add(Box.createHorizontalStrut(5));
			add(new DialogButton("SEND"));
			add(Box.createHorizontalStrut(5));
			add(new DialogCancelButton("DELETE"));
		}
	}
}