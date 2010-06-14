package com.robonobo.core.service;

import java.util.HashMap;
import java.util.Map;

import com.robonobo.core.RobonoboInstance;
import com.robonobo.core.api.Robonobo;
import com.robonobo.spi.RuntimeServiceProvider;


@SuppressWarnings("unchecked")
abstract public class AbstractRuntimeServiceProvider implements RuntimeServiceProvider {
	boolean running = false;
	Map deps = new HashMap();
	RobonoboInstance robonobo;

	public AbstractRuntimeServiceProvider() {
		super();
	}

	protected RobonoboInstance getRobonobo() {
		return robonobo;
	}

	public void setRobonobo(Robonobo robonobo) {
		this.robonobo = (RobonoboInstance) robonobo;
	}

	protected void addHardDependency(String need) {
		addDependency(need, true);
	}

	protected void addSoftDependency(String need) {
		addDependency(need, false);
	}

	protected void addDependency(String need, boolean required) {
		deps.put(need, new Boolean(required));
	}

	protected void removeDependency(String need) {
		deps.remove(need);
	}

	public Map getDependencies() {
		return deps;
	}

	abstract public void startup() throws Exception;

	abstract public void shutdown() throws Exception;

	public boolean isRunning() {
		return running;
	}

	public void setRunning(boolean running) {
		this.running = running;
	}
}
