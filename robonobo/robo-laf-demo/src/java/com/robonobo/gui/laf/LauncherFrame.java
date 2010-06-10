package com.robonobo.gui.laf;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;

@SuppressWarnings("serial")
public class LauncherFrame extends JFrame {
	ImportFrame importFrame = new ImportFrame();
	EditPlaylistFrame editFrame = new EditPlaylistFrame();
	ViewPlaylistFrame viewFrame = new ViewPlaylistFrame();
	SendPlaylistDialog sendDialog = new SendPlaylistDialog(this);
	SharePlaylistDialog shareDialog = new SharePlaylistDialog(this);
	
	public LauncherFrame() {
		setSize(200, 175);
		setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
		setTitle("robonobo laf demo");
		
		JButton importBtn = new JButton("Import screen");
		importBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				importFrame.setVisible(true);
			}
		});
		add(importBtn);
		add(Box.createVerticalStrut(10));
		
		JButton viewBtn = new JButton("View playlist screen");
		viewBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				viewFrame.setVisible(true);
			}
		});
		add(viewBtn);
		add(Box.createVerticalStrut(10));
		 
		JButton editBtn = new JButton("Edit playlist screen");
		editBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				editFrame.setVisible(true);
			}
		});
		add(editBtn);
		add(Box.createVerticalStrut(10));

		JButton dialogBtn = new JButton("Dialogs");
		dialogBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				sendDialog.setLocation(100, 100);
				sendDialog.setVisible(true);
				shareDialog.setLocation(100, 300);
				shareDialog.setVisible(true);
			}
		});
		add(dialogBtn);
	}
	
	public static void main(String[] args) {
		LauncherFrame flarp = new LauncherFrame();
		flarp.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});
		flarp.setVisible(true);
	}
}
