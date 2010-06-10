package com.robonobo.core.api.model;

import javax.swing.JProgressBar;

public class DownloadingTransferStatus implements TransferStatus {
	private int numSources;
	private JProgressBar progressBar;

	public DownloadingTransferStatus(int numSources) {
		this.numSources = numSources;
	}

	public JProgressBar getProgressBar() {
		return progressBar;
	}

	public void setProgressBar(JProgressBar progressBar) {
		this.progressBar = progressBar;
	}

	public int getNumSources() {
		return numSources;
	}
	
	public void setNumSources(int numSources) {
		this.numSources = numSources;
	}
}
