package com.robonobo.core.service;

import static com.robonobo.common.util.TimeUtil.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.robonobo.common.concurrent.Batcher;
import com.robonobo.common.concurrent.CatchingRunnable;
import com.robonobo.common.util.TimeUtil;
import com.robonobo.core.api.UserPlaylistListener;
import com.robonobo.core.api.config.MetadataServerConfig;
import com.robonobo.core.api.model.*;
import com.robonobo.core.api.proto.CoreApi.LibraryMsg;
import com.robonobo.core.service.UserService.UpdateChecker;

public class LibraryService extends AbstractService implements UserPlaylistListener {
	static final int LIBRARY_UPDATE_DELAY = 30; // Secs
	private AddBatcher addB;
	private DelBatcher delB;
	private Map<Long, Library> libs = new HashMap<Long, Library>();
	Date lastUpdated = now();
	ScheduledFuture<?> updateTask;
	boolean libsLoaded = false;

	public LibraryService() {
		addHardDependency("core.metadata");
		addHardDependency("core.users");
	}

	@Override
	public String getName() {
		return "Library service";
	}

	@Override
	public String getProvides() {
		return "core.library";
	}

	@Override
	public void startup() throws Exception {
		addB = new AddBatcher();
		delB = new DelBatcher();
		int updateFreq = rbnb.getConfig().getUserUpdateFrequency();
		updateTask = rbnb.getExecutor().scheduleAtFixedRate(new UpdateChecker(), updateFreq, updateFreq,
				TimeUnit.SECONDS);
		rbnb.getEventService().addUserPlaylistListener(this);
	}

	@Override
	public void shutdown() throws Exception {
		updateTask.cancel(true);
		// We might have some pending library updates - do them now
		addB.run();
		delB.run();
		
	}

	public synchronized Library getLibrary(long userId) {
		return libs.get(userId);
	}

	public void addToLibrary(String streamId) {
		addB.add(new LibraryTrack(streamId, now()));
	}

	public void delFromLibrary(String streamId) {
		delB.add(streamId);
	}

	@Override
	public void loggedIn() {
		// We will need to load the libraries again
		libsLoaded = false;
	}

	@Override
	public void userChanged(User u) {
		checkAllPlaylistsLoaded();
	}

	@Override
	public void playlistChanged(Playlist p) {
		checkAllPlaylistsLoaded();
	}

	/** We only load libraries after all playlists have been done, as library loading takes a while */
	private void checkAllPlaylistsLoaded() {
		if (libsLoaded)
			return;
		boolean gotEveryone = true;
		final UserService usrs = rbnb.getUsersService();
		nextFriend: for (long friendId : usrs.getMyUser().getFriendIds()) {
			User friend = usrs.getUser(friendId);
			if (friend == null) {
				gotEveryone = false;
				break nextFriend;
			}
			for (String plId : friend.getPlaylistIds()) {
				Playlist pl = usrs.getPlaylist(plId);
				if (pl == null) {
					gotEveryone = false;
					break nextFriend;
				}
			}
		}
		if (gotEveryone) {
			libsLoaded = true;
			rbnb.getExecutor().execute(new CatchingRunnable() {
				public void doRun() throws Exception {
					MetadataServerConfig msc = usrs.getMsc();
					for (long friendId : usrs.getMyUser().getFriendIds()) {
						LibraryMsg.Builder b = LibraryMsg.newBuilder();
						try {
							rbnb.getSerializationManager().getObjectFromUrl(b, msc.getLibraryUrl(friendId, null));
						} catch (IOException e) {
							log.error("Error getting library", e);
						}
						Library lib = new Library(b.build());
						// They might not have a library, in which case it'll be returned with no tracks
						if (lib.getTracks().size() > 0) {
							synchronized (LibraryService.this) {
								libs.put(friendId, lib);
							}
							// Check we have all streams
							for (String sid : lib.getTracks().keySet()) {
								rbnb.getMetadataService().getStream(sid);
							}
							rbnb.getEventService().fireLibraryUpdated(friendId, lib);
						}
					}
				}
			});
		}
	}

	@Override
	public void libraryUpdated(Library lib) {
		// Do nothing
	}

	class UpdateChecker extends CatchingRunnable {
		public void doRun() throws Exception {
			Long[] userIds;
			synchronized (LibraryService.this) {
				userIds = new Long[libs.size()];
				libs.keySet().toArray(userIds);
			}
			for (Long userId : userIds) {
				LibraryMsg.Builder b = LibraryMsg.newBuilder();
				MetadataServerConfig msc = rbnb.getUsersService().getMsc();
				try {
					rbnb.getSerializationManager().getObjectFromUrl(b, msc.getLibraryUrl(userId, lastUpdated));
				} catch (IOException e) {
					log.error("Error getting library", e);
				}
				Library nLib = new Library(b.build());
				if (nLib.getTracks().size() > 0) {
					Library cLib;
					synchronized (LibraryService.this) {
						cLib = libs.get(userId);
						cLib.getTracks().putAll(nLib.getTracks());
					}
					// Make sure we have the streams
					for (String sid : nLib.getTracks().keySet()) {
						rbnb.getMetadataService().getStream(sid);
					}
					rbnb.getEventService().fireLibraryUpdated(userId, cLib);
				}
			}
			lastUpdated = now();
		}
	}

	class LibraryTrack {
		String streamId;
		Date dateAdded;

		public LibraryTrack(String streamId, Date dateAdded) {
			this.streamId = streamId;
			this.dateAdded = dateAdded;
		}
	}

	class AddBatcher extends Batcher<LibraryTrack> {
		public AddBatcher() {
			super(LIBRARY_UPDATE_DELAY * 1000, rbnb.getExecutor());
		}

		@Override
		protected void runBatch(Collection<LibraryTrack> tracks) throws Exception {
			if(tracks.size() == 0)
				return;
			Library lib = new Library();
			for (LibraryTrack t : tracks) {
				lib.getTracks().put(t.streamId, t.dateAdded);
			}
			User me = rbnb.getUsersService().getMyUser();
			MetadataServerConfig msc = rbnb.getUsersService().getMsc();
			rbnb.getSerializationManager().putObjectToUrl(lib.toMsg(), msc.getLibraryAddUrl(me.getUserId()));
		}
	}

	class DelBatcher extends Batcher<String> {
		public DelBatcher() {
			super(LIBRARY_UPDATE_DELAY * 1000, rbnb.getExecutor());
		}

		@Override
		protected void runBatch(Collection<String> streamIds) throws Exception {
			if(streamIds.size() == 0)
				return;
			Library lib = new Library();
			for (String sid : streamIds) {
				lib.getTracks().put(sid, null);
			}
			User me = rbnb.getUsersService().getMyUser();
			MetadataServerConfig msc = rbnb.getUsersService().getMsc();
			rbnb.getSerializationManager().putObjectToUrl(lib.toMsg(), msc.getLibraryDelUrl(me.getUserId()));
		}
	}
}
