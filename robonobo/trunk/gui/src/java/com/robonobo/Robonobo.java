package com.robonobo;

import java.awt.GraphicsEnvironment;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;

import com.robonobo.common.concurrent.CatchingRunnable;
import com.robonobo.console.RobonoboConsole;
import com.robonobo.core.Platform;
import com.robonobo.core.RobonoboController;
import com.robonobo.core.api.RobonoboStatus;
import com.robonobo.gui.frames.EULAFrame;
import com.robonobo.gui.frames.RobonoboFrame;

/**
 * Just a mainline - starts a RobonoboFrame or RobonoboConsole as appropriate
 * 
 * @author macavity
 */
public class Robonobo {
	private static final String HTML_EULA_PATH = "/eula.html";
	private static final String TEXT_EULA_PATH = "/eula.txt";

	public static void main(String[] args) throws Exception {
		// 1st-stage arg checker
		boolean consoleOnly = false;
		for (int i = 0; i < args.length; i++) {
			if (args[i].equalsIgnoreCase("-console"))
				consoleOnly = true;
		}
		if (GraphicsEnvironment.isHeadless())
			consoleOnly = true;
		Platform.getPlatform().init();
		if (!consoleOnly)
			Platform.getPlatform().setLookAndFeel();
		checkEulaAndStartup(args, consoleOnly);
	}

	public static void checkEulaAndStartup(final String[] args, boolean consoleOnly) throws Exception {
		// Make sure they've agreed to the eula
		final RobonoboController controller = new RobonoboController(args);
		if (controller.getConfig().getAgreedToEula())
			startup(controller, args, consoleOnly);
		else {
			if (consoleOnly) {
				boolean acceptedEula = showConsoleEula();
				if (acceptedEula) {
					controller.getConfig().setAgreedToEula(true);
					controller.saveConfig();
					startup(controller, args, true);
				} else {
					System.exit(0);
				}
			} else {
				CatchingRunnable onAccept = new CatchingRunnable() {
					public void doRun() throws Exception {
						controller.getConfig().setAgreedToEula(true);
						controller.saveConfig();
						startup(controller, args, false);
					}
				};
				CatchingRunnable onCancel = new CatchingRunnable() {
					public void doRun() throws Exception {
						System.exit(0);
					}
				};
				EULAFrame eulaFrame = new EULAFrame(HTML_EULA_PATH, controller.getExecutor(), onAccept, onCancel);
				eulaFrame.setVisible(true);
			}
		}

	}

	private static boolean showConsoleEula() throws IOException {
		// Copy the eula to a temporary file and ask them to read it
		InputStream is = Robonobo.class.getResourceAsStream(TEXT_EULA_PATH);
		File eulaFile = File.createTempFile("robonobo-eula-", ".txt");
		OutputStream os = new FileOutputStream(eulaFile);
		byte[] buf = new byte[1024];
		int numRead;
		while ((numRead = is.read(buf)) > 0) {
			os.write(buf, 0, numRead);
		}
		is.close();
		os.close();
		String promptText = "The robonobo End-User License Agreement has been copied to the file "
				+ eulaFile.getAbsolutePath()
				+ " - please read this file carefully and then type 'accept' or 'cancel' below.  By typing 'accept', you are agreeing to the terms of the agreement.\n";
		PrintStream out = System.out;
		out.println(promptText);
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		try {
			while (true) {
				out.print("> ");
				String response = in.readLine();
				if (response.equalsIgnoreCase("accept"))
					return true;
				if (response.equalsIgnoreCase("cancel"))
					return false;
				out.println("Please type either 'accept' or 'cancel'.");
			}
		} finally {
			eulaFile.delete();
		}
	}

	/**
	 * If this is a cold startup, argControl will be non-null as we need to create a controller to see if they've agreed to the eula. If it's a restart,
	 * argControl will be null, so we make a new controller
	 */
	public static void startup(RobonoboController argControl, String[] args, boolean consoleOnly) throws Exception, InterruptedException {
		final RobonoboController controller = (argControl == null) ? new RobonoboController(args) : argControl;
		// If there is no Download location set (probably first time through, set it)
		// Note, we do this here as Platform is not visible inside core
		if (controller.getConfig().getDownloadDirectory() == null) {
			File dd = Platform.getPlatform().getDefaultDownloadDirectory();
			dd.mkdirs();
			String ddPath = dd.getAbsolutePath();
			controller.getConfig().setDownloadDirectory(ddPath);
		}

		// Start the controller and the gui in parallel
		Thread cThread = new Thread(new CatchingRunnable() {
			public void doRun() throws Exception {
				controller.start();
			}
		});
		cThread.start();

		if (consoleOnly) {
			BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
			PrintWriter out = new PrintWriter(System.out);
			RobonoboConsole console = new RobonoboConsole("Console", controller, in, out);
			Thread consoleThread = new Thread(console);
			consoleThread.start();
			return;
		}

		System.out.println("flarp 3");
		final RobonoboFrame frame = new RobonoboFrame(controller, args);
		System.out.println("flarp 4");
		Platform.getPlatform().initMainWindow(frame);
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				frame.shutdown();
			}
		});
		frame.setVisible(true);
		// If we have no user, ask for login... otherwise wait until we have
		// tried to login, and if it fails prompt for details
		if (controller.getConfig().getMetadataServerUsername() == null)
			frame.showLogin(null);

		// Wait until the controller has started before checking to see
		// if we've logged in
		while (controller.getStatus() == RobonoboStatus.Stopped || controller.getStatus() == RobonoboStatus.Starting)
			Thread.sleep(100);
		if (controller.getMyUser() != null)
			frame.updateStatus("Login as " + frame.getController().getMyUser().getEmail() + " succeeded", 5, 30);
		else
			frame.showLogin(null);
	}
}
