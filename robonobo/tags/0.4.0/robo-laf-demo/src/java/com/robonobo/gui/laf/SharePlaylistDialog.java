package com.robonobo.gui.laf;

import info.clearthought.layout.TableLayout;

import java.awt.Component;
import java.awt.Dimension;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

@SuppressWarnings("serial")
public class SharePlaylistDialog extends JDialog {
	private Dimension size = new Dimension(445, 230);
	private JList friendList;
	private JTextField emailField;
	private ButtonGroup btnGroup;
	private DialogButton shareBtn;
	private DialogCancelButton cancelBtn;

	public SharePlaylistDialog(JFrame frame) {
		super(frame);
		setPreferredSize(size);
		setSize(size);
		setTitle("Share Playlist 'A shared list'");
		double[][] cellSizen = { { 5, 150, 5, 270, 5 }, { 5, 25, 5, 100, 5, 25, 5, 25, 5 } };
		setLayout(new TableLayout(cellSizen));
		btnGroup = new ButtonGroup();

		add(new JLabel("Share playlist with:"), "1,1,3,1");
		add(new JLabel("Existing friends:"), "1,3,l,t");

		Vector<String> friends = new Vector<String>();
		friends.add("Ray (ray@wirestorm.net)");
		friends.add("Will (macavity@well.com)");
		friendList = new JList(friends);
		friendList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		friendList.setCellRenderer(new MyCellRenderer());
		add(new JScrollPane(friendList), "3,3");

		add(new JLabel("New friends:"), "1,5");
		emailField = new JTextField("Emails");
		add(emailField, "3,5");
		emailField.setEnabled(false);

		add(new ButtonPanel(), "3,7,r,t");

	}
	
	private class ButtonPanel extends JPanel {
		public ButtonPanel() {
			setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
			shareBtn = new DialogButton("SHARE");
			add(shareBtn);
			SharePlaylistDialog.this.getRootPane().setDefaultButton(shareBtn);
			Dimension fillerD = new Dimension(20, 1);
			add(new Box.Filler(fillerD, fillerD, fillerD));
			cancelBtn = new DialogCancelButton("CANCEL");
			add(cancelBtn);
		}
	}

	private class MyCellRenderer extends DefaultListCellRenderer {
		public Component getListCellRendererComponent(
				JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
			String s = (String) value;
			JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			if(s.startsWith("W"))
				lbl.setEnabled(false);
			return lbl;
		}
	}

}
