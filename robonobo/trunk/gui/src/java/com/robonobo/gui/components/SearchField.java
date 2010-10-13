package com.robonobo.gui.components;

import static com.robonobo.gui.RoboColor.MID_GRAY;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.robonobo.gui.RoboFont;
import com.robonobo.gui.panels.LeftSidebar;

@SuppressWarnings("serial")
public class SearchField extends JPanel {
	private JTextField searchField;

	public SearchField(final LeftSidebar leftSidebar) {
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
		setOpaque(true);
		setBackground(MID_GRAY);
		setPreferredSize(new Dimension(185, 30));
		setMinimumSize(new Dimension(185, 30));
		setMaximumSize(new Dimension(185, 30));
		setAlignmentX(0f);
		searchField = new JTextField("Search...");
		searchField.setName("robonobo.search.textfield");
		searchField.setFont(RoboFont.getFont(11, false));
		searchField.setPreferredSize(new Dimension(170, 25));
		searchField.setMinimumSize(new Dimension(170, 25));
		searchField.setMaximumSize(new Dimension(170, 25));
		searchField.setSelectionStart(0);
		searchField.setSelectionEnd(searchField.getText().length());
		searchField.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if(searchField.getText().equals("Search..."))
					searchField.setText("");
			}
		});
		searchField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				leftSidebar.searchAdded(searchField.getText());
				searchField.setText("Search...");
				searchField.setSelectionStart(0);
				searchField.setSelectionEnd(searchField.getText().length());
			}
		});
		add(searchField);
	}
}
