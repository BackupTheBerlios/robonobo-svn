package com.robonobo.oldgui;

import gnu.iou.sh.Shell;

import java.awt.BorderLayout;
import java.awt.HeadlessException;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;

import com.robonobo.core.Platform;

public class ConsoleFrame extends JFrame {
	private RobonoboFrame frame;
	private Thread thread;

	public ConsoleFrame(RobonoboFrame frame) throws HeadlessException {
		this.frame = frame;
		if(Platform.getPlatform().shouldSetMenuBarOnDialogs())
			setJMenuBar(Platform.getPlatform().getMenuBar(frame));
		setTitle("Robonobo Console");
		setIconImage(RobonoboFrame.getRobonoboIconImage());
		Shell shell = new Shell(null, new ConsoleShellPlugin(frame), 20, 150);
		getContentPane().add(shell, BorderLayout.CENTER);
		pack();
		thread = new Thread(shell);
		thread.start();
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				thread.interrupt();
			}
		});
	}
}
