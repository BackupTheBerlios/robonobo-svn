package com.robonobo.oldgui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.IOException;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.robonobo.common.concurrent.CatchingRunnable;
import com.robonobo.core.Platform;
import com.robonobo.gui.dialogs.AboutDialog;

public class MenuBar extends JMenuBar {
	private Log log;

	public MenuBar(final RobonoboFrame frame) {
		log = LogFactory.getLog(getClass());
		JMenu fileMenu = new JMenu("File");
		add(fileMenu);

		JMenuItem login = new JMenuItem("Login...", KeyEvent.VK_L);
		login.setAccelerator(Platform.getPlatform().getAccelKeystroke(KeyEvent.VK_L));
		login.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				frame.showLogin(null);
			}
		});
		fileMenu.add(login);

		JMenuItem shareFiles = new JMenuItem("Share files...", KeyEvent.VK_O);
		shareFiles.setAccelerator(Platform.getPlatform().getAccelKeystroke(KeyEvent.VK_O));
		shareFiles.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				frame.showAddSharesDialog();
			}
		});
		fileMenu.add(shareFiles);

		if (Platform.getPlatform().iTunesAvailable()) {
			JMenuItem iTunesImport = new JMenuItem("Share tracks/playlists from iTunes...", KeyEvent.VK_I);
			iTunesImport.setAccelerator(Platform.getPlatform().getAccelKeystroke(KeyEvent.VK_I));
			iTunesImport.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					frame.getController().getExecutor().execute(new CatchingRunnable() {
						public void doRun() throws Exception {
							frame.importITunes();
						}
					});

				}
			});
			fileMenu.add(iTunesImport);
		}

		JMenuItem watchDir = new JMenuItem("Watch directory...", KeyEvent.VK_D);
		watchDir.setAccelerator(Platform.getPlatform().getAccelKeystroke(KeyEvent.VK_D));
		watchDir.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				frame.showWatchDirDialog();
			}
		});
		fileMenu.add(watchDir);

		if (Platform.getPlatform().shouldShowQuitInFileMenu()) {
			JMenuItem quit = new JMenuItem("Quit", KeyEvent.VK_Q);
			quit.setAccelerator(Platform.getPlatform().getAccelKeystroke(KeyEvent.VK_Q));
			quit.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					frame.shutdown();
				}
			});
			fileMenu.add(quit);
		}

		JMenu networkMenu = new JMenu("Network");
		add(networkMenu);
		JMenuItem updateUsers = new JMenuItem("Update friends & playlists");
		updateUsers.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				frame.getController().checkUsersUpdate();
			}
		});
		networkMenu.add(updateUsers);

		if (Platform.getPlatform().shouldShowOptionsMenu()) {
			JMenu optionsMenu = new JMenu("Options");
			add(optionsMenu);
			JMenuItem showPrefs = new JMenuItem("Preferences...");
			showPrefs.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					frame.showPreferences();
				}
			});
			optionsMenu.add(showPrefs);
		}

		JMenu debugMenu = new JMenu("Debug");
		add(debugMenu);
		JMenuItem openConsole = new JMenuItem("Open Console");
		openConsole.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				frame.showConsole();
			}
		});
		debugMenu.add(openConsole);
		JMenuItem showLog = new JMenuItem("Show Log Window");
		showLog.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				frame.showLogFrame();
			}
		});
		debugMenu.add(showLog);

		JMenu helpMenu = new JMenu("Help");
		add(helpMenu);
		if (Platform.getPlatform().shouldShowAboutInHelpMenu()) {
			JMenuItem showAbout = new JMenuItem("About robonobo");
			showAbout.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
//					AboutDialog dialog = new AboutDialog(frame);
					AboutDialog dialog = new AboutDialog(null);
					dialog.setVisible(true);
				}
			});
			helpMenu.add(showAbout);
		}
		JMenuItem showHelpPage = new JMenuItem("Go to online help...");
		showHelpPage.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				openUrl(frame.getController().getConfig().getHelpUrl());
			}
		});
		helpMenu.add(showHelpPage);
		JMenuItem showWiki = new JMenuItem("Go to developer wiki...");
		showWiki.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				openUrl(frame.getController().getConfig().getWikiUrl());
			}
		});
		helpMenu.add(showWiki);
		JMenuItem submitBugReport = new JMenuItem("Submit bug report...");
		submitBugReport.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				openUrl(frame.getController().getConfig().getBugReportUrl());
			}
		});
		helpMenu.add(submitBugReport);
	}

	private void openUrl(String url) {
		try {
			Platform.getPlatform().openUrl(url);
		} catch (IOException e) {
			log.error("Caught error opening url " + url);
		}
	}
}
