package com.robonobo.core.service;

import static com.robonobo.common.util.TimeUtil.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.robonobo.common.concurrent.Batcher;
import com.robonobo.common.concurrent.CatchingRunnable;
import com.robonobo.common.util.TimeUtil;
import com.robonobo.core.api.config.MetadataServerConfig;
import com.robonobo.core.api.model.Library;
import com.robonobo.core.api.model.User;
import com.robonobo.core.api.proto.CoreApi.LibraryMsg;
import com.robonobo.core.service.UserService.UpdateChecker;

public class LibraryService extends AbstractService {
	static final int LIBRARY_UPDATE_DELAY = 30; // Secs
	private AddBatcher addB;
	private DelBatcher delB;
	private Map<Long, Library> libs = new HashMap<Long, Library>();
	Date lastUpdated = now();
	ScheduledFuture<?> updateTask;

	public LibraryService() {
		addB = new AddBatcher();
		delB = new DelBatcher();
		addHardDependency("core.metadata");
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
		int updateFreq = rbnb.getConfig().getUserUpdateFrequency();
		updateTask = rbnb.getExecutor().scheduleAtFixedRate(new UpdateChecker(), updateFreq, updateFreq, TimeUnit.SECONDS);
	}

	@Override
	public void shutdown() throws Exception {
		updateTask.cancel(true);
	}

	public void fetchLibrary(final long userId) {
		rbnb.getExecutor().execute(new CatchingRunnable() {
			public void doRun() throws Exception {
				LibraryMsg.Builder b = LibraryMsg.newBuilder();
				MetadataServerConfig msc = rbnb.getUsersService().getMsc();
				try {
					rbnb.getSerializationManager().getObjectFromUrl(b, msc.getLibraryUrl(userId, null));
				} catch (IOException e) {
					log.error("Error getting library", e);
				}
				Library lib = new Library(b.build());
				synchronized (LibraryService.this) {
					libs.put(userId, lib);
				}
				rbnb.getEventService().fireLibraryUpdated(userId, lib);
			}
		});
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
				if(nLib.getTracks().size() > 0) {
					Library cLib;
					synchronized (LibraryService.this) {
						cLib = libs.get(userId);
						cLib.getTracks().putAll(nLib.getTracks());
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
