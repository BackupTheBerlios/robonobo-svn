package com.robonobo.gui.panels;

import java.awt.Component;
import java.util.Enumeration;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SwingConstants;
import javax.swing.RowSorter.SortKey;
import javax.swing.event.RowSorterEvent;
import javax.swing.event.RowSorterListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import org.debian.tablelayout.TableLayout;

import com.robonobo.gui.RobonoboFont;
import com.robonobo.gui.components.DownloadStatusProgressBar;
import com.robonobo.gui.frames.RobonoboFrame;

@SuppressWarnings("serial")
public class TrackTablePanel extends JPanel {
	public TrackTablePanel() {
		double[][] cellSizen = { { TableLayout.FILL }, { TableLayout.FILL } };
		setLayout(new TableLayout(cellSizen));
		setBorder(BorderFactory.createEmptyBorder(0, 1, 0, 1));
		final DefaultTableModel tableModel = new DefaultTableModel(new Object[] { "Title", "Album", "Track", "Year", "Album", "Time", "Status", "Comment" }, 0);
		final JTable playList = new JTable(tableModel);
		playList.setFont(RobonoboFont.getFont(12, false));
		playList.getTableHeader().setFont(RobonoboFont.getFont(12, true));
		final JScrollPane middleScroller = new JScrollPane(playList, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		add(middleScroller, "0,0");
		playList.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		playList.getTableHeader().setReorderingAllowed(false);
		playList.getColumnModel().getColumn(0).setPreferredWidth(180);
		playList.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
				Component result = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
				((JComponent)result).setBorder(BorderFactory.createEmptyBorder(1, 5, 1, 1));
				result.setFont(RobonoboFont.getFont(12, true));
				return result;
			}
		});
		playList.getColumnModel().getColumn(1).setPreferredWidth(130);
		playList.getColumnModel().getColumn(2).setPreferredWidth(70);
		playList.getColumnModel().getColumn(3).setPreferredWidth(55);
		playList.getColumnModel().getColumn(4).setPreferredWidth(135);
		playList.getColumnModel().getColumn(5).setPreferredWidth(50);
		playList.getColumnModel().getColumn(6).setPreferredWidth(200);
		playList.getColumnModel().getColumn(7).setPreferredWidth(1000);
		playList.getColumnModel().getColumn(6).setCellRenderer(new DownloadProgressCellRenderer());
		tableModel.insertRow(0, new Object[] { "Mi Tierra", "Guitarra de Pasion", "01 of 18", "2005", "Guitarra de Pasion", "4:24", "Sharing" });
		tableModel.insertRow(1, new Object[] { "EI Sueno", "Guitarra de Pasion", "02 of 18", "2005", "Guitarra de Pasion", "5:06", "Sharing" });
		tableModel.insertRow(2, new Object[] { "EI Bambuquero", "Guitarra de Pasion", "03 of 18", "2005", "Guitarra de Pasion", "3:50", "Sharing" });
		tableModel.insertRow(3, new Object[] { "EI Ultimo Baile", "Guitarra de Pasion", "04 of 18", "2005", "Guitarra de Pasion", "4:29", "Sharing" });
		tableModel.insertRow(4, new Object[] { "Los Primos", "Guitarra de Pasion", "05 of 18", "2005", "Guitarra de Pasion", "4:48", "Sharing" });
		tableModel.insertRow(5, new Object[] { "Juntos", "Guitarra de Pasion", "06 of 18", "2005", "Guitarra de Pasion", "4:12", "Sharing" });
		tableModel.insertRow(6, new Object[] { "Alma Libre", "Guitarra de Pasion", "07 of 18", "2005", "Guitarra de Pasion", "4:58", new Integer(-1) });
		tableModel.insertRow(7, new Object[] { "Cafe Colombia", "Guitarra de Pasion", "08 of 18", "2005", "Guitarra de Pasion", "4:14", "Sharing" });
		tableModel.insertRow(8, new Object[] { "Anoche", "Guitarra de Pasion", "09 of 18", "2005", "Guitarra de Pasion", "6:43", new Integer(25) });
		tableModel
				.insertRow(9, new Object[] { "La Cumbia y la Luna", "Guitarra de Pasion", "10 of 18", "2005", "Guitarra de Pasion", "4:46", new Integer(69) });
		tableModel.insertRow(10, new Object[] { "Los Bandidos", "Guitarra de Pasion", "11 of 18", "2005", "Guitarra de Pasion", "5:39", "Sharing" });
		tableModel.insertRow(11, new Object[] { "Los Bandidos2", "Guitarra de Pasion", "12 of 18", "2005", "Guitarra de Pasion", "5:39", "Sharing" });
		tableModel.insertRow(12, new Object[] { "Los Bandidos3", "Guitarra de Pasion", "13 of 18", "2005", "Guitarra de Pasion", "5:39", "Sharing" });
		tableModel.insertRow(13, new Object[] { "Los Bandidos4", "Guitarra de Pasion", "14 of 18", "2005", "Guitarra de Pasion", "5:39", "Sharing" });
		tableModel.insertRow(14, new Object[] { "Los Bandidos5", "Guitarra de Pasion", "15 of 18", "2005", "Guitarra de Pasion", "5:39", "Sharing" });
		tableModel.insertRow(15, new Object[] { "Los Bandidos6", "Guitarra de Pasion", "16 of 18", "2005", "Guitarra de Pasion", "5:39", "Sharing" });
		tableModel.insertRow(16, new Object[] { "Los Bandidos7", "Guitarra de Pasion", "17 of 18", "2005", "Guitarra de Pasion", "5:39", "Sharing" });
		tableModel.insertRow(17, new Object[] { "Los Bandidos8", "Guitarra de Pasion", "18 of 18", "2005", "Guitarra de Pasion", "5:39", "Sharing" });

		// Code for column sorting (JRE 6+ needed)
		final TableRowSorter<TableModel> sorter = new TableRowSorter<TableModel>(tableModel);
		playList.setRowSorter(sorter);
		sorter.addRowSorterListener(new RowSorterListener() {
			public void sorterChanged(RowSorterEvent e) {
				if (e.getType() == RowSorterEvent.Type.SORT_ORDER_CHANGED) {
					// set header renderer when sorting by column
					final Enumeration<TableColumn> columns = playList.getColumnModel().getColumns();
					while (columns.hasMoreElements()) {
						final TableColumn col = columns.nextElement();
						col.setHeaderRenderer(new SortableHeaderRenderer());
					}
				}
			}
		});
	}
	
	private class DownloadProgressCellRenderer extends DefaultTableCellRenderer {
		private JProgressBar pBar;
		private JPanel pnl;
		
		public DownloadProgressCellRenderer() {
			pBar = new DownloadStatusProgressBar();
			pnl = new JPanel();
			pnl.setLayout(new BoxLayout(pnl, BoxLayout.X_AXIS));
			pnl.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
			pnl.setOpaque(false);
			pnl.add(pBar);
		}
		
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			if (value instanceof Integer) {
				pBar.setMinimum(0);
				pBar.setMaximum(100);
				final int curVal = ((Integer) value).intValue();
				if (curVal == -1) {
					pBar.setValue(0);
					pBar.setEnabled(false);
					pBar.setString("queued");
				} else {
					pBar.setValue(curVal);
					pBar.setEnabled(true);
					pBar.setString("Downloading (1): "+curVal+"%");
				}
				return pnl;
			}
			Component result = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			return result;
		}
	}

	// Sortable Table Header Renderer
	private static class SortableHeaderRenderer extends DefaultTableCellRenderer {
		private static final long serialVersionUID = 1L;

		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			setFont(RobonoboFont.getFont(12, false));
			setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
			final RowSorter<?> sorter = table.getRowSorter();
			if (sorter != null) {
				final List<? extends SortKey> sortKeys = sorter.getSortKeys();
				if (sortKeys.size() > 0) {
					final SortKey sk = sortKeys.get(0);
					if (column == sk.getColumn()) {
						if (sk.getSortOrder() == SortOrder.ASCENDING) {
							setIcon(new ImageIcon(RobonoboFrame.class.getResource("/img/icon/arrow_up.png")));
						} else if (sk.getSortOrder() == SortOrder.DESCENDING) {
							setIcon(new ImageIcon(RobonoboFrame.class.getResource("/img/icon/arrow_down.png")));
						} else {
							setIcon(null);
						}
					}
				}
			}
			setHorizontalTextPosition(SwingConstants.LEFT);
			return this;
		}
	}
}
