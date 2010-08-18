package com.robonobo.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.RowSorter.SortKey;
import javax.swing.event.RowSorterEvent;
import javax.swing.event.RowSorterListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import com.robonobo.gui.laf.RobonoboLookAndFeel;

public class Main {

	public static void main(String[] args) {

		// Extract the .ttf files if needed
//		final File fontsDir = new File(System.getProperty("java.home") + "/lib/fonts");
//		if (fontsDir.exists() && fontsDir.isDirectory()) {
//			final File veraFile = new File(fontsDir, "Vera.ttf");
//			if (!veraFile.exists()) {
//				extractFileFromResource("Vera.ttf", veraFile);
//			}
//			final File veraBdFile = new File(fontsDir, "VeraBd.ttf");
//			if (!veraBdFile.exists()) {
//				extractFileFromResource("VeraBd.ttf", veraBdFile);
//			}
//			final File veraBIFile = new File(fontsDir, "VeraBI.ttf");
//			if (!veraBIFile.exists()) {
//				extractFileFromResource("VeraBI.ttf", veraBIFile);
//			}
//			final File veraItFile = new File(fontsDir, "VeraIt.ttf");
//			if (!veraItFile.exists()) {
//				extractFileFromResource("VeraIt.ttf", veraItFile);
//			}
//		}

		// Set look and feel
		try {
			UIManager.setLookAndFeel(new RobonoboLookAndFeel());
		} catch (UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}

		final JFrame mainFrame = new JFrame("Test");
		mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		// MenuBar
		final JMenuBar menuBar = new JMenuBar();
		mainFrame.setJMenuBar(menuBar);

		final JMenu fileMenu = new JMenu("File");
		menuBar.add(fileMenu);
		fileMenu.add(new JMenuItem("Open"));
		fileMenu.add(new JMenuItem("Save"));
		fileMenu.add(new JMenuItem("Save As..."));
		final JMenuItem closeItem = new JMenuItem("Close");
		closeItem.setEnabled(false);
		fileMenu.add(closeItem);
		fileMenu.addSeparator();
		fileMenu.add(new JMenuItem("Quit"));

		final JMenu networkMenu = new JMenu("Network");
		menuBar.add(networkMenu);
		final JRadioButtonMenuItem connectItem = new JRadioButtonMenuItem("Connect", true);
		networkMenu.add(connectItem);
		final JRadioButtonMenuItem disconnectItem = new JRadioButtonMenuItem("Disconnect", false);
		networkMenu.add(disconnectItem);
		final ButtonGroup connectGroup = new ButtonGroup();
		connectGroup.add(connectItem);
		connectGroup.add(disconnectItem);
		final JRadioButtonMenuItem directLinkItem = new JRadioButtonMenuItem("Direct Link", true);
		directLinkItem.setEnabled(false);
		directLinkItem.setSelected(true);
		networkMenu.add(directLinkItem);
		final JRadioButtonMenuItem useProxyItem = new JRadioButtonMenuItem("Use Proxy", false);
		useProxyItem.setEnabled(false);
		networkMenu.add(useProxyItem);

		final JMenu optionsMenu = new JMenu("Options");
		menuBar.add(optionsMenu);
		optionsMenu.add(new JCheckBoxMenuItem("Random", true));
		optionsMenu.add(new JCheckBoxMenuItem("Loop"));
		final JCheckBoxMenuItem reverseItem = new JCheckBoxMenuItem("Reverse", true);
		reverseItem.setEnabled(false);
		optionsMenu.add(reverseItem);
		final JCheckBoxMenuItem noDuplicatedItem = new JCheckBoxMenuItem("No Duplicated", false);
		noDuplicatedItem.setEnabled(false);
		optionsMenu.add(noDuplicatedItem);

		final JMenu debugMenu = new JMenu("Debug");
		menuBar.add(debugMenu);
		final JMenu debugModeMenu = new JMenu("Mode");
		debugMenu.add(debugModeMenu);
		debugModeMenu.add(new JMenuItem("Mode A"));
		debugModeMenu.add(new JMenuItem("Mode B"));
		final JMenu logMenu = new JMenu("Log...");
		logMenu.setEnabled(false);
		debugMenu.add(logMenu);

		final JMenu helpMenu = new JMenu("Help");
		helpMenu.setEnabled(false);
		menuBar.add(helpMenu);

		// main layout
		final JPanel mainPanel = new JPanel(new BorderLayout());
		mainFrame.setContentPane(mainPanel);

		// left panel
		final JPanel leftPanel = new JPanel();
		mainPanel.add(leftPanel, BorderLayout.WEST);
		leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
		leftPanel.setOpaque(true);
		leftPanel.setBackground(new Color(28, 28, 28));
		leftPanel.setPreferredSize(new Dimension(200, 700));
		leftPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		final JPanel musicLibPanel = new JPanel(new BorderLayout());
		leftPanel.add(musicLibPanel);
		musicLibPanel.setOpaque(false);
		musicLibPanel.setPreferredSize(new Dimension(200, 30));
		musicLibPanel.setMaximumSize(new Dimension(200, 30));
		final JLabel musicLibLabel = new JLabel(" Music Library");
		musicLibLabel.setForeground(Color.WHITE);
		musicLibPanel.add(musicLibLabel, BorderLayout.WEST);
		final JLabel plusLabel = new JLabel("[+]");
		plusLabel.setForeground(Color.BLUE);
		plusLabel.setFont(new Font("Bitstream Vera Sans", Font.PLAIN, 20));
		musicLibPanel.add(plusLabel, BorderLayout.EAST);

		final JPanel treeListView = new JPanel();
		final JScrollPane treeListScroller = new JScrollPane(treeListView, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		leftPanel.add(treeListScroller);
		treeListScroller.getViewport().getView().setBackground(Color.WHITE);
		treeListView.setLayout(new BoxLayout(treeListView, BoxLayout.Y_AXIS));
		final JLabel everyOneMusicLabel = new JLabel("Everyone's Music");
		treeListView.add(everyOneMusicLabel);
		everyOneMusicLabel.setName("robonobo.white.bar");
		everyOneMusicLabel.setMinimumSize(new Dimension(180, 40));
		everyOneMusicLabel.setPreferredSize(new Dimension(180, 40));
		everyOneMusicLabel.setMaximumSize(new Dimension(180, 40));
		everyOneMusicLabel.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
		final DefaultMutableTreeNode treeRoot = new DefaultMutableTreeNode("Friends");
		final JTree everyOneMusicTree = new JTree(new DefaultTreeModel(treeRoot));
		everyOneMusicTree.setName("robonobo.playlist.tree");
		treeListView.add(everyOneMusicTree);
		everyOneMusicTree.setRootVisible(true);
		final DefaultMutableTreeNode geffensNode = new DefaultMutableTreeNode("Geffen's");
		treeRoot.insert(geffensNode, 0);
		geffensNode.insert(new DefaultMutableTreeNode("No music found"), 0);
		final DefaultMutableTreeNode willsNode = new DefaultMutableTreeNode("Will's");
		treeRoot.insert(willsNode, 1);
		willsNode.insert(new DefaultMutableTreeNode("Playlist 001 [over]"), 0);
		willsNode.insert(new DefaultMutableTreeNode("Playlist For That Night"), 1);
		willsNode.insert(new DefaultMutableTreeNode("I Hate This One"), 2);
		everyOneMusicTree.expandPath(new TreePath(willsNode.getPath()));
		everyOneMusicTree.setCellRenderer(new DefaultTreeCellRenderer() {
			private static final long serialVersionUID = 1L;

			public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
				final JLabel rdr = (JLabel) super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
				final TreeNode node = (TreeNode) value;
				if (!node.isLeaf()) {
					rdr.setIcon(new ImageIcon(Main.class.getResource("/img/icon/playlist_people.png")));
				} else {
					rdr.setIcon(new ImageIcon(Main.class.getResource("/img/icon/music_icon.png")));
				}
				return rdr;
			}

			public void paint(Graphics g) {
				paintComponent(g);
			}
		});
		everyOneMusicTree.setMinimumSize(new Dimension(175, 130));
		everyOneMusicTree.setPreferredSize(new Dimension(175, 130));
		everyOneMusicTree.setAlignmentX(0.0f);
		everyOneMusicTree.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		everyOneMusicTree.setMaximumSize(new Dimension(65535, 65535));
		final JLabel myMusicLabel = new JLabel("My Music");
		treeListView.add(myMusicLabel);
		myMusicLabel.setName("robonobo.black.bar");
		myMusicLabel.setForeground(Color.white);
		myMusicLabel.setMinimumSize(new Dimension(180, 40));
		myMusicLabel.setPreferredSize(new Dimension(180, 40));
		myMusicLabel.setMaximumSize(new Dimension(180, 40));
		myMusicLabel.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
		final JList myMusicList = new JList(new Object[] { "New Playlist", "Playlist 001", "Playlist For That Night", "I Hate This One", "DJ Mix FIFE!~" });
		treeListView.add(myMusicList);
		myMusicList.setName("robonobo.playlist.list");
		myMusicList.setCellRenderer(new DefaultListCellRenderer() {
			private static final long serialVersionUID = 1L;

			public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
				final JLabel rdr = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				rdr.setIcon(new ImageIcon(Main.class.getResource("/img/icon/music_icon.png")));
				rdr.setBorder(null);
				return rdr;
			}
		});
		myMusicList.setAlignmentX(0.0f);
		myMusicList.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		myMusicList.setMaximumSize(new Dimension(65535, 65535));

		final JLabel bigLabel = new JLabel("345.00");
		leftPanel.add(bigLabel);
		bigLabel.setPreferredSize(new Dimension(200, 50));
		bigLabel.setForeground(Color.ORANGE);
		bigLabel.setFont(new Font("Bitstream Vera Sans", Font.PLAIN, 22));
		bigLabel.setAlignmentX(0.5f);

		final JLabel statusLabel = new JLabel("<html>Status:<b>Connected</b></html>");
		leftPanel.add(statusLabel);
		statusLabel.setPreferredSize(new Dimension(200, 25));
		statusLabel.setAlignmentX(0.5f);
		statusLabel.setOpaque(true);
		statusLabel.setBackground(Color.LIGHT_GRAY);
		statusLabel.setHorizontalAlignment(JLabel.CENTER);
		statusLabel.setBorder(BorderFactory.createLoweredBevelBorder());

		final JLabel networkLabel = new JLabel("<html><font color=white style='font-size:9pt;'>22/108 Linked Connections<br>25k up / 5k down</font></html>");
		leftPanel.add(networkLabel);
		networkLabel.setPreferredSize(new Dimension(200, 50));
		networkLabel.setAlignmentX(0.5f);
		networkLabel.setIcon(new ImageIcon(Main.class.getResource("/img/icon/earth.png")));

		// right panel (top + middle + bottom)
		final JPanel rightPanel = new JPanel(new BorderLayout(10, 10));
		mainPanel.add(rightPanel, BorderLayout.CENTER);
		rightPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		final JPanel topPanel = new JPanel(new BorderLayout());
		rightPanel.add(topPanel, BorderLayout.NORTH);
		topPanel.setPreferredSize(new Dimension(800, 100));
		topPanel.setName("playback.background.panel");
		topPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		topPanel.setBackground(Color.LIGHT_GRAY);
		final JPanel titlesPanel = new JPanel();
		titlesPanel.setLayout(new BoxLayout(titlesPanel, BoxLayout.PAGE_AXIS));
		titlesPanel.setOpaque(false);
		topPanel.add(titlesPanel, BorderLayout.CENTER);
		final JLabel headTitleLabel = new JLabel("Buenas Tardes Amigo (5:32)");
		headTitleLabel.setPreferredSize(new Dimension(450, 40));
		headTitleLabel.setMinimumSize(new Dimension(450, 40));
		headTitleLabel.setMaximumSize(new Dimension(450, 40));
		headTitleLabel.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
		headTitleLabel.setFont(new Font("Bitstream Vera Sans", Font.PLAIN, 24));
		headTitleLabel.setForeground(new Color(0, 0, 128));
		titlesPanel.add(headTitleLabel);
		final JLabel subTitleLabel = new JLabel("Ween / Don't Shit Where You Eat");
		subTitleLabel.setPreferredSize(new Dimension(450, 20));
		subTitleLabel.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
		subTitleLabel.setFont(new Font("Bitstream Vera Sans", Font.PLAIN, 18));
		titlesPanel.add(subTitleLabel);
		final JPanel playerPanel = new JPanel(new BorderLayout(5, 5));
		topPanel.add(playerPanel, BorderLayout.EAST);
		playerPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		final PlaybackProgressBar progressBar = new PlaybackProgressBar();
		progressBar.setPreferredSize(new Dimension(305, 24));
		progressBar.addListener(new IPlaybackProgressBar.Listener() {
			public void sliderFinishedMoving() {
			}

			public void sliderMoved(int newProgress) {
				final int totalSec = progressBar.getValue();
				final int hours = totalSec / 3600;
				final int minutes = (totalSec % 3600) / 60;
				final int seconds = (totalSec % 60);
				if (hours > 0) {
					progressBar.setSliderText(String.format("%d:%02d:%02d", hours, minutes, seconds));
				} else {
					progressBar.setSliderText(String.format("%02d:%02d", minutes, seconds));
				}
			}
		});
		progressBar.setValue(18000);
		playerPanel.add(progressBar, BorderLayout.NORTH);
		final JPanel playerCtrlPanel = new JPanel(new BorderLayout());
		playerPanel.add(playerCtrlPanel, BorderLayout.CENTER);
		playerCtrlPanel.setOpaque(false);
		final JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
		playerCtrlPanel.add(buttonsPanel, BorderLayout.WEST);
		buttonsPanel.setOpaque(false);
		final JButton backButton = new JButton();
		backButton.setName("robonobo.round.button");
		backButton.setIcon(new ImageIcon(Main.class.getResource("/img/icon/play_back.png")));
		backButton.setPreferredSize(new Dimension(50, 50));
		buttonsPanel.add(backButton);
		final JButton ejectButton = new JButton();
		ejectButton.setName("robonobo.round.button");
		ejectButton.setIcon(new ImageIcon(Main.class.getResource("/img/icon/play_eject.png")));
		ejectButton.setPreferredSize(new Dimension(50, 50));
		buttonsPanel.add(ejectButton);
		final JButton playButton = new JButton();
		playButton.setName("robonobo.round.button");
		playButton.setIcon(new ImageIcon(Main.class.getResource("/img/icon/play_play.png")));
		playButton.setPreferredSize(new Dimension(50, 50));
		buttonsPanel.add(playButton);
		final JButton nextButton = new JButton();
		nextButton.setName("robonobo.round.button");
		nextButton.setIcon(new ImageIcon(Main.class.getResource("/img/icon/play_next.png")));
		nextButton.setPreferredSize(new Dimension(50, 50));
		buttonsPanel.add(nextButton);
		buttonsPanel.add(Box.createHorizontalStrut(50));
		final JButton closeButton = new JButton();
		closeButton.setName("robonobo.exit.button");
		closeButton.setPreferredSize(new Dimension(40, 40));
		buttonsPanel.add(closeButton);

		final DefaultTableModel tableModel = new DefaultTableModel(new Object[] { "Title", "Alum", "Track", "Year", "Album", "Time", "Status", "Comment" }, 0);
		final JTable playList = new JTable(tableModel);
		final JScrollPane middleScroller = new JScrollPane(playList, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		rightPanel.add(middleScroller, BorderLayout.CENTER);
		middleScroller.setPreferredSize(new Dimension(800, 400));
		playList.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		playList.getTableHeader().setReorderingAllowed(false);
		playList.getColumnModel().getColumn(0).setPreferredWidth(180);
		playList.getColumnModel().getColumn(1).setPreferredWidth(130);
		playList.getColumnModel().getColumn(2).setPreferredWidth(70);
		playList.getColumnModel().getColumn(3).setPreferredWidth(55);
		playList.getColumnModel().getColumn(4).setPreferredWidth(135);
		playList.getColumnModel().getColumn(5).setPreferredWidth(50);
		playList.getColumnModel().getColumn(6).setPreferredWidth(200);
		playList.getColumnModel().getColumn(7).setPreferredWidth(1000);
		playList.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
			private static final long serialVersionUID = 1L;
			private JProgressBar progressRdr = new JProgressBar(JProgressBar.HORIZONTAL);

			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
				if (value instanceof Integer) {
					progressRdr.setMinimum(0);
					progressRdr.setMaximum(100);
					final int curVal = ((Integer) value).intValue();
					if (curVal == -1) {
						progressRdr.setValue(0);
						progressRdr.setEnabled(false);
					} else {
						progressRdr.setValue(curVal);
						progressRdr.setEnabled(true);
					}
					return progressRdr;

				}
				return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			}
		});
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

		final JPanel bottomPanel = new JPanel(new BorderLayout());
		rightPanel.add(bottomPanel, BorderLayout.SOUTH);
		bottomPanel.setPreferredSize(new Dimension(800, 175));
		bottomPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		final JTabbedPane tabs = new JTabbedPane(JTabbedPane.TOP);
		bottomPanel.add(tabs, BorderLayout.CENTER);
		final JPanel editPlaylistPanel = new JPanel();
		tabs.addTab("edit playlist", editPlaylistPanel);
		editPlaylistPanel.setPreferredSize(new java.awt.Dimension(800, 130));
		GridBagLayout editPlaylistPanelLayout = new GridBagLayout();
		editPlaylistPanelLayout.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0 };
		editPlaylistPanelLayout.rowHeights = new int[] { 30, 30, 30, 30 };
		editPlaylistPanelLayout.columnWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 };
		editPlaylistPanelLayout.columnWidths = new int[] { 115, 290, 90, 90, 90, 90 };
		editPlaylistPanel.setLayout(editPlaylistPanelLayout);
		final JLabel plistTitleLabel = new JLabel("Title:");
		editPlaylistPanel.add(plistTitleLabel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0,
				0, 0), 0, 0));
		final JTextField plistTitleEdit = new JTextField();
		plistTitleEdit.setOpaque(false);
		plistTitleEdit.addFocusListener(new FocusAdapter() {
			public void focusGained(FocusEvent e) {
				plistTitleEdit.repaint();
			}

			public void focusLost(FocusEvent e) {
				plistTitleEdit.repaint();
			}
		});
		editPlaylistPanel.add(plistTitleEdit, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(0, 0, 0, 10), 0, 0));
		final JLabel plistDescLabel = new JLabel("Description:");
		editPlaylistPanel.add(plistDescLabel, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0,
				0, 0), 0, 0));
		final JTextArea plistDescEdit = new JTextArea();
		plistDescEdit.setOpaque(false);
		plistDescEdit.addFocusListener(new FocusAdapter() {
			public void focusGained(FocusEvent e) {
				plistDescEdit.repaint();
			}

			public void focusLost(FocusEvent e) {
				plistDescEdit.repaint();
			}
		});
		final JScrollPane plistDescScroller = new JScrollPane(plistDescEdit, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		plistDescScroller.setOpaque(false);
		plistDescScroller.getViewport().setOpaque(false);
		editPlaylistPanel.add(plistDescScroller, new GridBagConstraints(1, 1, 1, 3, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0,
				0, 0, 10), 0, 0));
		final JCheckBox friendSeeCheckBox = new JCheckBox("Let friends see editPlaylistPanel playlist");
		friendSeeCheckBox.setSelected(true);
		editPlaylistPanel.add(friendSeeCheckBox, new GridBagConstraints(2, 1, 4, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(0, 5, 0, 0), 0, 0));
		final JCheckBox autoDownloadCheckBox = new JCheckBox("Download new tracks automatically");
		editPlaylistPanel.add(autoDownloadCheckBox, new GridBagConstraints(2, 2, 4, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(0, 5, 0, 0), 0, 0));
		final JButton saveButton = new JButton("SAVE");
		editPlaylistPanel.add(saveButton, new GridBagConstraints(2, 3, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 5, 0,
				0), 0, 0));
		final JButton shareButton = new JButton("SHARE");
		editPlaylistPanel.add(shareButton, new GridBagConstraints(3, 3, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 5, 0,
				0), 0, 0));
		final JButton sendButton = new JButton("SEND");
		editPlaylistPanel.add(sendButton, new GridBagConstraints(4, 3, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 5, 0,
				0), 0, 0));
		final JButton deleteButton = new JButton("DELETE");
		deleteButton.setName("robonobo.red.button");
		editPlaylistPanel.add(deleteButton, new GridBagConstraints(5, 3, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 5,
				0, 5), 0, 0));
		final JPanel futurePanel = new JPanel(new GridLayout(4, 2));
		tabs.addTab("future additions", futurePanel);
		futurePanel.add(new JRadioButton("Radio Button"));
		futurePanel.add(new JRadioButton("Selected Radio Button", true));
		final JRadioButton disabledRadioButton = new JRadioButton("Disabled Radio Button");
		disabledRadioButton.setEnabled(false);
		futurePanel.add(disabledRadioButton);
		final JRadioButton disabledSelectedRadioButton = new JRadioButton("Disabled Selected Radio Button", true);
		disabledSelectedRadioButton.setEnabled(false);
		futurePanel.add(disabledSelectedRadioButton);
		final JCheckBox disabledCheckBox = new JCheckBox("Disabled CheckBox");
		disabledCheckBox.setEnabled(false);
		futurePanel.add(disabledCheckBox);
		final JCheckBox disabledSelectedCheckBox = new JCheckBox("Disabled Selected CheckBox", true);
		disabledSelectedCheckBox.setEnabled(false);
		futurePanel.add(disabledSelectedCheckBox);
		futurePanel.add(new JButton("Normal Button"));
		final JButton disableButton = new JButton("Disabled Button");
		disableButton.setEnabled(false);
		futurePanel.add(disableButton);
		final JPanel dialogsPanel = new JPanel(new BorderLayout());
		tabs.addTab("dialog components", dialogsPanel);
		final JPanel topDlgPanel = new JPanel();
		dialogsPanel.add(topDlgPanel, BorderLayout.NORTH);
		final JTextField textEdit = new JTextField("Text Here");
		textEdit.setOpaque(false);
		textEdit.addFocusListener(new FocusAdapter() {
			public void focusGained(FocusEvent e) {
				textEdit.repaint();
			}

			public void focusLost(FocusEvent e) {
				textEdit.repaint();
			}
		});
		topDlgPanel.add(textEdit);
		final JButton searchButton = new JButton("Search");
		searchButton.setName("robonobo.small.round.button");
		topDlgPanel.add(searchButton);
		final JButton stopSearchButton = new JButton("Stop");
		stopSearchButton.setName("robonobo.small.round.button");
		stopSearchButton.setEnabled(false);
		topDlgPanel.add(stopSearchButton);
		final JComboBox combo1 = new JComboBox(new Object[] { "AAAAAA", "BBBBBB", "CCCCCC", "DDDDDD" });
		combo1.setPreferredSize(new Dimension(100, 30));
		topDlgPanel.add(combo1);
		final JComboBox combo2 = new JComboBox(new Object[] { "AAAAAA", "BBBBBB", "CCCCCC", "DDDDDD", "EEEEEE", "FFFFFF", "GGGGGG" });
		combo2.setEditable(true);
		combo2.setPreferredSize(new Dimension(100, 30));
		topDlgPanel.add(combo2);
		final JPanel treeListPanel = new JPanel(new GridLayout(1, 2));
		treeListPanel.setBorder(BorderFactory.createEtchedBorder());
		dialogsPanel.add(treeListPanel, BorderLayout.CENTER);
		final JList dlgList = new JList(new Object[] { "List Item 1", "List Item 2", "List Item 3", "List Item 4", "List Item 5" });
		dlgList.setCellRenderer(new DefaultListCellRenderer() {
			private static final long serialVersionUID = 1L;

			public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
				final JLabel rdr = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				final Font rdrFont = rdr.getFont();
				if (isSelected) {
					rdr.setFont(rdrFont.deriveFont(rdrFont.getStyle() | Font.BOLD));
				}
				return rdr;
			}
		});
		dlgList.setPreferredSize(new Dimension(100, 100));
		treeListPanel.add(new JScrollPane(dlgList, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS));
		final JTree dlgTree = new JTree();
		dlgTree.setPreferredSize(new Dimension(100, 100));
		treeListPanel.add(new JScrollPane(dlgTree, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS));

		mainFrame.setPreferredSize(new Dimension(1024, 723));
		mainFrame.pack();
		mainFrame.setLocationRelativeTo(null);
		mainFrame.setVisible(true);
	}

	/**
	 * Extract file from resource
	 * 
	 * @param resName
	 * @param filePath
	 */
	private static final void extractFileFromResource(String resName, File targetFile) {
		try {
			final byte[] buf = new byte[1024];
			final InputStream resIs = Main.class.getResourceAsStream(resName);
			if (resIs != null) {
				final FileOutputStream fileOs = new FileOutputStream(targetFile);
				int read = resIs.read(buf);
				while (read != -1) {
					fileOs.write(buf, 0, read);
					read = resIs.read(buf);
				}
				fileOs.close();
				resIs.close();
			} else {
				System.out.println("Resource not found: " + resName);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// Sortable Table Header Renderer
	private static class SortableHeaderRenderer extends DefaultTableCellRenderer {
		private static final long serialVersionUID = 1L;

		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			setFont(new Font("Bitstream Vera Sans", Font.BOLD, 11));
			setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
			final RowSorter<?> sorter = table.getRowSorter();
			if (sorter != null) {
				final List<? extends SortKey> sortKeys = sorter.getSortKeys();
				if (sortKeys.size() > 0) {
					final SortKey sk = sortKeys.get(0);
					if (column == sk.getColumn()) {
						if (sk.getSortOrder() == SortOrder.ASCENDING) {
							setIcon(new ImageIcon(Main.class.getResource("/img/icon/arrow_up.png")));
						} else if (sk.getSortOrder() == SortOrder.DESCENDING) {
							setIcon(new ImageIcon(Main.class.getResource("/img/icon/arrow_down.png")));
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