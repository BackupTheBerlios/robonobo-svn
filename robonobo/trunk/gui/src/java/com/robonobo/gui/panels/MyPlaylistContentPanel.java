package com.robonobo.gui.panels;

import static com.robonobo.common.util.TextUtil.*;

import java.awt.ComponentOrientation;
import java.awt.Toolkit;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.*;

import javax.swing.*;

import org.debian.tablelayout.TableLayout;

import com.robonobo.common.concurrent.CatchingRunnable;
import com.robonobo.common.exceptions.SeekInnerCalmException;
import com.robonobo.common.util.FileUtil;
import com.robonobo.common.util.NetUtil;
import com.robonobo.core.Platform;
import com.robonobo.core.api.RobonoboException;
import com.robonobo.core.api.UserPlaylistListener;
import com.robonobo.core.api.model.*;
import com.robonobo.gui.RoboColor;
import com.robonobo.gui.RoboFont;
import com.robonobo.gui.components.base.*;
import com.robonobo.gui.frames.RobonoboFrame;
import com.robonobo.gui.model.PlaylistTableModel;
import com.robonobo.gui.model.StreamTransfer;
import com.robonobo.gui.sheets.*;
import com.robonobo.gui.tasks.ImportFilesTask;

@SuppressWarnings("serial")
public class MyPlaylistContentPanel extends ContentPanel implements UserPlaylistListener {
	protected PlaylistConfig pc;
	protected RTextField titleField;
	protected RTextField urlField;
	protected RTextArea descField;
	protected RButton saveBtn;
	protected RButton shareBtn;
	protected RButton delBtn;
	protected RCheckBox iTunesCB;
	protected RRadioButton visMeBtn;
	protected RRadioButton visFriendsBtn;
	protected RRadioButton visAllBtn;
	protected Map<String, RCheckBox> options = new HashMap<String, RCheckBox>();

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
					log.error("Error creating playlist", e);
				}
			}
		});
	}

	protected PlaylistTableModel getModel() {
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
	public void libraryChanged(Library lib) {
		// Do nothing
	}

	@Override
	public void userConfigChanged(UserConfig cfg) {
		// Do nothing
	}

	@Override
	public void playlistChanged(Playlist p) {
		if (p.equals(getModel().getPlaylist())) {
			getModel().setPlaylist(p);
			titleField.setText(p.getTitle());
			descField.setText(p.getDescription());
			String vis = p.getVisibility();
			if (vis.equals(Playlist.VIS_ALL))
				visAllBtn.setSelected(true);
			else if (vis.equals(Playlist.VIS_FRIENDS))
				visFriendsBtn.setSelected(true);
			else if (vis.equals(Playlist.VIS_ME))
				visMeBtn.setSelected(true);
			else
				throw new SeekInnerCalmException("invalid visibility " + vis);
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
			List<File> files = null;
			try {
				files = Platform.getPlatform().getDnDImportFiles(t);
			} catch (IOException e) {
				log.error("Caught exception dropping files", e);
				return false;
			}
			List<File> allFiles = new ArrayList<File>();
			for (File selFile : files)
				if (selFile.isDirectory())
					allFiles.addAll(FileUtil.getFilesWithinPath(selFile, "mp3"));
				else
					allFiles.add(selFile);
			frame.getController().runTask(new PlaylistImportTask(allFiles, insertRow));
			return true;
		}
	}

	class PlaylistImportTask extends ImportFilesTask {
		int insertRow;

		public PlaylistImportTask(List<File> files, int insertRow) {
			super(frame.getController(), files);
			this.insertRow = insertRow;
		}

		@Override
		protected void streamsAdded(List<String> streamIds) {
			PlaylistTableModel tm = (PlaylistTableModel) trackList.getModel();
			tm.addStreams(streamIds, insertRow);
		}
	}

	class PlaylistDetailsPanel extends JPanel implements ClipboardOwner {

		public PlaylistDetailsPanel() {
			double[][] cellSizen = { { 5, 35, 5, 215, 5, 30, 5, 30, 5, 90, 10, 150, 5, TableLayout.FILL, 5 },
					{ 5, 25, 5, 25, 25, 0, TableLayout.FILL, 5, 30, 5 } };
			setLayout(new TableLayout(cellSizen));

			KeyListener kl = new KeyAdapter() {
				@Override
				public void keyTyped(KeyEvent e) {
					saveBtn.setEnabled(detailsChanged());
				}
			};
			final Playlist p = getModel().getPlaylist();
			JLabel titleLbl = new JLabel("Title:");
			titleLbl.setFont(RoboFont.getFont(13, false));
			add(titleLbl, "1,1");
			titleField = new RTextField(p.getTitle());
			titleField.addKeyListener(kl);
			add(titleField, "3,1,9,1");

			RLabel urlLbl = new RLabel13("URL:");
			add(urlLbl, "1,3");
			String urlBase = frame.getController().getConfig().getPlaylistUrlBase();
			String urlText = (p.getPlaylistId() > 0) ? urlBase + Long.toHexString(p.getPlaylistId()) : "(none)";
			urlField = new RTextField(urlText);
			urlField.setEnabled(false);
			add(urlField, "3,3");
			RButton fbBtn = new RSmallRoundButton(new ImageIcon(
					RobonoboFrame.class.getResource("/img/icon/facebook.png")));
			// TODO If we are not set up for facebook/twitter, take us to our account page instead...
			fbBtn.setToolTipText("Post playlist update to facebook");
			fbBtn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					fbUpdateBtnPressed();
				}
			});
			fbBtn.setEnabled(p.getPlaylistId() > 0);
			add(fbBtn, "5,3");
			RButton twitBtn = new RSmallRoundButton(new ImageIcon(
					RobonoboFrame.class.getResource("/img/icon/twitter.png")));
			twitBtn.setToolTipText("Post playlist update to twitter");
			twitBtn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					twitUpdateBtnPressed();
				}
			});
			twitBtn.setEnabled(p.getPlaylistId() > 0);
			add(twitBtn, "7,3");
			RButton copyBtn = new RSmallRoundButton("Copy URL");
			copyBtn.setToolTipText("Copy playlist URL to clipboard");
			copyBtn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					Clipboard c = Toolkit.getDefaultToolkit().getSystemClipboard();
					StringSelection s = new StringSelection(urlField.getText());
					c.setContents(s, PlaylistDetailsPanel.this);
				}
			});
			copyBtn.setEnabled(p.getPlaylistId() > 0);
			add(copyBtn, "9,3");

			RLabel descLbl = new RLabel13("Description:");
			add(descLbl, "1,4,9,4");
			descField = new RTextArea(p.getDescription());
			descField.setBGColor(RoboColor.MID_GRAY);
			descField.addKeyListener(kl);
			add(new JScrollPane(descField), "1,6,9,8");
			add(new VisPanel(), "11,1,11,6");
			add(new OptsPanel(), "13,1,13,6");
			add(new ButtonsPanel(), "11,8,13,8");
		}

		@Override
		public void lostOwnership(Clipboard clipboard, Transferable contents) {
			// Do nothing
		}

		private void fbUpdateBtnPressed() {
			UserConfig uc = frame.getController().getMyUserConfig();
			if (uc == null || uc.getItem("facebookId") == null) {
				// We don't seem to be registered for facebook - fetch a fresh copy of the usercfg from midas in
				// case they've recently added themselves to fb, but GTFOTUT
				frame.getController().getExecutor().execute(new CatchingRunnable() {
					public void doRun() throws Exception {
						UserConfig freshUc = frame.getController().refreshMyUserConfig();
						if (freshUc == null || freshUc.getItem("facebookId") == null) {
							// They haven't associated their facebook account with their rbnb one... bounce them to
							// their account page so they can do so
							NetUtil.browse(frame.getController().getConfig().getUserAccountUrl());
						} else {
							SwingUtilities.invokeLater(new CatchingRunnable() {
								public void doRun() throws Exception {
									frame.dim();
									frame.showSheet(new PostToFacebookSheet(frame, getModel().getPlaylist()));
								}
							});
						}
					}
				});
			} else {
				frame.dim();
				frame.showSheet(new PostToFacebookSheet(frame, getModel().getPlaylist()));
			}
		}
		
		private void twitUpdateBtnPressed() {
			UserConfig uc = frame.getController().getMyUserConfig();
			if (uc == null || uc.getItem("twitterId") == null) {
				// We don't seem to be registered for twitter - fetch a fresh copy of the usercfg from midas in
				// case they've recently added themselves, but GTFOTUT
				frame.getController().getExecutor().execute(new CatchingRunnable() {
					public void doRun() throws Exception {
						UserConfig freshUc = frame.getController().refreshMyUserConfig();
						if (freshUc == null || freshUc.getItem("twitterId") == null) {
							// They haven't associated their twitter account with their rbnb one... bounce them to
							// their account page so they can do so
							NetUtil.browse(frame.getController().getConfig().getUserAccountUrl());
						} else {
							SwingUtilities.invokeLater(new CatchingRunnable() {
								public void doRun() throws Exception {
									frame.dim();
									frame.showSheet(new PostToTwitterSheet(frame, getModel().getPlaylist()));
								}
							});
						}
					}
				});
			} else {
				frame.dim();
				frame.showSheet(new PostToTwitterSheet(frame, getModel().getPlaylist()));
			}			
		}
	}

	class VisPanel extends JPanel {
		public VisPanel() {
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

			ActionListener al = new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					saveBtn.setEnabled(detailsChanged());
				}
			};

			RLabel visLbl = new RLabel12B("Show playlist to:");
			add(visLbl);
			add(Box.createVerticalStrut(5));
			ButtonGroup bg = new ButtonGroup();
			// TODO multiple owners?
			Playlist p = getModel().getPlaylist();
			String vis = p.getVisibility();
			visMeBtn = new RRadioButton("Just me");
			visMeBtn.addActionListener(al);
			if (vis.equals(Playlist.VIS_ME))
				visMeBtn.setSelected(true);
			bg.add(visMeBtn);
			add(visMeBtn);
			visFriendsBtn = new RRadioButton("Friends");
			visFriendsBtn.addActionListener(al);
			if (vis.equals(Playlist.VIS_FRIENDS))
				visFriendsBtn.setSelected(true);
			bg.add(visFriendsBtn);
			add(visFriendsBtn);
			visAllBtn = new RRadioButton("Everyone");
			visAllBtn.addActionListener(al);
			if (vis.equals(Playlist.VIS_ALL))
				visAllBtn.setSelected(true);
			bg.add(visAllBtn);
			add(visAllBtn);
		}
	}

	class OptsPanel extends JPanel {
		public OptsPanel() {
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			add(Box.createVerticalStrut(20));
			ActionListener al = new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					saveBtn.setEnabled(detailsChanged());
				}
			};
			if (Platform.getPlatform().iTunesAvailable()) {
				iTunesCB = new RCheckBox("Export playlist to iTunes");
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
				delBtn = new RRedGlassButton("DELETE");
				delBtn.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						DeletePlaylistSheet dPanel = new DeletePlaylistSheet(frame, getModel().getPlaylist());
						frame.dim();
						frame.showSheet(dPanel);
					}
				});
				add(delBtn);
				add(Box.createHorizontalStrut(5));
			}

			if (allowShare()) {
				shareBtn = new RGlassButton("SHARE");
				shareBtn.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						SharePlaylistSheet shPanel = new SharePlaylistSheet(frame, getModel().getPlaylist());
						frame.dim();
						frame.showSheet(shPanel);
					}
				});
				add(shareBtn);
				add(Box.createHorizontalStrut(5));
			}

			saveBtn = new RGlassButton("SAVE");
			saveBtn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					Playlist p = getModel().getPlaylist();
					if (visAllBtn.isSelected())
						p.setVisibility(Playlist.VIS_ALL);
					else if (visFriendsBtn.isSelected())
						p.setVisibility(Playlist.VIS_FRIENDS);
					else if (visMeBtn.isSelected())
						p.setVisibility(Playlist.VIS_ME);
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
