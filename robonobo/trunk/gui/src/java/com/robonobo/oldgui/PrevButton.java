package com.robonobo.oldgui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import com.robonobo.core.RobonoboController;

public class PrevButton extends MainButton /* implements NextPrevListener */{
	public static final String IMG_PREV = "/img/Previous.png";
	public PrevButton(final RobonoboController controller) {
		super(IMG_PREV, "Play previous track");
//		controller.addNextPrevListener(this);
		setEnabled(false);
		addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
//				controller.previous();
			}
		});
	}

//	@Override
//	public void updateNextPrev(boolean canNext, boolean canPrev) {
//		setEnabled(canPrev);
//	}
}
