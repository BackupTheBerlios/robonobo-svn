package com.robonobo.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.debian.tablelayout.TableLayout;

import com.robonobo.common.concurrent.CatchingRunnable;
import com.robonobo.common.exceptions.SeekInnerCalmException;
import com.robonobo.core.RobonoboController;
import com.robonobo.core.api.model.User;

public class UserDetailsPanel extends JPanel {
	private JTextField nameField;
	private JTextArea descArea;
	private JLabel nameLbl;
	private JLabel emailLbl;
	private JLabel descLbl;
	private JLabel imgLbl;
	private JButton saveBtn;
	private JButton imgUploadBtn;

	public UserDetailsPanel(final RobonoboController controller, final User user, boolean isMe) {
		emailLbl = new JLabel(user.getEmail());
		if (user.getImgUrl() != null) {
			try {
				imgLbl = new JLabel(new ImageIcon(new URL(user.getImgUrl())));
			} catch (MalformedURLException e) {
				throw new SeekInnerCalmException();
			}
		}
		if (isMe) {
			nameField = new JTextField(user.getFriendlyName());
			nameField.addKeyListener(new KeyAdapter() {
				public void keyPressed(KeyEvent e) {
					saveBtn.setEnabled(true);
				}
			});
			descArea = new JTextArea(user.getDescription());
			descArea.addKeyListener(new KeyAdapter() {
				public void keyPressed(KeyEvent e) {
					saveBtn.setEnabled(true);
				}
			});
			saveBtn = new JButton("Save");
			saveBtn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					user.setFriendlyName(nameField.getText());
					user.setDescription(descArea.getText());
					saveBtn.setEnabled(false);
					controller.getExecutor().execute(new CatchingRunnable() {
						public void doRun() throws Exception {
							controller.updateUser(user);
						}
					});
				}
			});
			saveBtn.setEnabled(false);
			// TODO implement user images
//			imgUploadBtn = new JButton("New Image...");
//			imgUploadBtn.addActionListener(new ActionListener() {
//				public void actionPerformed(ActionEvent e) {
//					// TODO implement me
//				}
//			});			
		} else {
			nameLbl = new JLabel(user.getFriendlyName());
			descLbl = new JLabel(user.getDescription());
		}
		
		double[][] cellSizen = { {10, TableLayout.FILL, 10, 50, 10}, {10, 15, 10, 15, 10, 60, 10, 25, TableLayout.FILL} };
		setLayout(new TableLayout(cellSizen));
		add(emailLbl, "1,3");
		if(isMe) {
			add(nameField, "1,1");
			add(descArea, "1,5");
		} else {
			add(nameLbl, "1,1");
			add(descLbl, "1,5");
		}
		if(imgLbl != null)
			add(imgLbl, "3,1,3,5");
		add(new ButtonPanel(), "1,7,3,7");
	}

	private class ButtonPanel extends JPanel {
		public ButtonPanel() {
			double[][] cellSizen = { {40, TableLayout.FILL, 50}, {TableLayout.FILL} };
			setLayout(new TableLayout(cellSizen));
			if(saveBtn != null)
				add(saveBtn, "0,0");
			if(imgUploadBtn != null)
				add(imgUploadBtn, "2,0");
		}
	}
}
