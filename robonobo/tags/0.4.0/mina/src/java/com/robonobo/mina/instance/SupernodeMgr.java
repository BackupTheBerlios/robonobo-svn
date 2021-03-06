package com.robonobo.mina.instance;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.robonobo.core.api.proto.CoreApi.Node;
import com.robonobo.mina.message.MessageHolder;
import com.robonobo.mina.message.proto.MinaProtocol.AdvSource;
import com.robonobo.mina.message.proto.MinaProtocol.DontWantSource;
import com.robonobo.mina.message.proto.MinaProtocol.UnAdvSource;
import com.robonobo.mina.message.proto.MinaProtocol.WantSource;
import com.robonobo.mina.util.StreamNodeMap;

public class SupernodeMgr {
	MinaInstance mina;
	StreamNodeMap sources;
	StreamNodeMap searchers;
	
	public SupernodeMgr(MinaInstance mina) {
		this.mina = mina;
		sources = new StreamNodeMap();
		searchers = new StreamNodeMap();
	}
	
	public synchronized Node[] getSearchers(String streamId) {
		return searchers.getNodes(streamId);
	}
	
	public synchronized Node[] getSources(String streamId) {
		return sources.getNodes(streamId);
	}
	
	/**
	 * Returns map<streamid, list<source-nodedesc>>
	 */
	public synchronized Map<String, List<Node>> notifyWantSource(MessageHolder mh) {
		WantSource ws = (WantSource) mh.getMessage();
		Map<String, List<Node>> result = new HashMap<String, List<Node>>(); 
		for (String streamId : ws.getStreamIdList()) {
			searchers.addMapping(streamId, mh.getFromCC().getNodeDescriptor());
			result.put(streamId, Arrays.asList(sources.getNodes(streamId)));
		}
		return result;
	}
	
	public synchronized void notifyDontWantSource(MessageHolder mh) {
		DontWantSource dws = (DontWantSource) mh.getMessage();
		for (String streamId : dws.getStreamIdList()) {
			searchers.removeMapping(streamId, mh.getFromCC().getNodeDescriptor());
		}
	}

	/**
	 * Returns map<streamid, list<searcher-nodedesc>>
	 */
	public synchronized Map<String, List<Node>> notifyAdvSource(MessageHolder mh) {
		AdvSource as = (AdvSource) mh.getMessage();
		Map<String, List<Node>> result = new HashMap<String, List<Node>>();
		for (String streamId : as.getStreamIdList()) {
			// Don't pass on the local attr (if any)
			Node sourceNode = Node.newBuilder().mergeFrom(mh.getFromCC().getNodeDescriptor()).setLocal(false).build();
			sources.addMapping(streamId, sourceNode);
			result.put(streamId, Arrays.asList(searchers.getNodes(streamId)));
		}
		return result;
	}
	
	public synchronized void notifyUnAdvSource(MessageHolder mh) {
		UnAdvSource uas = (UnAdvSource) mh.getMessage();
		for (String streamId : uas.getStreamIdList()) {
			sources.removeMapping(streamId, mh.getFromCC().getNodeDescriptor());
		}
	}
	
	public synchronized void notifyDeadConnection(Node node) {
		sources.removeNode(node);
		searchers.removeNode(node);
	}

}
