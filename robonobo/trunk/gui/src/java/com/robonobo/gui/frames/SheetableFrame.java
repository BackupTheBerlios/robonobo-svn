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

import furbelow.AbstractComponentDecorator;

public class SheetableFrame extends JFrame {
	private Dimmer dimmer;
	private JPanel glass;
	protected JComponent sheet;
	Log log = LogFactory.getLog(getClass());
	protected ReentrantLock sheetLock = new ReentrantLock(); // One at a time, fellas

	public SheetableFrame() {
		glass = (JPanel) getGlassPane();
	}

	public void showSheet(JComponent sheet) {
		sheetLock.lock();
		try {
			log.debug("showing sheet: " + sheet);
			SheetContainer sc = new SheetContainer(sheet);
			glass.removeAll();
			double[][] cellSizen = { { TableLayout.FILL, sc.getPreferredSize().width, TableLayout.FILL },
					{ sc.getPreferredSize().height, TableLayout.FILL } };
			glass.setLayout(new TableLayout(cellSizen));
			glass.add(sc, "1,0");
			glass.setVisible(true);
		} finally {
			sheetLock.unlock();
		}
	}

	public synchronized void dim() {
		sheetLock.lock();
		try {
			log.fatal("dimming");
			if (dimmer == null)
				dimmer = new Dimmer();
		} finally {
			sheetLock.unlock();
		}
	}

	public synchronized void undim() {
		sheetLock.lock();
		try {
			log.debug("undimming");
			glass.setVisible(false);
			if (dimmer != null) {
				dimmer.dispose();
				dimmer = null;
			}
			sheet = null;
		} finally {
			sheetLock.unlock();
		}
	}

	class SheetContainer extends JPanel {
		public SheetContainer(JComponent sheet) {
			// Make a 1px dark border and a 5px hatched background around the sheet
			double[][] cellSizen = { { 3, sheet.getPreferredSize().width, 5 }, { 2, sheet.getPreferredSize().height } };
			setLayout(new TableLayout(cellSizen));
			add(sheet, "1,1");
			Dimension sz = new Dimension(sheet.getPreferredSize().width + 8, sheet.getPreferredSize().height + 7);
			setPreferredSize(sz);
			setOpaque(true);
			// TODO Apply hatched bg here
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
