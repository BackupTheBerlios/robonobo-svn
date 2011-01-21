package com.robonobo.common.concurrent;

import java.util.*;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * Like Batcher, but won't add an object if it's been added already
 */
public abstract class UniqueBatcher<T> extends Batcher<T> {
	private Set<T> set = new HashSet<T>();

	public UniqueBatcher(long timespan, ScheduledThreadPoolExecutor executor) {
		super(timespan, executor);
	}

	public void add(T obj) {
		lock.lock();
		try {
			if (set.contains(obj))
				return;
			set.add(obj);
		} finally {
			lock.unlock();
		}
		super.add(obj);
	}

	@Override
	public void doRun() throws Exception {
		lock.lock();
		try {
			set.clear();
		} finally {
			lock.unlock();
		}
		super.doRun();
	}
}
