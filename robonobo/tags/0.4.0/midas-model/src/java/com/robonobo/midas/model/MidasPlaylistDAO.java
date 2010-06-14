package com.robonobo.midas.model;

import static com.robonobo.common.persistence.PersistenceManager.*;
import static com.robonobo.common.util.TextUtil.*;

public class MidasPlaylistDAO {
	public static void deletePlaylist(MidasPlaylist playlist) {
		currentSession().delete(playlist);
	}

	public static MidasPlaylist loadPlaylist(String playlistId) {
		MidasPlaylist playlist = (MidasPlaylist) currentSession().get(MidasPlaylist.class, playlistId);
		return playlist;
	}

	public static void savePlaylist(MidasPlaylist playlist) {
		sanitizePlaylist(playlist);
		currentSession().saveOrUpdate(playlist);
	}
	
	private static void sanitizePlaylist(MidasPlaylist p) {
		p.setTitle(truncate(p.getTitle(), 128));
		p.setDescription(truncate(p.getDescription(), 512));
	}
}
