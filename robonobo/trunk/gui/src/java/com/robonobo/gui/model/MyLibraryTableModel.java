package com.robonobo.gui.model;

import java.util.List;

import javax.swing.SwingUtilities;

import com.robonobo.common.concurrent.CatchingRunnable;
import com.robonobo.core.RobonoboController;
import com.robonobo.core.api.RobonoboException;
import com.robonobo.core.api.model.*;

@SuppressWarnings("serial")
public class MyLibraryTableModel extends FreeformTrackListTableModel  {
	public MyLibraryTableModel(RobonoboController controller) {
		super(controller);
		// If everything's started already before we get here, load it now
		if (controller.haveAllTransfersStarted())
			allTracksLoaded();
	}

	public void allTracksLoaded() {
		synchronized (this) {
			streams.clear();
			streamIndices.clear();
			for(String streamId : control.getShares()) {
				Track t = control.getTrack(streamId);
				add(t, false);
			}
			for(String streamId : control.getDownloads()) {
				Track t = control.getTrack(streamId);
				add(t, false);
			}
		}
		
		SwingUtilities.invokeLater(new CatchingRunnable() {
			public void doRun() throws Exception { 
				fireTableDataChanged();	
			}
		});
	}

	@Override
	public boolean allowDelete() {
		return true;
	}
	
	@Override
	public void deleteTracks(List<String> streamIds) {
		for (String sid : streamIds) {
			Track t = control.getTrack(sid);
			try {
				if (t instanceof DownloadingTrack)
					control.deleteDownload(sid);
				else if (t instanceof SharedTrack)
					control.deleteShare(sid);
			} catch (RobonoboException ex) {
				log.error("Error deleting share/download", ex);
			}
		}
	
	}
	
	public void trackUpdated(String streamId) {
		Track t = control.getTrack(streamId);
		boolean shouldAdd = false;
		boolean shouldRm = false;
		int index = -1;
		if(t instanceof CloudTrack) {
			// We're not interested - if we currently have it, remove it
			synchronized (this) {
				if(streamIndices.containsKey(streamId))
					shouldRm = true;
			}
		} else {
			// We are interested - if we don't have it, add it
			synchronized (this) {
				if(streamIndices.containsKey(streamId))
					index = streamIndices.get(streamId);
				else
					shouldAdd = true;
			}
		}
		if(shouldAdd)
			add(t);
		else if(shouldRm)
			remove(t);
		else if(index >= 0 ){
			// Updated
			final int findex = index;
			SwingUtilities.invokeLater(new CatchingRunnable() {
				public void doRun() throws Exception {
					fireTableRowsUpdated(findex, findex);
				}
			});
		}
	}
}
