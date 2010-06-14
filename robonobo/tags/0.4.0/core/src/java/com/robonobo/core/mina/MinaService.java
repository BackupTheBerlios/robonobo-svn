package com.robonobo.core.mina;

import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.robonobo.core.service.AbstractRuntimeServiceProvider;
import com.robonobo.mina.Mina;
import com.robonobo.mina.external.Application;
import com.robonobo.mina.external.MinaConfig;
import com.robonobo.mina.external.MinaControl;

public class MinaService extends AbstractRuntimeServiceProvider {
	SonarNodeLocator locator;
	Log log = LogFactory.getLog(getClass());
	protected MinaControl mina;

	public MinaService() {
		super();
		addHardDependency("core.gateway");
		addHardDependency("core.event");
		addHardDependency("core.wang");
	}

	public String getName() {
		return "Core Networking Service";
	}

	public String getProvides() {
		return "core.mina";
	}

	public MinaControl getMina() {
		return mina;
	}

	public SonarNodeLocator getSonarNodeLocator() {
		return locator;
	}

	public void shutdown() throws Exception {
		mina.stop();
		mina = null;
	}

	public void startup() throws Exception {
		MinaConfig minaCfg = (MinaConfig) getRobonobo().getConfig("mina");
		Application application = getRobonobo().getApplication();
		ScheduledThreadPoolExecutor executor = getRobonobo().getExecutor();
		mina = Mina.newInstance(minaCfg, application, executor);
		mina.addMinaListener(getRobonobo().getEventService());
		locator = new SonarNodeLocator();
		locator.addLocatorUri(getRobonobo().getConfig().getSonarServerUrl());
		mina.addNodeLocator(locator);

		if(getRobonobo().getConfig().isAgoric())
			mina.setCurrencyClient(getRobonobo().getWangService());
		mina.start();
	}
}
