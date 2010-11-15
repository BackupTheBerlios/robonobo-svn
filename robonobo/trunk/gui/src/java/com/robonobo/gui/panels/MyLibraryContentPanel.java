package com.robonobo.gui.panels;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.*;

import org.debian.tablelayout.TableLayout;

import com.robonobo.common.concurrent.CatchingRunnable;
import com.robonobo.core.Platform;
import com.robonobo.gui.frames.RobonoboFrame;
import com.robonobo.gui.model.MyLibraryTableModel;

@SuppressWarnings("serial")
public class MyLibraryContentPanel extends ContentPanel {

	public MyLibraryContentPanel(RobonoboFrame frame) {
		super(frame, new MyLibraryTableModel(frame.getController()));
		tabPane.insertTab("library", null, new MyLibraryTabPanel(), null, 0);
		tabPane.setSelectedIndex(0);
	}

	@Override
	public boolean canImport(JComponent comp, DataFlavor[] transferFlavors) {
		return Platform.getPlatform().canDnDImport(transferFlavors);
	}

	@Override
	public boolean importData(JComponent comp, Transferable t) {
		List<File> l = null;
		try {
			l = Platform.getPlatform().getDnDImportFiles(t);
		} catch (IOException e) {
			log.error("Caught exception dropping files", e);
			return false;
		}
		final List<File> fl = l;
		frame.getController().getExecutor().execute(new CatchingRunnable() {
			public void doRun() throws Exception {
				frame.importFilesOrDirectories(fl);
			}
		});
		return true;
	}

	class MyLibraryTabPanel extends JPanel {
		public MyLibraryTabPanel() {
			double[][] cellSizen = { { 10, 160, 10, TableLayout.FILL, 10 }, { 10, 25, 10, 25, 10, 25, 10 } };
			setLayout(new TableLayout(cellSizen));
			JCheckBox shareLibCheckBox = new JCheckBox("Share library with friends?");
			// TODO Plumb this in
			shareLibCheckBox.setSelected(true);
			add(shareLibCheckBox, "1,1,3,1");
			JButton shareFilesBtn = new JButton("Share from files...");
			shareFilesBtn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					frame.showAddSharesDialog();
				}
			});
			add(shareFilesBtn, "1,3");
			if (Platform.getPlatform().iTunesAvailable()) {
				JButton shareITunesBtn = new JButton("Share from iTunes...");
				shareITunesBtn.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						// TODO Show iTunes stuff being added
						frame.importITunes();
					}
				});
				add(shareITunesBtn, "1,5");
			}
		}
	}
}
