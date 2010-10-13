package com.robonobo.gui.panels;

import javax.swing.*;

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
	protected TrackListTableModel tableModel;
	
	public ContentPanel(RobonoboFrame frame, TrackListTableModel tableModel) {
		this.frame = frame;
		this.tableModel = tableModel;
		double[][] cellSizen = { { TableLayout.FILL }, { TableLayout.FILL, 5, 175 } };
		setLayout(new TableLayout(cellSizen));
		trackList = new TrackList(frame, tableModel, null, null);
		add(trackList, "0,0");
//		add(new DemoTrackTablePanel(), "0,0");
		tabPane = new JTabbedPane(JTabbedPane.TOP);
		tabPane.setBorder(BorderFactory.createEmptyBorder(0, 1, 0, 1));
		add(tabPane, "0,2");
		// TODO Add the track details once we have identifiable tracks...
		tabPane.addTab("track", new JPanel());
		tabPane.setEnabledAt(0, false);
	}	
	
	public TrackListTableModel getTableModel() {
		return tableModel;
	}
}
