package com.robonobo.console;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import com.robonobo.common.concurrent.CatchingRunnable;
import com.robonobo.common.util.TextUtil;
import com.robonobo.console.cmds.ConsoleCommand;
import com.robonobo.core.RobonoboController;

/**
 * @author Ray
 */
public class RobonoboConsole extends CatchingRunnable {
	private RobonoboController controller;
	private BufferedReader in;
	private PrintWriter out;
	private DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

	public static void main(String[] args) throws Exception {
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		PrintWriter out = new PrintWriter(System.out);
		RobonoboController control = new RobonoboController(args);
		control.start();
		RobonoboConsole instance = new RobonoboConsole("Console", control, in, out);
		Thread myThread = new Thread(instance);
		myThread.start();
	}


	public RobonoboConsole(String threadName, RobonoboController controller, BufferedReader in, PrintWriter out) {
		super(threadName);
		this.controller = controller;
		this.in = in;
		this.out = out;
	}


	@Override
	public void doRun() throws Exception {
		printMotd();
		while(true) {
			printPrompt();
			out.flush();
			String cmdLine = in.readLine();
			String[] args = TextUtil.getQuotedArgs(cmdLine);
			if(args.length == 0)
				continue;
			String cmdName = args[0];
			String className = "com.robonobo.console.cmds."+cmdName;
			Class cmdClass = null;
			try {
				cmdClass = Class.forName(className);
			} catch(ClassNotFoundException e) {
				out.println("No such cmd '"+cmdName+"'\n");
				out.flush();
				continue;
			}
			ConsoleCommand cmd = (ConsoleCommand)cmdClass.newInstance();
			String[] cmdArgs = new String[args.length-1];
			System.arraycopy(args, 1, cmdArgs, 0, args.length-1);
			try {
				cmd.run(this, cmdArgs, out);
			} catch(Exception e) {
				e.printStackTrace();
			}
			out.flush();
		}
	}


	private void printMotd() {
		out.println("Welcome to the robonobo console.  Type 'help' for help.");
	}

	private void printPrompt() {
		out.print("rbnb > ");
	}

	public RobonoboController getController() {
		return controller;
	}

	public DateFormat getDateFormat() {
		return dateFormat;
	}	
}
