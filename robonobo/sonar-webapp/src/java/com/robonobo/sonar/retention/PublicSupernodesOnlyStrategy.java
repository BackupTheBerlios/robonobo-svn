package com.robonobo.sonar.retention;

import com.robonobo.core.api.proto.CoreApi.EndPoint;
import com.robonobo.core.api.proto.CoreApi.Node;
import com.robonobo.mina.external.node.EonEndPoint;

public class PublicSupernodesOnlyStrategy implements RetentionStrategy {

	public boolean shouldRetainNode(Node node) {
		if(!node.getSupernode())
			return false;
		boolean isPublic = false;
		for (EndPoint ep : node.getEndPointList()) {
			if(EonEndPoint.isEonUrl(ep.getUrl())) {
				EonEndPoint eonEp = new EonEndPoint(ep.getUrl());
				if(eonEp.getAddress().isSiteLocalAddress()) {
					isPublic = true;
					break;
				}
			}
		}
		return isPublic;
	}

}
