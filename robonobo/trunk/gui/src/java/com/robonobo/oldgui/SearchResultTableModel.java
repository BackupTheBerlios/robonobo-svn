package com.robonobo.oldgui;

import javax.swing.SwingUtilities;

import com.robonobo.common.concurrent.CatchingRunnable;
import com.robonobo.core.RobonoboController;
import com.robonobo.core.api.SearchExecutor;
import com.robonobo.core.api.SearchListener;
import com.robonobo.core.api.model.CloudTrack;
import com.robonobo.core.api.model.Stream;
import com.robonobo.core.api.model.Track;
import com.robonobo.mina.external.FoundSourceListener;

@SuppressWarnings("serial")
public class SearchResultTableModel extends FreeformTrackListTableModel implements SearchExecutor, SearchListener,
		FoundSourceListener {

	public SearchResultTableModel(RobonoboController controller) {
		super(controller);
	}

	public void trackUpdated(String streamId) {
		int updateIndex = -1;
		synchronized (this) {
			if(streamIndices.containsKey(streamId))
				updateIndex = streamIndices.get(streamId);
		}
		if(updateIndex >= 0) {
			final int i = updateIndex;
			if(SwingUtilities.isEventDispatchThread())
				fireTableRowsUpdated(updateIndex, updateIndex);
			else {
				SwingUtilities.invokeLater(new CatchingRunnable() {
					public void doRun() throws Exception {
						fireTableRowsUpdated(i, i);
					}
				});
			}
		}
	}
	
	public void allTracksLoaded() {
		// Do nothing
	}
	
	public synchronized void search(String query) {
		for (Stream s : streams) {
			controller.stopFindingSources(s.getStreamId(), this);
		}
		streams.clear();
		streamIndices.clear();
		controller.search(query, 0, this);
		SwingUtilities.invokeLater(new CatchingRunnable() {
			public void doRun() throws Exception {
				fireTableDataChanged();
			}
		});
	}

	public void gotNumberOfResults(int numResults) {
		// Do nothing
	}
	
	public void foundResult(final Stream s) {
		Track t = controller.getTrack(s.getStreamId());
		add(t);
		if(t instanceof CloudTrack)
			controller.findSources(s.getStreamId(), this);
	}

	public void foundBroadcaster(String streamId, String nodeId) {
		trackUpdated(streamId);
	}
}
