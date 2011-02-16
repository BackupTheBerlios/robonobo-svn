package com.robonobo.gui.frames;

import java.awt.*;
import java.awt.event.*;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.*;
import javax.swing.border.LineBorder;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.debian.tablelayout.TableLayout;

import com.robonobo.common.exceptions.SeekInnerCalmException;
import com.robonobo.core.Platform;
import com.robonobo.gui.RoboColor;
import com.robonobo.gui.sheets.Sheet;

import furbelow.AbstractComponentDecorator;

public class SheetableFrame extends JFrame {
	private Dimmer dimmer;
	private JPanel glass;
	protected Sheet sheet;
	Log log = LogFactory.getLog(getClass());
	protected ReentrantLock sheetLock = new ReentrantLock(); // One at a time, fellas

	public SheetableFrame() {
		glass = (JPanel) getGlassPane();
	}

	public void showSheet(Sheet sheet) {
		if(sheet == null) {
			undim();
			return;
		}
		sheetLock.lock();
		try {
			if (dimmer == null)
				dimmer = new Dimmer();
			this.sheet = sheet;
			SheetContainer sc = new SheetContainer(sheet);
			glass.removeAll();
			double[][] cellSizen = { { TableLayout.FILL, sc.getPreferredSize().width, TableLayout.FILL },
					{ sc.getPreferredSize().height, TableLayout.FILL } };
			glass.setLayout(new TableLayout(cellSizen));
			glass.add(sc, "1,0");
			glass.setVisible(true);
			getRootPane().setDefaultButton(sheet.defaultButton());
		} finally {
			sheetLock.unlock();
		}
		sheet.onShow();
	}

	public synchronized void undim() {
		sheetLock.lock();
		try {
			if(sheet == null)
				return;
			sheet.onUndim();
			glass.setVisible(false);
			if (dimmer != null) {
				dimmer.dispose();
				dimmer = null;
			}
			sheet = null;
			getRootPane().setDefaultButton(null);
		} finally {
			sheetLock.unlock();
		}
	}

	public boolean isShowingSheet() {
		sheetLock.lock();
		try {
			return (sheet != null);
		} finally {
			sheetLock.unlock();
		}
	}
	class SheetContainer extends JPanel {
		public SheetContainer(JComponent sheet) {
			// Make a 1px grey border and a 5px white background around the sheet
			double[][] cellSizen = { { 3, sheet.getPreferredSize().width, 5 }, { 2, sheet.getPreferredSize().height } };
			setLayout(new TableLayout(cellSizen));
			add(sheet, "1,1");
			Dimension sz = new Dimension(sheet.getPreferredSize().width + 8, sheet.getPreferredSize().height + 7);
			setPreferredSize(sz);
			setOpaque(true);
			setBackground(Color.WHITE);
			setBorder(BorderFactory.createLineBorder(RoboColor.LIGHT_GRAY));
		}
	}

	class Dimmer extends AbstractComponentDecorator implements KeyEventDispatcher {
		public Dimmer() {
			super(SheetableFrame.this.getLayeredPane());
			KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(this);
			getPainter().addMouseListener(new MouseAdapter() {
			});
			getPainter().addMouseMotionListener(new MouseMotionAdapter() {
			});
		}

		@Override
		public void paint(Graphics g) {
			Color bg = getComponent().getBackground();
			Color c = new Color(bg.getRed(), bg.getGreen(), bg.getBlue(), 128);
			Rectangle r = getDecorationBounds();
			g = g.create();
			g.setColor(c);
			g.fillRect(r.x, r.y, r.width, r.height);
			g.dispose();
		}

		@Override
		public void dispose() {
			super.dispose();
			KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(this);
		}

		@Override
		public boolean dispatchKeyEvent(KeyEvent e) {
			return SwingUtilities.isDescendingFrom(e.getComponent(), getComponent());
		}
	}
}
