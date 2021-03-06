package com.robonobo.core.service;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.robonobo.core.api.RobonoboException;
import com.robonobo.core.api.config.MetadataServerConfig;
import com.robonobo.core.api.model.Stream;
import com.robonobo.core.api.model.User;
import com.robonobo.core.api.proto.CoreApi.StreamMsg;

public class MetadataService extends AbstractRuntimeServiceProvider {
	Log log = LogFactory.getLog(getClass());
	private MetadataServerConfig metadataServer;
	private Lock serverLock = new ReentrantLock();
	private Condition returnedFromServer;
	/** Stream IDs we're currently looking up from the server */
	private Set<String> lookupStreamIds = new HashSet<String>();

	public MetadataService() {
		returnedFromServer = serverLock.newCondition();
		addHardDependency("core.db");
	}

	public String getProvides() {
		return "core.metadata";
	}

	public String getName() {
		return "Metadata Manager";
	}

	public void startup() throws Exception {
		metadataServer = new MetadataServerConfig(getRobonobo().getConfig().getMetadataServerUrl());
	}

	public void shutdown() throws Exception {
	}

	public Stream getStream(String streamId) {
		// Thread programming is FUN
		serverLock.lock();
		try {
			while (lookupStreamIds.contains(streamId)) {
				// Another thread is looking up this stream - wait for them to get back
				try {
					returnedFromServer.await();
				} catch (InterruptedException e) {
					return null;
				}
			}

			Stream s = getRobonobo().getDbService().getStream(streamId);
			if (s != null)
				return s;

			lookupStreamIds.add(streamId);
		} finally {
			serverLock.unlock();
		}

		try {
			String streamUrl = metadataServer.getStreamUrl(streamId);
			try {
				StreamMsg.Builder sb = StreamMsg.newBuilder();
				getRobonobo().getSerializationManager().getObjectFromUrl(sb, streamUrl);
				Stream s = new Stream(sb.build());
				getRobonobo().getDbService().putStream(s);
				return s;
			} catch (Exception e) {
				throw new RobonoboException(e);
			} finally {
				serverLock.lock();
				lookupStreamIds.remove(streamId);
				returnedFromServer.signalAll();
				serverLock.unlock();
			}
		} catch (RobonoboException e) {
			log.error("Exception getting stream", e);
			return null;
		}
	}

	public void putStream(Stream stream) throws RobonoboException {
		try {
			getRobonobo().getDbService().putStream(stream);
			User me = getRobonobo().getUsersService().getMyUser();
			getRobonobo().getSerializationManager().putObjectToUrl(stream.toMsg(), metadataServer.getStreamUrl(stream.getStreamId()), me.getEmail(),
					me.getPassword());
		} catch (Exception e) {
			throw new RobonoboException(e);
		}
	}
}
