package com.robonobo.gui.panels;

import java.awt.Dimension;
import java.awt.event.*;

import javax.swing.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.debian.tablelayout.TableLayout;

import com.robonobo.common.concurrent.CatchingRunnable;
import com.robonobo.core.Platform;
import com.robonobo.core.api.RobonoboException;
import com.robonobo.core.api.model.Playlist;
import com.robonobo.gui.RoboFont;
import com.robonobo.gui.frames.RobonoboFrame;

@SuppressWarnings("serial")
public class DeletePlaylistPanel extends JPanel implements KeyListener {
	RobonoboFrame frame;
	Playlist p;
	Log log = LogFactory.getLog(getClass());
	
	public DeletePlaylistPanel(RobonoboFrame rFrame, Playlist pl) {
		this.frame = rFrame;
		this.p = pl;
		double[][] cellSizen = { {10, TableLayout.FILL, 100, 5, 100, 10}, { 10, 25, 10, TableLayout.FILL, 10, 30, 10 } };
		setLayout(new TableLayout(cellSizen));
		setName("playback.background.panel");
		JLabel title = new JLabel("Delete playlist '"+p.getTitle()+"'");
		title.setFont(RoboFont.getFont(14, true));
		add(title, "1,1,4,1,LEFT,CENTER");
		
		JLabel blurb = new JLabel("<html><center>Are you sure you want to delete this playlist?</center></html>");
		blurb.setFont(RoboFont.getFont(12, false));
		add(blurb, "1,3,4,3");
		
		JButton delBtn = new JButton("DELETE");
		delBtn.setFont(RoboFont.getFont(12, true));
		delBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				frame.getLeftSidebar().selectMyMusic();
				frame.getController().getExecutor().execute(new CatchingRunnable() {
					public void doRun() throws Exception {
						try {
							frame.getController().nukePlaylist(p);
						} catch (RobonoboException e) {
							log.error("Error deleting playlist", e);
						}
					}
				});
				DeletePlaylistPanel.this.setVisible(false);
			}
		});
		add(delBtn, "2,5");
		JButton cancelBtn = new JButton("CANCEL");
		cancelBtn.setName("robonobo.red.button");
		cancelBtn.setFont(RoboFont.getFont(12, true));
		cancelBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				DeletePlaylistPanel.this.setVisible(false);
			}
		});
		add(cancelBtn, "4,5");
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if(!visible)
			frame.undim();
	}


	public void keyPressed(KeyEvent e) {
		int code = e.getKeyCode();
		int modifiers = e.getModifiers();
		if (code == KeyEvent.VK_ESCAPE)
			setVisible(false);
		if (code == KeyEvent.VK_Q && modifiers == Platform.getPlatform().getCommandModifierMask())
			frame.shutdown();
	}

	public void keyReleased(KeyEvent e) {
	}// Do nothing

	public void keyTyped(KeyEvent e) {
	}// Do nothing

}
