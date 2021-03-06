package com.robonobo.gui;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.SwingUtilities;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.robonobo.common.concurrent.CatchingRunnable;
import com.robonobo.common.exceptions.SeekInnerCalmException;
import com.robonobo.core.RobonoboController;
import com.robonobo.core.api.TrackListener;
import com.robonobo.core.api.model.CloudTrack;
import com.robonobo.core.api.model.Playlist;
import com.robonobo.core.api.model.Track;
import com.robonobo.mina.external.FoundSourceListener;

@SuppressWarnings("serial")
public class PlaylistTableModel extends TrackListTableModel implements TrackListener, FoundSourceListener {
	private RobonoboController controller;
	private Playlist p;
	private boolean myPlaylist;
	private boolean isDummy;
	private Map<String, Integer> streamIndices = new HashMap<String, Integer>();
	Log log = LogFactory.getLog(getClass());

	public PlaylistTableModel(RobonoboController controller, Playlist p, boolean myPlaylist, boolean isDummy) {
		this.controller = controller;
		this.myPlaylist = myPlaylist;
		this.isDummy = isDummy;
		controller.addTrackListener(this);
		update(p, false);
	}

	public void update(Playlist p, boolean fireChangedEvent) {
		Playlist oldP = this.p;
		this.p = p;
		// Any items on the old list that aren't on the new, stop finding
		// sources for them
		if (oldP != null) {
			for (String streamId : oldP.getStreamIds()) {
				if (!p.getStreamIds().contains(streamId))
					controller.stopFindingSources(streamId, this);
			}
		}
		synchronized (this) {
			updateStreamIndices();
			for (String streamId : p.getStreamIds()) {
				Track t = controller.getTrack(streamId);
				if (t instanceof CloudTrack)
					controller.findSources(streamId, this);
			}
		}
		if (fireChangedEvent)
			fireTableDataChanged();
	}

	public synchronized void nuke() {
		controller.removeTrackListener(this);
		for (String streamId : p.getStreamIds()) {
			controller.stopFindingSources(streamId, this);
		}
	}

	@Override
	public synchronized int getTrackIndex(String streamId) {
		if (streamIndices.containsKey(streamId))
			return streamIndices.get(streamId);
		return -1;
	}

	@Override
	public synchronized String getStreamId(int index) {
		return p.getStreamIds().get(index);
	}

	@Override
	public synchronized Track getTrack(int index) {
		return controller.getTrack(p.getStreamIds().get(index));
	}

	@Override
	public synchronized int numTracks() {
		return p.getStreamIds().size();
	}

	public synchronized void foundBroadcaster(String streamId, String nodeId) {
		if (!streamIndices.containsKey(streamId))
			return;
		trackUpdated(streamId);
	}

	/**
	 * If any of these streams are already in this playlist, they will be removed before being added in their new position
	 */
	public void addStreams(List<String> streamIds, int position) {
		if (!myPlaylist)
			throw new SeekInnerCalmException();
		synchronized (this) {
			// First, scan through our playlist and remove any that are in this
			// list (they're being moved)
			for (Iterator<String> iter = p.getStreamIds().iterator(); iter.hasNext();) {
				String pStreamId = iter.next();
				if (streamIds.contains(pStreamId))
					iter.remove();
			}
			if (position > p.getStreamIds().size())
				position = p.getStreamIds().size();
			p.getStreamIds().addAll(position, streamIds);
			updateStreamIndices();
		}
		SwingUtilities.invokeLater(new CatchingRunnable() {
			public void doRun() throws Exception {
				fireTableDataChanged();
			}
		});
		if (!isDummy) {
			controller.getExecutor().execute(new CatchingRunnable() {
				public void doRun() throws Exception {
					controller.addOrUpdatePlaylist(p);
				}
			});
		}
	}

	public void removeStreamIds(List<String> streamIds) {
		synchronized (this) {
			for (String sid : streamIds) {
				p.getStreamIds().remove(sid);
				controller.stopFindingSources(sid, this);
			}
			updateStreamIndices();
		}
		SwingUtilities.invokeLater(new CatchingRunnable() {
			public void doRun() throws Exception {
				fireTableDataChanged();
			}
		});
		if (!isDummy) {
			controller.getExecutor().execute(new CatchingRunnable() {
				public void doRun() throws Exception {
					controller.addOrUpdatePlaylist(p);
				}
			});
		}
	}

	public void tracksUpdated(Collection<String> streamIds) {
		// Could do something smarter here, but probably don't need to
		for (String streamId : streamIds) {
			trackUpdated(streamId);
		}
	}

	public void trackUpdated(final String streamId) {
		synchronized (this) {
			if(!streamIndices.containsKey(streamId))
				return;
		}
		SwingUtilities.invokeLater(new CatchingRunnable() {
			@Override
			public void doRun() throws Exception {
				synchronized (PlaylistTableModel.this) {
					final int rowIndex = (streamIndices.containsKey(streamId)) ? streamIndices.get(streamId) : -1;
					if(rowIndex >= 0)
						fireTableRowsUpdated(rowIndex, rowIndex);
				}
			}
		});
		Track t = controller.getTrack(streamId);
		if(t instanceof CloudTrack)
			controller.findSources(streamId, this);
	}

	public void allTracksLoaded() {
		// Do nothing
	}

	public Playlist getP() {
		return p;
	}

	/** Must only be called from inside sync block */
	private void updateStreamIndices() {
		streamIndices.clear();
		for (int i = 0; i < numTracks(); i++) {
			String streamId = p.getStreamIds().get(i);
			streamIndices.put(streamId, i);
		}
	}
}
