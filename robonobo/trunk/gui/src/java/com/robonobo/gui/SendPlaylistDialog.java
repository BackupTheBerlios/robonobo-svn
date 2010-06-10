	package com.robonobo.gui;

import info.clearthought.layout.TableLayout;

import java.awt.Dimension;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.robonobo.common.concurrent.CatchingRunnable;
import com.robonobo.common.exceptions.SeekInnerCalmException;
import com.robonobo.core.Platform;
import com.robonobo.core.RobonoboController;
import com.robonobo.core.api.RobonoboException;
import com.robonobo.core.api.model.Playlist;
import com.robonobo.core.api.model.User;

public class SendPlaylistDialog extends JDialog implements KeyListener {
	private RobonoboFrame frame;
	private Playlist p;
	private Dimension size = new Dimension(445, 155);
	private JRadioButton exisFriendRb, newFriendRb;
	private JComboBox friendCombo;
	private JTextField emailField;
	private ButtonGroup btnGroup;
	private JButton sendBtn;
	private JButton cancelBtn;
	private Log log = LogFactory.getLog(getClass());
	private RobonoboController controller;

	public SendPlaylistDialog(RobonoboFrame rFrame, Playlist p) throws HeadlessException {
		super(rFrame, true);
		this.p = p;
		this.frame = rFrame;
		controller = frame.getController();
		setPreferredSize(size);
		setSize(size);
		setTitle("Send Playlist '"+p.getTitle()+"'");
		double[][] cellSizen = { { 5, 150, 5, 270, 5 }, { 5, 25, 5, 25, 5, 25, 5, 25, 5 } };
		setLayout(new TableLayout(cellSizen));
		btnGroup = new ButtonGroup();

		JLabel blurbLbl = new JLabel("Send playlist to:");
		add(blurbLbl, "1,1,3,1");

		exisFriendRb = new JRadioButton("Existing friend: ");
		exisFriendRb.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				friendCombo.setEnabled(true);
				emailField.setEnabled(false);
			}
		});
		exisFriendRb.addKeyListener(this);
		exisFriendRb.setSelected(true);
		btnGroup.add(exisFriendRb);
		add(exisFriendRb, "1,3");
		Vector<UserWrapper> friends = new Vector<UserWrapper>();
		for (Long friendId : controller.getMyUser().getFriendIds()) {
			User user = controller.getUser(friendId);
			if(user != null)
				friends.add(new UserWrapper(user));
		}
		friendCombo = new JComboBox(friends);
		add(friendCombo, "3,3");

		newFriendRb = new JRadioButton("New friend: ");
		newFriendRb.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				emailField.setEnabled(true);
				friendCombo.setEnabled(false);
			}
		});
		newFriendRb.addKeyListener(this);
		btnGroup.add(newFriendRb);
		add(newFriendRb, "1,5");
		emailField = new JTextField("Email");
		// Blank on focus
		emailField.addFocusListener(new FocusAdapter() {
			public void focusGained(FocusEvent e) {
				if (emailField.getText().equals("Email"))
					emailField.setText("");
			}
		});
		emailField.addKeyListener(this);
		add(emailField, "3,5");

		// DEBUG - this is disabled for now
		newFriendRb.setEnabled(false);
		emailField.setEnabled(false);

		add(new ButtonPanel(), "3,7,r,t");

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

	public void keyReleased(KeyEvent e) {
	}// Do nothing

	public void keyTyped(KeyEvent e) {
	}// Do nothing

	private class ButtonPanel extends JPanel {
		public ButtonPanel() {
			setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
			sendBtn = new JButton("Send");
			sendBtn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					frame.getController().getExecutor().execute(new CatchingRunnable() {
						public void doRun() throws Exception {
							if (exisFriendRb.isSelected()) {
								User friend = ((UserWrapper) friendCombo.getSelectedItem()).u;
								long friendId = friend.getUserId();
								try {
									controller.sendPlaylist(p, friendId);
									frame.updateStatus("Playlist sent to " + friend.getFriendlyName(), 5, 30);
									frame.getLeftSidebar().selectMyMusic();
								} catch (RobonoboException ex) {
									log.error("Caught exception sending playlist", ex);
									frame.updateStatus("Error sending playlist: " + ex.getMessage(), 10, 30);
								}
							} else if (newFriendRb.isSelected()) {
								String email = emailField.getText();
								try {
									controller.sendPlaylist(p, email);
									frame.updateStatus("Playlist sent to " + email, 5, 30);
									frame.getLeftSidebar().selectMyMusic();
								} catch (RobonoboException ex) {
									log.error("Caught exception sending playlist", ex);
									frame.updateStatus("Error sending playlist: " + ex.getMessage(), 10, 30);
								}
							} else
								throw new SeekInnerCalmException();
						}
					});
					SendPlaylistDialog.this.setVisible(false);
				}
			});
			sendBtn.addKeyListener(SendPlaylistDialog.this);
			add(sendBtn);
			SendPlaylistDialog.this.getRootPane().setDefaultButton(sendBtn);
			Dimension fillerD = new Dimension(20, 1);
			add(new Box.Filler(fillerD, fillerD, fillerD));
			cancelBtn = new JButton("Cancel");
			cancelBtn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					SendPlaylistDialog.this.setVisible(false);
				}
			});
			cancelBtn.addKeyListener(SendPlaylistDialog.this);
			add(cancelBtn);
		}
	}

	private class UserWrapper {
		User u;

		public UserWrapper(User u) {
			this.u = u;
		}

		@Override
		public String toString() {
			return u.getFriendlyName() + " (" + u.getEmail() + ")";
		}
	}
}
