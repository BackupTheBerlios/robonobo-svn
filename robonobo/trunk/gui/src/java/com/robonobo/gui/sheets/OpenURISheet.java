package com.robonobo.gui.sheets;

import java.awt.event.*;

import javax.swing.JPanel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.debian.tablelayout.TableLayout;

import com.robonobo.common.concurrent.CatchingRunnable;
import com.robonobo.core.Platform;
import com.robonobo.gui.components.base.*;
import com.robonobo.gui.frames.RobonoboFrame;

public class OpenURISheet extends Sheet {
	private RButton openBtn;
	private RTextField uriField;

	public OpenURISheet(RobonoboFrame f) {
		super(f);
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
				OpenURISheet.this.setVisible(false);
			}
		});
	}
	
	@Override
	public void onShow() {
		uriField.requestFocusInWindow();
	}
}
