package com.robonobo.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JToggleButton;

/**
 * A button within a group.  Of the group, when one button is selected, all the others are unselected.
 * @author macavity
 */
public class GroupButton extends JToggleButton {
	private GroupButtonGroup group;
	
	public GroupButton(Action arg0) {
		super(arg0);
		setupListener();
	}

	public GroupButton(Icon arg0, boolean arg1) {
		super(arg0, arg1);
		setupListener();
	}

	public GroupButton(Icon arg0) {
		super(arg0);
		setupListener();
	}

	public GroupButton(String arg0, boolean arg1) {
		super(arg0, arg1);
		setupListener();
	}

	public GroupButton(String arg0, Icon arg1, boolean arg2) {
		super(arg0, arg1, arg2);
		setupListener();
	}

	public GroupButton(String arg0, Icon arg1) {
		super(arg0, arg1);
		setupListener();
	}

	public GroupButton(String arg0) {
		super(arg0);
		setupListener();
	}

	/**
	 * When we get pressed, make sure the group knows about it
	 */
	protected void setupListener() {
		addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(group != null) {
					if(isSelected())
						group.notifyButtonPressed(GroupButton.this);
					else  // Not allowed to deselect, have to press another one
						setSelected(true);
				}
			}
		});
	}
	
	public void setGroup(GroupButtonGroup group) {
		this.group = group;
	}
}
