package com.robonobo.oldgui;

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
import com.robonobo.core.api.UserPlaylistListener;
import com.robonobo.core.api.model.CloudTrack;
import com.robonobo.core.api.model.Playlist;
import com.robonobo.core.api.model.PlaylistConfig;
import com.robonobo.core.api.model.Track;
import com.robonobo.core.api.model.User;
import com.robonobo.gui.model.PlaylistTableModel;

@SuppressWarnings("serial")
public class FriendPlaylistContentPanel extends ContentPanel implements UserPlaylistListener {
	RobonoboController controller;
	Playlist p;
	PlaylistConfig pc;
	FriendPlaylistDetailsPanel detailsPanel;
	PlaylistTableModel tableModel;
	FriendPlaylistTablePanel tablePanel;
	MainButtonBar mainBtnBar;
	PrevButton prevBtn;
	NextButton nextBtn;
	PlayPauseButton playBtn;
	MainButton dloadBtn;

	FriendPlaylistContentPanel(RobonoboFrame frame, Playlist p, PlaylistConfig pc) {
		super(frame);
		controller = frame.controller;
		this.p = p;
		this.pc = pc;
		double[][] cellSizen = { { 10, TableLayout.FILL, 10 }, { 10, 70, 10, 100, 10, TableLayout.FILL } };
		setLayout(new TableLayout(cellSizen));
		initComponents();
		controller.addUserPlaylistListener(this);
	}

	@Override
	protected void finalize() throws Throwable {
		controller.removeUserPlaylistListener(this);
	}
	
	void initComponents() {
		KeyAdapter keyListener = new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				// Spacebar == play/pause
				if (e.getKeyChar() == KeyEvent.VK_SPACE) {
					playBtn.doClick();
					e.consume();
				}
			}
		};
		tableModel = new PlaylistTableModel(controller, p, false);
//		tableModel = new PlaylistTableModel(controller, p, true, true);
		tablePanel = new FriendPlaylistTablePanel(frame, tableModel, new SelectionListener(), keyListener);
		List<MainButton> btnList = new ArrayList<MainButton>();
		prevBtn = new PrevButton(frame.getController());
		btnList.add(prevBtn);
//		playBtn = new PlayPauseButton(controller, tablePanel, tableModel, tablePanel);
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
	
	public void loggedIn() {
		// Do nothing
	}
	
	public void userChanged(User u) {
		// Do nothing
	}
	
	public void playlistChanged(Playlist p) {
		if(p.equals(this.p)) {
			if(p.getOwnerIds().contains(controller.getMyUser().getUserId()))
				return;
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
	
	protected FriendPlaylistDetailsPanel createDetailsPanel() {
		return new FriendPlaylistDetailsPanel(this);
	}

	void reselectTreeNode() {
		tablePanel.reselectTreeNode();
	}
	
	@Override
	public PlayPauseButton getPlayPauseButton() {
		return playBtn;
	}

	@Override
	public void focus() {
	}
	
	void savePlaylistConfig() {
		pc.setPlaylistId(p.getPlaylistId());
		controller.putPlaylistConfig(pc);
		SwingUtilities.invokeLater(new CatchingRunnable() {
			public void doRun() throws Exception {
				reselectTreeNode();
			}
		});
	}

	
	class SelectionListener implements ListSelectionListener {
		public void valueChanged(ListSelectionEvent e) {
			// Only do our thang when mouse button is released, save a few
			// cycles
			if (e.getValueIsAdjusting())
				return;
			List<Track> trax = tablePanel.getSelectedTracks();
			if (trax.size() == 0) {
				playBtn.setEnabledIfStopped(false);
				dloadBtn.setEnabled(false);
			} else {
				playBtn.setEnabledIfStopped(true);
				// Only activate the Download button if we're not already
				// Download/sharing at least one of these tracks
				boolean activeDl = false;
				for (Track t : trax) {
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
