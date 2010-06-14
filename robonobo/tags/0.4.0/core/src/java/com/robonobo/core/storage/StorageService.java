package com.robonobo.core.storage;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.robonobo.core.api.model.Stream;
import com.robonobo.core.service.AbstractRuntimeServiceProvider;
import com.robonobo.mina.external.buffer.PageBuffer;
import com.robonobo.mina.external.buffer.PageInfo;

@SuppressWarnings("unchecked")
public class StorageService extends AbstractRuntimeServiceProvider {
	protected Map<String, PageBuffer> bufferCache = new HashMap<String, PageBuffer>();
	Log log = LogFactory.getLog(getClass());
	PageInfoMgr pim;

	public StorageService() {
		addHardDependency("core.format");
	}

	@Override
	public void startup() throws Exception {
		String s = File.separator;
		File dbDir = new File(getRobonobo().getHomeDir(), "db");
		if(!dbDir.exists())
			dbDir.mkdirs();
		pim = new PageInfoMgr(dbDir.getAbsolutePath() + s + "pages");
	}

	public String getName() {
		return "Storage service";
	}

	public String getProvides() {
		return "core.storage";
	}

	public void shutdown() {
		try {
			pim.shutdown();
		} catch (Exception e) {
			log.error("Error shutting down PIM", e);
		}
	}

	public PageBuffer createPageBufForReception(Stream s, File dataFile) throws IOException {
		pim.init(s.getStreamId());
		PageBuffer pb = pim.createPageBuf(s, dataFile);
		bufferCache.put(s.getStreamId(), pb);
		return pb;
	}

	public PageBuffer createPageBufForBroadcast(Stream s, File dataFile) throws IOException {
		return createPageBufForBroadcast(s, dataFile, false);
	}

	public PageBuffer createPageBufForBroadcast(Stream s, File dataFile, boolean alreadyHavePageInfo) throws IOException {
		if (!alreadyHavePageInfo)
			pim.init(s.getStreamId());
		PageBuffer pb = pim.createPageBuf(s, dataFile);
		bufferCache.put(s.getStreamId(), pb);
		return pb;
	}

	public PageBuffer loadPageBuf(String streamId) throws IOException {
		if (bufferCache.containsKey(streamId))
			return bufferCache.get(streamId);
		PageBuffer pb = pim.getPageBuffer(streamId);
		bufferCache.put(streamId, pb);
		return pb;
	}

	public PageInfo getPageInfo(String streamId, long pageNum) {
		return pim.getPageInfo(streamId, pageNum);
	}

	public void nukePageBuf(String streamId) {
		log.debug("Nuking pagebuf for stream " + streamId);
		pim.nuke(streamId);
		bufferCache.remove(streamId);
	}
	
	public Connection getPageDbConnection() throws SQLException {
		return pim.getConnection();
	}
	
	public void returnPageDbConnection(Connection conn) {
		pim.returnConnection(conn);
	}
}
