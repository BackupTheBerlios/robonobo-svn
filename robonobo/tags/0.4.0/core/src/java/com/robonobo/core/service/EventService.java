package com.robonobo.core.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.robonobo.core.RobonoboInstance;
import com.robonobo.core.api.NextPrevListener;
import com.robonobo.core.api.PlaybackListener;
import com.robonobo.core.api.RobonoboStatus;
import com.robonobo.core.api.RobonoboStatusListener;
import com.robonobo.core.api.TrackListener;
import com.robonobo.core.api.TransferSpeed;
import com.robonobo.core.api.UserPlaylistListener;
import com.robonobo.core.api.model.DownloadingTrack;
import com.robonobo.core.api.model.Playlist;
import com.robonobo.core.api.model.SharedTrack;
import com.robonobo.core.api.model.Stream;
import com.robonobo.core.api.model.Track;
import com.robonobo.core.api.model.User;
import com.robonobo.core.wang.WangListener;
import com.robonobo.mina.external.ConnectedNode;
import com.robonobo.mina.external.MinaControl;
import com.robonobo.mina.external.MinaListener;

public class EventService extends AbstractRuntimeServiceProvider implements MinaListener {
	private List<TrackListener> trList = new ArrayList<TrackListener>();
	private List<PlaybackListener> plList = new ArrayList<PlaybackListener>();
	private List<UserPlaylistListener> upList = new ArrayList<UserPlaylistListener>();
	private List<RobonoboStatusListener> stList = new ArrayList<RobonoboStatusListener>();
	private List<NextPrevListener> npList = new ArrayList<NextPrevListener>();
	private List<WangListener> wList = new ArrayList<WangListener>();
	private int minaSupernodes = 0;
	private Log log = LogFactory.getLog(getClass());

	public EventService() {
	}

	public synchronized void addTrackListener(TrackListener listener) {
		trList.add(listener);
	}

	public synchronized void removeTrackListener(TrackListener listener) {
		trList.remove(listener);
	}

	public synchronized void addPlaybackListener(PlaybackListener l) {
		plList.add(l);
	}

	public synchronized void removePlaybackListener(PlaybackListener l) {
		plList.remove(l);
	}

	public synchronized void addStatusListener(RobonoboStatusListener l) {
		stList.add(l);
	}

	public synchronized void removeStatusListener(RobonoboStatusListener l) {
		stList.remove(l);
	}

	public synchronized void addNextPrevListener(NextPrevListener l) {
		npList.add(l);
	}

	public synchronized void removeNextPrevListener(NextPrevListener l) {
		npList.remove(l);
	}

	public synchronized void addUserPlaylistListener(UserPlaylistListener l) {
		upList.add(l);
	}
	
	public synchronized void removeUserPlaylistListener(UserPlaylistListener l) {
		upList.remove(l);
	}
	
	public synchronized void addWangListener(WangListener l) {
		wList.add(l);
	}
	
	public synchronized void removeWangListener(WangListener l) {
		wList.remove(l);
	}
	
	public void fireTrackUpdated(String streamId) {
		TrackListener[] arr;
		synchronized (this) {
			arr = getTrArr();
		}
		for (TrackListener listener : arr) {
			listener.trackUpdated(streamId);
		}
	}

	public void fireTracksUpdated(Collection<String> streamIds) {
		TrackListener[] arr;
		synchronized (this) {
			arr = getTrArr();
		}
		for (TrackListener listener : arr) {
			listener.tracksUpdated(streamIds);
		}
	}

	public void fireAllTracksLoaded() {
		TrackListener[] arr;
		synchronized (this) {
			arr = getTrArr();
		}
		for (TrackListener listener : arr) {
			listener.allTracksLoaded();
		}
	}
	
	public void firePlaybackStarted() {
		PlaybackListener[] arr;
		synchronized (this) {
			arr = getPlArr();
		}
		for (PlaybackListener listener : arr) {
			listener.playbackStarted();
		}
	}

	public void firePlaybackStarting() {
		PlaybackListener[] arr;
		synchronized (this) {
			arr = getPlArr();
		}
		for (PlaybackListener listener : arr) {
			listener.playbackStarting();
		}
	}

	public void firePlaybackPaused() {
		PlaybackListener[] arr;
		synchronized (this) {
			arr = getPlArr();
		}
		for (PlaybackListener listener : arr) {
			listener.playbackPaused();
		}
	}

	public void firePlaybackStopped() {
		PlaybackListener[] arr;
		synchronized (this) {
			arr = getPlArr();
		}
		for (PlaybackListener listener : arr) {
			listener.playbackStopped();
		}
	}

	public void firePlaybackCompleted() {
		PlaybackListener[] arr;
		synchronized (this) {
			arr = getPlArr();
		}
		for (PlaybackListener listener : arr) {
			listener.playbackCompleted();
		}
	}

	public void firePlaybackProgress(long microsecs) {
		PlaybackListener[] arr;
		synchronized (this) {
			arr = getPlArr();
		}
		for (PlaybackListener listener : arr) {
			listener.playbackProgress(microsecs);
		}
	}

	public void firePlaybackError(String error) {
		PlaybackListener[] arr;
		synchronized (this) {
			arr = getPlArr();
		}
		for (PlaybackListener listener : arr) {
			listener.playbackError(error);
		}
	}

	public void fireUserChanged(User u) {
		UserPlaylistListener[] arr;
		synchronized (this) {
			arr = getUpArr();
		}
		for (UserPlaylistListener listener : arr) {
			listener.userChanged(u);
		}
	}

	public void firePlaylistChanged(Playlist p) {
		UserPlaylistListener[] arr;
		synchronized (this) {
			arr = getUpArr();
		}
		for (UserPlaylistListener listener : arr) {
			listener.playlistChanged(p);
		}
	}

	public void fireLoggedIn() {
		UserPlaylistListener[] arr;
		synchronized (this) {
			arr = getUpArr();
		}
		for (UserPlaylistListener listener : arr) {
			listener.loggedIn();
		}
	}

	public void fireStatusChanged() {
		RobonoboStatusListener[] arr;
		synchronized (this) {
			arr = getStArr();
		}
		for (RobonoboStatusListener listener : arr) {
			listener.statusChanged();
		}
	}

	public void fireNextPrevChanged(boolean canNext, boolean canPrev) {
		NextPrevListener[] arr;
		synchronized (this) {
			arr = getNpArr();
		}
		for (NextPrevListener listener : arr) {
			listener.canPlayNext(canNext);
			listener.canPlayPrevious(canPrev);
		}
	}

	public void removeAllListeners() {
		trList.clear();
		plList.clear();
		upList.clear();
		stList.clear();
		npList.clear();
	}

	public void nodeConnected(ConnectedNode node) {
		if(node.isSupernode()) {
			minaSupernodes++;
			if(minaSupernodes > 0 && getRobonobo().getUsersService().isLoggedIn()) {
				getRobonobo().setStatus(RobonoboStatus.Connected);
				fireStatusChanged();
			}
		}
	}

	public void nodeDisconnected(ConnectedNode node) {
		if (node.isSupernode()) {
			if (minaSupernodes == 1) {
				robonobo.setStatus(RobonoboStatus.NotConnected);
				fireStatusChanged();
			}
			if(minaSupernodes > 0)
				minaSupernodes--;
		}
	}

	public void fireWangBalanceChanged(double newBalance) {
		WangListener[] arr;
		synchronized (this) {
			arr = getWArr();
		}
		for (WangListener listener : arr) {
			listener.balanceChanged(newBalance);
		}
	}
	
	/** Copy the list of listeners, to remove deadlock possibilities */
	private TrackListener[] getTrArr() {
		TrackListener[] result = new TrackListener[trList.size()];
		trList.toArray(result);
		return result;
	}

	/** Copy the list of listeners, to remove deadlock possibilities */
	private PlaybackListener[] getPlArr() {
		PlaybackListener[] result = new PlaybackListener[plList.size()];
		plList.toArray(result);
		return result;
	}

	/** Copy the list of listeners, to remove deadlock possibilities */
	private UserPlaylistListener[] getUpArr() {
		UserPlaylistListener[] result = new UserPlaylistListener[upList.size()];
		upList.toArray(result);
		return result;
	}

	private RobonoboStatusListener[] getStArr() {
		RobonoboStatusListener[] result = new RobonoboStatusListener[stList.size()];
		stList.toArray(result);
		return result;
	}

	private NextPrevListener[] getNpArr() {
		NextPrevListener[] result = new NextPrevListener[npList.size()];
		npList.toArray(result);
		return result;
	}

	private WangListener[] getWArr() {
		WangListener[] result = new WangListener[wList.size()];
		wList.toArray(result);
		return result;
	}

	public void broadcastStarted(String streamId) {
	}

	public void broadcastStopped(String streamId) {
	}

	public void minaStarted(MinaControl mina) {
	}

	public void minaStopped(MinaControl mina) {
	}

	public void receptionCompleted(String streamId) {
	}

	public void receptionStarted(String streamId) {
	}

	public void receptionStopped(String streamId) {
	}

	public void receptionConnsChanged(String streamId) {
		DownloadingTrack d = robonobo.getDownloadService().getDownload(streamId);
		if(d != null)
			fireTrackUpdated(streamId);
	}
	
	@Override
	public void shutdown() throws Exception {
	}

	@Override
	public void startup() throws Exception {
		getRobonobo().setStatus(RobonoboStatus.Starting);
		fireStatusChanged();
	}

	public String getName() {
		return "Event Service";
	}

	public String getProvides() {
		return "core.event";
	}
}
