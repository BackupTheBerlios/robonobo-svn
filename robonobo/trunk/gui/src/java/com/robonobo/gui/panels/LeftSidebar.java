package com.robonobo.gui.panels;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;

import com.robonobo.gui.RobonoboFont;
import com.robonobo.gui.components.ExpandoTree;
import com.robonobo.gui.frames.RobonoboFrame;

public class LeftSidebar extends JPanel {
	static final Color DARK_BG = new Color(28, 28, 28);
	static final Color GREY_BG = new Color(0xb2, 0xb5, 0xb9);
	
	public LeftSidebar() {
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setPreferredSize(new Dimension(200, 700));
		setBorder(BorderFactory.createEmptyBorder(11, 5, 5, 5));

		final JPanel treeListView = new JPanel();
		final JScrollPane treeListScroller = new JScrollPane(treeListView, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		add(treeListScroller);
		treeListScroller.getViewport().getView().setBackground(Color.WHITE);
		treeListView.setLayout(new BoxLayout(treeListView, BoxLayout.Y_AXIS));
//		treeListView.setBorder(BorderFactory.createLineBorder(GREY_BG, 5));
		treeListView.setBackground(GREY_BG);
		
		JPanel searchPanel = new JPanel();
		searchPanel.setLayout(new BoxLayout(searchPanel, BoxLayout.Y_AXIS));
		searchPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 0));
		searchPanel.setOpaque(true);
		searchPanel.setBackground(GREY_BG);
		searchPanel.setPreferredSize(new Dimension(175, 30));
		searchPanel.setMinimumSize(new Dimension(175, 30));
		searchPanel.setMaximumSize(new Dimension(175, 30));
		searchPanel.setAlignmentX(0f);
		JTextField searchField = new JTextField("Search...");
		searchField.setFont(RobonoboFont.getFont(10, false));
		searchField.setPreferredSize(new Dimension(165, 25));
		searchField.setMinimumSize(new Dimension(165, 25));
		searchField.setMaximumSize(new Dimension(165, 25));
		searchPanel.add(searchField);
		treeListView.add(searchPanel);
		
		final DefaultMutableTreeNode treeRoot = new DefaultMutableTreeNode("Friends");
		final JTree everyOneMusicTree = new ExpandoTree(new DefaultTreeModel(treeRoot));
		everyOneMusicTree.setFont(RobonoboFont.getFont(11, false));
		everyOneMusicTree.setName("robonobo.playlist.tree");
		treeListView.add(everyOneMusicTree);
		everyOneMusicTree.setRootVisible(true);
		final DefaultMutableTreeNode geffensNode = new DefaultMutableTreeNode("Joe Geffen");
		treeRoot.insert(geffensNode, 0);
		geffensNode.insert(new DefaultMutableTreeNode("No music found"), 0);
		final DefaultMutableTreeNode willsNode = new DefaultMutableTreeNode("Will Morton");
		treeRoot.insert(willsNode, 1);
		willsNode.insert(new DefaultMutableTreeNode("Playlist 001 [over]"), 0);
		willsNode.insert(new DefaultMutableTreeNode("Playlist For That Night"), 1);
		willsNode.insert(new DefaultMutableTreeNode("I Hate This One"), 2);
//		everyOneMusicTree.expandPath(new TreePath(willsNode.getPath()));
		everyOneMusicTree.setCellRenderer(new DefaultTreeCellRenderer() {
			private static final long serialVersionUID = 1L;

			public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
				final JLabel rdr = (JLabel) super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
				final TreeNode node = (TreeNode) value;
				if (!node.isLeaf()) {
					rdr.setIcon(new ImageIcon(RobonoboFrame.class.getResource("/img/icon/playlist_people.png")));
				} else {
					rdr.setIcon(new ImageIcon(RobonoboFrame.class.getResource("/img/icon/music_icon.png")));
				}
				return rdr;
			}

			public void paint(Graphics g) {
				paintComponent(g);
			}
		});
		everyOneMusicTree.setAlignmentX(0.0f);
		everyOneMusicTree.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
		
		JPanel myMusicLblPanel = new JPanel();
		myMusicLblPanel.setBackground(GREY_BG);
		myMusicLblPanel.setOpaque(true);
		myMusicLblPanel.setAlignmentX(0f);
		myMusicLblPanel.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
		myMusicLblPanel.setLayout(new BoxLayout(myMusicLblPanel, BoxLayout.Y_AXIS));
		myMusicLblPanel.setPreferredSize(new Dimension(175, 19));
		myMusicLblPanel.setMinimumSize(new Dimension(175, 19));
		myMusicLblPanel.setMaximumSize(new Dimension(175, 19));
		JLabel myMusicLbl = new JLabel("My Music Library", new ImageIcon(RobonoboFrame.class.getResource("/img/icon/music_icon.png")), JLabel.LEFT);
		myMusicLbl.setFont(RobonoboFont.getFont(11, true));
		myMusicLblPanel.add(myMusicLbl);
		treeListView.add(myMusicLblPanel);
		
		final JList myMusicList = new JList(new Object[] { "New Playlist", "Playlist 001", "Playlist For That Night", "I Hate This One", "DJ Mix FIFE!~" });
		treeListView.add(myMusicList);
		myMusicList.setName("robonobo.playlist.list");
		myMusicList.setFont(RobonoboFont.getFont(11, false));
		myMusicList.setCellRenderer(new DefaultListCellRenderer() {
			private static final long serialVersionUID = 1L;
			public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
				final JLabel rdr = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				rdr.setIcon(new ImageIcon(RobonoboFrame.class.getResource("/img/icon/music_icon.png")));
				rdr.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
				return rdr;
			}
		});
		myMusicList.setAlignmentX(0.0f);
		myMusicList.setMaximumSize(new Dimension(65535, 65535));

		final JPanel statusPanel = new JPanel();
		statusPanel.setPreferredSize(new Dimension(200, 125));
		statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.Y_AXIS));
		statusPanel.setBackground(DARK_BG);
		statusPanel.setOpaque(true);
		add(statusPanel);
		final JLabel bigLabel = new JLabel("345.00");
		statusPanel.add(bigLabel);
		bigLabel.setPreferredSize(new Dimension(200, 50));
		bigLabel.setOpaque(true);
		bigLabel.setBackground(DARK_BG);
		bigLabel.setForeground(Color.ORANGE);
		bigLabel.setFont(new Font("Bitstream Vera Sans", Font.PLAIN, 22));
		bigLabel.setAlignmentX(0.5f);

		final JLabel statusLabel = new JLabel("<html>Status:<b>Connected</b></html>");
		statusPanel.add(statusLabel);
		statusLabel.setPreferredSize(new Dimension(200, 25));
		statusLabel.setAlignmentX(0.5f);
		statusLabel.setOpaque(true);
		statusLabel.setBackground(Color.LIGHT_GRAY);
		statusLabel.setHorizontalAlignment(JLabel.CENTER);
		statusLabel.setBorder(BorderFactory.createLoweredBevelBorder());

		final JLabel networkLabel = new JLabel("<html><font color=white style='font-size:9pt;'>22/108 Linked Connections<br>25k up / 5k down</font></html>");
		statusPanel.add(networkLabel);
		networkLabel.setPreferredSize(new Dimension(200, 50));
		networkLabel.setOpaque(true);
		networkLabel.setBackground(DARK_BG);
		networkLabel.setAlignmentX(0.5f);
		networkLabel.setIcon(new ImageIcon(RobonoboFrame.class.getResource("/img/icon/earth.png")));
	}
}
