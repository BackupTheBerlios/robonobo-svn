package com.robonobo.gui.panels;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.*;

import org.debian.tablelayout.TableLayout;

import com.robonobo.gui.RoboColor;
import com.robonobo.gui.RoboFont;
import com.robonobo.gui.frames.RobonoboFrame;

@SuppressWarnings("serial")
public class WelcomePanel extends JPanel {
	private RobonoboFrame frame;
	private Dimension size = new Dimension(600, 510);
	private JCheckBox shutUpCB;

	public WelcomePanel(RobonoboFrame rFrame) {
		this.frame = rFrame;
		setPreferredSize(size);
		setSize(size);
		double[][] cellSizen = {
				{ 20, TableLayout.FILL, 20 },
				{ 20 /* sp */, 30 /* logo */, 30 /* title */, 10 /* sp */, 20 /* blurb */, 10 /* sp */,
						25 /* filechoose */, 10 /* sp */, 50 /* blurb */, 10 /* sp */, 30 /* title */, 10 /* sp */,
						30 /* btn */, 30 /* sp */, 30 /* title */, 10 /* sp */, 30 /* btn */, 40 /* sp */, 1 /* sep */,
						10 /* sp */, 30 /* btn */, 5 /* sp */, 30 /* cb */, 10 /* sp */} };
		setLayout(new TableLayout(cellSizen));
		setName("playback.background.panel");

		JLabel titleLbl = new JLabel("welcome to robonobo");
		titleLbl.setFont(RoboFont.getFont(24, true));
		add(titleLbl, "1,2");

		JLabel dloadBlurb = new JLabel("<html><p>" + "robonobo will store your downloaded music in this folder:"
				+ "</p></html>");
		add(dloadBlurb, "1,4");

		FileChoosePanel filePanel = new FileChoosePanel();
		add(filePanel, "1,6");

		JLabel shareBlurb = new JLabel(
				"<html><p>"
						+ "Before you can share your music and playlists with your friends, you must add tracks to your robonobo music library.  "
						+ "You can add tracks from iTunes, or else you can add them from MP3 files on your computer."
						+ "</p></html>");
		shareBlurb.setFont(RoboFont.getFont(12, false));
		add(shareBlurb, "1,8,l,t");

		JLabel iTunesTitle = new JLabel("Share Tracks/Playlists from iTunes");
		iTunesTitle.setFont(RoboFont.getFont(18, true));
		add(iTunesTitle, "1,10");

		JButton iTunesBtn = new JButton("Share from iTunes...");
		iTunesBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				frame.undim();
				frame.importITunes();
			}
		});
		addButton(iTunesBtn, "1,12");

		JLabel fileTitle = new JLabel("Share Tracks from Files");
		fileTitle.setFont(RoboFont.getFont(18, true));
		add(fileTitle, "1,14");

		JButton fileBtn = new JButton("Share from files...");
		fileBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				frame.undim();
				frame.showAddSharesDialog();
			}
		});
		addButton(fileBtn, "1,16");

		add(new Sep(), "1,18");

		JButton feckOffBtn = new JButton("Don't share anything");
		feckOffBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(shutUpCB.isSelected()) {
					frame.getGuiConfig().setShowWelcomePanel(false);
					frame.getController().saveConfig();
				}
				frame.undim();
			}
		});
		addButton(feckOffBtn, "1,20");

		shutUpCB = new JCheckBox("Don't show this screen on startup");
		shutUpCB.setSelected(false);
		add(shutUpCB, "1,22");
	}

	private void addButton(JButton btn, String layoutPos) {
		btn.setFont(RoboFont.getFont(12, true));
		JPanel pnl = new JPanel();
		pnl.setLayout(new BoxLayout(pnl, BoxLayout.X_AXIS));
		pnl.add(btn);
		add(pnl, layoutPos);
	}

	class Sep extends JSeparator {
		public Sep() {
			super(SwingConstants.HORIZONTAL);
			setBackground(RoboColor.DARK_GRAY);
		}
	}

	class FileChoosePanel extends JPanel {
		private JTextField tf;

		public FileChoosePanel() {
			setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
			tf = new JTextField();
			tf.setMaximumSize(new Dimension(300, 30));
			tf.setFont(RoboFont.getFont(11, false));
			tf.setText(frame.getController().getConfig().getDownloadDirectory());
			tf.setEnabled(false);
			add(tf);
			add(Box.createHorizontalStrut(10));
			JButton btn = new JButton("...");
			btn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					JFileChooser fc = new JFileChooser(new File(tf.getText()));
					fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
					int retVal = fc.showOpenDialog(WelcomePanel.this);
					if(retVal == JFileChooser.APPROVE_OPTION) {
						File f = fc.getSelectedFile();
						tf.setText(f.getAbsolutePath());
						frame.getController().getConfig().setDownloadDirectory(f.getAbsolutePath());
						frame.getController().saveConfig();
					}
				}
			});
			add(btn);
		}
	}
}