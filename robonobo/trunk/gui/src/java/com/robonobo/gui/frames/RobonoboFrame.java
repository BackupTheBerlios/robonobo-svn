package com.robonobo.gui.frames;

import java.awt.Dimension;

import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.debian.tablelayout.TableLayout;

import com.robonobo.common.exceptions.SeekInnerCalmException;
import com.robonobo.core.Platform;
import com.robonobo.core.RobonoboController;
import com.robonobo.gui.laf.RobonoboLookAndFeel;
import com.robonobo.gui.panels.LeftSidebar;
import com.robonobo.gui.panels.MainPanel;

@SuppressWarnings("serial")
public class RobonoboFrame extends JFrame {
	private JMenuBar menuBar;

	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel(new RobonoboLookAndFeel());
		} catch (UnsupportedLookAndFeelException e) {
			throw new SeekInnerCalmException(e);
		}
		final JFrame mainFrame = new RobonoboFrame();
		mainFrame.setVisible(true);
		mainFrame.setLocationRelativeTo(null);
		mainFrame.setVisible(true);
	}

	public RobonoboFrame() {
		setTitle("robonobo");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		menuBar = Platform.getPlatform().getMenuBar(this);
//		setJMenuBar(menuBar);

		JPanel contentPane = new JPanel();
		double[][] cellSizen = { { 5, 200, 5, TableLayout.FILL, 5 }, { 3, TableLayout.FILL, 5 } };
		contentPane.setLayout(new TableLayout(cellSizen));
		setContentPane(contentPane);

		JPanel leftSidebar = new LeftSidebar(this);
		contentPane.add(leftSidebar, "1,1");

		JPanel mainPanel = new MainPanel();
		contentPane.add(mainPanel, "3,1");

		// right panel (top + middle + bottom)

		setPreferredSize(new Dimension(1024, 723));
		pack();
	}

	public RobonoboController getController() {
		return null;
	}
}