package com.robonobo.gui.panels;

import static com.robonobo.common.util.TextUtil.*;

import java.awt.ComponentOrientation;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.*;

import javax.swing.*;

import org.debian.tablelayout.TableLayout;

import com.robonobo.common.concurrent.CatchingRunnable;
import com.robonobo.common.exceptions.SeekInnerCalmException;
import com.robonobo.common.util.TextUtil;
import com.robonobo.core.Platform;
import com.robonobo.core.api.RobonoboException;
import com.robonobo.core.api.UserPlaylistListener;
import com.robonobo.core.api.model.*;
import com.robonobo.gui.RoboFont;
import com.robonobo.gui.dialogs.SharePlaylistDialog;
import com.robonobo.gui.frames.RobonoboFrame;
import com.robonobo.gui.model.*;

@SuppressWarnings("serial")
public class MyPlaylistContentPanel extends ContentPanel implements UserPlaylistListener {
	protected PlaylistConfig pc;
	protected JTextField titleField;
	protected JTextArea descField;
	protected JButton saveBtn;
	protected JButton shareBtn;
	protected JButton delBtn;
	protected JCheckBox friendsCB;
	protected JCheckBox iTunesCB;
	protected Map<String, JCheckBox> options = new HashMap<String, JCheckBox>();

	public MyPlaylistContentPanel(RobonoboFrame frame, Playlist p, PlaylistConfig pc) {
		super(frame, new PlaylistTableModel(frame.getController(), p, true));
		this.pc = pc;
		tabPane.insertTab("playlist", null, new PlaylistDetailsPanel(), null, 0);
		tabPane.setSelectedIndex(0);
		if (addAsListener())
			frame.getController().addUserPlaylistListener(this);
	}

	protected MyPlaylistContentPanel(RobonoboFrame frame, Playlist p, PlaylistConfig pc, PlaylistTableModel model) {
		super(frame, model);
		this.pc = pc;
		tabPane.insertTab("playlist", null, new PlaylistDetailsPanel(), null, 0);
		tabPane.setSelectedIndex(0);
		if (addAsListener())
			frame.getController().addUserPlaylistListener(this);
	}
	
	protected boolean addAsListener() {
		return true;
	}

	protected boolean allowShare() {
		return true;
	}

	protected boolean allowDel() {
		return true;
	}

	protected boolean detailsChanged() {
		return isNonEmpty(titleField.getText());
	}

	protected void savePlaylist() {
		frame.getController().getExecutor().execute(new CatchingRunnable() {
			public void doRun() throws Exception {
				Playlist p = getModel().getPlaylist();
				p.setTitle(titleField.getText());
				p.setDescription(descField.getText());
				try {
					// This creates the playlist's id if it doesn't already
					// exist, so update the playlist config
					frame.getController().putPlaylistConfig(pc);
					frame.getController().addOrUpdatePlaylist(p);
					pc.setPlaylistId(p.getPlaylistId());
				} catch (RobonoboException e) {
					frame.updateStatus("Error creating playlist: " + e.getMessage(), 10, 30);
					log.error("Error creating playlist", e);
				}
			}
		});
	}

	protected void deletePlaylist() {
		frame.getLeftSidebar().selectMyMusic();
		frame.getController().getExecutor().execute(new CatchingRunnable() {
			public void doRun() throws Exception {
				try {
					frame.getController().nukePlaylist(getModel().getPlaylist());
				} catch (RobonoboException e) {
					frame.updateStatus("Error deleting playlist: " + e.getMessage(), 10, 30);
					log.error("Error deleting playlist", e);
				}
			}
		});
	}

	protected PlaylistTableModel getModel(){
		return (PlaylistTableModel) trackList.getModel();
	}
	
	@Override
	public void loggedIn() {
		// Do nothing
	}

	@Override
	public void userChanged(User u) {
		// Do nothing
	}

	@Override
	public void playlistChanged(Playlist p) {
		if (p.equals(getModel().getPlaylist())) {
			getModel().setPlaylist(p);
			titleField.setText(p.getTitle());
			descField.setText(p.getDescription());
			friendsCB.setSelected(p.getAnnounce());
			PlaylistTableModel ptm = (PlaylistTableModel) trackList.getModel();
			ptm.update(p, true);
		}
	}

	@Override
	public boolean canImport(JComponent comp, DataFlavor[] transferFlavors) {
		for (DataFlavor dataFlavor : transferFlavors) {
			if (dataFlavor.equals(StreamTransfer.DATA_FLAVOR))
				return true;
		}
		return Platform.getPlatform().canDnDImport(transferFlavors);
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean importData(JComponent comp, Transferable t) {
		JTable table = trackList.getJTable();
		final PlaylistTableModel tm = (PlaylistTableModel) trackList.getModel();
		// If we have a mouse location, drop things there, otherwise
		// at the end
		int mouseRow = (table.getMousePosition() == null) ? -1 : table.rowAtPoint(table.getMousePosition());
		final int insertRow = (mouseRow >= 0) ? mouseRow : tm.getRowCount();
		boolean transferFromRobo = false;
		for (DataFlavor flavor : t.getTransferDataFlavors()) {
			if (flavor.equals(StreamTransfer.DATA_FLAVOR)) {
				transferFromRobo = true;
				break;
			}
		}
		if (transferFromRobo) {
			// DnD streams from inside robonobo
			List<String> streamIds;
			try {
				streamIds = (List<String>) t.getTransferData(StreamTransfer.DATA_FLAVOR);
			} catch (Exception e) {
				throw new SeekInnerCalmException();
			}
			tm.addStreams(streamIds, insertRow);
			return true;
		} else {
			// DnD files from somewhere else
			frame.updateStatus("Importing tracks...", 0, 30);
			List<File> importFiles = null;
			try {
				importFiles = Platform.getPlatform().getDnDImportFiles(t);
			} catch (IOException e) {
				log.error("Caught exception dropping files", e);
				return false;
			}
			final List<File> fl = importFiles;
			frame.getController().getExecutor().execute(new CatchingRunnable() {
				public void doRun() throws Exception {
					List<Stream> streams = frame.importFilesOrDirectories(fl);
					List<String> streamIds = new ArrayList<String>();
					for (Stream s : streams) {
						streamIds.add(s.getStreamId());
					}
					tm.addStreams(streamIds, insertRow);
				}
			});
			return true;
		}
	}

	class PlaylistDetailsPanel extends JPanel {
		public PlaylistDetailsPanel() {
			double[][] cellSizen = { { 5, 35, 5, 365, 20, TableLayout.FILL, 5 },
					{ 5, 25, 0, 25, 0, TableLayout.FILL, 5, 30, 5 } };
			setLayout(new TableLayout(cellSizen));

			KeyListener kl = new KeyAdapter() {
				@Override
				public void keyTyped(KeyEvent e) {
					saveBtn.setEnabled(detailsChanged());
				}
			};
			Playlist p = getModel().getPlaylist();
			JLabel titleLbl = new JLabel("Title:");
			titleLbl.setFont(RoboFont.getFont(13, false));
			add(titleLbl, "1,1");
			titleField = new JTextField(p.getTitle());
			titleField.setFont(RoboFont.getFont(11, false));
			titleField.addKeyListener(kl);
			add(titleField, "3,1");

			JLabel descLbl = new JLabel("Description:");
			descLbl.setFont(RoboFont.getFont(13, false));
			add(descLbl, "1,3,3,3");
			descField = new JTextArea(p.getDescription());
			descField.setFont(RoboFont.getFont(11, false));
			descField.addKeyListener(kl);
			add(new JScrollPane(descField), "1,5,3,7");

			add(new OptsPanel(), "5,1,5,5");
			add(new ButtonsPanel(), "5,7");
		}
	}

	class OptsPanel extends JPanel {
		public OptsPanel() {
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

			ActionListener al = new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					saveBtn.setEnabled(detailsChanged());
				}
			};

			friendsCB = new JCheckBox("Let friends see this playlist");
			friendsCB.setFont(RoboFont.getFont(12, true));
			friendsCB.setSelected(getModel().getPlaylist().getAnnounce());
			friendsCB.addActionListener(al);
			add(friendsCB);
			add(Box.createVerticalStrut(5));

			if (Platform.getPlatform().iTunesAvailable()) {
				iTunesCB = new JCheckBox("Export playlist to iTunes");
				iTunesCB.setFont(RoboFont.getFont(12, true));
				iTunesCB.setSelected("true".equalsIgnoreCase(pc.getItem("iTunesExport")));
				options.put("iTunesExport", iTunesCB);
				iTunesCB.addActionListener(al);
				add(iTunesCB);
			}
		}
	}

	class ButtonsPanel extends JPanel {
		public ButtonsPanel() {
			setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
			setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));

			// Laying out right-to-left
			if (allowDel()) {
				delBtn = new JButton("DELETE");
				delBtn.setFont(RoboFont.getFont(12, true));
				delBtn.setName("robonobo.red.button");
				delBtn.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						int result = JOptionPane.showConfirmDialog(ButtonsPanel.this,
								"Are you sure you want to delete this playlist?", "Delete this playlist?",
								JOptionPane.YES_NO_OPTION);
						if (result == JOptionPane.YES_OPTION)
							deletePlaylist();
					}
				});
				add(delBtn);
				add(Box.createHorizontalStrut(5));
			}

			if (allowShare()) {
				shareBtn = new JButton("SHARE");
				shareBtn.setFont(RoboFont.getFont(12, true));
				shareBtn.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						SharePlaylistDialog dialog = new SharePlaylistDialog(frame, getModel().getPlaylist());
						dialog.setLocationRelativeTo(frame);
						dialog.setVisible(true);
					}
				});
				add(shareBtn);
				add(Box.createHorizontalStrut(5));
			}

			saveBtn = new JButton("SAVE");
			saveBtn.setFont(RoboFont.getFont(12, true));
			saveBtn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					getModel().getPlaylist().setAnnounce(friendsCB.isSelected());
					pc.getItems().clear();
					for (String opt : options.keySet()) {
						JCheckBox cb = options.get(opt);
						if (cb.isSelected())
							pc.setItem(opt, "true");
					}
					saveBtn.setEnabled(false);
					savePlaylist();
				}
			});
			saveBtn.setEnabled(false);
			add(saveBtn);

		}
	}
}