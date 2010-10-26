package com.robonobo.gui.panels;

import javax.swing.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.debian.tablelayout.TableLayout;

import com.robonobo.gui.components.TrackList;
import com.robonobo.gui.frames.RobonoboFrame;
import com.robonobo.gui.model.TrackListTableModel;

/**
 * The track list and the tabs below
 */
@SuppressWarnings("serial")
public abstract class ContentPanel extends JPanel {
	protected TrackList trackList;
	protected JTabbedPane tabPane;
	protected RobonoboFrame frame;
	protected Log log = LogFactory.getLog(getClass());
	
	public ContentPanel(RobonoboFrame frame, TrackListTableModel tableModel) {
		this.frame = frame;
		double[][] cellSizen = { { TableLayout.FILL }, { TableLayout.FILL, 5, 175 } };
		setLayout(new TableLayout(cellSizen));
		trackList = new TrackList(frame, tableModel);
		add(trackList, "0,0");
		tabPane = new JTabbedPane(JTabbedPane.TOP);
		tabPane.setBorder(BorderFactory.createEmptyBorder(0, 1, 0, 1));
		add(tabPane, "0,2");
		// TODO Add the track details once we have identifiable tracks...
		tabPane.addTab("track", new JPanel());
		tabPane.setEnabledAt(0, false);
	}	
	
	public TrackList getTrackList() {
		return trackList;
	}
}
