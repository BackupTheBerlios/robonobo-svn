package com.robonobo.console.cmds;

import static com.robonobo.common.util.FileUtil.*;
import static com.robonobo.common.util.TextUtil.*;

import java.io.PrintWriter;
import java.util.List;

import com.robonobo.console.RobonoboConsole;
import com.robonobo.core.RobonoboController;
import com.robonobo.mina.external.ConnectedNode;

public class network implements ConsoleCommand {
	static final char GAMMA = 0x03b3;
	
	public void printHelp(PrintWriter out) {
		out.println("'network' gives current network status\n");
	}

	public void run(RobonoboConsole console, String[] args, PrintWriter out) throws Exception {
		RobonoboController controller = console.getController();
		if (args.length == 0) {
			if (controller.isNetworkRunning()) {
				out.println("I am " + controller.getMyNodeId());
				List<String> urls = controller.getMyEndPointUrls();
				out.println("My listening endpoints:");
				for (String url : urls) {
					out.println(url);
				}
				List<ConnectedNode> nodes = controller.getConnectedNodes();
				out.println("\nConnections:");
				if (nodes.size() > 0)
					out.println(rightPad("Id", 36) + rightPad(" Url", 37) + rightPad(" Super", 6) + rightPad(" Up", 12)
							+ rightPad(" Down", 12) + rightPad(" My Bid", 7) + rightPad(" My "+GAMMA, 6)
							+ rightPad(" Their Bid", 10) + rightPad(" Their "+GAMMA, 8));
				for (ConnectedNode node : nodes) {
					out.println(rightPadOrTruncate(node.getNodeId(), 36) + " "
							+ rightPadOrTruncate(node.getEndPointUrl(), 36) + " "
							+ rightPadOrTruncate((node.isSupernode()) ? "Yes" : "No", 5) + " "
							+ rightPadOrTruncate(humanReadableSize(node.getUploadRate()) + "/s", 11) + " "
							+ rightPadOrTruncate(humanReadableSize(node.getDownloadRate()) + "/s", 11) + " "
							+ rightPad(padToMinWidth(node.getMyBid(), 4), 6) + " "
							+ rightPad(padToMinWidth(node.getMyGamma(), 4), 5) + " "
							+ rightPad(padToMinWidth(node.getTheirBid(), 4), 9) + " "
							+ rightPad(padToMinWidth(node.getTheirGamma(), 4), 7)
					);
				}
			} else {
				out.println("[Network stopped]");
			}
		} else
			printHelp(out);
	}
}
