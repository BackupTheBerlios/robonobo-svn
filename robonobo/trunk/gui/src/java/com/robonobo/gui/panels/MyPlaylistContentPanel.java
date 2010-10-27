package com.robonobo.gui.panels;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.*;

import org.debian.tablelayout.TableLayout;

import com.robonobo.common.concurrent.CatchingRunnable;
import com.robonobo.core.Platform;
import com.robonobo.core.api.RobonoboException;
import com.robonobo.core.api.UserPlaylistListener;
import com.robonobo.core.api.model.*;
import com.robonobo.gui.RoboFont;
import com.robonobo.gui.dialogs.SharePlaylistDialog;
import com.robonobo.gui.frames.RobonoboFrame;
import com.robonobo.gui.model.PlaylistTableModel;

@SuppressWarnings("serial")
public class MyPlaylistContentPanel extends ContentPanel implements UserPlaylistListener {
	protected Playlist p;
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
		this.p = p;
		this.pc = pc;
		tabPane.insertTab("playlist", null, new PlaylistDetailsPanel(), null, 0);
		tabPane.setSelectedIndex(0);
		if(addAsListener())
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
		return (p.getTitle() != null && p.getTitle().length() > 0);
	}
	
	protected void savePlaylist() {
		frame.getController().getExecutor().execute(new CatchingRunnable() {
			public void doRun() throws Exception {
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
					frame.getController().nukePlaylist(p);
				} catch (RobonoboException e) {
					frame.updateStatus("Error deleting playlist: " + e.getMessage(), 10, 30);
					log.error("Error deleting playlist", e);
				}
			}
		});
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
		if(p.equals(this.p)) {
			this.p = p;
			titleField.setText(p.getTitle());
			descField.setText(p.getDescription());
			friendsCB.setSelected(p.getAnnounce());
			PlaylistTableModel ptm = (PlaylistTableModel) trackList.getTableModel();
			ptm.update(p, true);
		}
	}
	
	class PlaylistDetailsPanel extends JPanel {
		public PlaylistDetailsPanel() {
			double[][] cellSizen = { { 5, 150, 5, 250, 5, TableLayout.FILL, 5 },
					{ 5, 30, 5, 30, 5, TableLayout.FILL, 5, 30, 5 } };
			setLayout(new TableLayout(cellSizen));

			JLabel titleLbl = new JLabel("Title:");
			titleLbl.setFont(RoboFont.getFont(13, false));
			add(titleLbl, "1,1");
			titleField = new JTextField(p.getTitle());
			titleField.setFont(RoboFont.getFont(13, false));
			add(titleField, "3,1");

			JLabel descLbl = new JLabel("Description:");
			descLbl.setFont(RoboFont.getFont(13, false));
			add(descLbl, "1,3,3,3,f,t");
			descField = new JTextArea(p.getDescription());
			descField.setFont(RoboFont.getFont(13, false));
			add(descField, "1,5,3,5");

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
			friendsCB.setSelected(p.getAnnounce());
			friendsCB.addActionListener(al);
			add(friendsCB);
			
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
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			
			saveBtn = new JButton("SAVE");
			saveBtn.setFont(RoboFont.getFont(12, true));
			saveBtn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					p.setAnnounce(friendsCB.isSelected());
					pc.getItems().clear();
					for (String opt : options.keySet()) {
						JCheckBox cb = options.get(opt);
						if(cb.isSelected())
							pc.setItem(opt, "true");
					}
					saveBtn.setEnabled(false);
					savePlaylist();
				}
			});
			saveBtn.setEnabled(false);
			add(saveBtn);
			
			if (allowShare()) {
				shareBtn = new JButton("SHARE");
				shareBtn.setFont(RoboFont.getFont(12, true));
				shareBtn.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						SharePlaylistDialog dialog = new SharePlaylistDialog(frame, p);
						dialog.setLocationRelativeTo(frame);
						dialog.setVisible(true);
					}
				});
				add(shareBtn);
			}

			if (allowDel()) {
				delBtn = new JButton("DELETE");
				delBtn.setFont(RoboFont.getFont(12, true));
				delBtn.setName("robonobo.red.button");
				delBtn.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						int result = JOptionPane.showConfirmDialog(ButtonsPanel.this, "Are you sure you want to delete this playlist?", "Delete this playlist?", JOptionPane.YES_NO_OPTION);
						if(result == JOptionPane.YES_OPTION)
							deletePlaylist();
					}
				});
				add(delBtn);
			}
		}
	}
}
