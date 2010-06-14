package com.robonobo.gui;

import java.util.Date;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.robonobo.common.concurrent.CatchingRunnable;
import com.robonobo.common.util.TimeUtil;

/**
 * If the set text is too big, will do a scrolling-marquee thingie
 * 
 * @author macavity
 * 
 */
public class MarqueeLabel extends JLabel {
	/** Millisecs */
	private static final int WAIT_TIME_AT_START = 3000;
	/** Time to advance marquee by one character - millisecs */
	private static final int TIME_BETWEEN_MOVES = 200;
	private static final String SPACER = "   |   ";

	private ScheduledThreadPoolExecutor executor;
	String completeText;
	String scrollText;
	String displayText;
	int textPos = 0;
	Date waitUntil;
	Future<?> updateTask = null;

	Log log = LogFactory.getLog(getClass());

	public MarqueeLabel(String text, ScheduledThreadPoolExecutor executor) {
		this.executor = executor;
		setText(text);
	}

	@Override
	public synchronized void setText(String text) {
		if(log == null) { // Called by superclass constructor
			super.setText(text);
			return;
		}
		if (updateTask != null) {
			updateTask.cancel(true);
		}
		if (textWouldOverflow(text)) {
			completeText = text;
			scrollText = completeText + SPACER;
			textPos = 0;
			waitUntil = TimeUtil.timeInFuture(WAIT_TIME_AT_START);
			displayText = getWidestFittingString(textPos);
			SwingUtilities.invokeLater(new CatchingRunnable() {
				public void doRun() throws Exception {
					setDisplayText(displayText);
				}
			});
			updateTask = executor.scheduleWithFixedDelay(new TextUpdater(), WAIT_TIME_AT_START, TIME_BETWEEN_MOVES, TimeUnit.MILLISECONDS);
		} else {
			// This fits normally, so we behave like a normal label
			completeText = text;
			displayText = text;
			super.setText(text);
		}
	}

	private synchronized void setDisplayText(String text) {
		super.setText(text);
	}

	@Override
	public String getToolTipText() {
		// Display tooltip only when text overflows
		if (textWouldOverflow(completeText)) {
			return completeText;
		}
		return null;
	}

	private boolean textWouldOverflow(String text) {
		if (text == null || getFont() == null)
			return false;
		int textWidth = getFontMetrics(getFont()).stringWidth(text);
		return textWidth > getSize().width;
	}

	private String getWidestFittingString(int startIndex) {
		// Could do lots of buggering about with modulo indices, but easiest
		// to just concat the string with itself and grab the substring from
		// that
		String doubleText = scrollText + scrollText;
		String goodStr = "";
		for (int i = 1; i < scrollText.length(); i++) {
			String tryText = doubleText.substring(startIndex, (startIndex + i));
			if (textWouldOverflow(tryText)) {
				return goodStr;
			}
			goodStr = tryText;
		}
		// Should never get here
		return completeText;
	}

	private class TextUpdater extends CatchingRunnable {
		@Override
		public void doRun() throws Exception {
			synchronized (MarqueeLabel.this) {
				if (waitUntil != null && TimeUtil.now().before(waitUntil)) {
					return;
				}
				textPos++;
				if (textPos >= scrollText.length()) {
					textPos = 0;
					// When we get back to the start, pause for a while before
					// we start scrolling again
					waitUntil = TimeUtil.timeInFuture(WAIT_TIME_AT_START);
				}
				displayText = getWidestFittingString(textPos);
				SwingUtilities.invokeLater(new CatchingRunnable() {
					public void doRun() throws Exception {
						setDisplayText(displayText);
					}
				});
			}
		}
	}
}
