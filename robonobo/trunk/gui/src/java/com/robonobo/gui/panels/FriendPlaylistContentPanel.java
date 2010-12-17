package com.robonobo.gui.panels;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.*;

import org.debian.tablelayout.TableLayout;

import com.robonobo.common.concurrent.CatchingRunnable;
import com.robonobo.common.util.TextUtil;
import com.robonobo.core.Platform;
import com.robonobo.core.api.RobonoboException;
import com.robonobo.core.api.UserPlaylistListener;
import com.robonobo.core.api.model.*;
import com.robonobo.gui.RoboFont;
import com.robonobo.gui.components.base.*;
import com.robonobo.gui.frames.RobonoboFrame;
import com.robonobo.gui.model.PlaylistTableModel;

@SuppressWarnings("serial")
public class FriendPlaylistContentPanel extends ContentPanel implements UserPlaylistListener {
	Playlist p;
	PlaylistConfig pc;
	RLabel titleField;
	RLabel descField;
	RButton saveBtn;
	RCheckBox autoDownloadCB;
	RCheckBox iTunesCB;
	protected Map<String, JCheckBox> options = new HashMap<String, JCheckBox>();

	public FriendPlaylistContentPanel(RobonoboFrame frame, Playlist p, PlaylistConfig pc) {
		super(frame, new PlaylistTableModel(frame.getController(), p, false));
		this.p = p;
		this.pc = pc;
		tabPane.insertTab("playlist", null, new PlaylistDetailsPanel(), null, 0);
		tabPane.setSelectedIndex(0);
		frame.getController().addUserPlaylistListener(this);
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
		if (p.equals(this.p)) {
			// TODO Left over from old GUI - is this needed?
			if (p.getOwnerIds().contains(frame.getController().getMyUser().getUserId())) {
				log.debug("DEBUG: not updating playlist content panel for playlist '" + p.getTitle()
						+ "' - I am an owner!");
				return;
			}
			this.p = p;
			SwingUtilities.invokeLater(new CatchingRunnable() {
				public void doRun() throws Exception {
					updateFields();
				}
			});
		}
	}

	void updateFields() {
		titleField.setText(p.getTitle());
		descField.setText("<html>" + TextUtil.escapeHtml(p.getDescription()) + "</html>");
	}

	class PlaylistDetailsPanel extends JPanel {
		public PlaylistDetailsPanel() {
			double[][] cellSizen = { { 5, 150, 5, 250, 5, TableLayout.FILL, 5 },
					{ 5, 30, 5, 30, 5, TableLayout.FILL, 5, 30, 5 } };
			setLayout(new TableLayout(cellSizen));

			RLabel titleLbl = new RLabel13("Title:");
			add(titleLbl, "1,1");
			titleField = new RLabel13();
			add(titleField, "3,1");

			RLabel descLbl = new RLabel13("Description:");
			add(descLbl, "1,3,3,3,f,t");
			descField = new RLabel13();
			add(descField, "1,5,3,5");

			updateFields();

			add(new OptsPanel(), "5,1,5,5");
			add(new ButtonsPanel(), "5,7");
		}
	}

	class OptsPanel extends JPanel {
		public OptsPanel() {
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

			ActionListener al = new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					saveBtn.setEnabled(true);
				}
			};

			autoDownloadCB = new RCheckBox("Download tracks automatically");
			autoDownloadCB.setSelected("true".equals(pc.getItem("autoDownload")));
			options.put("autoDownload", autoDownloadCB);
			autoDownloadCB.addActionListener(al);
			add(autoDownloadCB);

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
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

			saveBtn = new RGlassButton("SAVE");
			saveBtn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					pc.getItems().clear();
					for (String opt : options.keySet()) {
						JCheckBox cb = options.get(opt);
						if (cb.isSelected())
							pc.setItem(opt, "true");
					}
					saveBtn.setEnabled(false);
					pc.setPlaylistId(p.getPlaylistId());
					frame.getController().putPlaylistConfig(pc);
					// Checking playlist update will kick off autodownloads, if necessary
					frame.getController().getExecutor().execute(new CatchingRunnable() {
						public void doRun() throws Exception {
							try {
								frame.getController().checkPlaylistUpdate(p.getPlaylistId());
							} catch (RobonoboException e) {
								log.info("Error checking playlist update", e);
							}
						}
					});
				}
			});
			saveBtn.setEnabled(false);
			add(saveBtn);
		}
	}

}
