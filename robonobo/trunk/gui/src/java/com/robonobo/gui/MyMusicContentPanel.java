package com.robonobo.gui;

import info.clearthought.layout.TableLayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.robonobo.common.concurrent.CatchingRunnable;
import com.robonobo.common.exceptions.SeekInnerCalmException;
import com.robonobo.core.Platform;
import com.robonobo.core.RobonoboController;
import com.robonobo.core.api.model.DownloadingTrack;
import com.robonobo.core.api.model.SharedTrack;
import com.robonobo.core.api.model.Track;

import static com.robonobo.common.util.FileUtil.*;

@SuppressWarnings("serial")
public class MyMusicContentPanel extends ContentPanel {
	private RobonoboController controller;
	private PlayPauseButton playBtn;
	private MainButton addBtn, delBtn, nextBtn, prevBtn, addITunesBtn;
	private MainButtonBar mainBtnBar;
	private SearchPanel searchPanel;
	private TrackListTablePanel tablePanel;
	private MyMusicTableModel tableModel;

	public MyMusicContentPanel(final RobonoboFrame frame) {
		super(frame);
		this.controller = frame.getController();
		double[][] cellSizen = { { 10, TableLayout.FILL, 10 }, { 10, 70, 10, 30, 10, TableLayout.FILL } };
		setLayout(new TableLayout(cellSizen));
		tableModel = new MyMusicTableModel(controller);
		KeyAdapter keyListener = new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				// Spacebar == play/pause
				if (e.getKeyChar() == KeyEvent.VK_SPACE) {
					playBtn.doClick();
					e.consume();
				}
			}
		};
		tablePanel = new MyMusicTablePanel(frame, tableModel, new SelectionListener(), keyListener);
		List<MainButton> btnList = new ArrayList<MainButton>();

		prevBtn = new PrevButton(controller);
		btnList.add(prevBtn);

		playBtn = new PlayPauseButton(controller, tablePanel, tableModel, tablePanel);
		playBtn.setEnabled(false);
		btnList.add(playBtn);

		nextBtn = new NextButton(controller);
		btnList.add(nextBtn);

		addBtn = new MainButton("/img/Plus.png", "Share more tracks");
		addBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				frame.showAddSharesDialog();
			}
		});
		btnList.add(addBtn);

		if (Platform.getPlatform().iTunesAvailable()) {
			addITunesBtn = new MainButton("/img/Plus.png", "Share tracks from iTunes library");
			addITunesBtn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					frame.updateStatus("Importing tracks from iTunes...", 0, 10);
					frame.getController().getExecutor().execute(new CatchingRunnable() {
						public void doRun() throws Exception {
							List<File> filez = controller.getITunesLibrary(new FileFilter() {
								public boolean accept(File f) {
									return "mp3".equalsIgnoreCase(getFileExtension(f));
								}
							});
							frame.importFiles(filez);
						}
					});
				}
			});
			btnList.add(addITunesBtn);
		}
		delBtn = new MainButton("/img/Trash.png", "Stop sharing selected tracks");
		delBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				final List<Track> trax = tablePanel.getSelectedTracks();
				if (trax.size() == 0)
					return;
				controller.getExecutor().execute(new CatchingRunnable() {
					@Override
					public void doRun() throws Exception {
						for (Track t : trax)
							if (t instanceof SharedTrack) {
								controller.deleteShare(t.getStream().getStreamId());
								frame.updateStatus("Deleted share " + t.getStream().getTitle(), 2, 10);
							} else if (t instanceof DownloadingTrack) {
								controller.deleteDownload(t.getStream().getStreamId());
								frame.updateStatus("Deleted Download " + t.getStream().getTitle(), 2, 10);
							} else
								throw new SeekInnerCalmException();
					}
				});

			}
		});
		delBtn.setEnabled(false);
		btnList.add(delBtn);

		mainBtnBar = new MainButtonBar(btnList, frame, tablePanel);
		add(mainBtnBar, "1,1");
		searchPanel = new SearchPanel(tablePanel, true);
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
			boolean tracksSelected = tablePanel.getSelectedStreamIds().size() > 0;
			if (tracksSelected) {
				playBtn.setEnabledIfStopped(true);
				delBtn.setEnabled(true);
			} else {
				playBtn.setEnabledIfStopped(false);
				delBtn.setEnabled(false);
			}
		}
	}

	@Override
	public PlayPauseButton getPlayPauseButton() {
		return playBtn;
	}
}
