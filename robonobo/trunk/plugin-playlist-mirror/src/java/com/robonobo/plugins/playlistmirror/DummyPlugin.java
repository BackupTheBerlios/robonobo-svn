package com.robonobo.plugins.playlistmirror;

import com.robonobo.core.service.AbstractService;

public class DummyPlugin extends AbstractService {

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
