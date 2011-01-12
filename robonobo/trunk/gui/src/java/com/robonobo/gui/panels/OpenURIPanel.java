package com.robonobo.gui.panels;

import java.awt.event.*;

import javax.swing.JPanel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.debian.tablelayout.TableLayout;

import com.robonobo.common.concurrent.CatchingRunnable;
import com.robonobo.core.Platform;
import com.robonobo.gui.components.base.*;
import com.robonobo.gui.frames.RobonoboFrame;

public class OpenURIPanel extends JPanel implements KeyListener {
	private RobonoboFrame frame;
	private RButton openBtn;
	private RTextField uriField;

	public OpenURIPanel(RobonoboFrame f) {
		frame = f;
		double[][] cellSizen = { { 10, 160, 5, 80, 10 }, { 10, 20, 5, 30, 5, 30, 10 } };
		setLayout(new TableLayout(cellSizen));
		setName("playback.background.panel");
		add(new RLabel14B("Open URI"), "1,1,3,1");
		uriField = new RTextField();
		uriField.addKeyListener(this);
		add(uriField, "1,3,3,3");
		openBtn = new RGlassButton("OPEN");
		openBtn.addKeyListener(this);
		add(openBtn, "3,5");
		openBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				frame.getController().getExecutor().execute(new CatchingRunnable() {
					public void doRun() throws Exception {
						frame.openRbnbUri(uriField.getText());
					}
				});
				OpenURIPanel.this.setVisible(false);
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

	@Override
	public void keyTyped(KeyEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void keyReleased(KeyEvent e) {
		// TODO Auto-generated method stub
		
	}
}
