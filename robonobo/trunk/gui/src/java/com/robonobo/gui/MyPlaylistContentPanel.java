package com.robonobo.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.debian.tablelayout.TableLayout;

import com.robonobo.common.concurrent.CatchingRunnable;
import com.robonobo.core.RobonoboController;
import com.robonobo.core.api.RobonoboException;
import com.robonobo.core.api.UserPlaylistListener;
import com.robonobo.core.api.model.CloudTrack;
import com.robonobo.core.api.model.Playlist;
import com.robonobo.core.api.model.PlaylistConfig;
import com.robonobo.core.api.model.Track;
import com.robonobo.core.api.model.User;

@SuppressWarnings("serial")
public class MyPlaylistContentPanel extends ContentPanel implements UserPlaylistListener {
	RobonoboController controller;
	Playlist p;
	PlaylistConfig pc;
	MyPlaylistDetailsPanel detailsPanel;
	PlaylistTableModel tableModel;
	MyPlaylistTablePanel tablePanel;
	MainButtonBar mainBtnBar;
	PrevButton prevBtn;
	NextButton nextBtn;
	PlayPauseButton playBtn;
	MainButton dloadBtn;
	MainButton delBtn;

	MyPlaylistContentPanel(RobonoboFrame frame, Playlist p, PlaylistConfig pc) {
		super(frame);
		controller = frame.controller;
		this.p = p;
		this.pc = pc;
		double[][] cellSizen = { { 10, TableLayout.FILL, 10 }, { 10, 70, 10, 140, 10, TableLayout.FILL } };
		setLayout(new TableLayout(cellSizen));
		initComponents();
		controller.addUserPlaylistListener(this);
	}

	@Override
	protected void finalize() throws Throwable {
		controller.removeUserPlaylistListener(this);
	}

	void initComponents() {
		tableModel = new PlaylistTableModel(controller, p, true, dummyTracklist());
		KeyAdapter keyListener = new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				// Spacebar == play/pause
				if (e.getKeyChar() == KeyEvent.VK_SPACE) {
					playBtn.doClick();
					e.consume();
				}
			}
		};
		tablePanel = new MyPlaylistTablePanel(frame, tableModel, new SelectionListener(), keyListener);
		List<MainButton> btnList = new ArrayList<MainButton>();
		prevBtn = new PrevButton(frame.getController());
		btnList.add(prevBtn);
		playBtn = new PlayPauseButton(controller, tablePanel, tableModel, tablePanel);
		playBtn.setEnabled(false);
		btnList.add(playBtn);
		nextBtn = new NextButton(frame.getController());
		btnList.add(nextBtn);
		dloadBtn = new MainButton("/img/Download.png", "Download selected tracks");
		dloadBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				controller.spawnNecessaryDownloads(tablePanel.getSelectedStreamIds());
				tablePanel.clearTableSelection();
				reselectTreeNode();
			}
		});
		dloadBtn.setEnabled(false);
		btnList.add(dloadBtn);
		delBtn = new MainButton("/img/Trash.png", "Delete selected tracks from playlist");
		delBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				tableModel.removeStreamIds(tablePanel.getSelectedStreamIds());
				reselectTreeNode();
			}
		});
		delBtn.setEnabled(false);
		btnList.add(delBtn);
		mainBtnBar = new MainButtonBar(btnList, frame, tablePanel);
		add(mainBtnBar, "1,1");
		detailsPanel = createDetailsPanel();
		add(detailsPanel, "1,3");
		add(tablePanel, "1,5");
		tablePanel.getJTable().addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					playBtn.play(tablePanel.getSelectedStreamIds());
					reselectTreeNode();
				}
			}
		});
	}

	protected MyPlaylistDetailsPanel createDetailsPanel() {
		return new MyPlaylistDetailsPanel(frame, this);
	}

	void reselectTreeNode() {
		tablePanel.reselectTreeNode();
	}

	/**
	 * This method makes http calls to the metadata server, so don't call it
	 * from the gui thread
	 */
	void savePlaylist() {
		p.setTitle(detailsPanel.getPlaylistTitle());
		p.setDescription(detailsPanel.getPlaylistDesc());
		try {
			// This creates the playlist's id if it doesn't already
			// exist, so update the playlist config
			controller.putPlaylistConfig(pc);
			controller.addOrUpdatePlaylist(p);
			pc.setPlaylistId(p.getPlaylistId());
		} catch (RobonoboException e) {
			frame.updateStatus("Error creating playlist: " + e.getMessage(), 10, 30);
			log.error("Error creating playlist", e);
		}
		SwingUtilities.invokeLater(new CatchingRunnable() {
			@Override
			public void doRun() throws Exception {
				reselectTreeNode();
			}
		});
	}

	void deletePlaylist() {
		frame.getLeftSidebar().selectMyMusic();
		controller.getExecutor().execute(new CatchingRunnable() {
			public void doRun() throws Exception {
				try {
					controller.nukePlaylist(p);
				} catch (RobonoboException e) {
					frame.updateStatus("Error deleting playlist: " + e.getMessage(), 10, 30);
					log.error("Error deleting playlist", e);
				}
			}
		});
	}

	@Override
	public void focus() {
		detailsPanel.focus();
	}

	protected boolean dummyTracklist() {
		return false;
	}

	@Override
	public PlayPauseButton getPlayPauseButton() {
		return playBtn;
	}

	public void loggedIn() {
		// Do nothing
	}

	public void userChanged(User u) {
		// Do nothing
	}

	public void playlistChanged(Playlist p) {
		if (p.equals(this.p)) {
			this.p = p;
			SwingUtilities.invokeLater(new CatchingRunnable() {
				public void doRun() throws Exception {
					update();
				}
			});
		}
	}

	void update() {
		tableModel.update(p, true);
		detailsPanel.update();
	}

	class SelectionListener implements ListSelectionListener {
		public void valueChanged(ListSelectionEvent e) {
			// Only do our thang when mouse button is released, save a few
			// cycles
			if (e.getValueIsAdjusting())
				return;
			List<Track> selTrax = tablePanel.getSelectedTracks();
			if (selTrax.size() == 0) {
				playBtn.setEnabledIfStopped(false);
				dloadBtn.setEnabled(false);
				delBtn.setEnabled(false);
			} else {
				playBtn.setEnabledIfStopped(true);
				delBtn.setEnabled(true);
				// Only activate the Download button if we're not already
				// Download/sharing at least one of these tracks
				boolean activeDl = false;
				for (Track t : selTrax) {
					if(t instanceof CloudTrack) {
						activeDl = true;
						break;
					}
				}
				dloadBtn.setEnabled(activeDl);
			}
		}
	}

}
