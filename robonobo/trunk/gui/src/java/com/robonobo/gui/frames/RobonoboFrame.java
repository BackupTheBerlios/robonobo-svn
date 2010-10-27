package com.robonobo.gui.frames;

import static com.robonobo.common.util.FileUtil.*;

import java.awt.Dimension;
import java.io.File;
import java.io.FileFilter;
import java.security.SecureRandom;
import java.util.*;

import javax.imageio.ImageIO;
import javax.swing.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.debian.tablelayout.TableLayout;
import org.doomdark.uuid.UUIDGenerator;

import com.robonobo.Robonobo;
import com.robonobo.common.concurrent.CatchingRunnable;
import com.robonobo.common.exceptions.SeekInnerCalmException;
import com.robonobo.common.util.FileUtil;
import com.robonobo.core.Platform;
import com.robonobo.core.RobonoboController;
import com.robonobo.core.api.*;
import com.robonobo.core.api.model.*;
import com.robonobo.gui.GUIUtils;
import com.robonobo.gui.dialogs.LoginDialog;
import com.robonobo.gui.laf.RobonoboLookAndFeel;
import com.robonobo.gui.panels.*;
import com.robonobo.gui.preferences.PrefDialog;
import com.robonobo.mina.external.ConnectedNode;
import com.sun.org.apache.xml.internal.utils.StopParseException;

@SuppressWarnings("serial")
public class RobonoboFrame extends JFrame implements RobonoboStatusListener {
	private RobonoboController control;
	private String[] cmdLineArgs;
	private JMenuBar menuBar;
	PrefDialog prefDialog;
	private MainPanel mainPanel;
	private LeftSidebar leftSidebar;
	private Log log = LogFactory.getLog(RobonoboFrame.class);

	/**
	 * For debugging only! Delete me when the GUI is done :-)
	 */
	public static void main(String[] args) throws Exception {
		try {
			UIManager.setLookAndFeel(new RobonoboLookAndFeel());
		} catch (UnsupportedLookAndFeelException e) {
			throw new SeekInnerCalmException(e);
		}
		RobonoboController control = new RobonoboController(new String[0]);
		final JFrame mainFrame = new RobonoboFrame(control, args);
		mainFrame.setVisible(true);
		mainFrame.setLocationRelativeTo(null);
		mainFrame.setVisible(true);
	}

	public RobonoboFrame(RobonoboController control, String[] args) {
		this.control = control;
		this.cmdLineArgs = args;

		setTitle("robonobo");
		setIconImage(GUIUtils.getImage("/img/icon/robonobo-64x64.png"));
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		menuBar = Platform.getPlatform().getMenuBar(this);
		setJMenuBar(menuBar);
		
		JPanel contentPane = new JPanel();
		double[][] cellSizen = { { 5, 200, 5, TableLayout.FILL, 5 }, { 3, TableLayout.FILL, 5 } };
		contentPane.setLayout(new TableLayout(cellSizen));
		setContentPane(contentPane);
		leftSidebar = new LeftSidebar(this);
		contentPane.add(leftSidebar, "1,1");
		mainPanel = new MainPanel(this);
		contentPane.add(mainPanel, "3,1");
		setPreferredSize(new Dimension(1024, 723));
		pack();
		leftSidebar.selectMyMusic();
		
		if(control.getStatus() != RobonoboStatus.Stopped)
			setupPrefDialog();
	}

	public LeftSidebar getLeftSidebar() {
		return leftSidebar;
	}
	
	public PlaybackPanel getPlaybackPanel() {
		return mainPanel.getPlaybackPanel();
	}
	
	public MainPanel getMainPanel() {
		return mainPanel;
	}
	
	@Override
	public void roboStatusChanged() {
		// The preference dialog depends on the controller's config being available
		if(prefDialog == null)
			setupPrefDialog();
	}
	
	@Override
	public void connectionAdded(ConnectedNode node) {
		// Do nothing
	}
	
	@Override
	public void connectionLost(ConnectedNode node) {
		// Do nothing
	}
	
	private void setupPrefDialog() {
		prefDialog = new PrefDialog(this);
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
			RobonoboStatus status = control.getStatus();
			if (status == RobonoboStatus.Stopping || status == RobonoboStatus.Stopped)
				return streams;
			String filePath = file.getAbsolutePath();
			updateStatus("Adding share from file " + filePath, 0, 60);
			Stream s = null;
			try {
				s = control.addShare(filePath);
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

			List<File> files = control.getITunesLibrary(mp3Filter);
			importFiles(files);
			Map<String, List<File>> itPls = control.getITunesPlaylists(mp3Filter);
			for (String pName : itPls.keySet()) {
				Playlist p = control.getMyPlaylistByTitle(pName);
				if (p == null) {
					p = new Playlist();
					p.setPlaylistId(UUIDGenerator.getInstance().generateRandomBasedUUID(new SecureRandom()).toString());
					p.setTitle(pName);
					p.getOwnerIds().add(control.getMyUser().getUserId());
					List<File> tracks = itPls.get(pName);
					for (File track : tracks) {
						SharedTrack sh = control.getShareByFilePath(track);
						if (sh == null)
							log.error("ITunes playlist '" + pName + "' has track '" + track + "', but I am not sharing it");
						else
							p.getStreamIds().add(sh.getStream().getStreamId());
					}
					control.addOrUpdatePlaylist(p);
				} else {
					// Update existing playlist - add each track if it's not already in there
					List<File> tracks = itPls.get(pName);
					for (File track : tracks) {
						SharedTrack sh = control.getShareByFilePath(track);
						if (sh == null)
							log.error("ITunes playlist '" + pName + "' has track '" + track + "', but I am not sharing it");
						else if(!p.getStreamIds().contains(sh.getStream().getStreamId()))
							p.getStreamIds().add(sh.getStream().getStreamId());
					}
					control.addOrUpdatePlaylist(p);
				}
			}
			updateStatus("Finished iTunes import", 1, 5);
		} catch (RobonoboException e) {
			log.error("Error importing from iTunes", e);
			updateStatus("Error importing from iTunes: "+e.getMessage(), 10, 60);
		}
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
					control.getExecutor().execute(new CatchingRunnable() {
						public void doRun() throws Exception {
							importFilesOrDirectories(Arrays.asList(selFiles));
						}
					});
				}
			}
		};
		if (control.getMyUser() != null)
			flarp.run();
		else
			showLogin(flarp);
	}

	/**
	 * @param onLogin
	 *            If the login is successful, this will be executed on the Swing
	 *            GUI thread (so don't do too much in it)
	 */
	public void showLogin(Runnable onLogin) {
		LoginDialog ld = new LoginDialog(this, onLogin);
		ld.setLocationRelativeTo(this);
		ld.setVisible(true);
	}

	public void showPreferences() {
		prefDialog.setVisible(true);
	}

	public void updateStatus(String msg, int minShowSecs, int maxShowSecs) {
		// TODO figure out how we're doing this
	}
	
	public void shutdown() {
		setVisible(false);
		Thread shutdownThread = new Thread(new CatchingRunnable() {
			public void doRun() throws Exception {
				control.shutdown();
				System.exit(0);
			}
		});
		shutdownThread.start();
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
				control.shutdown();
				// Hide this frame - don't dispose of it yet as this might make
				// the jvm exit
				SwingUtilities.invokeLater(new CatchingRunnable() {
					public void doRun() throws Exception {
						RobonoboFrame.this.setVisible(false);
					}
				});
				// Startup a new frame and controller
				Robonobo.startup(null, cmdLineArgs, false);
				// Dispose of the old frame
				RobonoboFrame.this.dispose();
			}
		});
		restartThread.setName("Restart");
		restartThread.start();
	}

	public RobonoboController getController() {
		return control;
	}
}