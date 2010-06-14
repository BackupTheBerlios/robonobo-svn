package com.robonobo.gui;

import java.util.ArrayList;
import java.util.List;

public class GroupButtonGroup {
	List<GroupButton> btns = new ArrayList<GroupButton>();
	List<SelectListener> listeners = new ArrayList<SelectListener>();

	public GroupButtonGroup() {
	}

	public void addButton(GroupButton btn) {
		btns.add(btn);
		btn.setGroup(this);
	}

	public void deselectAll() {
		for (GroupButton btn : btns) {
			btn.setSelected(false);
		}
	}

	public void addListener(SelectListener listener) {
		listeners.add(listener);
	}

	public void removeListener(SelectListener listener) {
		listeners.remove(listener);
	}

	void notifyButtonPressed(GroupButton btn) {
		for (GroupButton thisBtn : btns) {
			if (thisBtn != btn)
				thisBtn.setSelected(false);
		}
		for (SelectListener listener : listeners) {
			listener.itemSelected();
		}
	}

	public interface SelectListener {
		public void itemSelected();
	}
}
