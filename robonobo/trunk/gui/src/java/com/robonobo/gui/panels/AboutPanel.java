package com.robonobo.gui.panels;

import java.awt.Dimension;
import java.awt.event.*;
import java.io.IOException;
import java.io.InputStream;

import javax.swing.*;

import org.debian.tablelayout.TableLayout;

import com.robonobo.common.exceptions.SeekInnerCalmException;
import com.robonobo.core.Platform;
import com.robonobo.gui.RoboFont;
import com.robonobo.gui.frames.RobonoboFrame;

public class AboutPanel extends JPanel implements KeyListener {
	private static final String CREDITS_PATH = "/credits.html";
	RobonoboFrame frame;
	Dimension sz = new Dimension(500, 400);
	
	public AboutPanel(RobonoboFrame frame) {
		this.frame = frame;
		addKeyListener(this);
		double[][] cellSizen = { {10, TableLayout.FILL, 100, 10}, { 10, 25, 10, TableLayout.FILL, 10, 30, 10 } };
		setName("playback.background.panel");
		setLayout(new TableLayout(cellSizen));
		setPreferredSize(sz);
		
		JLabel title = new JLabel("About robonobo (version "+frame.getController().getVersion()+")");
		title.setFont(RoboFont.getFont(14, true));
		add(title, "1,1,2,1,LEFT,CENTER");
		
		JTextPane textPane = new JTextPane();
		textPane.setContentType("text/html");
		textPane.setText(getCredits());
		textPane.setFont(RoboFont.getFont(12, false));
		textPane.setEditable(false);
		JScrollPane scrollPane = new JScrollPane(textPane);
		add(scrollPane, "1,3,2,3");

		JButton closeBtn = new JButton("CLOSE");
		closeBtn.setName("robonobo.red.button");
		closeBtn.setFont(RoboFont.getFont(12, true));
		closeBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
			}
		});
		add(closeBtn, "2,5");
	}

	private String getCredits() {
		InputStream is = getClass().getResourceAsStream(CREDITS_PATH);
		StringBuffer sb = new StringBuffer();
		byte[] buf = new byte[1024];
		int numRead;
		try {
			while ((numRead = is.read(buf)) > 0) {
				sb.append(new String(buf, 0, numRead));
			}
			is.close();
		} catch (IOException e) {
			throw new SeekInnerCalmException(e);
		}
		return sb.toString().replace("!VERSION!", frame.getController().getVersion());
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

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if(!visible)
			frame.undim();
	}
}
