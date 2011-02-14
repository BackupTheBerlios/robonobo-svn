package com.robonobo.oldgui;

import static com.robonobo.common.util.FileUtil.*;

import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileFilter;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.debian.tablelayout.TableLayout;
import org.doomdark.uuid.UUIDGenerator;

import com.robonobo.Robonobo;
import com.robonobo.common.concurrent.CatchingRunnable;
import com.robonobo.common.util.FileUtil;
import com.robonobo.core.Platform;
import com.robonobo.core.RobonoboController;
import com.robonobo.core.api.RobonoboException;
import com.robonobo.core.api.RobonoboStatus;
import com.robonobo.core.api.UserPlaylistListener;
import com.robonobo.core.api.model.*;
import com.robonobo.gui.GUIUtils;
import com.robonobo.gui.preferences.PrefDialog;
import com.robonobo.gui.sheets.LoginSheet;

@SuppressWarnings("serial")
public class RobonoboFrame extends JFrame implements UserPlaylistListener {
	String[] cmdLineArgs;
	Log log = LogFactory.getLog(getClass());
	LeftSidebar lbar;
	ContentPanelHolder contentHolder;
	private Dimension initSize = new Dimension(1024, 768);
	NetworkStatusPanel netStatPnl;
	WangPanel wangPanel;
	StatusBar statusBar;
	JMenuBar menuBar;
	PrefDialog prefDialog;
	RobonoboController controller;
	FileImportDropHandler fileImportDropHandler;

	public RobonoboFrame(RobonoboController controller, String[] cmdLineArgs) {
		this.controller = controller;
		this.cmdLineArgs = cmdLineArgs;
		
		setTitle("robonobo");
		setPreferredSize(initSize);
		setSize(initSize);

		setIconImage(getRobonoboIconImage());
		double[][] cellSizen = { { 170, TableLayout.FILL, 80 }, { TableLayout.FILL, 20, 5 } };
		setLayout(new TableLayout(cellSizen));

		menuBar = Platform.getPlatform().getMenuBar(this);
		setJMenuBar(menuBar);

		lbar = new LeftSidebar(this);
		contentHolder = new ContentPanelHolder(this);
		netStatPnl = new NetworkStatusPanel(controller);
		statusBar = new StatusBar(controller);
		wangPanel = new WangPanel(controller);

		add(lbar, "0,0");
		add(contentHolder, "1,0,2,0");
		add(netStatPnl, "0,1");
		add(statusBar, "1,1");
		add(wangPanel, "2,1");

		contentHolder.addContentPanel("allMusic", new SearchResultContentPanel(this));
		contentHolder.addContentPanel("myMusic", new MyMusicContentPanel(this));
		contentHolder.addContentPanel("playlist-new", new NewPlaylistContentPanel(this));

		fileImportDropHandler = new FileImportDropHandler(this);
//		prefDialog = new PrefDialog(this);

		// This is stupid, shouldn't need to do this, but if I just call
		// lbar.selectAllMusic, the search box doesn't get the focus
		addWindowFocusListener(new WindowAdapter() {
			private boolean first = true;

			@Override
			public void windowGainedFocus(WindowEvent e) {
				if (first)
					lbar.selectAllMusic();
				first = false;
			}
		});
	}

	public static Image getRobonoboIconImage() {
		return GUIUtils.getImage("/img/robonobo-128x128.png");
	}

	public ContentPanelHolder getContentHolder() {
		return contentHolder;
	}

	public LeftSidebar getLeftSidebar() {
		return lbar;
	}

	/**
	 * @param onLogin
	 *            If the login is successful, this will be executed on the Swing
	 *            GUI thread (so don't do too much in it)
	 */
	public void showLogin(Runnable onLogin) {
		getGlassPane().setVisible(true);
//		LoginDialog ld = new LoginDialog(this, onLogin);
//		ld.setLocationRelativeTo(this);
//		ld.setVisible(true);
	}

	public void showLogFrame() {
		Log4jMonitorFrame logFrame = new Log4jMonitorFrame(this);
		logFrame.setVisible(true);
	}

	public void showConsole() {
		ConsoleFrame consoleFrame = new ConsoleFrame(this);
		consoleFrame.setVisible(true);
	}

	public void showAddSharesDialog() {
		// Define this as a runnable as we might need to login first
		Runnable flarp = new CatchingRunnable() {
			@Override
			public void doRun() throws Exception {
				JFileChooser fc = new JFileChooser();
				fc.setFileFilter(new javax.swing.filechooser.FileFilter() {
					public boolean accept(File f) {
						if (f.isDirectory())
							return true;
						return "mp3".equalsIgnoreCase(FileUtil.getFileExtension(f));
					}

					public String getDescription() {
						return "MP3 files";
					}
				});
				fc.setMultiSelectionEnabled(true);
				fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
				int retVal = fc.showOpenDialog(RobonoboFrame.this);
				if (retVal == JFileChooser.APPROVE_OPTION) {
					final File[] selFiles = fc.getSelectedFiles();
					controller.getExecutor().execute(new CatchingRunnable() {
						public void doRun() throws Exception {
							importFilesOrDirectories(Arrays.asList(selFiles));
						}
					});
				}
			}
		};
		if (controller.getMyUser() != null)
			flarp.run();
		else
			showLogin(flarp);
	}

	public void showWatchDirDialog() {
		// Define this as a runnable as we might need to login first
		Runnable flarp = new CatchingRunnable() {
			public void doRun() throws Exception {
				JFileChooser fc = new JFileChooser();
				fc.setFileFilter(new javax.swing.filechooser.FileFilter() {
					public boolean accept(File f) {
						return f.isDirectory();
					}

					public String getDescription() {
						return "Directory to watch";
					}
				});
				fc.setMultiSelectionEnabled(false);
				fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				int retVal = fc.showOpenDialog(RobonoboFrame.this);
				if (retVal == JFileChooser.APPROVE_OPTION) {
					final File selFile = fc.getSelectedFile();
					controller.getExecutor().execute(new CatchingRunnable() {
						public void doRun() throws Exception {
							controller.addWatchDir(selFile);
							updateStatus("Now watching directory " + selFile.getAbsolutePath(), 5, 30);
						}
					});
				}
			}
		};
		if (controller.getMyUser() != null)
			flarp.run();
		else
			showLogin(flarp);
	}

	public void showPreferences() {
		prefDialog.setVisible(true);
	}

	public List<Stream> importFilesOrDirectories(final List<File> files) {
		List<File> allFiles = new ArrayList<File>();
		for (File selFile : files)
			if (selFile.isDirectory())
				allFiles.addAll(FileUtil.getFilesWithinPath(selFile, "mp3"));
			else
				allFiles.add(selFile);
		return importFiles(allFiles);
	}

	public List<Stream> importFiles(final List<File> files) {
		List<Stream> streams = new ArrayList<Stream>();
		for (File file : files) {
			RobonoboStatus status = controller.getStatus();
			if (status == RobonoboStatus.Stopping || status == RobonoboStatus.Stopped)
				return streams;
			String filePath = file.getAbsolutePath();
			updateStatus("Adding share from file " + filePath, 0, 60);
			Stream s = null;
			try {
				s = controller.addShare(filePath);
			} catch (RobonoboException e) {
				log.error("Error adding share from file " + filePath, e);
				updateStatus("Not adding share from file " + file.getName() + ": " + e.getMessage(), 5, 10);
				continue;
			}
			streams.add(s);
			updateStatus("Added share '" + s.getTitle() + "'", 0, 10);
		}
		return streams;
	}

	public void importITunes() {
		FileFilter mp3Filter = new FileFilter() {
			public boolean accept(File f) {
				return "mp3".equalsIgnoreCase(getFileExtension(f));
			}
		};
		try {
			updateStatus("Importing from iTunes...", 0, 60);

			List<File> files = controller.getITunesLibrary(mp3Filter);
			importFiles(files);
			Map<String, List<File>> itPls = controller.getITunesPlaylists(mp3Filter);
			for (String pName : itPls.keySet()) {
				Playlist p = controller.getMyPlaylistByTitle(pName);
				if (p == null) {
					p = new Playlist();
//					p.setPlaylistId(UUIDGenerator.getInstance().generateRandomBasedUUID(new SecureRandom()).toString());
					p.setTitle(pName);
					p.getOwnerIds().add(controller.getMyUser().getUserId());
					List<File> tracks = itPls.get(pName);
					for (File track : tracks) {
						SharedTrack sh = controller.getShareByFilePath(track);
						if (sh == null)
							log.error("ITunes playlist '" + pName + "' has track '" + track + "', but I am not sharing it");
						else
							p.getStreamIds().add(sh.getStream().getStreamId());
					}
					controller.addOrUpdatePlaylist(p);
				} else {
					// Update existing playlist - add each track if it's not already in there
					List<File> tracks = itPls.get(pName);
					for (File track : tracks) {
						SharedTrack sh = controller.getShareByFilePath(track);
						if (sh == null)
							log.error("ITunes playlist '" + pName + "' has track '" + track + "', but I am not sharing it");
						else if(!p.getStreamIds().contains(sh.getStream().getStreamId()))
							p.getStreamIds().add(sh.getStream().getStreamId());
					}
					controller.addOrUpdatePlaylist(p);
				}
			}
			updateStatus("Finished iTunes import", 1, 5);
		} catch (RobonoboException e) {
			log.error("Error importing from iTunes", e);
			updateStatus("Error importing from iTunes: "+e.getMessage(), 10, 60);
		}
	}

	public void restart() {
		log.fatal("robonobo restarting");
		Thread restartThread = new Thread(new CatchingRunnable() {
			public void doRun() throws Exception {
				// Show a message that we're restarting
				SwingUtilities.invokeLater(new CatchingRunnable() {
					public void doRun() throws Exception {
						String[] butOpts = { "Quit" };
						int result = JOptionPane.showOptionDialog(RobonoboFrame.this, "robonobo is restarting, please wait...", "robonobo restarting",
								JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, butOpts, "Force Quit");
						if (result >= 0) {
							// They pressed the button... just kill everything
							log.fatal("Emergency shutdown during restart... pressing Big Red Switch");
							System.exit(1);
						}
					}
				});
				// Shut down the controller - this will block until the
				// controller exits
				controller.shutdown();
				// Hide this frame - don't dispose of it yet as this might make
				// the jvm exit
				SwingUtilities.invokeLater(new CatchingRunnable() {
					public void doRun() throws Exception {
						RobonoboFrame.this.setVisible(false);
					}
				});
				// Startup a new frame and controller
//				Robonobo.startup(null, cmdLineArgs, false);
				// Dispose of the old frame
				RobonoboFrame.this.dispose();
			}
		});
		restartThread.setName("Restart");
		restartThread.start();
	}

	public void shutdown() {
		setVisible(false);
		Thread shutdownThread = new Thread(new CatchingRunnable() {
			public void doRun() throws Exception {
				controller.shutdown();
				System.exit(0);
			}
		});
		shutdownThread.start();
	}

	public String getVersion() {
		return controller.getVersion();
	}

	public RobonoboController getController() {
		return controller;
	}

	public FileImportDropHandler getFileImportDropHandler() {
		return fileImportDropHandler;
	}

	/**
	 * Updates the status bar on the swing ui thread
	 */
	public void updateStatus(final String text, final int minDisplayTime, final int maxDisplayTime) {
		if (SwingUtilities.isEventDispatchThread())
			statusBar.setText(text, minDisplayTime, maxDisplayTime);
		else {
			SwingUtilities.invokeLater(new CatchingRunnable() {
				public void doRun() throws Exception {
					statusBar.setText(text, minDisplayTime, maxDisplayTime);
				}
			});
		}
	}

	public void loggedIn() {
		// Do nothing
	}

	@Override
	public void libraryChanged(Library lib) {
		// TODO Auto-generated method stub
		
	}
	
	public void playlistChanged(Playlist p) {
		// Do nothing
	}

	public void userChanged(User u) {
		// Do nothing
	}

	@Override
	public void userConfigChanged(UserConfig cfg) {
		// TODO Auto-generated method stub
		
	}
}
