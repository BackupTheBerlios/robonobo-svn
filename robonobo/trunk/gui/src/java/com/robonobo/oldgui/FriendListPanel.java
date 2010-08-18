package com.robonobo.oldgui;

import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JLabel;
import javax.swing.JPanel;

import com.robonobo.core.Platform;
import com.robonobo.core.api.model.User;

public class FriendListPanel extends JPanel {
	public FriendListPanel(final RobonoboFrame frame) {
		setLayout(new FlowLayout());
		JLabel titleLbl = new JLabel("Friends");
		titleLbl.setFont(titleLbl.getFont().deriveFont(Font.BOLD));
		add(titleLbl);
		for (Long friendId : frame.getController().getMyUser().getFriendIds()) {
			final User ff = frame.getController().getUser(friendId);
			JLabel plLbl = new JLabel(ff.getFriendlyName());
			plLbl.setForeground(Platform.getPlatform().getLinkColor());
			plLbl.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent e) {
					frame.getContentHolder().bringPanelToFront("friend-"+ff.getEmail());
				}
			});
			add(plLbl);
		}

	}
}
