package com.robonobo.gui.frames;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.border.LineBorder;

import org.debian.tablelayout.TableLayout;

import com.robonobo.common.exceptions.SeekInnerCalmException;
import com.robonobo.core.Platform;

import furbelow.AbstractComponentDecorator;

public class SheetableFrame extends JFrame {
	private Dimmer dimmer;
	private JPanel glass;
	protected JComponent sheet;

	public SheetableFrame() {
		glass = (JPanel) getGlassPane();
	}

	public void showSheet(JComponent sheet) {
		glass.removeAll();
		double[][] cellSizen = { { TableLayout.FILL, sheet.getPreferredSize().width, TableLayout.FILL },
				{ sheet.getPreferredSize().height, TableLayout.FILL } };
		glass.setLayout(new TableLayout(cellSizen));
		glass.add(sheet, "1,0");
		glass.setVisible(true);
	}

	public synchronized void dim() {
		if (dimmer == null)
			dimmer = new Dimmer();
	}

	public synchronized void undim() {
		glass.setVisible(false);
		if (dimmer != null) {
			dimmer.dispose();
			dimmer = null;
		}
		sheet = null;
	}

	class Dimmer extends AbstractComponentDecorator implements KeyEventDispatcher {
		public Dimmer() {
			super(SheetableFrame.this.getLayeredPane());
			KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(this);
	        getPainter().addMouseListener(new MouseAdapter() { });
	        getPainter().addMouseMotionListener(new MouseMotionAdapter() { });
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
