package com.robonobo.gui.panels;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;

import javax.swing.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.debian.tablelayout.TableLayout;

import com.robonobo.gui.components.TrackList;
import com.robonobo.gui.frames.RobonoboFrame;
import com.robonobo.gui.model.StreamTransfer;
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

	public ContentPanel() {
	}

	public ContentPanel(RobonoboFrame frame, TrackListTableModel tableModel) {
		this.frame = frame;
		double[][] cellSizen = { { TableLayout.FILL }, { TableLayout.FILL, 5, 175 } };
		setLayout(new TableLayout(cellSizen));
		trackList = new TrackList(frame, tableModel);
		trackList.getJTable().setDragEnabled(true);
		trackList.getJTable().setTransferHandler(createTrackListTransferHandler());
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

	/**
	 * For dropping stuff onto the tracklist - default impl can't import nothin
	 */
	public boolean canImport(JComponent comp, DataFlavor[] transferFlavors) {
		return false;
	}

	/**
	 * For dropping onto the track list - default impl does nothing (and in fact will never get called)
	 */
	public boolean importData(JComponent comp, Transferable t) {
		return false;
	}

	private TransferHandler createTrackListTransferHandler() {
		return new TransferHandler() {
			@Override
			public int getSourceActions(JComponent c) {
				return COPY;
			}

			@Override
			protected Transferable createTransferable(JComponent c) {
				return new StreamTransfer(trackList.getSelectedStreamIds());
			}

			@Override
			public boolean canImport(JComponent comp, DataFlavor[] transferFlavors) {
				return ContentPanel.this.canImport(comp, transferFlavors);
			}

			@Override
			public boolean importData(JComponent comp, Transferable t) {
				return ContentPanel.this.importData(comp, t);
			}
		};
	}
}
