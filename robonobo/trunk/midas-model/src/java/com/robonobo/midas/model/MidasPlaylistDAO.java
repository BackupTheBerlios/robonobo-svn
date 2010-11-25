package com.robonobo.midas.model;

import static com.robonobo.common.persistence.PersistenceManager.*;
import static com.robonobo.common.util.TextUtil.*;

import java.util.List;

public class MidasPlaylistDAO {
	/**
	 * Returns the playlist id that is currently highest
	 * @return
	 */
	public static long getHighestPlaylistId() {
		String hql = "select max(playlistId) from MidasPlaylist";
		List l = currentSession().createQuery(hql).list();
		if(l.size() == 0 || l.get(0) == null)
			return 0;
		return ((Long)l.get(0)).longValue();
	}
	
	public static void deletePlaylist(MidasPlaylist playlist) {
		currentSession().delete(playlist);
	}

	public static MidasPlaylist loadPlaylist(long playlistId) {
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
