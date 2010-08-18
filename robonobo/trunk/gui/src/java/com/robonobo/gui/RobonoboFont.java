package com.robonobo.gui;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.robonobo.common.exceptions.SeekInnerCalmException;

/**
 * robonobo only uses one font family, Bitstream Vera Sans.  We can't rely on this being available on the 
 * system, so we bundle it in our jar and build it as required.  This class keeps track of our 
 * instatiated font instances and re-uses them to save ram.
 * 
 * @author macavity
 */
public class RobonoboFont {
	static final String FONT_NAME = "Bitstream Vera Sans";
	static Font basePlainFont;
	static Font baseBoldFont;
	static Map<Integer, Font> derivedPlainFonts;
	static Map<Integer, Font> derivedBoldFonts;

	static {
		derivedPlainFonts = new HashMap<Integer, Font>();
		derivedBoldFonts = new HashMap<Integer, Font>();
		// See if our font is available to us from the system
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		Set<String> fontNames = new HashSet<String>(Arrays.asList(ge.getAvailableFontFamilyNames()));
		if(fontNames.contains(FONT_NAME)) {
			// w00t
			basePlainFont = new Font(FONT_NAME, Font.PLAIN, 12);
			baseBoldFont = new Font(FONT_NAME, Font.BOLD, 12);
			derivedPlainFonts.put(12, basePlainFont);
			derivedBoldFonts.put(12, baseBoldFont);
		} else {
			// Not on the system - build this sucka from our ttf file
			InputStream plainIs = RobonoboFont.class.getResourceAsStream("/font/Vera.ttf");
			InputStream boldIs = RobonoboFont.class.getResourceAsStream("/font/VeraBd.ttf");
			try {
				Font onePoint = Font.createFont(Font.TRUETYPE_FONT, plainIs);
				basePlainFont = onePoint.deriveFont(Font.PLAIN, 12);
				derivedPlainFonts.put(12, basePlainFont);
				onePoint = Font.createFont(Font.TRUETYPE_FONT, boldIs);
				baseBoldFont = onePoint.deriveFont(Font.BOLD, 12);
				derivedBoldFonts.put(12, baseBoldFont);
			} catch (Exception e) {
				throw new SeekInnerCalmException();
			}
		}
	}

	private RobonoboFont() {
		// Never instantiate this class
	}
	
	public static Font getFont(int size, boolean bold) {
		if(bold) {
			if(!derivedBoldFonts.containsKey(size))
				derivedBoldFonts.put(size, baseBoldFont.deriveFont(Font.BOLD, size));
			return derivedBoldFonts.get(size);
		} else {
			if(!derivedPlainFonts.containsKey(size))
				derivedPlainFonts.put(size, basePlainFont.deriveFont(Font.PLAIN, size));
			return derivedPlainFonts.get(size);
		}
	}
}
