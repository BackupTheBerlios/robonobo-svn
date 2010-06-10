package com.robonobo.core.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.robonobo.common.concurrent.CatchingRunnable;
import com.robonobo.common.exceptions.SeekInnerCalmException;
import com.robonobo.core.api.RobonoboException;
import com.robonobo.core.api.RobonoboStatus;
import com.robonobo.core.api.config.MetadataServerConfig;
import com.robonobo.core.api.model.CloudTrack;
import com.robonobo.core.api.model.Playlist;
import com.robonobo.core.api.model.PlaylistConfig;
import com.robonobo.core.api.model.SharedTrack;
import com.robonobo.core.api.model.Track;
import com.robonobo.core.api.model.User;
import com.robonobo.core.api.proto.CoreApi.PlaylistMsg;
import com.robonobo.core.api.proto.CoreApi.UserMsg;

/**
 * Managers users (me and my friends) and associated playlists. We pull everything down via http on startup (and update it periodically); nothing is persisted
 * locally
 */
@SuppressWarnings("unchecked")
public class UserService extends AbstractRuntimeServiceProvider {
	Log log = LogFactory.getLog(getClass());
	private User me;
	MetadataServerConfig msc;
	/**
	 * We keep users and playlists in a hashmap, and look them up on demand. This is because they are being updated asynchronously, and so if we kept pointers,
	 * they'd go out of date.
	 */
	private Map<String, User> usersByEmail = Collections.synchronizedMap(new HashMap<String, User>());
	private Map<Long, User> usersById = Collections.synchronizedMap(new HashMap<Long, User>());
	private Map<String, Playlist> playlists = Collections.synchronizedMap(new HashMap<String, Playlist>());
	private Map<String, String> myPlaylistsByTitle = Collections.synchronizedMap(new HashMap<String, String>());
	private ScheduledFuture updateTask;
	private ReentrantLock userUpdateLock = new ReentrantLock();

	public UserService() {
		addHardDependency("core.metadata");
		addHardDependency("core.storage");
	}

	@Override
	public void startup() throws Exception {
		int updateFreq = robonobo.getConfig().getUserUpdateFrequency();
		updateTask = robonobo.getExecutor().scheduleAtFixedRate(new UpdateChecker(), updateFreq, updateFreq, TimeUnit.SECONDS);
	}

	public String getName() {
		return "User service";
	}

	public String getProvides() {
		return "core.users";
	}

	@Override
	public void shutdown() throws Exception {
		if (updateTask != null)
			updateTask.cancel(true);
	}

	public void checkUsersUpdate() {
		robonobo.getExecutor().execute(new UpdateChecker());
	}

	public boolean tryLogin(final String email, final String password) {
		MetadataServerConfig msc = new MetadataServerConfig(robonobo.getConfig().getMetadataServerUrl());
		try {
			log.info("Attempting login as user " + email);
			UserMsg.Builder ub = UserMsg.newBuilder();
			robonobo.getSerializationManager().getObjectFromUrl(ub, msc.getUserUrl(email), email, password);
			User tryUser = new User(ub.build());
			tryUser.setPassword(password);
			robonobo.getConfig().setMetadataServerUsername(email);
			robonobo.getConfig().setMetadataServerPassword(password);
			robonobo.saveConfig();
			// Reload everything again
			usersByEmail.clear();
			usersById.clear();
			playlists.clear();
			myPlaylistsByTitle.clear();
			usersByEmail.put(email, tryUser);
			usersById.put(tryUser.getUserId(), tryUser);
			this.msc = msc;
			me = tryUser;
			robonobo.getEventService().fireLoggedIn();
			log.info("Login as " + email + " successful");
			robonobo.getExecutor().execute(new CatchingRunnable() {
				public void doRun() throws Exception {
					UserLookerUpper ulu = new UserLookerUpper(me);
					ulu.doRun();
				}
			});
			if (robonobo.getMina().isConnectedToSupernode()) {
				robonobo.setStatus(RobonoboStatus.Connected);
				robonobo.getEventService().fireStatusChanged();
			}
			return true;
		} catch (Exception e) {
			log.error("Error logging in", e);
			return false;
		}
	}

	public boolean isLoggedIn() {
		return me != null;
	}

	public User getMyUser() {
		if (me == null)
			return null;
		return getUser(me.getEmail());
	}

	public void updateMyUser(User u) throws IOException {
		if (u.getEmail() != me.getEmail()) {
			throw new SeekInnerCalmException();
		}
		userUpdateLock.lock();
		try {
			robonobo.getSerializationManager().putObjectToUrl(u.toMsg(false), msc.getUserUrl(u.getEmail()), me.getEmail(), me.getPassword());
			synchronized (this) {
				me = u;
				usersByEmail.put(u.getEmail(), u);
				usersById.put(u.getUserId(), u);
			}
		} finally {
			userUpdateLock.unlock();
		}
		robonobo.getEventService().fireUserChanged(u);
	}

	public void addOrUpdatePlaylist(Playlist newP) throws IOException, RobonoboException {
		robonobo.getSerializationManager().putObjectToUrl(newP.toMsg(), msc.getPlaylistUrl(newP.getPlaylistId()), me.getEmail(), me.getPassword());
		Playlist origP = playlists.get(newP.getPlaylistId());
		if (origP == null) {
			// Grab a new copy of my user, it'll have the new playlist in it
			checkUserUpdate(me, false);
		}
		// Check that the server has our update
		checkPlaylistUpdate(newP.getPlaylistId());
	}

	public void nukePlaylist(Playlist pl) throws IOException, RobonoboException {
		robonobo.getSerializationManager().deleteObjectAtUrl(msc.getPlaylistUrl(pl.getPlaylistId()), me.getEmail(), me.getPassword());
		myPlaylistsByTitle.remove(pl.getTitle());
		// Get a new copy of my user without this playlist
		checkUserUpdate(me, false);
		// If we were sharing this with any of our friends, update it so that we
		// are no longer registered as an owner
		boolean sharing = false;
		synchronized (this) {
			for (Long friendId : me.getFriendIds()) {
				User friend = getUser(friendId);
				if (friend.getPlaylistIds().contains(pl.getPlaylistId())) {
					sharing = true;
					break;
				}
			}
		}
		if (sharing) {
			try {
				checkPlaylistUpdate(pl.getPlaylistId());
			} catch (RobonoboException e) {
				log.error("Error checking updated playlist after delete", e);
			}
		}
	}

	public void sendPlaylist(Playlist p, long toUserId) throws IOException {
		robonobo.getSerializationManager().hitUrl(msc.getSendPlaylistUrl(p.getPlaylistId(), toUserId), me.getEmail(), me.getPassword());
		// This playlist will be removed from my list - just remove it
		// locally, don't bother to hit the server again
		me.getPlaylistIds().remove(p.getPlaylistId());
		robonobo.getEventService().fireUserChanged(me);
	}

	public void sendPlaylist(Playlist p, String email) throws IOException {
		robonobo.getSerializationManager().hitUrl(msc.getSendPlaylistUrl(p.getPlaylistId(), email), me.getEmail(), me.getPassword());
		// This playlist will be removed from my list - just remove it
		// locally, don't bother to hit the server again
		me.getPlaylistIds().remove(p.getPlaylistId());
		robonobo.getEventService().fireUserChanged(me);
	}

	public void sharePlaylist(Playlist p, Set<Long> friendIds, Set<String> emails) throws IOException, RobonoboException {
		robonobo.getSerializationManager().hitUrl(msc.getSharePlaylistUrl(p.getPlaylistId(), friendIds, emails), me.getEmail(), me.getPassword());
		List<User> friends = new ArrayList<User>(friendIds.size());
		for (Long friendId : friendIds) {
			friends.add(getUser(friendId));
		}
		checkUserUpdate(friends);
	}

	public synchronized User getUser(String email) {
		return usersByEmail.get(email);
	}

	public synchronized User getUser(long userId) {
		return usersById.get(userId);
	}

	public synchronized Playlist getPlaylist(String playlistId) {
		return playlists.get(playlistId);
	}

	public synchronized Playlist getMyPlaylistByTitle(String title) {
		String plId = myPlaylistsByTitle.get(title);
		if (plId == null)
			return null;
		return getPlaylist(plId);
	}

	private User getUpdatedUser(long userId) throws IOException {
		UserMsg.Builder ub = UserMsg.newBuilder();
		robonobo.getSerializationManager().getObjectFromUrl(ub, msc.getUserUrl(userId), me.getEmail(), me.getPassword());
		return new User(ub.build());
	}

	private Playlist getUpdatedPlaylist(String playlistId) throws IOException {
		String playlistUrl = msc.getPlaylistUrl(playlistId);
		PlaylistMsg.Builder pb = PlaylistMsg.newBuilder();
		robonobo.getSerializationManager().getObjectFromUrl(pb, playlistUrl, me.getEmail(), me.getPassword());
		return new Playlist(pb.build());
	}

	public void checkPlaylistUpdate(String playlistId) throws IOException, RobonoboException {
		Playlist currentP = playlists.get(playlistId);
		final Playlist updatedP = getUpdatedPlaylist(playlistId);
		PlaylistConfig pc = robonobo.getDbService().getPlaylistConfig(playlistId);
		boolean autoDownload = (pc != null) && "true".equalsIgnoreCase(pc.getItem("autoDownload"));
		// Make sure we have copies of all streams
		if (autoDownload) {
			for (String streamId : updatedP.getStreamIds()) {
				Track t = robonobo.getTrackService().getTrack(streamId);
				if (t instanceof CloudTrack)
					robonobo.getDownloadService().addDownload(streamId);
			}
		}
		if (currentP == null || currentP.getUpdated() == null || updatedP.getUpdated().after(currentP.getUpdated())) {
			playlists.put(playlistId, updatedP);
			if (me.getPlaylistIds().contains(updatedP.getPlaylistId())) {
				if (currentP != null)
					myPlaylistsByTitle.remove(currentP.getTitle());
				myPlaylistsByTitle.put(updatedP.getTitle(), updatedP.getPlaylistId());
			}
			robonobo.getEventService().firePlaylistChanged(updatedP);
		}
		if (pc != null && "true".equalsIgnoreCase(pc.getItem("iTunesExport"))) {
			final List<User> contUsers = new ArrayList<User>();
			synchronized (this) {
				for (User u : usersById.values()) {
					if (u.getPlaylistIds().contains(playlistId))
						contUsers.add(u);
				}
			}
			// Update itunes in another thread
			getRobonobo().getExecutor().execute(new CatchingRunnable() {
				public void doRun() throws Exception {
					for (User u : contUsers) {
						getRobonobo().getITunesService().syncPlaylist(u, updatedP);
					}
				}
			});
		}
	}

	/**
	 * Update things that need to be updated on playlists containing this track we're now sharing
	 */
	public void checkPlaylistsForNewShare(SharedTrack sh) {
		// Currently, just sync to itunes if necessary
		final Map<User, List<Playlist>> contPls = new HashMap<User, List<Playlist>>();
		synchronized (this) {
			for (User u : usersById.values()) {
				for (String plId : u.getPlaylistIds()) {
					PlaylistConfig pc = getRobonobo().getDbService().getPlaylistConfig(plId);
					if ("true".equalsIgnoreCase(pc.getItem("iTunesExport"))) {
						if (!contPls.containsKey(u))
							contPls.put(u, new ArrayList<Playlist>());
						contPls.get(u).add(getPlaylist(plId));
					}
				}
			}
		}
		// Come out of sync and do this in another thread, it'll take a while
		getRobonobo().getExecutor().execute(new CatchingRunnable() {
			public void doRun() throws Exception {
				for (User u : contPls.keySet()) {
					for (Playlist p : contPls.get(u)) {
						getRobonobo().getITunesService().syncPlaylist(u, p);
					}
				}
			}
		});
	}

	private void checkUserUpdate(User u, boolean cascade) throws IOException, RobonoboException {
		User newU = getUpdatedUser(u.getUserId());
		if ((newU.getUpdated() == null && u.getUpdated() == null) || newU.getUpdated().after(u.getUpdated())) {
			for (long friendId : newU.getFriendIds()) {
				if (!usersById.containsKey(friendId))
					robonobo.getExecutor().execute(new UserLookerUpper(friendId));
			}
			synchronized (UserService.this) {
				if (newU.equals(me))
					newU.setPassword(me.getPassword());
				usersByEmail.put(newU.getEmail(), newU);
				usersById.put(newU.getUserId(), newU);
			}
			robonobo.getEventService().fireUserChanged(newU);
		}
		if (cascade) {
			for (String playlistId : newU.getPlaylistIds()) {
				checkPlaylistUpdate(playlistId);
			}
		}
	}

	/**
	 * This updates all the users before updating the playlists, so if they share playlists they all catch the changes
	 * 
	 * @throws RobonoboException
	 * @throws IOException
	 */
	private void checkUserUpdate(Collection<User> users) throws IOException, RobonoboException {
		Set<String> playlistsToCheck = new HashSet<String>();
		for (User u : users) {
			User newU = getUpdatedUser(u.getUserId());
			if ((newU.getUpdated() == null && u.getUpdated() == null) || newU.getUpdated().after(u.getUpdated())) {
				for (long friendId : newU.getFriendIds()) {
					if (!usersById.containsKey(friendId))
						robonobo.getExecutor().execute(new UserLookerUpper(friendId));
				}
				synchronized (UserService.this) {
					usersByEmail.put(newU.getEmail(), newU);
					usersById.put(newU.getUserId(), newU);
				}
				robonobo.getEventService().fireUserChanged(newU);
			}
			for (String playlistId : newU.getPlaylistIds()) {
				playlistsToCheck.add(playlistId);
			}
		}
		for (String playlistId : playlistsToCheck) {
			checkPlaylistUpdate(playlistId);
		}
	}

	class UserLookerUpper extends CatchingRunnable {
		long userId;
		User u;

		public UserLookerUpper(User u) {
			this.u = u;
		}

		public UserLookerUpper(long userId) {
			this.userId = userId;
		}

		@Override
		public void doRun() throws Exception {
			// If we don't have a user, look it up
			if (u == null) {
				u = getUpdatedUser(userId);
				synchronized (UserService.this) {
					usersByEmail.put(u.getEmail(), u);
					usersById.put(u.getUserId(), u);
				}
			}
			for (long friendId : u.getFriendIds()) {
				if (!usersById.containsKey(friendId)) {
					robonobo.getExecutor().execute(new UserLookerUpper(friendId));
				}
			}
			robonobo.getEventService().fireUserChanged(u);
			for (String playlistId : u.getPlaylistIds()) {
				// For shared playlists, don't hit the server again if we already have it - but fire the playlist changed event so it's added to the friend tree
				// for this user
				Playlist p = getPlaylist(playlistId);
				if(p == null)
					checkPlaylistUpdate(playlistId);
				else
					robonobo.getEventService().firePlaylistChanged(p);
			}
		}
	}

	class UpdateChecker extends CatchingRunnable {
		@Override
		public void doRun() throws Exception {
			if (me == null) {
				return;
			}
			// Copy out users so we can iterate over them safely
			User[] uArr = new User[usersByEmail.size()];
			usersByEmail.values().toArray(uArr);
			for (User u : uArr) {
				if (me.equals(u)) {
					// This is me - check to see if we're currently updating my
					// user, and if we are, don't bother to check for updates -
					// just leave it until next time
					if (userUpdateLock.isLocked())
						continue;
				}
				checkUserUpdate(u, true);
			}
		}
	}

}
