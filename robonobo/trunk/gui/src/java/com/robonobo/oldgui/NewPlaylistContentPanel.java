package com.robonobo.oldgui;

import java.security.SecureRandom;

import javax.swing.SwingUtilities;

import org.doomdark.uuid.UUIDGenerator;

import com.robonobo.common.concurrent.CatchingRunnable;
import com.robonobo.core.api.RobonoboException;
import com.robonobo.core.api.model.Playlist;
import com.robonobo.core.api.model.PlaylistConfig;

public class NewPlaylistContentPanel extends MyPlaylistContentPanel {

	NewPlaylistContentPanel(RobonoboFrame frame) {
		super(frame, new Playlist(), new PlaylistConfig());
//		super(frame, new Playlist(), new PlaylistConfig(), false);
	}

	void savePlaylist() {
		p.setTitle(detailsPanel.getPlaylistTitle());
		p.setDescription(detailsPanel.getPlaylistDesc());
		controller.getExecutor().execute(new CatchingRunnable() {
			public void doRun() throws Exception {
				// Create the new playlist in midas
				try {
//					p.setPlaylistId(UUIDGenerator.getInstance().generateRandomBasedUUID(new SecureRandom()).toString());
					p.getOwnerIds().add(controller.getMyUser().getUserId());
					pc.setPlaylistId(p.getPlaylistId());
					controller.putPlaylistConfig(pc);
					controller.addOrUpdatePlaylist(p);
				} catch (RobonoboException e) {
					frame.updateStatus("Error creating playlist: " + e.getMessage(), 10, 30);
					log.error("Error creating playlist", e);
					return;
				}
				SwingUtilities.invokeLater(new CatchingRunnable() {
					public void doRun() throws Exception {
						// A content panel should have been created for the new
						// playlist - switch to it now
						frame.getLeftSidebar().selectMyPlaylist(p);
						// Now that they're not looking, re-init everything with
						// a new empty playlist
						removeAll();
						p = new Playlist();
						initComponents();
						validate();
					}
				});
			}
		});
	}

	@Override
	protected boolean dummyTracklist() {
		return true;
	}
	
	@Override
	protected MyPlaylistDetailsPanel createDetailsPanel() {
		return new NewPlaylistDetailsPanel(frame, this);
	}
	
	@Override
	void reselectTreeNode() {
		SwingUtilities.invokeLater(new CatchingRunnable() {
			public void doRun() throws Exception {
				frame.getLeftSidebar().selectMyMusic();
				frame.getLeftSidebar().selectNewPlaylist();
			}
		});
	}

	class NewPlaylistDetailsPanel extends MyPlaylistDetailsPanel {
		NewPlaylistDetailsPanel(RobonoboFrame frame, MyPlaylistContentPanel myContentPanel) {
			super(frame, myContentPanel);
		}
		@Override
		protected boolean shareBtnsEnabled() {
			return false;
		}
		@Override
		protected boolean delBtnEnabled() {
			return false;
		}
		@Override
		void focus() {
			titleField.requestFocusInWindow();
		}
	}
}
