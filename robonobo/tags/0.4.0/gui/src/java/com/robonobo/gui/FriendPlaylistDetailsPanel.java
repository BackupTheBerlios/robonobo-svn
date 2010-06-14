package com.robonobo.gui;

import static com.robonobo.common.util.TextUtil.escapeHtml;
import info.clearthought.layout.TableLayout;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.robonobo.common.concurrent.CatchingRunnable;
import com.robonobo.core.Platform;
import com.robonobo.core.RobonoboController;
import com.robonobo.core.api.RobonoboException;
import com.robonobo.core.api.model.Playlist;

public class FriendPlaylistDetailsPanel extends JPanel {
	private Playlist p;
	JButton saveBtn;
	Map<String, JCheckBox> options = new HashMap<String, JCheckBox>();
	FriendPlaylistContentPanel myContentPanel;
	OptsPanel optsPanel;
	ButtonsPanel buttonsPanel;
	private JLabel titleLbl;
	private JLabel descLbl;

	FriendPlaylistDetailsPanel(final FriendPlaylistContentPanel myContentPanel) {
		this.myContentPanel = myContentPanel;
		p = myContentPanel.p;

		double[][] cellSizen = { { 5, TableLayout.FILL, 10, 320 }, { 15, 10, 50, 30 } };
		setLayout(new TableLayout(cellSizen));
		titleLbl = new JLabel(escapeHtml(p.getTitle()));
		Font titleFont = titleLbl.getFont().deriveFont((float) titleLbl.getFont().getSize() + 2).deriveFont(Font.BOLD);
		titleLbl.setFont(titleFont);
		add(titleLbl, "1,0");
		descLbl = new JLabel("<html>" + escapeHtml(p.getDescription()) + "</html>");
		add(descLbl, "1,2,l,t");
		optsPanel = new OptsPanel();
		add(optsPanel, "3,0,3,2");
		buttonsPanel = new ButtonsPanel();
		add(buttonsPanel, "3,3");
	}

	void focus() {
	}

	void update() {
		p = myContentPanel.p;
		titleLbl.setText(escapeHtml(p.getTitle()));
		descLbl.setText("<html>" + escapeHtml(p.getDescription()) + "</html>");
	}
	
	class OptsPanel extends JPanel {
		public OptsPanel() {
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

			ActionListener actL = new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					saveBtn.setEnabled(true);
				}
			};

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

	protected boolean sendBtnEnabled() {
		return true;
	}

	protected boolean delBtnEnabled() {
		return true;
	}

	class ButtonsPanel extends JPanel {
		public ButtonsPanel() {
			setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

			saveBtn = new JButton("Save Settings");
			saveBtn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					myContentPanel.pc.getItems().clear();
					for (String opt : options.keySet()) {
						JCheckBox cb = options.get(opt);
						if (cb.isSelected())
							myContentPanel.pc.setItem(opt, "true");
					}
					saveBtn.setEnabled(false);
					myContentPanel.savePlaylistConfig();
					SwingUtilities.invokeLater(new CatchingRunnable() {
						@Override
						public void doRun() throws Exception {
							RobonoboFrame frame = myContentPanel.frame;
							RobonoboController controller = myContentPanel.controller;
							try {
								controller.checkPlaylistUpdate(myContentPanel.p.getPlaylistId());
							} catch (RobonoboException e) {
								frame.updateStatus("Error checking playlist update: " + e.getMessage(), 10, 30);
							}
						}
					});
				}
			});
			saveBtn.setEnabled(false);
			add(saveBtn);
		}
	}
}
