package com.robonobo.midas.model;

import com.robonobo.core.api.model.Library;
import com.robonobo.core.api.proto.CoreApi.LibraryMsg;

public class MidasLibrary extends Library {
	private long userId;
	
	public MidasLibrary() {
	}

	public MidasLibrary(LibraryMsg msg) {
		super(msg);
	}

	public long getUserId() {
		return userId;
	}

	public void setUserId(long userId) {
		this.userId = userId;
	}
}
