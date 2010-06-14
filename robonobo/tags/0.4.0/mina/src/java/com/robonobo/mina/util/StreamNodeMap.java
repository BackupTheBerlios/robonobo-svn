package com.robonobo.mina.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.robonobo.core.api.proto.CoreApi.Node;

/**
 * Keeps a bidirectional mapping between streams and nodes
 */
public class StreamNodeMap {
	Map<String, Set<Node>> streamMap;
	/** Map<NodeId, Set<StreamId>> */
	Map<String, Set<String>> nodeMap;

	public StreamNodeMap() {
		streamMap = new HashMap<String, Set<Node>>();
		nodeMap = new HashMap<String, Set<String>>();
	}

	public synchronized void addMapping(String streamId, Node node) {
		Set<Node> nodesForThisStream = streamMap.get(streamId);
		if (nodesForThisStream == null) {
			nodesForThisStream = new HashSet<Node>();
			streamMap.put(streamId, nodesForThisStream);
		}
		nodesForThisStream.add(node);

		Set<String> streamsForThisNode = nodeMap.get(node.getId());
		if (streamsForThisNode == null) {
			streamsForThisNode = new HashSet<String>();
			nodeMap.put(node.getId(), streamsForThisNode);
		}
		streamsForThisNode.add(streamId);
	}

	public synchronized void removeMapping(String streamId, Node node) {
		Set<Node> nodesForThisStream = streamMap.get(streamId);
		if (nodesForThisStream != null) {
			nodesForThisStream.remove(node);
			if (nodesForThisStream.size() == 0)
				streamMap.remove(streamId);
		}

		Set<String> streamsForThisNode = nodeMap.get(node.getId());
		if (streamsForThisNode != null) {
			streamsForThisNode.remove(streamId);
			if (streamsForThisNode.size() == 0)
				nodeMap.remove(node.getId());
		}
	}

	public synchronized void removeNode(Node node) {
		Set<String> streamsForThisNode = nodeMap.get(node.getId());
		if (streamsForThisNode != null) {
			for (String streamId : streamsForThisNode) {
				Set<Node> nodesForThisStream = streamMap.get(streamId);
				if (nodesForThisStream != null)
					nodesForThisStream.remove(node);
			}
			nodeMap.remove(node.getId());
		}
	}

	public synchronized Node[] getNodes(String streamId) {
		Set<Node> nodesForThisStream = streamMap.get(streamId);
		if (nodesForThisStream == null)
			return new Node[0];
		else {
			Node[] result = new Node[nodesForThisStream.size()];
			nodesForThisStream.toArray(result);
			return result;
		}
	}

	public synchronized Set<String> getAllStreams() {
		Set<String> streams = new HashSet<String>();
		streams.addAll(streamMap.keySet());
		return streams;
	}

	public synchronized void clear() {
		streamMap.clear();
		nodeMap.clear();
	}
}
