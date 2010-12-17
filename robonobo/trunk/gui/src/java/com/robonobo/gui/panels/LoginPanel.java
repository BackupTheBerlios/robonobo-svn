package com.robonobo.gui.panels;

import java.awt.ComponentOrientation;
import java.awt.HeadlessException;
import java.awt.event.*;

import javax.swing.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.debian.tablelayout.TableLayout;

import com.robonobo.common.concurrent.CatchingRunnable;
import com.robonobo.common.serialization.UnauthorizedException;
import com.robonobo.core.Platform;
import com.robonobo.gui.RoboColor;
import com.robonobo.gui.components.base.*;
import com.robonobo.gui.frames.RobonoboFrame;

public class LoginPanel extends JPanel implements KeyListener {
	private RobonoboFrame frame;
	private RButton loginBtn;
	private RButton cancelBtn;
	private RTextField emailField;
	private RPasswordField passwordField;
	private RLabel statusLbl;
	private Runnable onLogin;
	private Log log = LogFactory.getLog(getClass());

	public LoginPanel(RobonoboFrame rFrame, Runnable onLogin) throws HeadlessException {
		this.frame = rFrame;
		this.onLogin = onLogin;
		double[][] cellSizen = { { 10, 100, 10, 180, 10 }, { 10, 25, 5, 25, 5, 25, 5, 25, 5, 20, 5, 30, 10 } };
		setLayout(new TableLayout(cellSizen));
		setName("playback.background.panel");

		RLabel title = new RLabel14B("Please login to robonobo");
		add(title, "1,1,3,1,CENTER,CENTER");

		String blurbTxt = "<html><center>Visit <a href=\"http://robonobo.com\">http://robonobo.com</a> for an account.<br><br></center></html>";
		HyperlinkPane blurbLbl = new HyperlinkPane(blurbTxt, RoboColor.MID_GRAY);
		add(blurbLbl, "1,3,3,3");

		RLabel emailLbl = new RLabel12("Email:");
		add(emailLbl, "1,5,r,f");
		emailField = new RTextField();
		emailField.addKeyListener(this);
		String email = frame.getController().getConfig().getMetadataServerUsername();
		if(email != null)
			emailField.setText(email);
		add(emailField, "3,5");

		RLabel pwdLbl = new RLabel12("Password:");
		add(pwdLbl, "1,7,r,f");

		passwordField = new RPasswordField();
		passwordField.addKeyListener(this);
		String pwd = frame.getController().getConfig().getMetadataServerPassword();
		if(pwd != null)
			passwordField.setText(pwd);
		add(passwordField, "3,7");

		statusLbl = new RLabel12("");
		add(statusLbl, "1,9,3,9,RIGHT,CENTER");

		ButtonPanel btnPanel = new ButtonPanel();
		btnPanel.addKeyListener(this);
		add(btnPanel, "1,11,3,11");

	}

	public JTextField getEmailField() {
		return emailField;
	}
	
	public JPasswordField getPasswordField() {
		return passwordField;
	}
	
	public void tryLogin() {
		emailField.setEnabled(false);
		passwordField.setEnabled(false);
		loginBtn.setEnabled(false);
		statusLbl.setForeground(RoboColor.DARKISH_GRAY);
		statusLbl.setText("Logging in...");
		SwingUtilities.invokeLater(new CatchingRunnable() {
			public void doRun() throws Exception {
				try {
					frame.getController().login(emailField.getText(), new String(passwordField.getPassword()));
					LoginPanel.this.setVisible(false);
					if (onLogin != null)
						frame.getController().getExecutor().execute(onLogin);
				} catch(UnauthorizedException e) {
					statusLbl.setForeground(RoboColor.RED);
					statusLbl.setText("Login failed");
				} catch(Exception e) {
					statusLbl.setForeground(RoboColor.RED);
					statusLbl.setText("Server error");
				} finally {
					emailField.setEnabled(true);
					passwordField.setEnabled(true);
					loginBtn.setEnabled(true);
				}
			}
		});
	}

	public void keyPressed(KeyEvent e) {
		int code = e.getKeyCode();
		int modifiers = e.getModifiers();
		if (code == KeyEvent.VK_ESCAPE)
			setVisible(false);
		if (code == KeyEvent.VK_Q && modifiers == Platform.getPlatform().getCommandModifierMask())
			frame.shutdown();
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (!visible)
			frame.undim();
	}

	public void keyReleased(KeyEvent e) {
	}// Do nothing

	public void keyTyped(KeyEvent e) {
	}// Do nothing

	private class ButtonPanel extends JPanel {
		public ButtonPanel() {
			setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
			setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
			// Laying out right-to-left
			cancelBtn = new RRedGlassButton("CANCEL");
			cancelBtn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					LoginPanel.this.setVisible(false);
				}
			});
			cancelBtn.addKeyListener(LoginPanel.this);
			cancelBtn.getActionMap().put("ESCAPE", new AbstractAction() {
				public void actionPerformed(ActionEvent e) {
					LoginPanel.this.setVisible(false);
				}
			});
			add(cancelBtn);

			add(Box.createHorizontalStrut(10));

			loginBtn = new RGlassButton("LOGIN");
			loginBtn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					tryLogin();
				}
			});
			loginBtn.addKeyListener(LoginPanel.this);
			add(loginBtn);

		}
	}
}
