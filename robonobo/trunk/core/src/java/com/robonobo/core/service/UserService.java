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
import com.robonobo.common.serialization.SerializationManager;
import com.robonobo.core.api.RobonoboException;
import com.robonobo.core.api.RobonoboStatus;
import com.robonobo.core.api.config.MetadataServerConfig;
import com.robonobo.core.api.model.*;
import com.robonobo.core.api.proto.CoreApi.PlaylistMsg;
import com.robonobo.core.api.proto.CoreApi.UserConfigMsg;
import com.robonobo.core.api.proto.CoreApi.UserMsg;

/**
 * Managers users (me and my friends) and associated playlists. We pull everything down via http on startup (and update
 * it periodically); nothing is persisted locally
 */
@SuppressWarnings("unchecked")
public class UserService extends AbstractService {
	Log log = LogFactory.getLog(getClass());
	private User me;
	MetadataServerConfig msc;
	/**
	 * We keep users and playlists in a hashmap, and look them up on demand. This is because they are being updated
	 * asynchronously, and so if we kept pointers, they'd go out of date.
	 */
	private Map<String, User> usersByEmail = Collections.synchronizedMap(new HashMap<String, User>());
	private Map<Long, User> usersById = Collections.synchronizedMap(new HashMap<Long, User>());
	private Map<Long, Playlist> playlists = Collections.synchronizedMap(new HashMap<Long, Playlist>());
	private Map<String, Long> myPlaylistIdsByTitle = Collections.synchronizedMap(new HashMap<String, Long>());
	private ScheduledFuture updateTask;
	private ReentrantLock userUpdateLock = new ReentrantLock();

	public UserService() {
		addHardDependency("core.metadata");
		addHardDependency("core.storage");
	}

	@Override
	public void startup() throws Exception {
		int updateFreq = rbnb.getConfig().getUserUpdateFrequency();
		updateTask = rbnb.getExecutor().scheduleAtFixedRate(new UpdateChecker(), updateFreq, updateFreq,
				TimeUnit.SECONDS);
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
		rbnb.getExecutor().execute(new UpdateChecker());
	}

	public boolean tryLogin(final String email, final String password) {
		MetadataServerConfig msc = new MetadataServerConfig(rbnb.getConfig().getMetadataServerUrl());
		try {
			log.info("Attempting login as user " + email);
			UserMsg.Builder ub = UserMsg.newBuilder();
			// If the details are wrong, this will chuck an exception
			SerializationManager sm = rbnb.getSerializationManager();
			sm.setCreds(email, password);
			sm.getObjectFromUrl(ub, msc.getUserUrl(email));
			User tryUser = new User(ub.build());
			tryUser.setPassword(password);
			rbnb.getConfig().setMetadataServerUsername(email);
			rbnb.getConfig().setMetadataServerPassword(password);
			rbnb.saveConfig();
			// Reload everything again
			usersByEmail.clear();
			usersById.clear();
			playlists.clear();
			myPlaylistIdsByTitle.clear();
			usersByEmail.put(email, tryUser);
			usersById.put(tryUser.getUserId(), tryUser);
			this.msc = msc;
			me = tryUser;
			log.info("Login as " + email + " successful");
			rbnb.getExecutor().execute(new CatchingRunnable() {
				public void doRun() throws Exception {
					rbnb.getEventService().fireLoggedIn();
					UserLookerUpper ulu = new UserLookerUpper(me);
					ulu.doRun();
					lookupUserConfig();
				}
			});
			if (rbnb.getMina().isConnectedToSupernode()) {
				rbnb.setStatus(RobonoboStatus.Connected);
				rbnb.getEventService().fireStatusChanged();
			}
			return true;
		} catch (Exception e) {
			log.error("Error logging in", e);
			return false;
		}
	}

	private void lookupUserConfig() throws IOException {
		UserConfigMsg.Builder b = UserConfigMsg.newBuilder();
		rbnb.getSerializationManager().getObjectFromUrl(b, msc.getUserConfigUrl(me.getUserId()));
		UserConfig cfg = new UserConfig(b.build());
		rbnb.getEventService().fireUserConfigChanged(cfg);
	}

	public void saveUserConfigItem(String itemName, String itemValue) {
		if (me == null) {
			log.error("Error: tried to save user config, but I am not logged in");
			return;
		}
		UserConfig cfg = new UserConfig();
		cfg.setUserId(me.getUserId());
		cfg.getItems().put(itemName, itemValue);
		try {
			rbnb.getSerializationManager().putObjectToUrl(cfg.toMsg(), msc.getUserConfigUrl(me.getUserId()));
		} catch (IOException e) {
			log.error("Errot saving user config", e);
		}
	}

	public void requestTopUp() throws IOException {
		if (me != null) {
			try {
				rbnb.getSerializationManager().hitUrl(msc.getTopUpUrl());
			} catch (IOException e) {
				log.error("Error requesting topup", e);
				throw e;
			}
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

	public MetadataServerConfig getMsc() {
		return msc;
	}

	public void updateMyUser(User u) throws IOException {
		if (u.getEmail() != me.getEmail()) {
			throw new SeekInnerCalmException();
		}
		userUpdateLock.lock();
		try {
			rbnb.getSerializationManager().putObjectToUrl(u.toMsg(false), msc.getUserUrl(u.getEmail()));
			synchronized (this) {
				me = u;
				usersByEmail.put(u.getEmail(), u);
				usersById.put(u.getUserId(), u);
			}
		} finally {
			userUpdateLock.unlock();
		}
		rbnb.getEventService().fireUserChanged(u);
	}

	/**
	 * Returns the playlist with the playlist id set
	 */
	public Playlist addOrUpdatePlaylist(Playlist newP) throws IOException, RobonoboException {
		String playlistUrl = msc.getPlaylistUrl(newP.getPlaylistId());
		if (newP.getPlaylistId() <= 0) {
			// New playlist - the server will send it back with the playlist id set
			PlaylistMsg.Builder bldr = PlaylistMsg.newBuilder();
			rbnb.getSerializationManager().putObjectToUrl(newP.toMsg(), playlistUrl, bldr);
			Playlist updatedP = new Playlist(bldr.build());
			// Grab a new copy of my user, it'll have the new playlist in it
			checkUserUpdate(me, false);
			// Fire this playlist as being updated
			rbnb.getEventService().firePlaylistChanged(updatedP);
			return updatedP;
		} else {
			rbnb.getSerializationManager().putObjectToUrl(newP.toMsg(), playlistUrl);
			return newP;
		}
	}

	public void nukePlaylist(Playlist pl) throws IOException, RobonoboException {
		rbnb.getSerializationManager().deleteObjectAtUrl(msc.getPlaylistUrl(pl.getPlaylistId()));
		myPlaylistIdsByTitle.remove(pl.getTitle());
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

	public void sharePlaylist(Playlist p, Set<Long> friendIds, Set<String> emails) throws IOException,
			RobonoboException {
		rbnb.getSerializationManager().hitUrl(msc.getSharePlaylistUrl(p.getPlaylistId(), friendIds, emails));
		List<User> friends = new ArrayList<User>(friendIds.size());
		for (Long friendId : friendIds) {
			friends.add(getUser(friendId));
		}
		checkUserUpdate(friends);
	}

	public void postFacebookUpdate(long playlistId) throws IOException {
		rbnb.getSerializationManager().hitUrl(msc.getFacebookPlaylistUrl(playlistId));
	}

	public void postTwitterUpdate(long playlistId) throws IOException {
		rbnb.getSerializationManager().hitUrl(msc.getTwitterPlaylistUrl(playlistId));
	}

	public synchronized User getUser(String email) {
		return usersByEmail.get(email);
	}

	public synchronized User getUser(long userId) {
		return usersById.get(userId);
	}

	public synchronized Playlist getPlaylist(long playlistId) {
		return playlists.get(playlistId);
	}

	public synchronized Playlist getMyPlaylistByTitle(String title) {
		Long plId = myPlaylistIdsByTitle.get(title);
		if (plId == null)
			return null;
		return getPlaylist(plId);
	}

	private User getUpdatedUser(long userId) throws IOException {
		UserMsg.Builder ub = UserMsg.newBuilder();
		rbnb.getSerializationManager().getObjectFromUrl(ub, msc.getUserUrl(userId));
		return new User(ub.build());
	}

	private Playlist getUpdatedPlaylist(long playlistId) throws IOException {
		String playlistUrl = msc.getPlaylistUrl(playlistId);
		PlaylistMsg.Builder pb = PlaylistMsg.newBuilder();
		rbnb.getSerializationManager().getObjectFromUrl(pb, playlistUrl);
		return new Playlist(pb.build());
	}

	public void checkPlaylistUpdate(long playlistId) throws IOException, RobonoboException {
		Playlist currentP = playlists.get(playlistId);
		final Playlist updatedP = getUpdatedPlaylist(playlistId);
		PlaylistConfig pc = rbnb.getDbService().getPlaylistConfig(playlistId);
		if (currentP == null || currentP.getUpdated() == null || updatedP.getUpdated().after(currentP.getUpdated())) {
			// Make sure we have copies of all streams
			for (String sid : updatedP.getStreamIds()) {
				rbnb.getMetadataService().getStream(sid);
			}
			playlists.put(playlistId, updatedP);
			if (me.getPlaylistIds().contains(updatedP.getPlaylistId())) {
				if (currentP != null)
					myPlaylistIdsByTitle.remove(currentP.getTitle());
				myPlaylistIdsByTitle.put(updatedP.getTitle(), updatedP.getPlaylistId());
			}
			rbnb.getEventService().firePlaylistChanged(updatedP);
		}
		if (((pc != null) && "true".equalsIgnoreCase(pc.getItem("autoDownload")))) {
			for (String streamId : updatedP.getStreamIds()) {
				Track t = rbnb.getTrackService().getTrack(streamId);
				if (t instanceof CloudTrack)
					rbnb.getDownloadService().addDownload(streamId);
			}
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
				for (Long plId : u.getPlaylistIds()) {
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
					rbnb.getExecutor().execute(new UserLookerUpper(friendId));
			}
			synchronized (UserService.this) {
				if (newU.equals(me))
					newU.setPassword(me.getPassword());
				usersByEmail.put(newU.getEmail(), newU);
				usersById.put(newU.getUserId(), newU);
			}
			rbnb.getEventService().fireUserChanged(newU);
		}
		if (cascade) {
			for (Long playlistId : newU.getPlaylistIds()) {
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
		Set<Long> playlistsToCheck = new HashSet<Long>();
		for (User u : users) {
			User newU = getUpdatedUser(u.getUserId());
			if ((newU.getUpdated() == null && u.getUpdated() == null) || newU.getUpdated().after(u.getUpdated())) {
				for (long friendId : newU.getFriendIds()) {
					if (!usersById.containsKey(friendId))
						rbnb.getExecutor().execute(new UserLookerUpper(friendId));
				}
				synchronized (UserService.this) {
					usersByEmail.put(newU.getEmail(), newU);
					usersById.put(newU.getUserId(), newU);
				}
				rbnb.getEventService().fireUserChanged(newU);
			}
			for (Long playlistId : newU.getPlaylistIds()) {
				playlistsToCheck.add(playlistId);
			}
		}
		for (Long playlistId : playlistsToCheck) {
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
					rbnb.getExecutor().execute(new UserLookerUpper(friendId));
				}
			}
			rbnb.getEventService().fireUserChanged(u);
			for (Long playlistId : u.getPlaylistIds()) {
				// For shared playlists, don't hit the server again if we already have it - but fire the playlist
				// changed event so it's added to the friend tree
				// for this user
				Playlist p = getPlaylist(playlistId);
				if (p == null)
					checkPlaylistUpdate(playlistId);
				else
					rbnb.getEventService().firePlaylistChanged(p);
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
