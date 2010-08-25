package com.robonobo.gui.platform;

import java.awt.Event;
import java.io.IOException;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

import com.apple.eawt.Application;
import com.apple.eio.FileManager;
import com.robonobo.common.util.CodeUtil;
import com.robonobo.core.itunes.ITunesService;
import com.robonobo.gui.itunes.mac.MacITunesService;
import com.robonobo.oldgui.RobonoboFrame;

public class MacPlatform extends UnknownPlatform {
	
	@Override
	public void init() {
		System.setProperty("apple.laf.useScreenMenuBar", "true");
		System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Robonobo");				
	}
	
	@Override
	public void initMainWindow(JFrame jFrame) throws Exception {
		RobonoboFrame frame = (RobonoboFrame) jFrame;
		Application app = Application.getApplication();
		app.addApplicationListener(new MacAppListener(frame));
		app.addAboutMenuItem();
		app.setEnabledAboutMenu(true);
		app.addPreferencesMenuItem();
		app.setEnabledPreferencesMenu(true);
	}

	@Override
	public void setLookAndFeel() {
		// Leave default apple l&f - or should we use quaqua?
	}
	
	@Override
	public boolean shouldSetMenuBarOnDialogs() {
		return true;
	}
	
	@Override
	public boolean shouldShowPrefsInFileMenu() {
		return false;
	}

	
	@Override
	public boolean shouldShowQuitInFileMenu() {
		return false;
	}
	
	@Override
	public boolean shouldShowOptionsMenu() {
		return false;
	}
	
	@Override
	public boolean shouldShowAboutInHelpMenu() {
		return false;
	}
	
	@Override
	public KeyStroke getAccelKeystroke(int key) {
		return KeyStroke.getKeyStroke(key, Event.META_MASK);
	}
	
	@Override
	public int getCommandModifierMask() {
		return Event.META_MASK;
	}
	
	@Override
	public boolean iTunesAvailable() {
		return true;
	}
	
	@Override
	public ITunesService getITunesService() {
		return new MacITunesService();
	}
	
	@Override
	public void customizeMainbarButtons(List<? extends JButton> btns) {
		for(int i=0;i<btns.size();i++) {
			JButton btn = btns.get(i);
			btn.putClientProperty("JButton.buttonType", "bevel");
		}
	}
	
	@Override
	public void customizeSearchTextField(JTextField field) {
		field.putClientProperty("JTextField.variant", "search");
	}
	
	@Override
	public void openUrl(String url) throws IOException {
		if(CodeUtil.javaMajorVersion() >= 6)
			super.openUrl(url);
		else
			FileManager.openURL(url);
	}
}
