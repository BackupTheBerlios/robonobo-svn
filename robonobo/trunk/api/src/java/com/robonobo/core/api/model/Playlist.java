package com.robonobo.core.api.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.robonobo.core.api.proto.CoreApi.PlaylistMsg;

public class Playlist implements Comparable<Playlist> {
	String title;
	Date updated;
	List<String> streamIds = new ArrayList<String>();
	String description;
	String playlistId;
	boolean announce = true;
	Set<Long> ownerIds = new HashSet<Long>();
	
	public Playlist() {
	}
	
	public Playlist(PlaylistMsg msg) {
		playlistId = msg.getId();
		title = msg.getTitle();
		updated = new Date(msg.getUpdatedDate());
		description = msg.getDescription();
		announce = msg.getAnnounce();
		streamIds.addAll(msg.getStreamIdList());
		ownerIds.addAll(msg.getOwnerIdList());
	}
	
	public PlaylistMsg toMsg() {
		PlaylistMsg.Builder b = PlaylistMsg.newBuilder();
		b.setId(playlistId);
		b.setTitle(title);
		if(updated != null)
			b.setUpdatedDate(updated.getTime());
		if(description != null)
			b.setDescription(description);
		b.setAnnounce(announce);
		b.addAllStreamId(streamIds);
		b.addAllOwnerId(ownerIds);
		return b.build();
	}
	
	public void copyFrom(Playlist p) {
		title = p.title;
		description = p.description;
		playlistId = p.playlistId;
		updated = p.updated;
		announce = p.announce;
		streamIds.clear();
		streamIds.addAll(p.getStreamIds());
	}

	public boolean equals(Object obj) {
		if(!(obj instanceof Playlist))
			return false;
		Playlist p = (Playlist) obj;
		if(playlistId == null)
			return (p.playlistId == null);
		return playlistId.equals(p.playlistId);
	}

	@Override
	public int compareTo(Playlist o) {
		return title.compareTo(o.getTitle());
	}
	
	public String getPlaylistId() {
		return playlistId;
	}

	public String getDescription() {
		return description;
	}

	public String getTitle() {
		return title;
	}

	public Date getUpdated() {
		return updated;
	}

	public int hashCode() {
		return getClass().getName().hashCode() ^ getPlaylistId().hashCode();
	}

	public void setPlaylistId(String channelId) {
		this.playlistId = channelId;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public void setUpdated(Date updated) {
		this.updated = updated;
	}


	public String toString() {
		StringBuffer sb = new StringBuffer("[Playlist:id=").append(playlistId).append(",title=").append(title).append(",streams=(");
		int i = 0;
		for (String s : streamIds) {
			if(i++ > 0)
				sb.append(",");
			sb.append(s);			
		}
		sb.append(")]");
		return sb.toString();
	}

	public List<String> getStreamIds() {
		return streamIds;
	}

	public void setStreamIds(List<String> streamIds) {
		this.streamIds = streamIds;
	}

	public boolean getAnnounce() {
		return announce;
	}

	public void setAnnounce(boolean announce) {
		this.announce = announce;
	}

	public Set<Long> getOwnerIds() {
		return ownerIds;
	}

	public void setOwnerIds(Set<Long> ownerIds) {
		this.ownerIds = ownerIds;
	}
}