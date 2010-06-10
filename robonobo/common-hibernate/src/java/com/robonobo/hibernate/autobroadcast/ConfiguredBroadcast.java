package com.robonobo.hibernate.autobroadcast;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class ConfiguredBroadcast implements Serializable {
	private static final long serialVersionUID = 1L;
	int id;
	String channelUri;
	String broadcastSourceClass;
	Map sourceArgs = new HashMap();

	public ConfiguredBroadcast() {
	}

	public String getBroadcastSourceClass() {
		return broadcastSourceClass;
	}

	public String getChannelUri() {
		return channelUri;
	}

	public int getId() {
		return id;
	}

	public Map getSourceArgs() {
		return sourceArgs;
	}

	public void setBroadcastSourceClass(String broadcastSourceClass) {
		this.broadcastSourceClass = broadcastSourceClass;
	}

	public void setChannelUri(String channelUri) {
		this.channelUri = channelUri;
	}

	public void setId(int id) {
		this.id = id;
	}

	public void setSourceArgs(Map sourceArgs) {
		this.sourceArgs = sourceArgs;
	}
}
