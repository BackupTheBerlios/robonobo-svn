package com.robonobo.sonar.discovery;

import java.util.List;

import com.robonobo.core.api.proto.CoreApi.Node;

/**
 * Of the nodes we've saved, which ones should we send to a node?
 * @author macavity
 *
 */
public interface DiscoveryStrategy {

	public List<Node> discover(Node node);
}
