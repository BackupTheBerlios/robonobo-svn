package com.robonobo.oldgui;

import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.debian.tablelayout.TableLayout;

import com.robonobo.core.RobonoboController;
import com.robonobo.core.api.model.CloudTrack;
import com.robonobo.core.api.model.Track;
import com.robonobo.gui.model.SearchResultTableModel;

@SuppressWarnings("serial")
public class SearchResultContentPanel extends ContentPanel {
	protected RobonoboController controller;
	protected PrevButton prevBtn;
	protected PlayPauseButton playBtn;
	protected NextButton nextBtn;
	protected MainButton dloadBtn;
	protected MainButtonBar mainBtnBar;
	protected SearchPanel searchPanel;
	protected SearchResultTablePanel tablePanel;
	private SearchResultTableModel tableModel;

	public SearchResultContentPanel(RobonoboFrame frame) {
		super(frame);
		controller = frame.getController();
		double[][] cellSizen = { { 10, TableLayout.FILL, 10 }, { 10, 70, 10, 30, 10, TableLayout.FILL } };
		setLayout(new TableLayout(cellSizen));
		tableModel = new SearchResultTableModel(frame.getController());
		KeyAdapter keyListener = new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				// Spacebar == play/pause
				if(e.getKeyChar() == KeyEvent.VK_SPACE) {
					playBtn.doClick();
					e.consume();
				}
			}
		};
		tablePanel = new SearchResultTablePanel(frame, tableModel, new SelectionListener(), keyListener);
		List<MainButton> btnList = new ArrayList<MainButton>();
		prevBtn = new PrevButton(frame.getController());
		btnList.add(prevBtn);
		playBtn = new PlayPauseButton(controller, tablePanel, tableModel, tablePanel);
		playBtn.setEnabled(false);
		btnList.add(playBtn);
		nextBtn = new NextButton(frame.getController());
		btnList.add(nextBtn);
		dloadBtn = new MainButton("/img/Download.png", "Download selected tracks");
		dloadBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				controller.spawnNecessaryDownloads(tablePanel.getSelectedStreamIds());
				tablePanel.clearTableSelection();
			}
		});
		dloadBtn.setEnabled(false);
		btnList.add(dloadBtn);
		mainBtnBar = new MainButtonBar(btnList, frame, tablePanel);
		add(mainBtnBar, "1,1");
//		searchPanel = new SearchPanel(tableModel, false);
		add(searchPanel, "1,3");
		add(tablePanel, "1,5");
		tablePanel.getJTable().addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2)
					playBtn.play(tablePanel.getSelectedStreamIds());
			}
		});
	}

	@Override
	public void focus() {
		searchPanel.focus();
	}

	class SelectionListener implements ListSelectionListener {
		public void valueChanged(ListSelectionEvent e) {
			// Only do our thang when mouse button is released, save a few
			// cycles
			if (e.getValueIsAdjusting())
				return;
			List<Track> selTracks = tablePanel.getSelectedTracks();
			if (selTracks.size() == 0) {
				playBtn.setEnabledIfStopped(false);
				dloadBtn.setEnabled(false);
			} else {
				playBtn.setEnabledIfStopped(true);
				// Only activate the Download button if we're not already
				// Download/sharing at least one of these tracks
				boolean activeDl = false;
				for (Track t : selTracks) {
					if(t instanceof CloudTrack) {
						activeDl = true;
						break;
					}
				}
				dloadBtn.setEnabled(activeDl);
			}
		}
	}

	@Override
	public PlayPauseButton getPlayPauseButton() {
		return playBtn;
	}
}
