package com.robonobo.gui.panels;

import java.security.SecureRandom;

import javax.swing.SwingUtilities;

import org.doomdark.uuid.UUIDGenerator;

import com.robonobo.common.concurrent.CatchingRunnable;
import com.robonobo.core.RobonoboController;
import com.robonobo.core.api.RobonoboException;
import com.robonobo.core.api.model.Playlist;
import com.robonobo.core.api.model.PlaylistConfig;
import com.robonobo.gui.frames.RobonoboFrame;
import com.robonobo.gui.model.NewPlaylistTableModel;

@SuppressWarnings("serial")
public class NewPlaylistContentPanel extends MyPlaylistContentPanel {

	public NewPlaylistContentPanel(RobonoboFrame frame) {
		super(frame, new Playlist(), new PlaylistConfig(), new NewPlaylistTableModel(frame.getController()));
	}

	@Override
	protected void savePlaylist() {
		final Playlist p = getModel().getPlaylist();
		p.setTitle(titleField.getText());
		p.setDescription(descField.getText());
		final RobonoboController control = frame.getController();
		control.getExecutor().execute(new CatchingRunnable() {
			public void doRun() throws Exception {
				// Create the new playlist in midas
				try {
					p.getOwnerIds().add(control.getMyUser().getUserId());
					pc.setPlaylistId(p.getPlaylistId());
					control.putPlaylistConfig(pc);
					control.addOrUpdatePlaylist(p);
				} catch (RobonoboException e) {
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
						Playlist newP = new Playlist();
						titleField.setText("");
						descField.setText("");
//						friendsCB.setSelected(p.getAnnounce());
						if(iTunesCB != null)
							iTunesCB.setSelected(false);
						getModel().setPlaylist(newP);
					}
				});
			}
		});
	}
	
	@Override
	protected boolean allowDel() {
		return false;
	}
	
	@Override
	protected boolean allowShare() {
		return false;
	}
	
	@Override
	protected boolean addAsListener() {
		return false;		
	}
}
