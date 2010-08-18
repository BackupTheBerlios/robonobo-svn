package com.robonobo.oldgui;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.TransferHandler;

import com.robonobo.common.concurrent.CatchingRunnable;
import com.robonobo.common.exceptions.SeekInnerCalmException;

public class FileImportDropHandler extends TransferHandler {
	private RobonoboFrame frame;
	
	public FileImportDropHandler(RobonoboFrame frame) {
		this.frame = frame;
	}

	@Override
	public int getSourceActions(JComponent c) {
		return NONE;
	}
	
	@Override
	public boolean canImport(JComponent comp, DataFlavor[] transferFlavors) {
		for (DataFlavor dataFlavor : transferFlavors) {
			if(dataFlavor.equals(DataFlavor.javaFileListFlavor))
				return true;
		}
		return false;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public boolean importData(JComponent comp, Transferable t) {
		List<File> files = null;
		try {
			files = (List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);
		} catch (Exception e) {
			throw new SeekInnerCalmException(e);
		}
		final List<File> finalFiles = files;
		frame.getLeftSidebar().selectMyMusic();
		frame.getController().getExecutor().execute(new CatchingRunnable() {
			public void doRun() throws Exception {
				frame.importFiles(finalFiles);
			}
		});
		return true;
	}
}
