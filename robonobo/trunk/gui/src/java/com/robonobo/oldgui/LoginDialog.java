package com.robonobo.oldgui;

import java.awt.Dimension;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.debian.tablelayout.TableLayout;

import com.robonobo.core.Platform;

public class LoginDialog extends JDialog implements KeyListener {
	private RobonoboFrame frame;
	private Dimension size = new Dimension(320, 200);
	private JButton loginBtn;
	private JButton cancelBtn;
	private JTextField emailField;
	private JPasswordField passwordField;
	private Runnable onLogin;
	private Log log = LogFactory.getLog(getClass());

	public LoginDialog(RobonoboFrame rFrame, Runnable onLogin) throws HeadlessException {
		super(rFrame, true);
		this.frame = rFrame;
		this.onLogin = onLogin;
		setPreferredSize(size);
		setSize(size);
		setTitle("Robonobo Login");
		double[][] cellSizen = {
				{ 10, 100, 10, 180, 10 },
				{ 0, 60, 5, 25, 5, 25, 10, 40 }
		};
		setLayout(new TableLayout(cellSizen));
		JLabel blurbLbl = new JLabel("<html><center><p>Please login to Robonobo.<br>Email beta@robonobo.com for an account.</p></center></html>", SwingConstants.CENTER);
		add(blurbLbl, "1,1,3,1");
		add(new JLabel("Email:"), "1,3,r,f");
		emailField = new JTextField();
		emailField.addKeyListener(this);
		add(emailField, "3,3");
		add(new JLabel("Password:"), "1,5,r,f");
		passwordField = new JPasswordField();
		passwordField.addKeyListener(this);
		add(passwordField, "3,5");
		
		loginBtn = new JButton("Login");
		loginBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				tryLogin();
			}
		});
		loginBtn.addKeyListener(this);
		loginBtn.setPreferredSize(new Dimension(90, 29));
		getRootPane().setDefaultButton(loginBtn);
		cancelBtn = new JButton("Cancel");
		cancelBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				LoginDialog.this.setVisible(false);
			}
		});
		cancelBtn.addKeyListener(this);
		cancelBtn.getActionMap().put("ESCAPE", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				LoginDialog.this.setVisible(false);
			}
		});

		cancelBtn.setPreferredSize(new Dimension(90, 29));
		ButtonPanel btnPanel = new ButtonPanel();
		btnPanel.addKeyListener(this);
		add(btnPanel, "1,7,3,7");
		
	}
	
	private void tryLogin() {
		if(frame.getController().tryLogin(emailField.getText(), new String(passwordField.getPassword()))) {
			frame.updateStatus("Login as "+frame.getController().getMyUser().getEmail()+" succeeded", 5, 30);
			LoginDialog.this.setVisible(false);
			if(onLogin != null) {
				frame.getController().getExecutor().execute(onLogin);
			}
		} else {
			frame.updateStatus(emailField.getText(), 5, 30);
			// Shake your moneymaker
			GUIUtils.shakeWindow(this, Platform.getPlatform().getNumberOfShakesForShakeyWindow(), GUIUtils.DEFAULT_SHAKE_FORCE);
		}
	}

	public void keyPressed(KeyEvent e) {
		int code = e.getKeyCode();
		int modifiers = e.getModifiers();
		if(code == KeyEvent.VK_ESCAPE)
			setVisible(false);
		if(code == KeyEvent.VK_Q && modifiers == Platform.getPlatform().getCommandModifierMask())
			frame.shutdown();
	}

	public void keyReleased(KeyEvent e) {}// Do nothing

	public void keyTyped(KeyEvent e) {}// Do nothing

	private class ButtonPanel extends JPanel {
		public ButtonPanel() {
			add(loginBtn);
			Dimension fillerD = new Dimension(20, 1);
			add(new Box.Filler(fillerD, fillerD, fillerD));
			add(cancelBtn);
		}
	}	
}
