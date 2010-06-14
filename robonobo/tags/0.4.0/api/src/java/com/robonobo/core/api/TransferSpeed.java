/**
 * 
 */
package com.robonobo.core.api;

public class TransferSpeed {
	public String streamId;
	public int download;
	public int upload;
	
	public TransferSpeed(String streamId, int download, int upload) {
		this.streamId = streamId;
		this.download = download;
		this.upload = upload;
	}

	public String getStreamId() {
		return streamId;
	}

	public int getDownload() {
		return download;
	}

	public int getUpload() {
		return upload;
	}
	
	@Override
	public String toString() {
		return "[id="+streamId+",dl="+download+",ul="+upload+"]";
	}
}