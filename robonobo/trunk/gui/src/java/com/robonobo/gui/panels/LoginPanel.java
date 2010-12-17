package com.robonobo.gui.panels;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.debian.tablelayout.TableLayout;

import com.robonobo.common.concurrent.CatchingRunnable;
import com.robonobo.common.serialization.UnauthorizedException;
import com.robonobo.core.Platform;
import com.robonobo.gui.RoboColor;
import com.robonobo.gui.RoboFont;
import com.robonobo.gui.components.base.HyperlinkPane;
import com.robonobo.gui.frames.RobonoboFrame;

public class LoginPanel extends JPanel implements KeyListener {
	private RobonoboFrame frame;
	private JButton loginBtn;
	private JButton cancelBtn;
	private JTextField emailField;
	private JPasswordField passwordField;
	private JLabel statusLbl;
	private Runnable onLogin;
	private Log log = LogFactory.getLog(getClass());

	public LoginPanel(RobonoboFrame rFrame, Runnable onLogin) throws HeadlessException {
		this.frame = rFrame;
		this.onLogin = onLogin;
		double[][] cellSizen = { { 10, 100, 10, 180, 10 }, { 10, 25, 5, 25, 5, 25, 5, 25, 5, 20, 5, 30, 10 } };
		setLayout(new TableLayout(cellSizen));
		setName("playback.background.panel");

		JLabel title = new JLabel("Please login to robonobo");
		title.setFont(RoboFont.getFont(14, true));
		add(title, "1,1,3,1,CENTER,CENTER");

		String blurbTxt = "<html><center>Visit <a href=\"http://robonobo.com\">http://robonobo.com</a> for an account.<br><br></center></html>";
		HyperlinkPane blurbLbl = new HyperlinkPane(blurbTxt, RoboColor.MID_GRAY);
		add(blurbLbl, "1,3,3,3");

		JLabel emailLbl = new JLabel("Email:");
		add(emailLbl, "1,5,r,f");
		emailField = new JTextField();
		emailField.addKeyListener(this);
		add(emailField, "3,5");

		JLabel pwdLbl = new JLabel("Password:");
		add(pwdLbl, "1,7,r,f");

		passwordField = new JPasswordField();
		passwordField.addKeyListener(this);
		add(passwordField, "3,7");

		statusLbl = new JLabel("");
		statusLbl.setFont(RoboFont.getFont(12, false));
		add(statusLbl, "1,9,3,9,RIGHT,CENTER");

		ButtonPanel btnPanel = new ButtonPanel();
		btnPanel.addKeyListener(this);
		add(btnPanel, "1,11,3,11");

	}

	private void tryLogin() {
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
					statusLbl.setText("Server error - see log");
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
			cancelBtn = new JButton("CANCEL");
			cancelBtn.setName("robonobo.red.button");
			cancelBtn.setFont(RoboFont.getFont(12, true));
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

			loginBtn = new JButton("LOGIN");
			loginBtn.setFont(RoboFont.getFont(12, true));
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
