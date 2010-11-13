package com.robonobo.core.api.model;

import java.util.*;

import com.robonobo.core.api.proto.CoreApi.LibraryMsg;
import com.robonobo.core.api.proto.CoreApi.LibraryTrackMsg;

public class Library {
	private Map<String, Date> tracks = new HashMap<String, Date>();

	public Library() {
	}

	public Library(LibraryMsg msg) {
		for (int i = 0; i < msg.getTrackCount(); i++) {
			LibraryTrackMsg t = msg.getTrack(i);
			Date d = t.hasAddedDate() ? new Date(t.getAddedDate()) : null;
			tracks.put(t.getStreamId(), d);
		}
	}

	public LibraryMsg toMsg() {
		LibraryMsg.Builder b = LibraryMsg.newBuilder();
		for (String streamId : tracks.keySet()) {
			LibraryTrackMsg.Builder tb = LibraryTrackMsg.newBuilder();
			tb.setStreamId(streamId);
			Date d = tracks.get(streamId);
			if(d != null)
				tb.setAddedDate(d.getTime());
			b.addTrack(tb.build());
		}
		return b.build();
	}

	public Map<String, Date> getTracks() {
		return tracks;
	}

	public void setTracks(Map<String, Date> tracks) {
		this.tracks = tracks;
	}
}
