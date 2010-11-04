package com.robonobo.gui.components;

import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.image.BufferedImage;

import furbelow.AbstractAnimatedIcon;

/**
 * Renders a circle, starting at 9oclock and growing clockwise until it is complete (over 1 sec), and then disappearing
 * (also from 9oc, clockwise) until disappeared (also over 1 sec)
 * 
 * @author macavity
 * 
 */
class SpinnerIcon extends AbstractAnimatedIcon {
	static int INTERVAL_ANGLE = 30;
	static int FRAME_COUNT = (360 / INTERVAL_ANGLE) * 2;
	static int RENDER_INTERVAL = 1000 / (360 / INTERVAL_ANGLE);
	int w;
	Color c;
	int arcOffset, arcWidth;
	int clipOffset, clipWidth;
	float strokeSz;
	private Image[] frames;

	public SpinnerIcon(int w, Color c) {
		super(FRAME_COUNT, RENDER_INTERVAL);
		this.w = w;
		this.c = c;
		frames = new Image[getFrameCount()];
		strokeSz = w / 5f;
		arcOffset = Math.round(strokeSz / 2);
		arcWidth = (int) (w * 0.8f);
		clipOffset = -arcOffset;
		clipWidth = (int) (w * 1.2f);
	}

	@Override
	protected void paintFrame(Component cmp, Graphics graphics, int x, int y) {
		int idx = getFrame();
		if (frames[idx] == null) {
			Image image;
			if (cmp != null)
				image = cmp.getGraphicsConfiguration().createCompatibleImage(w, w, Transparency.TRANSLUCENT);
			else
				image = new BufferedImage(w, w, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = (Graphics2D) image.getGraphics();
			// For some reason if we draw an arc each frame, the lines 'jump' slightly between frames, so instead we
			// draw a circle each time and clip it with an pie arc
			g.setComposite(AlphaComposite.Clear);
			g.fillRect(0, 0, w, w);
			g.setComposite(AlphaComposite.Src);
			if (idx > 0) {
				g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g.setColor(c);
				int arcAngle;
				if (idx <= FRAME_COUNT / 2) {
					// Drawing the circle appearing
					arcAngle = -(INTERVAL_ANGLE * idx);
				} else {
					// Drawing the circle 'disappearing'
					arcAngle = 360 - (INTERVAL_ANGLE * (idx - (FRAME_COUNT / 2)));
				}
				Shape pieSegment = new Arc2D.Double(clipOffset, clipOffset, clipWidth, clipWidth, 180, arcAngle,
						Arc2D.PIE);
				g.setClip(pieSegment);
				g.setStroke(new BasicStroke(strokeSz, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER));
				g.drawArc(arcOffset, arcOffset, arcWidth, arcWidth, 0, 360);
			}
			g.dispose();
			frames[idx] = image;
		}
		graphics.drawImage(frames[idx], x, y, null);
	}
	@Override
	public int getIconWidth() {
		return w;
	}

	@Override
	public int getIconHeight() {
		return w;
	}

}