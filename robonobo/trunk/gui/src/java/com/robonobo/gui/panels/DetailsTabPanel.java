package com.robonobo.gui.panels;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import com.robonobo.gui.RoboFont;

public class DetailsTabPanel extends JPanel {
	public DetailsTabPanel() {
		setLayout(new BorderLayout());
		setBorder(BorderFactory.createEmptyBorder(0,1,0,1));
		final JTabbedPane tabs = new JTabbedPane(JTabbedPane.TOP);
		tabs.setFont(RoboFont.getFont(14, true));
		add(tabs, BorderLayout.CENTER);
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
		plistTitleLabel.setFont(RoboFont.getFont(13, false));
		editPlaylistPanel.add(plistTitleLabel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0,
				0, 0), 0, 0));
		final JTextField plistTitleEdit = new JTextField();
		plistTitleEdit.setFont(RoboFont.getFont(13, false));
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
		plistDescLabel.setFont(RoboFont.getFont(13, false));
		editPlaylistPanel.add(plistDescLabel, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0,
				0, 0), 0, 0));
		final JTextArea plistDescEdit = new JTextArea();
		plistDescEdit.setFont(RoboFont.getFont(13, false));		
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
		friendSeeCheckBox.setFont(RoboFont.getFont(12, false));
		friendSeeCheckBox.setSelected(true);
		editPlaylistPanel.add(friendSeeCheckBox, new GridBagConstraints(2, 1, 4, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(0, 5, 0, 0), 0, 0));
		final JCheckBox autoDownloadCheckBox = new JCheckBox("Download new tracks automatically");
		autoDownloadCheckBox.setFont(RoboFont.getFont(12, false));
		editPlaylistPanel.add(autoDownloadCheckBox, new GridBagConstraints(2, 2, 4, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(0, 5, 0, 0), 0, 0));
		final JButton saveButton = new JButton("SAVE");
		saveButton.setFont(RoboFont.getFont(12, true));
		editPlaylistPanel.add(saveButton, new GridBagConstraints(2, 3, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 5, 0,
				0), 0, 0));
		final JButton shareButton = new JButton("SHARE");
		shareButton.setFont(RoboFont.getFont(12, true));
		editPlaylistPanel.add(shareButton, new GridBagConstraints(3, 3, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 5, 0,
				0), 0, 0));
		final JButton deleteButton = new JButton("DELETE");
		deleteButton.setFont(RoboFont.getFont(12, true));
		deleteButton.setName("robonobo.red.button");
		editPlaylistPanel.add(deleteButton, new GridBagConstraints(5, 3, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 5,
				0, 5), 0, 0));
	}
}
