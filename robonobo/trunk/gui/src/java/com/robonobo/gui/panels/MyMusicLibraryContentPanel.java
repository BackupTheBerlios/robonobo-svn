package com.robonobo.gui.panels;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;

import org.debian.tablelayout.TableLayout;

import com.robonobo.core.Platform;
import com.robonobo.gui.frames.RobonoboFrame;
import com.robonobo.gui.model.TestyTrackListTableModel;

@SuppressWarnings("serial")
public class MyMusicLibraryContentPanel extends ContentPanel {

	public MyMusicLibraryContentPanel(RobonoboFrame frame) {
		super(frame, new TestyTrackListTableModel(frame.getController()));
//		super(frame, new MyMusicTableModel(frame.getController()));
		tabPane.insertTab("library", null, new MyLibraryTabPanel(), null, 0);
		tabPane.setSelectedIndex(0);
	}

	class MyLibraryTabPanel extends JPanel {
		public MyLibraryTabPanel() {
			double[][] cellSizen = { { 10, 160, 10, TableLayout.FILL, 10 }, { 10, 25, 10, 25, 10, 25, 10 } };
			setLayout(new TableLayout(cellSizen));
			JCheckBox shareLibCheckBox = new JCheckBox("Share library with friends?");
			// TODO Plumb this in
			shareLibCheckBox.setSelected(true);
			add(shareLibCheckBox,"1,1,3,1");
			JButton addFilesBtn = new JButton("Add files...");
			addFilesBtn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					frame.showAddSharesDialog();
				}
			});
			add(addFilesBtn, "1,3");
			if(Platform.getPlatform().iTunesAvailable()) {
				JButton addITunesBtn = new JButton("Add from iTunes...");
				addITunesBtn.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						// TODO Show iTunes stuff being added
						frame.importITunes();
					}
				});
				add(addITunesBtn, "1,5");
			}
		}
	}
}
