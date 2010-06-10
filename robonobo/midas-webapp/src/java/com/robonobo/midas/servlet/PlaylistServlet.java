package com.robonobo.midas.servlet;

import static com.robonobo.common.util.TimeUtil.now;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.robonobo.common.util.TimeUtil;
import com.robonobo.core.api.model.User;
import com.robonobo.core.api.proto.CoreApi.PlaylistMsg;
import com.robonobo.midas.model.MidasPlaylist;
import com.robonobo.midas.model.MidasUser;
import com.robonobo.remote.service.LocalMidasService;
import com.robonobo.remote.service.MidasService;

public class PlaylistServlet extends MidasServlet {
	private MidasService service = LocalMidasService.getInstance();
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String playlistId = getPlaylistId(req);
		MidasPlaylist p = service.getPlaylistById(playlistId);
		if (p == null) {
			send404(req, resp);
			return;
		}
		// Check to see if this user is allowed to see the playlist - is s/he
		// the owner, or a friend of the owner?
		User u = getAuthUser(req);
		if (u == null) {
			send401(req, resp);
			return;
		}
		boolean allowed = false;
		ownerLoop: for (Long ownerId : p.getOwnerIds()) {
			if (ownerId.equals(u.getUserId())) {
				allowed = true;
				break;
			}
			if (p.getAnnounce()) {
				User owner = service.getUserById(ownerId);
				for (Long friendId : owner.getFriendIds()) {
					if (u.getUserId() == friendId) {
						allowed = true;
						break ownerLoop;
					}
				}
			}
		}
		if (allowed) {
			resp.setContentType("application/data");
			log.info("Returning playlist " + playlistId + " to " + u.getEmail());
			writeToOutput(p.toMsg(), resp);
		} else {
			send401(req, resp);
		}
	}

	@Override
	protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		MidasUser u = getAuthUser(req);
		if (u == null) {
			send401(req, resp);
			return;
		}
		String playlistId = getPlaylistId(req);
		MidasPlaylist currentP = service.getPlaylistById(playlistId);
		PlaylistMsg.Builder pBldr = PlaylistMsg.newBuilder();
		readFromInput(pBldr, req);
		PlaylistMsg pMsg = pBldr.build();
		MidasPlaylist mp = new MidasPlaylist(pMsg);
		if (currentP == null) {
			// New playlist
			mp.getOwnerIds().clear();
			mp.getOwnerIds().add(u.getUserId());
			mp.setUpdated(TimeUtil.now());
			service.savePlaylist(mp);
			u.getPlaylistIds().add(playlistId);
			u.setUpdated(now());
			service.saveUser(u);
			writeToOutput(mp.toMsg(), resp);
		} else {
			// Existing playlist
			if (!currentP.getOwnerIds().contains(u.getUserId())) {
				send401(req, resp);
				return;
			}
			currentP.copyFrom(mp);
			currentP.setUpdated(getUpdatedDate(currentP.getUpdated()));
			service.savePlaylist(currentP);
			writeToOutput(currentP.toMsg(), resp);
		}
		log.info(u.getEmail() + " updated playlist " + playlistId);
		// DEBUG
		log.debug(pMsg);
	}

	@Override
	protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String playlistId = getPlaylistId(req);
		MidasUser u = getAuthUser(req);
		MidasPlaylist p = service.getPlaylistById(playlistId);
		if (p == null)
			return;
		if (u == null || !p.getOwnerIds().contains(u.getUserId())) {
			send401(req, resp);
			return;
		}
		if (p.getOwnerIds().size() == 1) {
			// If they were the only owner, just delete the whole thing
			service.deletePlaylist(p);
			u.getPlaylistIds().remove(playlistId);
			u.setUpdated(now());
			service.saveUser(u);
			log.info(u.getEmail() + " deleted playlist " + playlistId);
		} else {
			// Otherwise, just remove them from the owner list
			p.getOwnerIds().remove(u.getUserId());
			p.setUpdated(getUpdatedDate(p.getUpdated()));
			service.savePlaylist(p);
			u.getPlaylistIds().remove(playlistId);
			u.setUpdated(now());
			service.saveUser(u);
			log.info("Removed user " + u.getEmail() + " from owners of playlist " + playlistId	);
		}
	}

	private String getPlaylistId(HttpServletRequest req) {
		return req.getPathInfo().replaceFirst("/", "");
	}
}
