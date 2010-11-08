package com.robonobo.gui.components;

import static com.robonobo.common.util.TextUtil.*;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.swing.Icon;
import javax.swing.SwingUtilities;

import com.robonobo.common.concurrent.CatchingRunnable;
import com.robonobo.core.api.Task;
import com.robonobo.core.api.TaskListener;
import com.robonobo.gui.GUIUtils;
import com.robonobo.gui.RoboColor;
import com.robonobo.gui.frames.RobonoboFrame;
import com.robonobo.gui.panels.LeftSidebar;

public class TaskListSelector extends LeftSidebarSelector implements TaskListener {
	private static final int HIDE_TASKLIST_SECS = 30;
	static Icon runningIcon = new SpinnerIcon(16, RoboColor.DARKISH_GRAY);
	static Icon finishedIcon = GUIUtils.createImageIcon("/img/icon/tick.png", "All jobs done");

	Set<Task> tasks = new HashSet<Task>();

	public TaskListSelector(LeftSidebar sideBar, RobonoboFrame frame) {
		super(sideBar, frame, "All jobs done", false, finishedIcon, "tasklist");
		frame.getController().addTaskListener(this);
	}

	@Override
	public void taskUpdated(final Task t) {
		SwingUtilities.invokeLater(new CatchingRunnable() {
			public void doRun() throws Exception {
				synchronized (TaskListSelector.this) {
					if ((t.getCompletion() - 1f) == 0f)
						tasks.remove(t);
					if (tasks.size() == 0) {
						setIcon(finishedIcon);
						setText("All jobs done");
						setBold(false);
						startHideTimer();
					} else {
						setIcon(runningIcon);
						setText(numItems(tasks, "job") + " running");
						setBold(true);
						sideBar.showTaskList(true);
					}
				}
			}
		});
	}

	private void startHideTimer() {
		frame.getController().getExecutor().schedule(new CatchingRunnable() {
			public void doRun() throws Exception {
				synchronized (TaskListSelector.this) {
					if (tasks.size() == 0)
						sideBar.showTaskList(false);
				}
			}
		}, HIDE_TASKLIST_SECS, TimeUnit.SECONDS);
	}
}
