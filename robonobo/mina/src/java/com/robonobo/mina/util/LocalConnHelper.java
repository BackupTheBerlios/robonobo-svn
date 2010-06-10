package com.robonobo.mina.util;

import java.util.HashSet;
import java.util.Set;

import com.robonobo.mina.message.proto.MinaProtocol.AdvSource;
import com.robonobo.mina.message.proto.MinaProtocol.DontWantSource;
import com.robonobo.mina.message.proto.MinaProtocol.UnAdvSource;
import com.robonobo.mina.message.proto.MinaProtocol.WantSource;

/**
 * Keeps track of which streams the attached local node is a source for, and which it is searching for
 */
public class LocalConnHelper {
	Set<String> source = new HashSet<String>();
	Set<String> searching = new HashSet<String>();
	
	public LocalConnHelper() {
	}
	
	public synchronized void notifyWantSource(WantSource ws) {
		for (String streamId : ws.getStreamIdList()) {
			searching.add(streamId);
		}
	}
	
	public synchronized void notifyDontWantSource(DontWantSource dws) {
		for (String streamId : dws.getStreamIdList()) {
			searching.remove(streamId);
		}		
	}
	
	public synchronized void notifyAdvSource(AdvSource as) {
		for (String streamId : as.getStreamIdList()) {
			source.add(streamId);
		}
	}
	
	public synchronized void notifyUnAdvSource(UnAdvSource uas) {
		for (String streamId : uas.getStreamIdList()) {
			source.remove(streamId);
		}
	}
	
	public synchronized Set<String> getSourceStreams() {
		Set<String> result = new HashSet<String>();
		result.addAll(source);
		return result;
	}
	
	public synchronized Set<String> getSearchingStreams() {
		Set<String> result = new HashSet<String>();
		result.addAll(searching);
		return result;
	}
}
