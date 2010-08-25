package com.robonobo.gui.panels;

import static com.robonobo.gui.RoboColor.*;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import com.robonobo.gui.RoboFont;
import com.robonobo.gui.components.ActiveSearchList;
import com.robonobo.gui.components.FriendTree;
import com.robonobo.gui.components.LeftSidebarComponent;
import com.robonobo.gui.frames.RobonoboFrame;

@SuppressWarnings("serial")
public class LeftSidebar extends JPanel {
	List<LeftSidebarComponent> sideBarComps = new ArrayList<LeftSidebarComponent>();
	
	public LeftSidebar(RobonoboFrame frame) {
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
		
		final JPanel sideBarPanel = new JPanel();
		final JScrollPane treeListScroller = new JScrollPane(sideBarPanel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		add(treeListScroller);
		treeListScroller.getViewport().getView().setBackground(Color.WHITE);
		sideBarPanel.setLayout(new BoxLayout(sideBarPanel, BoxLayout.Y_AXIS));
		sideBarPanel.setBackground(MID_GRAY);
		
		JPanel searchPanel = new JPanel();
		searchPanel.setLayout(new BoxLayout(searchPanel, BoxLayout.Y_AXIS));
		searchPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
		searchPanel.setOpaque(true);
		searchPanel.setBackground(MID_GRAY);
		searchPanel.setPreferredSize(new Dimension(185, 30));
		searchPanel.setMinimumSize(new Dimension(185, 30));
		searchPanel.setMaximumSize(new Dimension(185, 30));
		searchPanel.setAlignmentX(0f);
		JTextField searchField = new JTextField("Search...");
		searchField.setName("robonobo.search.textfield");
		searchField.setFont(RoboFont.getFont(11, false));
		searchField.setPreferredSize(new Dimension(170, 25));
		searchField.setMinimumSize(new Dimension(170, 25));
		searchField.setMaximumSize(new Dimension(170, 25));
		searchField.setSelectionStart(0);
		searchField.setSelectionEnd(searchField.getText().length());
		searchPanel.add(searchField);
		sideBarPanel.add(searchPanel);
		
		ActiveSearchList asList = new ActiveSearchList(this, frame);
		asList.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
		sideBarPanel.add(asList);
		sideBarComps.add(asList);

		FriendTree fTree = new FriendTree(this, frame);
		fTree.setBorder(BorderFactory.createEmptyBorder(5, 10, 3, 10));
		sideBarPanel.add(fTree);
		sideBarComps.add(fTree);
		
		JPanel myMusicLblPanel = new JPanel();
		myMusicLblPanel.setBackground(MID_GRAY);
		myMusicLblPanel.setOpaque(true);
		myMusicLblPanel.setAlignmentX(0f);
		myMusicLblPanel.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
		myMusicLblPanel.setLayout(new BoxLayout(myMusicLblPanel, BoxLayout.Y_AXIS));
		myMusicLblPanel.setPreferredSize(new Dimension(185, 19));
		myMusicLblPanel.setMinimumSize(new Dimension(185, 19));
		myMusicLblPanel.setMaximumSize(new Dimension(185, 19));
		JLabel myMusicLbl = new JLabel("My Music Library", new ImageIcon(RobonoboFrame.class.getResource("/img/icon/home.png")), JLabel.LEFT);
		myMusicLbl.setFont(RoboFont.getFont(11, true));
		myMusicLblPanel.add(myMusicLbl);
		sideBarPanel.add(myMusicLblPanel);
		
		final JList myMusicList = new JList(new Object[] { "New Playlist", "Playlist 001", "Playlist For That Night", "I Hate This One", "DJ Mix FIFE!~" });
		sideBarPanel.add(myMusicList);
		myMusicList.setName("robonobo.playlist.list");
		myMusicList.setFont(RoboFont.getFont(11, false));
		myMusicList.setCellRenderer(new DefaultListCellRenderer() {
			private static final long serialVersionUID = 1L;
			public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
				final JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				if(value.equals("New Playlist"))
					lbl.setIcon(new ImageIcon(RobonoboFrame.class.getResource("/img/icon/new_playlist.png")));
				else
					lbl.setIcon(new ImageIcon(RobonoboFrame.class.getResource("/img/icon/playlist.png")));
				if(isSelected)
					lbl.setForeground(BLUE_GRAY);
				lbl.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
				return lbl;
			}
		});
		myMusicList.setAlignmentX(0.0f);
		myMusicList.setMaximumSize(new Dimension(65535, 65535));

		JPanel spacerPanel = new JPanel();
		spacerPanel.setLayout(new BoxLayout(spacerPanel, BoxLayout.X_AXIS));
		spacerPanel.setPreferredSize(new Dimension(200, 5));
		spacerPanel.setOpaque(false);
		add(spacerPanel);
		add(new StatusPanel());
	}
	
	public void clearSelectionExcept(LeftSidebarComponent selCmp) {
		for (LeftSidebarComponent cmp : sideBarComps) {
			if(cmp != selCmp)
				cmp.relinquishSelection();
		}
	}
}
