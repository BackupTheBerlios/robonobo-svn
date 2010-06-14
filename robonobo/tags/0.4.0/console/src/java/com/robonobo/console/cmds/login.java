package com.robonobo.console.cmds;

import java.io.PrintWriter;

import com.robonobo.console.RobonoboConsole;

public class login implements ConsoleCommand {
	public void printHelp(PrintWriter out) {
		out.println("'login <email> <password>' logs into robonobo - contact macavity@well.com for an account");
	}

	public void run(RobonoboConsole console, String[] args, PrintWriter out) throws Exception {
		if(args.length < 2) {
			printHelp(out);
			return;
		}
		
		if(!console.getController().tryLogin(args[0], args[1]))
			out.println("Login as '"+args[0]+"' FAILED");
	}
}
