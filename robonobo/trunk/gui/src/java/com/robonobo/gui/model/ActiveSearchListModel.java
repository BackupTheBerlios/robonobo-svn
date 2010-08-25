/**
 * 
 */
package com.robonobo.gui.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.DefaultListModel;

import com.robonobo.core.RobonoboController;

/**
 * TODO Make this actually do stuff
 */
@SuppressWarnings("serial")
public class ActiveSearchListModel extends DefaultListModel {
	List<String> flarp = new ArrayList<String>(Arrays.asList("a search", "another search", "a really really really really long search phrase"));
	RobonoboController control;
	
	public ActiveSearchListModel(RobonoboController control) {
		this.control = control;
	}
	
	public Object getElementAt(int index) {
		return flarp.get(index);
	}

	public int getSize() {
		return flarp.size();
	}

	public void removeElementAt(int index) {
		flarp.remove(index);
		fireIntervalRemoved(this, index, index);
	}
}