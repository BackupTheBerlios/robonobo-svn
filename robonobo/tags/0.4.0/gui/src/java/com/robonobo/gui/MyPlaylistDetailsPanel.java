package com.robonobo.gui;

import info.clearthought.layout.TableLayout;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import com.robonobo.common.concurrent.CatchingRunnable;
import com.robonobo.core.Platform;
import com.robonobo.core.api.RobonoboException;
import com.robonobo.core.api.model.Playlist;

public class MyPlaylistDetailsPanel extends JPanel {
	RobonoboFrame frame;
	private Playlist p;
	JTextField titleField;
	JTextArea descArea;
	JButton shareBtn, saveBtn, deleteBtn;
	JCheckBox friendsCheckBox;
	Map<String, JCheckBox> options = new HashMap<String, JCheckBox>();
	MyPlaylistContentPanel myContentPanel;
	OptsPanel optsPanel;
	ButtonsPanel buttonsPanel;

	MyPlaylistDetailsPanel(RobonoboFrame frame, final MyPlaylistContentPanel myContentPanel) {
		this.myContentPanel = myContentPanel;
		this.frame = frame;
		p = myContentPanel.p;
		titleField = new JTextField(p.getTitle());
		KeyAdapter keyL = new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				saveBtn.setEnabled(playlistDetailsChanged());
			}
		};
		titleField.addKeyListener(keyL);
		descArea = new JTextArea(p.getDescription());
		descArea.setLineWrap(true);
		descArea.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
		descArea.addKeyListener(keyL);

		double[][] cellSizen = { { 10, 80, TableLayout.FILL, 10, 320 }, { 25, 10, 65, 10, 30 } };
		setLayout(new TableLayout(cellSizen));
		add(new JLabel("Title"), "1,0,f,t");
		add(titleField, "2,0");
		add(new JLabel("Description"), "1,2,f,t");
		add(new JScrollPane(descArea), "2,2");
		optsPanel = new OptsPanel();
		add(optsPanel, "4,0,4,3");
		buttonsPanel = new ButtonsPanel();
		add(buttonsPanel, "2,4,4,4,r,t");
	}

	void focus() {
		// Do nothing
	}

	void update() {
		p = myContentPanel.p;
		titleField.setText(p.getTitle());
		descArea.setText(p.getDescription());
		friendsCheckBox.setSelected(p.getAnnounce());
	}
	
	String getPlaylistTitle() {
		return titleField.getText();
	}

	String getPlaylistDesc() {
		return descArea.getText();
	}

	boolean playlistDetailsChanged() {
		if (p == null)
			return (titleField.getText().length() > 0);
		else
			return true;
		// TODO Implement me
	}

	class OptsPanel extends JPanel {
		public OptsPanel() {
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

			ActionListener actL = new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					saveBtn.setEnabled(playlistDetailsChanged());
				}
			};

			friendsCheckBox = new JCheckBox("Let friends see this playlist");
			friendsCheckBox.setSelected(p.getAnnounce());
			friendsCheckBox.addActionListener(actL);
			add(friendsCheckBox);

			JCheckBox autoDownloadCB = new JCheckBox("Download new tracks automatically");
			autoDownloadCB.setSelected("true".equalsIgnoreCase(myContentPanel.pc.getItem("autoDownload")));
			options.put("autoDownload", autoDownloadCB);
			autoDownloadCB.addActionListener(actL);
			add(autoDownloadCB);

			if (Platform.getPlatform().iTunesAvailable()) {
				JCheckBox iTunesExportCB = new JCheckBox("Export playlist to iTunes");
				iTunesExportCB.setSelected("true".equalsIgnoreCase(myContentPanel.pc.getItem("iTunesExport")));
				options.put("iTunesExport", iTunesExportCB);
				iTunesExportCB.addActionListener(actL);
				add(iTunesExportCB);
			}
		}
	}

	protected boolean shareBtnsEnabled() {
		return true;
	}
	
	protected boolean delBtnEnabled() {
		return true;
	}
	
	class ButtonsPanel extends JPanel {
		public ButtonsPanel() {
			setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

			saveBtn = new JButton("Save changes");
			saveBtn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					myContentPanel.p.setAnnounce(friendsCheckBox.isSelected());
					myContentPanel.pc.getItems().clear();
					for (String opt : options.keySet()) {
						JCheckBox cb = options.get(opt);
						if(cb.isSelected())
							myContentPanel.pc.setItem(opt, "true");
					}
					saveBtn.setEnabled(false);
					frame.getController().getExecutor().execute(new CatchingRunnable() {
						public void doRun() throws Exception {
							myContentPanel.savePlaylist();
						}
					});
				}
			});
			saveBtn.setEnabled(false);
			add(saveBtn);

			shareBtn = new JButton("Share...");
			shareBtn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					SharePlaylistDialog dialog = new SharePlaylistDialog(frame, p);
					dialog.setLocationRelativeTo(frame);
					dialog.setVisible(true);
				}
			});
			shareBtn.setEnabled(shareBtnsEnabled());
			add(shareBtn);
			
			deleteBtn = new JButton("Delete");
			deleteBtn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					int result = JOptionPane.showConfirmDialog(ButtonsPanel.this, "Are you sure you want to delete this playlist?", "Delete this playlist?", JOptionPane.YES_NO_OPTION);
					if(result == JOptionPane.YES_OPTION)
						myContentPanel.deletePlaylist();
				}
			});
			deleteBtn.setEnabled(delBtnEnabled());
			add(deleteBtn);
		}
	}
}
