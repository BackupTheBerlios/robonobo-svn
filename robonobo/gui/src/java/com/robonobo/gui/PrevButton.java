package com.robonobo.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import com.robonobo.core.RobonoboController;
import com.robonobo.core.api.NextPrevListener;

public class PrevButton extends MainButton implements NextPrevListener {
	public static final String IMG_PREV = "/img/Previous.png";
	public PrevButton(final RobonoboController controller) {
		super(IMG_PREV, "Play previous track");
		controller.addNextPrevListener(this);
		setEnabled(false);
		addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				controller.previous();
			}
		});
	}

	public void canPlayNext(boolean canNext) {
		// Do nothing 
	}

	public void canPlayPrevious(boolean canPrev) {
		setEnabled(canPrev);
	}
}
