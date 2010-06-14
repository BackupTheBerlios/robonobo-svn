package com.robonobo.plugins.playlistmirror;

import com.robonobo.core.service.AbstractRuntimeServiceProvider;

public class DummyPlugin extends AbstractRuntimeServiceProvider {

	@Override
	public void shutdown() throws Exception {
	}

	@Override
	public void startup() throws Exception {
	}

	public String getName() {
		return "Dummy Plugin";
	}

	public String getProvides() {
		return "plugin.dummy";
	}

}
