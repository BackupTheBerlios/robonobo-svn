package com.robonobo.core;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.robonobo.common.concurrent.CatchingRunnable;
import com.robonobo.core.api.CurrencyException;
import com.robonobo.core.api.NextPrevListener;
import com.robonobo.core.api.NextTrackListener;
import com.robonobo.core.api.PlaybackListener;
import com.robonobo.core.api.RobonoboException;
import com.robonobo.core.api.RobonoboStatus;
import com.robonobo.core.api.RobonoboStatusListener;
import com.robonobo.core.api.SearchListener;
import com.robonobo.core.api.TrackListener;
import com.robonobo.core.api.UserPlaylistListener;
import com.robonobo.core.api.config.RobonoboConfig;
import com.robonobo.core.api.model.CloudTrack;
import com.robonobo.core.api.model.DownloadingTrack;
import com.robonobo.core.api.model.Playlist;
import com.robonobo.core.api.model.PlaylistConfig;
import com.robonobo.core.api.model.SharedTrack;
import com.robonobo.core.api.model.Stream;
import com.robonobo.core.api.model.Track;
import com.robonobo.core.api.model.User;
import com.robonobo.core.wang.WangListener;
import com.robonobo.mina.external.FoundSourceListener;

/**
 * Main external-facing Robonobo class
 * 
 * @author macavity
 */
public class RobonoboController {
	private RobonoboInstance inst;
	private JobRunner jobRunner = new JobRunner();
	private Log log;

	public RobonoboController(String[] args) throws Exception {
		inst = new RobonoboInstance(args);
		log = LogFactory.getLog(getClass());
	}

	public String getVersion() {
		return inst.getVersion();
	}

	public void addTrackListener(TrackListener listener) {
		inst.getEventService().addTrackListener(listener);
	}

	public void removeTrackListener(TrackListener listener) {
		inst.getEventService().removeTrackListener(listener);
	}

	public void addPlaybackListener(PlaybackListener l) {
		inst.getEventService().addPlaybackListener(l);
	}

	public void removePlaybackListener(PlaybackListener l) {
		inst.getEventService().removePlaybackListener(l);
	}

	public void addRobonoboStatusListener(RobonoboStatusListener l) {
		inst.getEventService().addStatusListener(l);
	}

	public void removeRobonoboStatusListener(RobonoboStatusListener l) {
		inst.getEventService().removeStatusListener(l);
	}

	public void addNextPrevListener(NextPrevListener l) {
		inst.getEventService().addNextPrevListener(l);
	}

	public void removeNextPrevListener(NextPrevListener l) {
		inst.getEventService().removeNextPrevListener(l);
	}

	public void addUserPlaylistListener(UserPlaylistListener l) {
		inst.getEventService().addUserPlaylistListener(l);
	}

	public void removeUserPlaylistListener(UserPlaylistListener l) {
		inst.getEventService().removeUserPlaylistListener(l);
	}

	public void addWangListener(WangListener l) {
		inst.getEventService().addWangListener(l);
	}

	public void removeWangListener(WangListener l) {
		inst.getEventService().removeWangListener(l);
	}

	public RobonoboStatus getStatus() {
		return inst.getStatus();
	}

	public List<File> getITunesLibrary(FileFilter filter) throws RobonoboException {
		try {
			return inst.getITunesService().getAllITunesFiles(filter);
		} catch (IOException e) {
			throw new RobonoboException(e);
		}
	}

	public Map<String, List<File>> getITunesPlaylists(FileFilter filter) throws RobonoboException {
		try {
			return inst.getITunesService().getAllITunesPlaylists(filter);
		} catch (IOException e) {
			throw new RobonoboException(e);
		}
	}

	public void addDownload(String streamId, String pathToFile) throws RobonoboException {
		inst.getDownloadService().addDownload(streamId, new File(pathToFile));
	}

	public void addDownload(String streamId) throws RobonoboException {
		inst.getDownloadService().addDownload(streamId);
	}

	/**
	 * Spawns off a thread to download any tracks that are not already being downloaded or shared - returns immediately
	 */
	public void spawnNecessaryDownloads(final Collection<String> streamIds) {
		getExecutor().execute(new CatchingRunnable() {
			public void doRun() throws Exception {
				for (String sid : streamIds) {
					Track t = getTrack(sid);
					if(t instanceof CloudTrack)
						addDownload(sid);
				}
			}
		});
	}

	public Stream addShare(String pathToFile) throws RobonoboException {
		File f = new File(pathToFile);
		Stream s;
		try {
			s = inst.getFormatService().getStreamForFile(f);
		} catch (IOException e) {
			throw new RobonoboException(e);
		}
		inst.getMetadataService().putStream(s);
		inst.getShareService().addShare(s.getStreamId(), f);
		return s;
	}

	public DownloadingTrack getDownload(String streamId) {
		return inst.getDownloadService().getDownload(streamId);
	}

	public Stream getStream(String streamId) {
		return inst.getMetadataService().getStream(streamId);
	}

	public SharedTrack getShare(String streamId) {
		return inst.getDbService().getShare(streamId);
	}

	public SharedTrack getShareByFilePath(File path) {
		return inst.getShareService().getShareByFilePath(path.getAbsolutePath());
	}

	public void deleteDownload(String streamId) throws RobonoboException {
		inst.getDownloadService().deleteDownload(streamId);
	}

	public void deleteShare(String streamId) throws RobonoboException {
		inst.getShareService().deleteShare(streamId);
	}

	public String getMyNodeId() {
		return inst.getMina().getMyNodeId();
	}

	public List<String> getMyEndPointUrls() {
		return inst.getMina().getMyEndPointUrls();
	}

	public List getConnectedNodes() {
		return Arrays.asList(inst.getMina().getConnectedNodes());
	}

	public List<String> getConnectedSources(String streamId) {
		return inst.getMina().getConnectedSources(streamId);
	}

	public List<String> getDownloads() {
		return inst.getDbService().getDownloads();
	}

	public String getMimeTypeForFile(File f) {
		return inst.getFormatService().getMimeTypeForFile(f);
	}

	public Set<String> getShares() {
		return inst.getDbService().getShares();
	}

	public List<SharedTrack> getSharesByPattern(String searchPattern) {
		ArrayList<SharedTrack> result = new ArrayList<SharedTrack>();
		result.addAll(inst.getShareService().getSharesByPattern(searchPattern));
		sortShares(result);
		return result;
	}

	public boolean isNetworkRunning() {
		if (inst.getMina() == null)
			return false;
		return inst.getMina().isStarted();
	}

	public void pauseDownload(String streamId) {
		inst.getDownloadService().pauseDownload(streamId);
	}

	private void sortShares(ArrayList<SharedTrack> result) {
		// Doesn't really matter how we sort as long as it's consistent
		Collections.sort(result, new Comparator<SharedTrack>() {
			public int compare(SharedTrack s1, SharedTrack s2) {
				return s1.getStream().getTitle().compareTo(s2.getStream().getTitle());
			}
		});
	}

	public void start() throws RobonoboException {
		inst.start();
		jobRunner.start();
		if (inst.getConfig().getMetadataServerUsername() != null) {
			try {
				tryLogin(inst.getConfig().getMetadataServerUsername(), inst.getConfig().getMetadataServerPassword());
			} catch (Exception ignore) {
			}
		}
		inst.setStatus(RobonoboStatus.NotConnected);
	}

	public Track getTrack(String streamId) {
		return inst.getTrackService().getTrack(streamId);
	}

	public void addJob(Runnable job) {
		jobRunner.addJob(job);
	}

	/**
	 * Download must already be added
	 */
	public void startDownload(String streamId) throws RobonoboException {
		try {
			inst.getDownloadService().startDownload(streamId);
		} catch (Exception e) {
			throw new RobonoboException(e);
		}
	}

	public void search(String query, int startResult, SearchListener listener) {
		inst.getSearchService().search(query, startResult, listener);
	}

	public int numUsefulSources(String streamId) {
		return inst.getMina().numSources(streamId);
	}

	/**
	 * @param streamId
	 * @return
	 */
	public Set<String> getSources(String streamId) {
		return inst.getMina().getSources(streamId);
	}

	public void findSources(String streamId, FoundSourceListener listener) {
		inst.getMina().addFoundSourceListener(streamId, listener);
	}

	public void stopFindingSources(String streamId, FoundSourceListener listener) {
		inst.getMina().removeFoundSourceListener(streamId, listener);
	}

	public void shutdown() {
		inst.getEventService().removeAllListeners();
		jobRunner.stop();
		inst.shutdown();
	}

	public boolean haveAllTransfersStarted() {
		return inst.getTrackService().haveAllSharesStarted();
	}

	public ScheduledThreadPoolExecutor getExecutor() {
		return inst.getExecutor();
	}

	public void addToPlayQueue(String streamId) {
		inst.getPlaybackService().addToPlayQueue(streamId);
	}

	public void clearPlayQueue() {
		inst.getPlaybackService().clearQueue();
	}

	public void play(NextTrackListener finishListener) {
		if (finishListener != null)
			inst.getPlaybackService().setFinishListener(finishListener);
		inst.getPlaybackService().play();
	}

	public void pause() {
		inst.getPlaybackService().pause();
	}

	/**
	 * @param ms
	 *            Position in the stream to seek to, as millisecs from stream start
	 */
	public void seek(long ms) {
		inst.getPlaybackService().seek(ms);
	}

	/** If we are playing, pause. If we are paused, play. Otherwise, do nothing */
	public void togglePlayPause() {
		inst.getPlaybackService().togglePlayPause();
	}

	public void next() {
		inst.getPlaybackService().next();
	}

	public void previous() {
		inst.getPlaybackService().previous();
	}

	public void stopPlayback() {
		inst.getPlaybackService().stop();
	}

	public Stream currentPlayingStream() {
		String sid = inst.getPlaybackService().getCurrentStreamId();
		if (sid == null)
			return null;
		return inst.getMetadataService().getStream(sid);
	}

	public List<File> getWatchDirs() {
		return inst.getDbService().getWatchDirs();
	}

	public void addWatchDir(final File dir) {
		inst.getDbService().putWatchDir(dir);
		// This may take a while, kick it off in another thread
		inst.getExecutor().execute(new CatchingRunnable() {
			public void doRun() throws Exception {
				inst.getShareService().checkWatchDir(dir);
			}
		});
	}

	public void deleteWatchDir(File dir) {
		inst.getDbService().deleteWatchDir(dir);
	}

	/**
	 * @return true if login successful, false otherwise
	 */
	public boolean tryLogin(String email, String password) {
		return inst.getUsersService().tryLogin(email, password);
	}

	public User getMyUser() {
		return inst.getUsersService().getMyUser();
	}

	public double getBankBalance() throws RobonoboException {
		try {
			return inst.getWangService().getBankBalance();
		} catch (CurrencyException e) {
			throw new RobonoboException(e);
		}
	}

	public double getOnHandBalance() throws RobonoboException {
		try {
			return inst.getWangService().getOnHandBalance();
		} catch (CurrencyException e) {
			throw new RobonoboException(e);
		}
	}

	public User getUser(String email) {
		return inst.getUsersService().getUser(email);
	}

	public User getUser(long userId) {
		return inst.getUsersService().getUser(userId);
	}

	public Playlist getPlaylist(String playlistId) {
		return inst.getUsersService().getPlaylist(playlistId);
	}

	public Playlist getMyPlaylistByTitle(String title) {
		return inst.getUsersService().getMyPlaylistByTitle(title);
	}

	public void checkUsersUpdate() {
		inst.getUsersService().checkUsersUpdate();
	}

	/**
	 * Can only update the logged-in user. Not allowed to change your email address.
	 */
	public void updateUser(User newUser) throws RobonoboException {
		try {
			inst.getUsersService().updateMyUser(newUser);
		} catch (IOException e) {
			throw new RobonoboException(e);
		}
	}

	public void addOrUpdatePlaylist(Playlist pl) throws RobonoboException {
		try {
			inst.getUsersService().addOrUpdatePlaylist(pl);
		} catch (IOException e) {
			throw new RobonoboException(e);
		}
	}

	public void sendPlaylist(Playlist p, long toUserId) throws RobonoboException {
		try {
			inst.getUsersService().sendPlaylist(p, toUserId);
		} catch (IOException e) {
			throw new RobonoboException(e);
		}
	}

	public void sendPlaylist(Playlist p, String email) throws RobonoboException {
		try {
			inst.getUsersService().sendPlaylist(p, email);
		} catch (IOException e) {
			throw new RobonoboException(e);
		}
	}

	public void sharePlaylist(Playlist p, Set<Long> friendIds, Set<String> emails) throws RobonoboException {
		try {
			inst.getUsersService().sharePlaylist(p, friendIds, emails);
		} catch (IOException e) {
			throw new RobonoboException(e);
		}
	}

	public void nukePlaylist(Playlist pl) throws RobonoboException {
		try {
			inst.getUsersService().nukePlaylist(pl);
		} catch (IOException e) {
			throw new RobonoboException(e);
		}
	}

	public void checkPlaylistUpdate(String playlistId) throws RobonoboException {
		try {
			inst.getUsersService().checkPlaylistUpdate(playlistId);
		} catch (IOException e) {
			throw new RobonoboException(e);
		}
	}

	public PlaylistConfig getPlaylistConfig(String playlistId) {
		return inst.getDbService().getPlaylistConfig(playlistId);
	}

	public void putPlaylistConfig(PlaylistConfig pc) {
		inst.getDbService().putPlaylistConfig(pc);
	}

	public RobonoboConfig getConfig() {
		return inst.getConfig();
	}

	public Object getConfig(String cfgName) {
		return inst.getConfig(cfgName);
	}

	public void saveConfig() {
		inst.saveConfig();
	}

	/**
	 * For debugging only
	 */
	public Connection getMetadataDbConnection() throws SQLException {
		return inst.getDbService().getConnection();
	}

	/**
	 * For debugging only
	 */
	public void returnMetadataDbConnection(Connection conn) {
		inst.getDbService().returnConnection(conn);
	}

	/**
	 * For debugging only
	 */
	public Connection getPageDbConnection() throws SQLException {
		return inst.getStorageService().getPageDbConnection();
	}

	/**
	 * For debugging only
	 */
	public void returnPageDbConnection(Connection conn) throws SQLException {
		inst.getStorageService().returnPageDbConnection(conn);
	}
}
