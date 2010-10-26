package com.robonobo.oldgui;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.event.ListSelectionListener;

import org.jdesktop.swingx.table.TableColumnModelExt;

import com.robonobo.common.concurrent.CatchingRunnable;
import com.robonobo.common.exceptions.SeekInnerCalmException;
import com.robonobo.core.Platform;
import com.robonobo.core.api.model.Stream;
import com.robonobo.gui.model.PlaylistTableModel;

@SuppressWarnings("serial")
public class MyPlaylistTablePanel extends TrackListTablePanel {
	PlaylistTableModel tm;

	public MyPlaylistTablePanel(RobonoboFrame frame, PlaylistTableModel model,
			ListSelectionListener selectionListener, KeyListener keyListener) {
		super(frame, model, selectionListener, keyListener);
		this.tm = model;
		// Because we're hiding track and year by default, expand other cols to take up their space
		TableColumnModelExt cm = (TableColumnModelExt) table.getColumnModel();
		cm.getColumn(1).setPreferredWidth(210); // Title
		cm.getColumn(2).setPreferredWidth(160); // Artist
		cm.getColumn(3).setPreferredWidth(180); // Album
		table.setSortable(false);
		setupDnD();
		setupKeyHandler();
	}

	@Override
	/** For playlists, by default hide track and year as well as streamid */
	protected int[] hiddenCols() {
		return new int[]{ 4, 5, 11 };
	}
	
	/**
	 * We need to keep re-selecting the playlist tree node, as it loses focus
	 * (quite annoying!)
	 */
	void reselectTreeNode() {
		SwingUtilities.invokeLater(new CatchingRunnable() {
			public void doRun() throws Exception {
				frame.getLeftSidebar().selectMyMusic();
				frame.getLeftSidebar().selectMyPlaylist(tm.getPlaylist());
			}
		});
	}

	protected void setupDnD() {
		table.setTransferHandler(createTransferHandler());
		table.setDragEnabled(true);
	}

	protected TransferHandler createTransferHandler() {
		return new TransferHandler() {
			public int getSourceActions(JComponent c) {
				if (anyStreamsSelected())
					return MOVE;
				return NONE;
			}

			@Override
			protected Transferable createTransferable(JComponent c) {
				return new StreamTransfer(getSelectedStreamIds());
			}

			@Override
			public boolean canImport(JComponent comp, DataFlavor[] transferFlavors) {
				for (DataFlavor dataFlavor : transferFlavors) {
					if (dataFlavor.equals(StreamTransfer.DATA_FLAVOR))
						return true;
				}
				return Platform.getPlatform().canDnDImport(transferFlavors);
			}

			@SuppressWarnings("unchecked")
			@Override
			public boolean importData(JComponent comp, Transferable t) {
				try {
					// If we have a mouse location, drop things there, otherwise
					// at the end
					int mouseRow = (table.getMousePosition() == null) ? -1 : table.rowAtPoint(table.getMousePosition());
					final int insertRow = (mouseRow >= 0) ? mouseRow : tm.getRowCount();
					boolean transferFromRobo = false;
					for (DataFlavor flavor : t.getTransferDataFlavors()) {
						if (flavor.equals(StreamTransfer.DATA_FLAVOR)) {
							transferFromRobo = true;
							break;
						}
					}
					if (transferFromRobo) {
						// DnD streams from inside robonobo
						List<String> streamIds;
						try {
							streamIds = (List<String>) t.getTransferData(StreamTransfer.DATA_FLAVOR);
						} catch (Exception e) {
							throw new SeekInnerCalmException();
						}
						tm.addStreams(streamIds, insertRow);
						reselectTreeNode();
						return true;
					} else {
						// DnD files from somewhere else
						frame.updateStatus("Importing tracks...", 0, 30);
						List<File> importFiles = null;
						try {
							importFiles = Platform.getPlatform().getDnDImportFiles(t);
						} catch (IOException e) {
							log.error("Caught exception dropping files", e);
						}
						final Object[] stupidFinal = new Object[1];
						stupidFinal[0] = importFiles;
						CatchingRunnable importRunner = new CatchingRunnable() {
							@Override
							public void doRun() throws Exception {
								List<File> myList = (List<File>) stupidFinal[0];
								List<Stream> streams = frame.importFilesOrDirectories(myList);
								List<String> streamIds = new ArrayList<String>();
								for (Stream s : streams) {
									streamIds.add(s.getStreamId());
								}
								tm.addStreams(streamIds, insertRow);
								reselectTreeNode();
							}
						};
						if (frame.getController().getMyUser() != null)
							frame.getController().getExecutor().execute(importRunner);
						else
							frame.showLogin(importRunner);
						return true;
					}
				} catch (Exception e) {
					log.error("Error importing data", e);
					return false;
				}
			}
		};
	}
	
	protected void setupKeyHandler() {
		table.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_DELETE) {
					tm.removeStreamIds(getSelectedStreamIds());
					reselectTreeNode();
				}
			}
		});
	}
}
