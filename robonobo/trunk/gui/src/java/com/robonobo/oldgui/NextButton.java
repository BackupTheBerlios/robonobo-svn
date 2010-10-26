package com.robonobo.oldgui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import com.robonobo.core.RobonoboController;
import com.robonobo.core.api.NextPrevListener;

public class NextButton extends MainButton implements NextPrevListener {
	public static final String IMG_NEXT = "/img/Next.png";

	public NextButton(final RobonoboController controller) {
		super(IMG_NEXT, "Play next track");
		controller.addNextPrevListener(this);
		setEnabled(false);
		addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				controller.next();
			}
		});
	}

	@Override
	public void updateNextPrev(boolean canNext, boolean canPrev) {
		setEnabled(canNext);
	}

//	public void canPlayNext(boolean canNext) {
//		setEnabled(canNext);
//	}
//
//	public void canPlayPrevious(boolean canPrev) {
//		// Do nothing
//	}
}
