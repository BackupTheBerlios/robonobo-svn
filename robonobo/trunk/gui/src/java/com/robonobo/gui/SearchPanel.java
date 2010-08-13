package com.robonobo.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.debian.tablelayout.TableLayout;

import com.robonobo.core.Platform;
import com.robonobo.core.api.SearchExecutor;

public class SearchPanel extends JPanel {
	private JTextField searchField;
	private JButton searchBtn;

	public SearchPanel(final SearchExecutor searcher, boolean searchEveryKeyPress) {
		double[][] cellSizen = { { 50, 5, TableLayout.FILL, 5, 65 }, { TableLayout.FILL } };
		setLayout(new TableLayout(cellSizen));

		add(new JLabel("Search"), "0,0");
		searchBtn = new JButton("Go");
		searchField = new JTextField();
		if (searchEveryKeyPress) {
			searchField.addKeyListener(new KeyAdapter() {
				public void keyPressed(KeyEvent e) {
					searcher.search(searchField.getText());
				}
			});
		}
		searchField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				searcher.search(searchField.getText());
			}
		});
		Platform.getPlatform().customizeSearchTextField(searchField);
		add(searchField, "2,0");
		searchBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				searcher.search(searchField.getText());
			}
		});
		add(searchBtn, "4,0");
	}

	public void focus() {
		searchField.requestFocusInWindow();
	}
}
