package com.robonobo.oldgui;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.KeyListener;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.event.ListSelectionListener;

import com.robonobo.common.concurrent.CatchingRunnable;
import com.robonobo.gui.model.PlaylistTableModel;

@SuppressWarnings("serial")
public class FriendPlaylistTablePanel extends MyPlaylistTablePanel {

	public FriendPlaylistTablePanel(RobonoboFrame frame, PlaylistTableModel model,
			ListSelectionListener selectionListener, KeyListener keyListener) {
		super(frame, model, selectionListener, keyListener);
	}

	/**
	 * We need to keep re-selecting the playlist tree node, as it loses focus
	 * (quite annoying!)
	 */
	void reselectTreeNode() {
		SwingUtilities.invokeLater(new CatchingRunnable() {
			public void doRun() throws Exception {
				frame.getLeftSidebar().selectMyMusic();
				frame.getLeftSidebar().selectFriendPlaylist(tm.getP());
			}
		});
	}

	@Override
	protected TransferHandler createTransferHandler() {
		return new TransferHandler() {
			public int getSourceActions(JComponent c) {
				if (anyStreamsSelected())
					return COPY;
				return NONE;
			}

			@Override
			protected Transferable createTransferable(JComponent c) {
				return new StreamTransfer(getSelectedStreamIds());
			}

			@Override
			public boolean canImport(JComponent comp, DataFlavor[] transferFlavors) {
				return false;
			}
		};
	}
	@Override
	protected void setupKeyHandler() {
		// Do nothing
	}
}
