package com.robonobo.core.api.config;

import java.io.Serializable;
import java.util.Set;

import static com.robonobo.common.util.TextUtil.*;

public class MetadataServerConfig implements Serializable {
	private static final long serialVersionUID = 1L;
	private String baseUrl;

	public MetadataServerConfig() {
	}

	public MetadataServerConfig(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	public String getPlaylistUrl(String playlistId) {
		return baseUrl + "/playlists/" + playlistId;
	}

	public String getSendPlaylistUrl(String playlistId, long friendId) {
		return baseUrl + "/share-playlist/send?plid="+playlistId+"&friendid="+friendId;
	}
	
	public String getSendPlaylistUrl(String playlistId, String email) {
		return baseUrl + "/share-playlist/send?plid="+playlistId+"&email="+urlEncode(email);
	}
	
	public String getSharePlaylistUrl(String playlistId, Set<Long> friendIds, Set<String> emails) {
		StringBuffer sb = new StringBuffer(baseUrl).append("/share-playlist/share?plid=");
		sb.append(playlistId);
		if(friendIds.size() > 0) {
			sb.append("&friendids=");
			boolean first=true;
			for (Long friendId : friendIds) {
				if(first)
					first = false;
				else
					sb.append(",");
				sb.append(String.valueOf(friendId));
			}
		}
		if(emails.size() > 0) {
			sb.append("&emails=");
			boolean first=true;
			for (String email : emails) {
				if(first)
					first = false;
				else
					sb.append(",");
				sb.append(urlEncode(email));
			}
		}
		return sb.toString();
	}
	
	public String getStreamUrl(String streamId) {
		return baseUrl + "/streams/" + streamId;
	}

	public String getUserUrl(String userEmail) {
		return baseUrl + "/users/byemail/" + urlEncode(userEmail);
	}
	
	public String getUserUrl(long userId) {
		return baseUrl + "/users/byid/" + userId;
	}
}
