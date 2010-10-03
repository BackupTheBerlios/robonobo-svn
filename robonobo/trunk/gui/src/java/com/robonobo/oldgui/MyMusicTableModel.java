package com.robonobo.oldgui;

import javax.swing.SwingUtilities;

import com.robonobo.common.concurrent.CatchingRunnable;
import com.robonobo.core.RobonoboController;
import com.robonobo.core.api.model.CloudTrack;
import com.robonobo.core.api.model.Track;
import com.robonobo.gui.model.FreeformTrackListTableModel;

@SuppressWarnings("serial")
public class MyMusicTableModel extends FreeformTrackListTableModel  {
	public MyMusicTableModel(RobonoboController controller) {
		super(controller);
		// If everything's started already before we get here, load it now
		if (controller.haveAllTransfersStarted())
			allTracksLoaded();
	}

	public void allTracksLoaded() {
		synchronized (this) {
			streams.clear();
			streamIndices.clear();
			for(String streamId : controller.getShares()) {
				Track t = controller.getTrack(streamId);
				add(t, false);
			}
			for(String streamId : controller.getDownloads()) {
				Track t = controller.getTrack(streamId);
				add(t, false);
			}
		}
		
		if(SwingUtilities.isEventDispatchThread())
			fireTableDataChanged();
		else {
			SwingUtilities.invokeLater(new CatchingRunnable() {
				public void doRun() throws Exception { 
					fireTableDataChanged();	
				}
			});
		}
	}

	public void trackUpdated(String streamId) {
		Track t = controller.getTrack(streamId);
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
			if(SwingUtilities.isEventDispatchThread())
				fireTableRowsUpdated(index, index);
			else {
				final int findex = index;
				SwingUtilities.invokeLater(new CatchingRunnable() {
					public void doRun() throws Exception {
						fireTableRowsUpdated(findex, findex);
					}
				});
			}
		}
	}
}
