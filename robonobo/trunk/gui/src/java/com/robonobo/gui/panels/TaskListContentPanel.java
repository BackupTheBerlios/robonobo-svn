package com.robonobo.gui.panels;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.*;

import org.debian.tablelayout.TableLayout;

import com.robonobo.common.concurrent.CatchingRunnable;
import com.robonobo.core.api.Task;
import com.robonobo.core.api.TaskListener;
import com.robonobo.gui.RoboFont;
import com.robonobo.gui.frames.RobonoboFrame;

@SuppressWarnings("serial")
public class TaskListContentPanel extends ContentPanel implements TaskListener {
	private Map<Integer, TaskPanel> tasks = new HashMap<Integer, TaskPanel>();
	private JPanel taskListPanel;

	public TaskListContentPanel(RobonoboFrame frame) {
		this.frame = frame;
		double[][] cellSizen = { { 10, TableLayout.FILL, 10 }, { 10, TableLayout.FILL, 10 } };
		setLayout(new TableLayout(cellSizen));

		taskListPanel = new JPanel();
		taskListPanel.setLayout(new BoxLayout(taskListPanel, BoxLayout.Y_AXIS));
		add(new JScrollPane(taskListPanel, JScrollPane.VERTICAL_SCROLLBAR_NEVER,
				JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS), "1,1");
		frame.getController().addTaskListener(this);
	}

	@Override
	public void taskUpdated(Task t) {
		if (tasks.containsKey(t.getId()))
			tasks.get(t.getId()).taskUpdated(t);
		else {
			TaskPanel p = new TaskPanel(t);
			tasks.put(t.getId(), p);
			taskListPanel.add(p);
		}
	}

	public void removeTask(Task t) {
		TaskPanel p = tasks.remove(t.getId());
		if (p != null)
			taskListPanel.remove(p);
	}

	class TaskPanel extends JPanel {
		Task t;
		JLabel titleLbl, statusLbl;
		JProgressBar progBar;
		private JButton cancelBtn;
		private JButton goPauseBtn;

		public TaskPanel(Task task) {
			this.t = task;

			double[][] cellSizen = { { 10, 50, 10, TableLayout.FILL, 10, 30, 10, 30, 10 }, { 10, 25, 10, 25, 10 } };
			setLayout(new TableLayout(cellSizen));

			titleLbl = new JLabel(t.getTitle());
			add(titleLbl, "1,1,3,1,l,c");

			statusLbl = new JLabel(t.getStatusText());
			add(statusLbl, "1,3");

			progBar = new JProgressBar(0, 100);
			progBar.setStringPainted(true);
			int pcnt = (int) (100 * t.getCompletion());
			progBar.setValue(pcnt);
			progBar.setString(pcnt + "%");
			add(progBar, "3,3");

			cancelBtn = new JButton("Cancel");
			cancelBtn.setFont(RoboFont.getFont(12, true));
			cancelBtn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if ((t.getCompletion() - 1f) == 0f) {
						removeTask(t);
					} else {
						t.cancel();
						cancelBtn.setEnabled(false);
					}
				}
			});
			add(cancelBtn, "5,3");

			goPauseBtn = new PlayPauseButton();
			add(goPauseBtn, "7,3");
		}

		void taskUpdated(final Task t) {
			this.t = t;
			SwingUtilities.invokeLater(new CatchingRunnable() {
				public void doRun() throws Exception {
					titleLbl.setText(t.getTitle());
					statusLbl.setText(t.getStatusText());
					int pcnt = (int) (100 * t.getCompletion());
					progBar.setValue(pcnt);
					progBar.setString(pcnt + "%");
					if (pcnt == 100) {
						cancelBtn.setText("Clear");
						goPauseBtn.setEnabled(false);
					}									
				}
			});
		}

		class PlayPauseButton extends JButton {
			boolean doPlay = false;

			public PlayPauseButton() {
				super("Pause");
				setFont(RoboFont.getFont(12, true));
				addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						if (doPlay) {
							t.resume();
							doPlay = false;
							setText("Pause");
						} else {
							t.pause();
							doPlay = true;
							setText("Resume");
						}
					}
				});
			}
		}
	}

}
