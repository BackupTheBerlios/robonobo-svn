package com.robonobo.gui.panels;

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
import com.robonobo.gui.components.base.*;
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
		RLabel title = new RLabel14B("Delete playlist '"+p.getTitle()+"'");
		add(title, "1,1,4,1,LEFT,CENTER");
		
		RLabel blurb = new RLabel12("<html><center>Are you sure you want to delete this playlist?</center></html>");
		add(blurb, "1,3,4,3");
		
		RButton delBtn = new RGlassButton("DELETE");
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
		RButton cancelBtn = new RRedGlassButton("CANCEL");
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
