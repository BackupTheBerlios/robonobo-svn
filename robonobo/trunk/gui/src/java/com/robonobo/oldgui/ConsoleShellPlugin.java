package com.robonobo.oldgui;

import gnu.iou.sh.Shell.Plugin;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;

import com.robonobo.console.RobonoboConsole;

public class ConsoleShellPlugin implements Plugin {
	RobonoboFrame frame;
	
	public ConsoleShellPlugin(RobonoboFrame frame) {
		this.frame = frame;
	}

	public void console(DataInputStream stdin, PrintStream stdout, PrintStream stderr) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(stdin));
		PrintWriter writer = new PrintWriter(stdout);
		RobonoboConsole console = new RobonoboConsole("ConsoleThread", frame.getController(), reader, writer);
		try {
			console.doRun();
		} catch (Exception e) {
			throw new IOException("Caught "+e.getClass().getName()+": "+e.getMessage());
		}
	}

	public boolean exception(Exception exc, PrintStream stdout, PrintStream stderr) {
		// Don't continue execution, just exit
		return false;
	}

	public String userVersion() {
		return "Robonobo Console v"+frame.getVersion();
	}

}
