package com.robonobo.gui.platform;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.Event;
import java.awt.Font;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileSystemView;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.robonobo.common.concurrent.CatchingRunnable;
import com.robonobo.common.exceptions.SeekInnerCalmException;
import com.robonobo.common.util.CodeUtil;
import com.robonobo.core.Platform;
import com.robonobo.core.api.Robonobo;
import com.robonobo.core.itunes.ITunesService;
import com.robonobo.gui.MenuBar;
import com.robonobo.gui.RobonoboFrame;

public class UnknownPlatform extends Platform {
	private Font tableBodyFont = new Font("sans-serif", Font.PLAIN, 12);
	Log log;
	protected RobonoboFrame rFrame;

	@Override
	public void init() {
		// TODO Auto-generated method stub
	}

	@Override
	public void initRobonobo(Robonobo ro) {
		 log = LogFactory.getLog(getClass());
	}
	
	@Override
	public JMenuBar getMenuBar(JFrame frame) {
		return new MenuBar((RobonoboFrame) frame);
	}

	@Override
	public void initMainWindow(JFrame frame) throws Exception {
		rFrame = (RobonoboFrame) frame;
		// TODO Auto-generated method stub
	}

	@Override
	public void setLookAndFeel() {
		// TODO Auto-generated method stub
	}

	@Override
	public boolean shouldSetMenuBarOnDialogs() {
		return false;
	}

	@Override
	public File getDefaultHomeDirectory() {
		return new File(FileSystemView.getFileSystemView().getDefaultDirectory(), ".robonobo");
	}

	@Override
	public File getDefaultDownloadDirectory() {
		String s = File.separator;
		return new File(FileSystemView.getFileSystemView().getDefaultDirectory()+s+"Music"+s+"robonobo");
	}

	@Override
	public boolean shouldShowPrefsInFileMenu() {
		return true;
	}

	@Override
	public boolean shouldShowQuitInFileMenu() {
		return true;
	}

	@Override
	public boolean shouldShowOptionsMenu() {
		return true;
	}
	
	@Override
	public boolean shouldShowAboutInHelpMenu() {
		return true;
	}
	
	@Override
	public int getNumberOfShakesForShakeyWindow() {
		return 10;
	}

	@Override
	public Font getTableBodyFont() {
		return tableBodyFont;
	}

	@Override
	public int getTrackProgressLabelWidth() {
		return 70;
	}

	@Override
	public boolean canDnDImport(DataFlavor[] transferFlavors) {
		try {
			DataFlavor flava = new DataFlavor("application/x-java-file-list; class=java.util.List");
			for (DataFlavor dataFlavor : transferFlavors) {
				if(flava.equals(dataFlavor))
					return true;
			}
			return false;
		} catch (ClassNotFoundException e) {
			throw new SeekInnerCalmException();
		}
	}

	@Override
	public List<File> getDnDImportFiles(Transferable t) throws IOException {
		try {
			DataFlavor flava = new DataFlavor("application/x-java-file-list; class=java.util.List");
			List result = (List) t.getTransferData(flava);
			return result;
		} catch (ClassNotFoundException e) {
			throw new SeekInnerCalmException();
		} catch (UnsupportedFlavorException e) {
			log.error("Error getting DnD files", e);
			return null;
		}
	}

	@Override
	public KeyStroke getAccelKeystroke(int key) {
		return KeyStroke.getKeyStroke(key, Event.CTRL_MASK);
	}
	
	@Override
	public int getCommandModifierMask() {
		return Event.CTRL_MASK;
	}
	
	@Override
	public boolean iTunesAvailable() {
		return false;
	}
	
	@Override
	public ITunesService getITunesService() {
		return null;
	}
	
	@Override
	public Color getLinkColor() {
		return Color.BLUE;
	}
	
	@Override
	public void customizeMainbarButtons(List<? extends JButton> btns) {
		// Do nothing
	}
	
	@Override
	public void customizeSearchTextField(JTextField field) {
		// Do nothing
	}
	
	@Override
	public void openUrl(String url) throws IOException {
		// Make sure we're in java6+ so that we have the java desktop classes, otherwise pop up a warning
		if(CodeUtil.javaMajorVersion() < 6) {
			SwingUtilities.invokeLater(new CatchingRunnable() {
				public void doRun() throws Exception {
					JOptionPane.showMessageDialog(rFrame, "We are sorry - to open URLs from robonobo, you must be running java version 6 or higher.  To get the latest version of java, visit http://java.sun.com");
				}
			});
			return;
		}
		try {
			Desktop.getDesktop().browse(new URI(url));
		} catch (URISyntaxException e) {
			throw new IOException(e);
		}
	}
}
