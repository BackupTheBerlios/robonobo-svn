package com.robonobo.gui;

import java.awt.Dimension;
import java.io.IOException;
import java.io.InputStream;

import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;

import org.debian.tablelayout.TableLayout;

import com.robonobo.common.exceptions.SeekInnerCalmException;

@SuppressWarnings("serial")
public class AboutDialog extends JDialog {
	private static final String CREDITS_PATH = "/credits.html";
	Dimension sz = new Dimension(450, 200);
	RobonoboFrame frame;

	public AboutDialog(RobonoboFrame frame) {
		super(frame, false);
		this.frame = frame;
		setTitle("About robonobo");
		setSize(sz);
		setPreferredSize(sz);
		double[][] cellSizen = { { TableLayout.FILL }, { TableLayout.FILL } };
		getContentPane().setLayout(new TableLayout(cellSizen));
		JTextPane textPane = new JTextPane();
		textPane.setContentType("text/html");
		textPane.setText(getCredits());
		textPane.setEditable(false);
		JScrollPane scrollPane = new JScrollPane(textPane);
		add(scrollPane, "0,0");
	}

	private String getCredits() {
		InputStream is = getClass().getResourceAsStream(CREDITS_PATH);
		StringBuffer sb = new StringBuffer();
		byte[] buf = new byte[1024];
		int numRead;
		try {
			while ((numRead = is.read(buf)) > 0) {
				sb.append(new String(buf, 0, numRead));
			}
			is.close();
		} catch (IOException e) {
			throw new SeekInnerCalmException(e);
		}
		return sb.toString().replace("!VERSION!", frame.getController().getVersion());
	}
}
