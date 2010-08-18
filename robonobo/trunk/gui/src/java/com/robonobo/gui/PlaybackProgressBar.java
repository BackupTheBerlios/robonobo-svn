package com.robonobo.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

public class PlaybackProgressBar extends JProgressBar implements IPlaybackProgressBar {

	private static final long serialVersionUID = 1L;
	private static final int DEFAULT_THUMB_WIDTH = 65;
	private static final int MAXIMUM_PROGRESS_VALUE = 35999;
	
	private int sliderThumbWidth;
	private boolean dragging;
	private List<Listener> listeners;

	private JButton sliderThumb;
	private JLabel startLabel;
	private JLabel endLabel;
	
	/**
	 * Constructor: do some initialization
	 */
	public PlaybackProgressBar() {
		super();
		setName("robonobo.playback.progressbar");
		listeners = new ArrayList<Listener>();
		setMinimum(0);
		setMaximum(MAXIMUM_PROGRESS_VALUE);
		
		// layout the progress bar
		setLayout(null);
		
		sliderThumbWidth = DEFAULT_THUMB_WIDTH;
		sliderThumb = new JButton();
		sliderThumb.setName("robonobo.playback.progressbar.thumb");
		sliderThumb.setFocusable(false);
		sliderThumb.setLocation(0, 0);
		add(sliderThumb);
		
		startLabel = new JLabel("0:00");
		startLabel.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
		startLabel.setSize(startLabel.getPreferredSize());
		startLabel.setForeground(Color.WHITE);
		add(startLabel);
		
		endLabel = new JLabel("9:59:59");
		endLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8));
		endLabel.setSize(endLabel.getPreferredSize());
		add(endLabel);
		
		// component resized
		addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent e) {
				// auto adjust thumb size
				sliderThumb.setSize(new Dimension(sliderThumbWidth, getHeight()));
				// update the thumb's position
				updateThumbPosition();
				// auto adjust the labels' position
				startLabel.setLocation(0, (getHeight() - startLabel.getHeight()) / 2);
				endLabel.setLocation(getWidth() - endLabel.getWidth(), (getHeight() - endLabel.getHeight()) / 2);
			}
		});
		
		// mouse event processing
		sliderThumb.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				dragging = true;
			}
			public void mouseReleased(MouseEvent e) {
				if (dragging) {
					dragging = false;
					for (Listener l : listeners) {	// notify listeners
						l.sliderFinishedMoving();
					}
				}
			}
		});
		sliderThumb.addMouseMotionListener(new MouseMotionAdapter() {
			public void mouseDragged(MouseEvent e) {
				final Point pos = SwingUtilities.convertPoint(sliderThumb, e.getPoint(), PlaybackProgressBar.this);
				int curValue = pos.x * MAXIMUM_PROGRESS_VALUE / getWidth();
				if (curValue < 0) {
					curValue = 0;
				} else if (curValue > MAXIMUM_PROGRESS_VALUE) {
					curValue = MAXIMUM_PROGRESS_VALUE;
				}
				setValue(curValue);
			}
		});
	}
	
	private void updateThumbPosition() {
		final int halfThumbWidth = sliderThumbWidth / 2;
		int curPos = getWidth() * getValue() / MAXIMUM_PROGRESS_VALUE - halfThumbWidth;
		if (curPos < 0) {
			curPos = 0;
		} else if (curPos > getWidth() - sliderThumbWidth) {
			curPos = getWidth() - sliderThumbWidth;
		}
		sliderThumb.setLocation(curPos, 0);
	}

	/**
	 * Override this method to avoid using vertical orientation.
	 */
	public void setOrientation(int newOrientation) {
		if (newOrientation != JProgressBar.HORIZONTAL) {
			throw new RuntimeException("PlaybackProgressBar only support horizontal orientation");
		}
	}
	
	/**
	 * Override this method to avoid using unexpected maximum value.
	 */
	public void setMaximum(int n) {
		if (n > MAXIMUM_PROGRESS_VALUE) {
			throw new RuntimeException("PlaybackProgressBar's maximum value must equal or less than " + MAXIMUM_PROGRESS_VALUE);
		}
		super.setMaximum(n);
	}

	/**
	 * Override this method to avoid using unexpected minimum value.
	 */
	public void setMinimum(int n) {
		if (n != 0) {
			throw new RuntimeException("PlaybackProgressBar's minimun value must be 0");
		}
		super.setMinimum(n);
	}

	/**
	 * Set the progress value and update the thumb position
	 */
	public void setValue(int n) {
		if (n != getValue()) {
			super.setValue(n);
			// move the thumb
			updateThumbPosition();
			// notify listeners
			for (Listener l : listeners) {
				l.sliderMoved(n);
			}
		}
	}

	/**
	 * Get the width of slider thumb
	 * 
	 * @return the width
	 */
	public int getSliderThumbWidth() {
		return sliderThumbWidth;
	}

	/**
	 * Set the width of slider thumb
	 * 
	 * @param sliderThumbWidth
	 */
	public void setSliderThumbWidth(int sliderThumbWidth) {
		this.sliderThumbWidth = sliderThumbWidth;
	}

	/** IPlaybackProgressBar */
	public void addListener(Listener listener) {
		listeners.add(listener);
	}

	public void removeListener(Listener listener) {
		listeners.remove(listener);
	}

	public int getProgress() {
		return getValue();
	}

	public void setProgress(int progress) {
		setValue(progress);
	}

	public void setEndText(String text) {
		endLabel.setText(text);
	}

	public void setSliderText(String text) {
		sliderThumb.setText(text);
	}

	public void setStartText(String text) {
		startLabel.setText(text);
	}
}