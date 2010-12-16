package com.robonobo.gui.frames;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ThreadPoolExecutor;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;

import org.debian.tablelayout.TableLayout;

import com.robonobo.common.exceptions.SeekInnerCalmException;

@SuppressWarnings("serial")
public class EULAFrame extends JFrame {
	private boolean madeDecision = false;
	
	public EULAFrame(String eulaPath, final ThreadPoolExecutor executor, final Runnable onAccept, final Runnable onCancel) {
		Dimension sz = new Dimension(600, 400);
		setSize(sz);
		setPreferredSize(sz);
		double[][] cellSizen = { { 5, TableLayout.FILL, 80, 5, 80, 5 }, { 5, TableLayout.FILL, 10, 30, 5 } };
		setLayout(new TableLayout(cellSizen));
		setTitle("robonobo license agreement");
		JTextPane textPane = new JTextPane();
		textPane.setContentType("text/html");
		textPane.setText(getHtmlEula(eulaPath));
		textPane.setEditable(false);
		add(new JScrollPane(textPane), "1,1,4,1");
		JButton acceptBtn = new JButton("Accept");
		acceptBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
				madeDecision = true;
				executor.execute(onAccept);
			}
		});
		add(acceptBtn, "2,3");
		JButton cancelBtn = new JButton("Cancel");
		cancelBtn.setName("robonobo.red.button");
		cancelBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
				madeDecision = true;
				executor.execute(onCancel);
			}
		});
		add(cancelBtn, "4,3");
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				if(!madeDecision)
					executor.execute(onCancel);
			}
		});

	}
	
	private String getHtmlEula(String eulaPath) {
		InputStream is = getClass().getResourceAsStream(eulaPath);
		StringBuffer sb = new StringBuffer();
		byte[] buf = new byte[1024];
		int numRead;
		try {
			while((numRead = is.read(buf)) > 0) {
				sb.append(new String(buf, 0, numRead));
			}
			is.close();
		} catch (IOException e) {
			throw new SeekInnerCalmException(e);
		}
		return sb.toString();
	}
}
