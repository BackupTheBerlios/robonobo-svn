package com.robonobo.gui.laf;

import java.awt.Component;
import java.awt.GridLayout;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.table.TableColumnExt;
import org.jdesktop.swingx.table.TableColumnModelExt;

@SuppressWarnings("serial")
public class TrackListPanel extends JPanel {
	ImageIcon playingIcon = GuiUtil.createImageIcon("/img/tracktable/playing.png", "Playing");
	ImageIcon pausedIcon = GuiUtil.createImageIcon("/img/tracktable/paused.png", "Playing");
	ImageIcon queuedIcon = GuiUtil.createImageIcon("/img/tracktable/queued.png", "Playing");
	ImageIcon downloadingIcon = GuiUtil.createImageIcon("/img/tracktable/downloading.png", "Playing");
	
	public TrackListPanel() {
		setLayout(new GridLayout(1, 0));
		JXTable table = new JXTable(new TrackListTableModel());
		table.setColumnControlVisible(true);
		table.setHorizontalScrollEnabled(true);
		table.setFillsViewportHeight(true);
		table.setDefaultRenderer(String.class, new TextRenderer());
		table.setDefaultRenderer(Integer.class, new TextRenderer());
		table.getColumn(0).setCellRenderer(new PlaybackStatusRenderer());
		table.getColumn(7).setCellRenderer(new TransferStatusRenderer());
		TableColumnModelExt cm = (TableColumnModelExt) table.getColumnModel();
		cm.getColumn(0).setPreferredWidth(30); // Status icon
		cm.getColumn(1).setPreferredWidth(180); // Title
		cm.getColumn(2).setPreferredWidth(130); // Artist
		cm.getColumn(3).setPreferredWidth(150); // Album
		cm.getColumn(4).setPreferredWidth(50); // Track
		cm.getColumn(5).setPreferredWidth(40); // Year
		cm.getColumn(6).setPreferredWidth(65); // Duration
		cm.getColumn(7).setPreferredWidth(160); // Status
		cm.getColumn(8).setPreferredWidth(80); // Download
		cm.getColumn(9).setPreferredWidth(80); // Upload
		cm.getColumn(10).setPreferredWidth(60); // Size
		cm.getColumn(11).setPreferredWidth(120); // Stream Id
		// Hide stream id
		((TableColumnExt)cm.getColumn(11)).setVisible(false);

		add(new JScrollPane(table), "0,0");
	}

	private class TrackListTableModel extends AbstractTableModel {
		String[] colNames = { " "/* StatusIcon */, "Title", "Artist", "Album", "Track", "Year", "Duration", "Status",
				"Download", "Upload", "Size", "Stream Id" };
		String[] trackNames = { "Mi Tierra", "El Sueno", "El Bambuquero", "El Ultimo Baile", "Los Primos", "Juntos",
				"Alma Libre", "Cafe Colombia", "Anoche", "La Cumbia y la Luna", "Los Bandidos" };
		String artist = "Rodriego y Gabriela";
		String album = "Guitarra de Pasion";
		String year = "2005";
		String[] times = { "4:24", "5:06", "3:50", "4:29", "4:48", "4:12", "4:58", "4:14", "6:43", "4:46", "5:39" };

		@Override
		public String getColumnName(int column) {
			return colNames[column];
		}

		@Override
		public int getColumnCount() {
			return colNames.length;
		}

		@Override
		public int getRowCount() {
			return trackNames.length;
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			switch (columnIndex) {
			case 0:
				return PlaybackStatus.class;
			case 1:
			case 2:
			case 3:
			case 5:
			case 7:
			case 9:
			case 10:
			case 11:
				return String.class;
			case 4:
				return Integer.class;
			default:
				return Object.class;
			}
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			switch(columnIndex) {
			case 1:
				return trackNames[rowIndex];
			case 2:
				return artist;
			case 3:
				return album;
			case 4:
				return (rowIndex+1)+" of "+trackNames.length;
			case 5:
				return year;
			case 6:
				return times[rowIndex];
			default:
				return null;
			}
		}
	}
	
	class TextRenderer extends DefaultTableCellRenderer {
		@Override
		public Component getTableCellRendererComponent(
				JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			if (column <= 3 || column >= 8)
				lbl.setHorizontalAlignment(JLabel.LEFT);
			else
				lbl.setHorizontalAlignment(JLabel.RIGHT);
			lbl.setVerticalAlignment(CENTER);
			lbl.setVerticalTextPosition(CENTER);
			lbl.setAlignmentX(0.5f);
			lbl.setAlignmentY(0.5f);
			return lbl;
		}
	}
	
	class PlaybackStatusRenderer extends DefaultTableCellRenderer {
		@Override
		public Component getTableCellRendererComponent(
				JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			lbl.setText(null);
			lbl.setHorizontalAlignment(JLabel.CENTER);
			switch(row) {
			case 4:
				lbl.setIcon(pausedIcon);
				break;
			case 6:
				lbl.setIcon(queuedIcon);
				break;
			case 8:
			case 9:
				lbl.setIcon(downloadingIcon);
				break;
			default:
				lbl.setIcon(null);
				break;
			}
			return lbl;
		}
	}

	class TransferStatusRenderer extends DefaultTableCellRenderer {
		@Override
		public Component getTableCellRendererComponent(
				JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			switch(row) {
			case 6:
				JProgressBar pbar = new JProgressBar();
				pbar.setEnabled(false);
				pbar.setStringPainted(true);
				pbar.setString("Queued");
				return pbar;
			case 8:
				pbar = new JProgressBar(0,100);
				pbar.setValue(25);
				pbar.setStringPainted(true);
				pbar.setString("Downloading 25%");
				return pbar;
			case 9:
				pbar = new JProgressBar(0,100);
				pbar.setValue(69);
				pbar.setStringPainted(true);
				pbar.setString("Downloading 69%");
				return pbar;
			default:
				JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
				lbl.setHorizontalAlignment(JLabel.LEFT);
				lbl.setText("Sharing");
				return lbl;
			}
		}
	}
	
	enum PlaybackStatus {
		Paused,
		Playing,
		Queued,
		Downloading
	}
}
