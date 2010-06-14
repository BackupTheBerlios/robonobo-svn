package com.robonobo.gui.laf;

import info.clearthought.layout.TableLayout;

import java.awt.Dimension;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

@SuppressWarnings("serial")
public class SendPlaylistDialog extends JDialog {
	private Dimension size = new Dimension(445, 155);
	private JRadioButton exisFriendRb, newFriendRb;
	private JComboBox friendCombo;
	private JTextField emailField;
	private ButtonGroup btnGroup;
	private DialogButton sendBtn;
	private DialogButton cancelBtn;
	
	public SendPlaylistDialog(JFrame frame) {
		super(frame);
		setPreferredSize(size);
		setSize(size);
		setTitle("Send Playlist 'A shared list'");
		double[][] cellSizen = { { 5, 150, 5, 270, 5 }, { 5, 25, 5, 25, 5, 25, 5, 25, 5 } };
		setLayout(new TableLayout(cellSizen));
		btnGroup = new ButtonGroup();

		JLabel blurbLbl = new JLabel("Send playlist to:");
		add(blurbLbl, "1,1,3,1");

		exisFriendRb = new JRadioButton("Existing friend: ");
		exisFriendRb.setSelected(true);
		btnGroup.add(exisFriendRb);
		add(exisFriendRb, "1,3");
		Vector<String> friends = new Vector<String>();
		friends.add("Ray (ray@wirestorm.net)");
		friends.add("Will (macavity@well.com)");
		friendCombo = new JComboBox(friends);
		add(friendCombo, "3,3");

		newFriendRb = new JRadioButton("New friend: ");
		btnGroup.add(newFriendRb);
		add(newFriendRb, "1,5");
		emailField = new JTextField("Email");
		add(emailField, "3,5");

		newFriendRb.setEnabled(false);
		emailField.setEnabled(false);

		add(new ButtonPanel(), "3,7,r,t");

	}
	
	private class ButtonPanel extends JPanel {
		public ButtonPanel() {
			setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
			sendBtn = new DialogButton("SEND");
			add(sendBtn);
			SendPlaylistDialog.this.getRootPane().setDefaultButton(sendBtn);
			Dimension fillerD = new Dimension(20, 1);
			add(new Box.Filler(fillerD, fillerD, fillerD));
			cancelBtn = new DialogCancelButton("CANCEL");
			add(cancelBtn);
		}
	}

}
