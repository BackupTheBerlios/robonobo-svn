package com.robonobo.gui.components;

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

import com.robonobo.gui.RobonoboFont;

/**
 * Maintains two 'progress' indicators. The first, 'availableData', displays how much data we have downloaded by means of a light blue bar, and is the limit to
 * how far we can seek by dragging the slider. The second, 'trackPosition', is our playback position within the track and displayed by means of the slider
 * position.
 * 
 * @author macavity
 * 
 */
@SuppressWarnings("serial")
public class PlaybackProgressBar extends JProgressBar {
	private static final int SLIDER_TOTAL_WIDTH = 65;
	private static final int SLIDER_OPAQUE_WIDTH = 62;

//	private int sliderThumbWidth;
	private boolean dragging;
	/** In reference to the slider thumb */
	private Point mouseDownPt;
	private List<Listener> listeners;

	private JButton sliderThumb;
	private JLabel endLabel;

	private long trackLengthMs;
	private long trackPositionMs;
	private float dataAvailable;

	public PlaybackProgressBar(int maxValue) {
		super();
		setName("robonobo.playback.progressbar");
		listeners = new ArrayList<Listener>();
		setMinimum(0);
		setMaximum(maxValue);

		// Absolute positioning of elements
		setLayout(null);

		sliderThumb = new JButton();
		sliderThumb.setName("robonobo.playback.progressbar.thumb");
		sliderThumb.setFont(RobonoboFont.getFont(11, false));
		sliderThumb.setFocusable(false);
		sliderThumb.setLocation(0, 0);
		add(sliderThumb);

		endLabel = new JLabel();
		endLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8));
		endLabel.setSize(endLabel.getPreferredSize());
		endLabel.setFont(RobonoboFont.getFont(12, false));
		add(endLabel);

		addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent e) {
				// auto adjust thumb size
				sliderThumb.setSize(new Dimension(SLIDER_TOTAL_WIDTH, getHeight()));
				// update the thumb's position
				setTrackPosition(0);
				// auto adjust the labels' position
				endLabel.setLocation(getWidth() - endLabel.getWidth(), (getHeight() - endLabel.getHeight()) / 2);
			}
		});

		// mouse event processing
		sliderThumb.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				dragging = true;
				mouseDownPt = e.getPoint();
			}

			public void mouseReleased(MouseEvent e) {
				if (dragging) {
					dragging = false;
					mouseDownPt = null;
					for (Listener l : listeners) {
						// notify listeners
						// TODO do this via the thread pool...
						l.sliderReleased(trackPositionMs);
					}
				}
			}
		});
		sliderThumb.addMouseMotionListener(new MouseMotionAdapter() {
			public void mouseDragged(MouseEvent e) {
				// Get position relative to progress bar
				Point progressBarPos = SwingUtilities.convertPoint(sliderThumb, e.getPoint(), PlaybackProgressBar.this);
				// Take into account which part of the slider they're dragging by using mouseDownPt
				int thumbX = progressBarPos.x - mouseDownPt.x;
				// thumbX <= 0, trackPosition = 0
				// thumbX >= maximum - thumbWidth, trackPosition = trackLength
				int maxX = getMaximum() - SLIDER_TOTAL_WIDTH;
				float relPos = (float) thumbX / maxX;
				if(relPos > dataAvailable)
					relPos = dataAvailable;
				long trackPos = (long) (relPos * trackLengthMs);
				if(trackPos < 0)
					trackPos = 0;
				if(trackPos > trackLengthMs)
					trackPos = trackLengthMs;
				setTrackPosition(trackPos);
			}
		});		
	}

	/**
	 * pos = 0, trackPosition = 0 <br/>
	 * pos = (maximum - thumbWidth), trackPosition = trackLength
	 */
	private void setThumbPosition(int pos) {
		if (pos < 0)
			pos = 0;
		if (pos > (getMaximum() - SLIDER_OPAQUE_WIDTH))
			pos = getMaximum() - SLIDER_OPAQUE_WIDTH;
		sliderThumb.setLocation(pos, 0);
	}

	public void setOrientation(int newOrientation) {
		if (newOrientation != JProgressBar.HORIZONTAL)
			throw new RuntimeException("PlaybackProgressBar only support horizontal orientation");
	}

	/** IPlaybackProgressBar */
	public void addListener(Listener listener) {
		listeners.add(listener);
	}

	public void removeListener(Listener listener) {
		listeners.remove(listener);
	}

	public void setTrackLength(long lengthMs) {
		this.trackLengthMs = lengthMs;
		setEndText(timeLblFromMs(lengthMs));
	}

	public void setTrackPosition(long positionMs) {
		trackPositionMs = positionMs;
		// pos = 0, trackPosition = 0
		// pos = (maximum - thumbWidth), trackPosition = trackLength
		int thumbPos = (int) ((getMaximum() - SLIDER_OPAQUE_WIDTH) * ((float) positionMs / trackLengthMs));
		setSliderText(timeLblFromMs(positionMs));
		setThumbPosition(thumbPos);
	}

	public void setDataAvailable(float available) {
		// Colour in progress bar value to illustrate seek limit - take into account thumb width
		this.dataAvailable = available;
		int val = (int) (SLIDER_OPAQUE_WIDTH + (available * getMaximum()));
		setValue(val);
	}

	private void setEndText(String text) {
		endLabel.setText(text);
		endLabel.setSize(endLabel.getPreferredSize());
		endLabel.setLocation(getWidth() - endLabel.getWidth(), (getHeight() - endLabel.getHeight()) / 2);
	}

	private void setSliderText(String text) {
		sliderThumb.setText(text);
	}

	private String timeLblFromMs(long ms) {
		int totalSec = (int) (ms / 1000);
		int hours = totalSec / 3600;
		int minutes = (totalSec % 3600) / 60;
		int seconds = (totalSec % 60);
		if (hours > 0)
			return String.format("%d:%d:%02d", hours, minutes, seconds);
		else
			return String.format("%d:%02d", minutes, seconds);
	}

	public interface Listener {
		public void sliderReleased(long trackPositionMs);
	}
}