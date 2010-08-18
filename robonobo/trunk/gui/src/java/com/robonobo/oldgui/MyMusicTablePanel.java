package com.robonobo.oldgui;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.TransferHandler;
import javax.swing.event.ListSelectionListener;

import com.robonobo.common.concurrent.CatchingRunnable;
import com.robonobo.core.Platform;
import com.robonobo.core.RobonoboController;
import com.robonobo.core.api.model.DownloadingTrack;
import com.robonobo.core.api.model.SharedTrack;
import com.robonobo.core.api.model.Track;

@SuppressWarnings("serial")
public class MyMusicTablePanel extends TrackListTablePanel {

	public MyMusicTablePanel(RobonoboFrame frame, TrackListTableModel model, ListSelectionListener selectionListener,
			KeyListener keyListener) {
		super(frame, model, selectionListener, keyListener);
		setupKeyHandler();
		setupDnD();
	}

	private void setupKeyHandler() {
		table.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_DELETE) {
					final List<String> streamIds = getSelectedStreamIds();
					if (streamIds.size() > 0) {
						frame.getController().getExecutor().execute(new CatchingRunnable() {
							public void doRun() throws Exception {
								RobonoboController controller = frame.getController();
								for (String sid : streamIds) {
									Track t = controller.getTrack(sid);
									if(t instanceof SharedTrack)
										controller.deleteShare(sid);
									else if(t instanceof DownloadingTrack)
										controller.deleteDownload(sid);
								}
							}
						});
					}
				}
			}
		});
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
			return Platform.getPlatform().canDnDImport(transferFlavors);
		}

		@Override
		public boolean importData(JComponent comp, Transferable t) {
			frame.updateStatus("Importing tracks...", 0, 30);
			List<File> l = null;
			try {
				l = Platform.getPlatform().getDnDImportFiles(t);
			} catch (IOException e) {
				log.error("Caught exception dropping files", e);
			}
			final Object[] stupidFinal = new Object[1];
			stupidFinal[0] = l;
			CatchingRunnable importRunner = new CatchingRunnable() {
				@SuppressWarnings("unchecked")
				@Override
				public void doRun() throws Exception {
					List<File> importFiles = (List<File>) stupidFinal[0];
					frame.importFilesOrDirectories(importFiles);
				}
			};
			if (frame.getController().getMyUser() != null)
				frame.getController().getExecutor().execute(importRunner);
			else
				frame.showLogin(importRunner);
			return true;
		}
	}
}
