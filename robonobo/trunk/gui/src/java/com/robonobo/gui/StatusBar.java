package com.robonobo.gui;

import info.clearthought.layout.TableLayout;

import java.awt.Font;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;

import com.robonobo.common.concurrent.CatchingRunnable;
import com.robonobo.core.RobonoboController;

public class StatusBar extends JPanel {
	List<Item> items = new LinkedList<Item>();
	JLabel status;
	private RobonoboController controller;

	public StatusBar(RobonoboController controller) {
		this.controller = controller;
		double[][] cellSizen = { 
				{ 10, TableLayout.FILL },
				{ 5, TableLayout.FILL } 
		};
		setLayout(new TableLayout(cellSizen));
		status = new JLabel("");
		status.setFont(new Font("sans-serif", Font.PLAIN, 10));
		add(status, "1,1");
		controller.getExecutor().execute(new TextUpdateRunner());
	}

	/**
	 * @param minDisplayTime Seconds
	 * @param maxDisplayTime Seconds
	 */
	public synchronized void setText(String text, int minDisplayTime, int maxDisplayTime) {
		items.add(new Item(text, minDisplayTime, maxDisplayTime));
		notifyAll();
	}

	private class Item {
		String text;
		int minDisplayTime;
		int maxDisplayTime;
		public Item(String text, int minDisplayTime, int maxDisplayTime) {
			this.text = text;
			this.minDisplayTime = minDisplayTime;
			this.maxDisplayTime = maxDisplayTime;
		}
	}

	private class TextUpdateRunner extends CatchingRunnable {
		@Override
		public void doRun() throws Exception {
			synchronized (StatusBar.this) {
				while(true) {
					while(items.size() == 0) {
						status.setText("");
						StatusBar.this.wait();
					}
					Item item = items.remove(0);
					status.setText(item.text);
					StatusBar.this.wait(item.minDisplayTime*1000);
					if(items.size() > 0) {
						continue;
					}
					int secsToWait = item.maxDisplayTime - item.minDisplayTime;
					StatusBar.this.wait(secsToWait*1000);
				}
			}
		}
	}
}
