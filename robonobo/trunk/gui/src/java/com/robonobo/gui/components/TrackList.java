package com.robonobo.gui.components;

import java.awt.*;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.*;
import org.jdesktop.swingx.decorator.SortOrder;
import org.jdesktop.swingx.table.TableColumnExt;
import org.jdesktop.swingx.table.TableColumnModelExt;

import com.robonobo.core.api.NextTrackListener;
import com.robonobo.core.api.SearchExecutor;
import com.robonobo.core.api.model.*;
import com.robonobo.core.api.model.DownloadingTrack.DownloadStatus;
import com.robonobo.core.api.model.Track.PlaybackStatus;
import com.robonobo.gui.GUIUtils;
import com.robonobo.gui.RoboFont;
import com.robonobo.gui.frames.RobonoboFrame;
import com.robonobo.gui.model.TrackListTableModel;

@SuppressWarnings("serial")
public class TrackList extends JPanel implements SearchExecutor, NextTrackListener {
	JScrollPane scrollPane;
	JXTable table;
	TrackListTableModel model;
	// TODO: Have a separate starting icon
	ImageIcon startingIcon = GUIUtils.createImageIcon("/img/table/play.png", null);
	ImageIcon playingIcon = GUIUtils.createImageIcon("/img/table/play.png", null);
	ImageIcon pausedIcon = GUIUtils.createImageIcon("/img/table/pause.png", null);
	ImageIcon downloadingIcon = GUIUtils.createImageIcon("/img/table/download.png", null);
	Log log;
	RobonoboFrame frame;
	
	public TrackList(RobonoboFrame frame, TrackListTableModel model, ListSelectionListener selectionListener, KeyListener keyListener) {
		this.model = model;
		this.frame = frame;
		log = LogFactory.getLog(getClass());
		setLayout(new GridLayout(1, 0));
		setBorder(BorderFactory.createEmptyBorder(0, 1, 0, 1));
		table = new JXTable(model);
		table.setFont(RoboFont.getFont(12, false));
		table.setRowHeight(21);
		table.setColumnControlVisible(true);
		table.setHorizontalScrollEnabled(true);
		table.setFillsViewportHeight(true);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		table.setBackground(Color.WHITE);
		table.setHighlighters(HighlighterFactory.createSimpleStriping());
		table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
		if (selectionListener != null)
			table.getSelectionModel().addListSelectionListener(selectionListener);
		
		// Cell renderers
		table.getColumn(0).setCellRenderer(new PlaybackStatusRenderer());
		TextRenderer tr = new TextRenderer();
		table.getColumn(1).setCellRenderer(tr);
		table.getColumn(2).setCellRenderer(tr);
		table.getColumn(3).setCellRenderer(tr);
		table.getColumn(4).setCellRenderer(tr);
		table.getColumn(5).setCellRenderer(tr);
		table.getColumn(6).setCellRenderer(tr);
		table.getColumn(7).setCellRenderer(new TransferStatusCellRenderer());
		table.getColumn(8).setCellRenderer(tr);
		table.getColumn(9).setCellRenderer(tr);
		table.getColumn(10).setCellRenderer(tr);
		table.getColumn(11).setCellRenderer(tr);
		
		if(keyListener != null)
			table.addKeyListener(keyListener);
		
		// Render table header as not bold
		table.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
			ImageIcon ascSortIcon = GUIUtils.createImageIcon("/img/icon/arrow_up.png", null);
			ImageIcon descSortIcon = GUIUtils.createImageIcon("/img/icon/arrow_down.png", null);
			
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
					boolean hasFocus, int row, int column) {
				JLabel result = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
				result.setFont(RoboFont.getFont(12, false));
				result.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 0));
				SortOrder so = TrackList.this.table.getSortOrder(column);
				if(so.isSorted()) {
					if(so.isAscending())
						setIcon(ascSortIcon);
					else
						setIcon(descSortIcon);
				} else
					result.setIcon(null);
				result.setHorizontalTextPosition(SwingConstants.LEFT);
				return result;
			}
		});
		
		TableColumnModelExt cm = (TableColumnModelExt) table.getColumnModel();
		cm.getColumn(0).setPreferredWidth(22); // Status icon
		cm.getColumn(1).setPreferredWidth(187); // Title
		cm.getColumn(2).setPreferredWidth(137); // Artist
		cm.getColumn(3).setPreferredWidth(155); // Album
		cm.getColumn(4).setPreferredWidth(44); // Track
		cm.getColumn(5).setPreferredWidth(40); // Year
		cm.getColumn(6).setPreferredWidth(47); // Time
		cm.getColumn(7).setPreferredWidth(160); // Status
		cm.getColumn(8).setPreferredWidth(80); // Download
		cm.getColumn(9).setPreferredWidth(80); // Upload
		cm.getColumn(10).setPreferredWidth(60); // Size
		cm.getColumn(11).setPreferredWidth(120); // Stream Id

		int[] hiddenCols = hiddenCols();
		List<TableColumn> cols = cm.getColumns(true);
		for(int i=0;i<hiddenCols.length;i++) {
			TableColumnExt colExt = (TableColumnExt) cols.get(hiddenCols[i]);
			colExt.setVisible(false);
		}
		scrollPane = new JScrollPane(table);
		add(scrollPane, "0,0");
	}

	// By default we just hide the stream id, subclasses hide more
	protected int[] hiddenCols() {
		return new int[]{ 11 } ;
	}
	
	public void search(String query) {
		table.clearSelection();
		if (query == null || query.length() == 0) {
			table.setFilters(null);
		} else {
			final String lcq = query.toLowerCase();
			// Only include rows that have a matching title, artist, album or
			// year
			final int[] cols = { 1, 2, 3, 7 };
			table.setFilters(new FilterPipeline(new MultiColumnPatternFilter(lcq, 0, cols)));
		}
	}

	public JTable getJTable() {
		return table;
	}

	public boolean anyStreamsSelected() {
		return (table.getSelectedRows().length > 0);
	}

	public List<String> getSelectedStreamIds() {
		int[] selRows = getSelectedRowsAsPerModel();
		List<String> result = new ArrayList<String>(selRows.length);
		for (int row : selRows) {
			String sid = model.getStreamId(row);
			if(sid != null)
				result.add(sid);
		}
		return result;
	}

	public List<Track> getSelectedTracks() {
		int[] selRows = getSelectedRowsAsPerModel();
		List<Track> result = new ArrayList<Track>(selRows.length);
		for (int row : selRows) {
			// This might be null if we are in the middle of deleting rows
			Track t = model.getTrack(row);
			if(t != null)
				result.add(t);
		}
		return result;
	}

	public void clearTableSelection() {
		table.removeRowSelectionInterval(0, table.getRowCount() - 1);
	}

	public String getNextTrack(String lastTrackStreamId) {
		int modelIndex = model.getTrackIndex(lastTrackStreamId);
		if (modelIndex < 0)
			return null;
		int tblIndex = table.convertRowIndexToView(modelIndex);
		if (tblIndex >= table.getRowCount() - 1)
			return null;
		int nextModelIndex = table.convertRowIndexToModel(tblIndex + 1);
		return model.getStreamId(nextModelIndex);
	}

	public void scrollTableToStream(String streamId) {
		int modelIndex = model.getTrackIndex(streamId);
		if (modelIndex < 0)
			return;
		int viewIndex = table.convertRowIndexToView(modelIndex);
		if (viewIndex < 0 || viewIndex >= table.getRowCount() - 1)
			return;
		table.scrollRowToVisible(viewIndex);
	}

	protected String getStreamIdAtRow(int viewIndex) {
		int modelIndex = table.convertRowIndexToModel(viewIndex);
		return model.getStreamId(modelIndex);
	}

	public int[] getSelectedRowsAsPerModel() {
		int[] tableRows = table.getSelectedRows();
		int[] modelRows = new int[tableRows.length];
		for (int i = 0; i < tableRows.length; i++) {
			modelRows[i] = table.convertRowIndexToModel(tableRows[i]);
		}
		return modelRows;
	}

	class TextRenderer extends DefaultTableCellRenderer {
		Font plainFont = RoboFont.getFont(12, false);
		Font boldFont = RoboFont.getFont(12, true);
		
		public Component getTableCellRendererComponent(
				JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			Component result = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			((JComponent) result).setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 0));
			if(column == 1)
				result.setFont(boldFont);
			else
				result.setFont(plainFont);
			return result;
		}
	}

	class PlaybackStatusRenderer extends DefaultTableCellRenderer {
		@Override
		public Component getTableCellRendererComponent(
				JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			lbl.setText(null);
			lbl.setHorizontalAlignment(JLabel.CENTER);
			if (value == null) {
				lbl.setIcon(null);
				return lbl;
			}
			PlaybackStatus status = (PlaybackStatus) value;
			switch (status) {
			case Starting:
				lbl.setIcon(startingIcon);
				break;
			case Playing:
				lbl.setIcon(playingIcon);
				break;
			case Paused:
				lbl.setIcon(pausedIcon);
				break;
			case Downloading:
				lbl.setIcon(downloadingIcon);
				break;
			default:
				lbl.setIcon(null);
				break;
			}
			return lbl;
		}
	}

	private class TransferStatusCellRenderer extends DefaultTableCellRenderer {
		JProgressBar pBar;
		JPanel pBarPnl;
		Border lblBorder = BorderFactory.createEmptyBorder(0, 5, 0, 5);

		public TransferStatusCellRenderer() {
			pBar = new DownloadStatusProgressBar();
			pBar.setMinimum(0);
			pBar.setMaximum(100);
			pBarPnl = new JPanel();
			pBarPnl.setLayout(new BoxLayout(pBarPnl, BoxLayout.X_AXIS));
			pBarPnl.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
			pBarPnl.setOpaque(true);
			pBarPnl.add(pBar);
		}

		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
				boolean hasFocus, int row, int column) {
			JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			if (value instanceof DownloadingTransferStatus) {
				DownloadingTransferStatus dStat = (DownloadingTransferStatus) value;
				// Show a progress bar with how much we're downloading
				String streamId = getStreamIdAtRow(row);
				DownloadingTrack d = (DownloadingTrack) model.getTrack(model.getTrackIndex(streamId));
				if (d == null)
					return new JLabel();
				long streamSz = d.getStream().getSize();
				float complete = (float) d.getBytesDownloaded() / streamSz;
				int pcnt = (int) (100 * complete);
				int numSources = d.getNumSources();
				if (d.getDownloadStatus() == DownloadStatus.Paused) {
					pBar.setValue(0);
					pBar.setEnabled(false);
					pBar.setString("queued");
				} else {
					pBar.setValue(pcnt);
					pBar.setEnabled(true);
					pBar.setString("Downloading ("+numSources+"): " + pcnt + "%");
				}
				Color bg = lbl.getBackground();
				pBarPnl.setBackground(bg);
				return pBarPnl;
			} else {
				lbl.setBorder(lblBorder);
				return lbl;
			}
		}
	}

	class MultiColumnPatternFilter extends PatternFilter {
		private int[] cols;

		/** cols are column indices as per model */
		public MultiColumnPatternFilter(String pattern, int matchFlags, int[] cols) {
			setPattern(pattern, matchFlags);
			this.cols = cols;
		}

		@Override
		public boolean test(int row) {
			if (pattern == null)
				return false;
			boolean result = false;
			for (int col : cols) {
				if (adapter.isTestable(col)) {
					String text = getInputString(row, col).toLowerCase();
					if (text != null && (text.length() > 0)) {
						Matcher m = pattern.matcher(text);
						if (m.find()) {
							result = true;
							break;
						}
					}
				}
			}
			return result;
		}
	}
}