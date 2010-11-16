package com.robonobo.gui.panels;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.*;

import org.debian.tablelayout.TableLayout;

import com.robonobo.common.concurrent.CatchingRunnable;
import com.robonobo.common.exceptions.SeekInnerCalmException;
import com.robonobo.core.Platform;
import com.robonobo.core.api.UserPlaylistListener;
import com.robonobo.core.api.model.*;
import com.robonobo.gui.frames.RobonoboFrame;
import com.robonobo.gui.model.MyLibraryTableModel;

@SuppressWarnings("serial")
public class MyLibraryContentPanel extends ContentPanel implements UserPlaylistListener {
	private JCheckBox shareLibCheckBox;

	public MyLibraryContentPanel(RobonoboFrame frame) {
		super(frame, new MyLibraryTableModel(frame.getController()));
		tabPane.insertTab("library", null, new MyLibraryTabPanel(), null, 0);
		tabPane.setSelectedIndex(0);
		frame.getController().addUserPlaylistListener(this);
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

	@Override
	public void libraryChanged(Library lib) {
		// Do nothing
	}
	
	@Override
	public void loggedIn() {
		// Do nothing
	}
	
	@Override
	public void playlistChanged(Playlist p) {
		// Do nothing
	}
	
	@Override
	public void userChanged(User u) {
		// Do nothing
	}
	
	@Override
	public void userConfigChanged(UserConfig cfg) {
		boolean libShared = true;
		if(cfg.getItems().containsKey("sharelibrary"))
			libShared = ("true".equalsIgnoreCase(cfg.getItems().get("sharelibrary")));
		final boolean flarp = libShared;
		SwingUtilities.invokeLater(new CatchingRunnable() {
			public void doRun() throws Exception {
				shareLibCheckBox.setEnabled(true);
				shareLibCheckBox.setSelected(flarp);
			}
		});
	}

	class MyLibraryTabPanel extends JPanel {
		public MyLibraryTabPanel() {
			double[][] cellSizen = { { 10, 160, 10, TableLayout.FILL, 10 }, { 10, 25, 10, 25, 10, 25, 10 } };
			setLayout(new TableLayout(cellSizen));
			shareLibCheckBox = new JCheckBox("Share library with friends?");
			shareLibCheckBox.addItemListener(new ItemListener() {
				public void itemStateChanged(final ItemEvent e) {
					frame.getController().getExecutor().execute(new CatchingRunnable() {
						@Override
						public void doRun() throws Exception {
							if(e.getStateChange() == ItemEvent.SELECTED) {
								frame.getController().saveUserConfigItem("sharelibrary", "true");
							} else if(e.getStateChange() == ItemEvent.DESELECTED) {
								frame.getController().saveUserConfigItem("sharelibrary", "false");
							}													
						}
					});
				}
			});
			shareLibCheckBox.setSelected(false);
			// We disable it first, it gets re-enabled when we get our user config
			shareLibCheckBox.setEnabled(false);
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
