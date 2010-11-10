package com.robonobo.gui.dialogs;

import static com.robonobo.common.util.TextUtil.*;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.debian.tablelayout.TableLayout;

import com.robonobo.common.concurrent.CatchingRunnable;
import com.robonobo.common.swing.SelectiveListSelectionModel;
import com.robonobo.core.Platform;
import com.robonobo.core.RobonoboController;
import com.robonobo.core.api.RobonoboException;
import com.robonobo.core.api.model.Playlist;
import com.robonobo.core.api.model.User;
import com.robonobo.gui.frames.RobonoboFrame;


public class SharePlaylistDialog extends JDialog implements KeyListener {
	private RobonoboFrame frame;
	private Playlist p;
	private Dimension size = new Dimension(445, 230);
	private JList friendList;
	private JTextField emailField;
	private ButtonGroup btnGroup;
	private JButton shareBtn;
	private JButton cancelBtn;
	private Log log = LogFactory.getLog(getClass());
	private RobonoboController controller;

	public SharePlaylistDialog(RobonoboFrame rFrame, Playlist rP) throws HeadlessException {
		super(rFrame, true);
		this.p = rP;
		this.frame = rFrame;
		controller = frame.getController();
		setPreferredSize(size);
		setSize(size);
		setTitle("Share Playlist '" + p.getTitle() + "'");
		double[][] cellSizen = { { 5, 150, 5, 270, 5 }, { 5, 25, 5, 100, 5, 25, 5, 25, 5 } };
		setLayout(new TableLayout(cellSizen));
		btnGroup = new ButtonGroup();

		add(new JLabel("Share playlist with:"), "1,1,3,1");
		add(new JLabel("Existing friends:"), "1,3,l,t");

		Vector<UserWrapper> friends = new Vector<UserWrapper>();
		for (Long friendId : controller.getMyUser().getFriendIds()) {
			User user = controller.getUser(friendId);
			if (user != null)
				friends.add(new UserWrapper(user));
		}
		friendList = new JList(friends);
		friendList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		friendList.setSelectionModel(new MySelectionModel(friendList.getModel()));
		friendList.setCellRenderer(new MyCellRenderer());
		add(new JScrollPane(friendList), "3,3");

		add(new JLabel("New friends:"), "1,5");
		emailField = new JTextField("Emails");
		// Blank on focus
		emailField.addFocusListener(new FocusAdapter() {
			public void focusGained(FocusEvent e) {
				if (emailField.getText().equals("Emails"))
					emailField.setText("");
			}
		});
		emailField.addKeyListener(this);
		add(emailField, "3,5");

		int invitesLeft = controller.getMyUser().getInvitesLeft();
		if (invitesLeft <= 0)
			emailField.setEnabled(false);
		add(new JLabel(numItems(invitesLeft, "invite") + " left"), "1,7");

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
			shareBtn = new JButton("Share");
			shareBtn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					frame.getController().getExecutor().execute(new CatchingRunnable() {
						public void doRun() throws Exception {
							Set<Long> selFriendIds = new HashSet<Long>();
							for (Object obj : friendList.getSelectedValues()) {
								selFriendIds.add(((UserWrapper) obj).u.getUserId());
							}
							Set<String> emails = new HashSet<String>();
							String emailFieldTxt = emailField.getText();
							if (isNonEmpty(emailFieldTxt) && !emailFieldTxt.equalsIgnoreCase("emails")) {
								for (String emailStr : emailFieldTxt.split(",")) {
									if (emailStr.trim().length() > 0)
										emails.add(emailStr.trim());
								}
							}
							try {
								controller.sharePlaylist(p, selFriendIds, emails);
								User me = controller.getMyUser();
								// This is just for show in the UI, the server will be updating this figure at their end
								me.setInvitesLeft(me.getInvitesLeft() - emails.size());
								log.info("Playlist '"+p.getTitle()+"' shared");
							} catch (RobonoboException ex) {
								log.error("Caught exception sharing playlist", ex);
							}
						}
					});
					SharePlaylistDialog.this.setVisible(false);
				}
			});
			shareBtn.addKeyListener(SharePlaylistDialog.this);
			add(shareBtn);
			SharePlaylistDialog.this.getRootPane().setDefaultButton(shareBtn);
			Dimension fillerD = new Dimension(20, 1);
			add(new Box.Filler(fillerD, fillerD, fillerD));
			cancelBtn = new JButton("Cancel");
			cancelBtn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					SharePlaylistDialog.this.setVisible(false);
				}
			});
			cancelBtn.addKeyListener(SharePlaylistDialog.this);
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

	private class MyCellRenderer extends DefaultListCellRenderer {
		public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
			UserWrapper uw = (UserWrapper) value;
			JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			if (p.getOwnerIds().contains(uw.u.getUserId()))
				lbl.setEnabled(false);
			return lbl;
		}
	}

	private class MySelectionModel extends SelectiveListSelectionModel {
		public MySelectionModel(ListModel model) {
			super(model);
		}

		@Override
		protected boolean isSelectable(Object obj) {
			UserWrapper uw = (UserWrapper) obj;
			// Folks who are already sharing cannot be selected
			return (!p.getOwnerIds().contains(uw.u.getUserId()));
		}

	}
}
