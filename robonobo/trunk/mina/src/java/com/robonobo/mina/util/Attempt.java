package com.robonobo.mina.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.robonobo.common.concurrent.CatchingRunnable;
import com.robonobo.mina.instance.MinaInstance;

public abstract class Attempt extends CatchingRunnable {
	private MinaInstance mina;
	private int timeoutMs;
	private ScheduledFuture<?> timeoutFuture;
	protected List<Attempt> contingentAttempts;

	public Attempt(MinaInstance mina, int timeoutMs, String name) {
		this.mina = mina;
		this.timeoutMs = timeoutMs;
		contingentAttempts = new ArrayList<Attempt>();
	}

	public synchronized void start() {
		timeoutFuture = mina.getExecutor().schedule(this, timeoutMs, TimeUnit.MILLISECONDS);
	}

	public synchronized void cancel() {
		if (timeoutFuture.isDone())
			return;
		timeoutFuture.cancel(false);
		for (Attempt a : contingentAttempts) {
			a.cancel();
		}
	}

	public synchronized void succeeded() {
		if (timeoutFuture != null) {
			if (timeoutFuture.isDone())
				return;
			timeoutFuture.cancel(false);
		}
		mina.getExecutor().execute(new CatchingRunnable() {
			public void doRun() throws Exception {
				onSuccess();
				for (Attempt a : contingentAttempts) {
					a.succeeded();
				}
			}
		});
	}

	public synchronized void failed() {
		if (timeoutFuture.isDone())
			return;

		timeoutFuture.cancel(false);
		mina.getExecutor().execute(new CatchingRunnable() {
			public void doRun() throws Exception {
				onFail();
			}
		});

		for (Attempt a : contingentAttempts) {
			a.failed();
		}
	}

	public void doRun() {
		timedOut();
	}

	public void addContingentAttempt(Attempt a) {
		contingentAttempts.add(a);
	}

	protected void timedOut() {
		mina.getExecutor().execute(new CatchingRunnable() {
			public void doRun() throws Exception {
				onTimeout();
			}
		});

		for (Attempt a : contingentAttempts) {
			a.failed();
		}
	}

	protected void onTimeout() {
	}

	protected void onFail() {
	}

	protected void onSuccess() {
	}
}
