package com.robonobo.oldgui;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.KeyListener;

import javax.swing.JComponent;
import javax.swing.TransferHandler;
import javax.swing.event.ListSelectionListener;

import com.robonobo.core.api.model.CloudTrack;
import com.robonobo.core.api.model.Track;
import com.robonobo.gui.model.SearchResultTableModel;
import com.robonobo.gui.model.StreamTransfer;

@SuppressWarnings("serial")
public class SearchResultTablePanel extends TrackListTablePanel {
	public SearchResultTablePanel(RobonoboFrame frame, SearchResultTableModel model, ListSelectionListener selectionListener,
			KeyListener keyListener) {
		super(frame, model, selectionListener, keyListener);
		setupDnD();
	}

	@Override
	public String getNextTrack(String lastTrackStreamId) {
		SearchResultTableModel amtModel = (SearchResultTableModel) model;
		int modelIndex = amtModel.getTrackIndex(lastTrackStreamId);
		if (modelIndex < 0)
			return null;
		int tblIndex = table.convertRowIndexToView(modelIndex);
		if (tblIndex >= table.getRowCount() - 1)
			return null;
		int nextModelIndex = table.convertRowIndexToModel(tblIndex + 1);
		Track t = amtModel.getTrack(nextModelIndex);
		if(t instanceof CloudTrack) {
			// If we have no sources, keep going...
			if(((CloudTrack)t).getNumSources() == 0)
				return getNextTrack(t.getStream().getStreamId());
		}
		return t.getStream().getStreamId();
	}
	
	private void setupDnD() {
		table.setDragEnabled(true);
		table.setTransferHandler(new MyTransferHandler());
	}
	
	private class MyTransferHandler extends TransferHandler {
		@Override
		public int getSourceActions(JComponent comp) {
			return COPY;
		}

		@Override
		protected Transferable createTransferable(JComponent comp) {
			return new StreamTransfer(getSelectedStreamIds());
		}

		@Override
		public boolean canImport(JComponent comp, DataFlavor[] transferFlavors) {
			return false;
		}
	}

}
