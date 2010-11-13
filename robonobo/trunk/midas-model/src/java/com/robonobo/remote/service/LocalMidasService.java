package com.robonobo.remote.service;

import java.util.*;
import java.util.Map.Entry;

import org.apache.commons.collections.MapIterator;

import com.robonobo.core.api.model.Playlist;
import com.robonobo.midas.model.*;
import com.twmacinta.util.MD5;

import static com.robonobo.common.util.TimeUtil.*;

/**
 * For clients within the same JVM. Assumes hibernate has been configured.
 * 
 * TODO All this use of statics and singletons is a bit antipatternish, would be
 * better to use spring beans, but I'm not sure it's worth the extra
 * dependency...
 * 
 * @author macavity
 * 
 */
public class LocalMidasService implements MidasService {
	private static LocalMidasService instance;

	public static LocalMidasService getInstance() {
		if (instance == null)
			instance = new LocalMidasService();
		return instance;
	}

	public LocalMidasService() {
	}

	public List<MidasUser> getAllUsers() {
		return MidasUserDAO.retrieveAll();
	}

	public MidasUser getUserByEmail(String email) {
		return MidasUserDAO.retrieveByEmail(email);
	}

	public MidasUser getUserById(long userId) {
		return MidasUserDAO.retrieveById(userId);
	}

	public MidasUser createUser(MidasUser user) {
		user.setVerified(true);
		user.setUpdated(now());
		return MidasUserDAO.create(user);
	}

	public MidasUser getUserAsVisibleBy(MidasUser targetU, MidasUser requestor) {
		// If this the user asking for themselves, give them everything. If
		// they're a friend, they get public playlists, but no friends.
		// Otherwise, they
		// get a null object
		MidasUser result;
		if (targetU.equals(requestor)) {
			result = new MidasUser(targetU);
		} else if (targetU.getFriendIds().contains(requestor.getUserId())) {
			result = new MidasUser(targetU);
			result.getFriendIds().clear();
			result.setInvitesLeft(0);
			Iterator<String> iter = result.getPlaylistIds().iterator();
			while (iter.hasNext()) {
				Playlist p = MidasPlaylistDAO.loadPlaylist(iter.next());
				if (!p.getAnnounce())
					iter.remove();
			}
		} else
			result = null;

		return result;
	}

	public void saveUser(MidasUser user) {
		MidasUserDAO.save(user);
	}

	public void deleteUser(long userId) {
		MidasUser u = MidasUserDAO.retrieveById(userId);
		// Go through all their playlists - if they are the only owner, delete it, otherwise remove them from the owners list
		for (String plId : u.getPlaylistIds()) {
			MidasPlaylist p = MidasPlaylistDAO.loadPlaylist(plId);
			p.getOwnerIds().remove(userId);
			if(p.getOwnerIds().size() == 0)
				MidasPlaylistDAO.deletePlaylist(p);
			else 
				MidasPlaylistDAO.savePlaylist(p);
		}
		// Go through all their friends, delete this user from their friendids
		for (long friendId : u.getFriendIds()) {
			MidasUser friend = MidasUserDAO.retrieveById(friendId);
			friend.getFriendIds().remove(userId);
			MidasUserDAO.save(friend);
		}
		// Delete any pending friend requests to this user
		List<MidasFriendRequest> frs = MidasFriendRequestDAO.retrieveByRequestee(userId);
		for (MidasFriendRequest fr : frs) {
			MidasFriendRequestDAO.delete(fr);
		}
		// Finally, delete the user itself
		MidasUserDAO.delete(u);
	}
	
	public MidasPlaylist getPlaylistById(String playlistId) {
		return MidasPlaylistDAO.loadPlaylist(playlistId);
	}

	public void savePlaylist(MidasPlaylist playlist) {
		MidasPlaylistDAO.savePlaylist(playlist);
	}

	public void deletePlaylist(MidasPlaylist playlist) {
		MidasPlaylistDAO.deletePlaylist(playlist);
	}

	public MidasStream getStreamById(String streamId) {
		return MidasStreamDAO.loadStream(streamId);
	}

	public void saveStream(MidasStream stream) {
		MidasStreamDAO.saveStream(stream);
	}

	public void deleteStream(MidasStream stream) {
		MidasStreamDAO.deleteStream(stream);
	}

	public Long countUsers() {
		return MidasUserDAO.getUserCount();
	}

	public MidasInvite createOrUpdateInvite(String email, MidasUser friend, MidasPlaylist pl) {
		MidasInvite result = MidasInviteDAO.retrieveByEmail(email);
		if (result == null) {
			// New invite
			result = new MidasInvite();
			result.setEmail(email);
			result.setInviteCode(generateEmailCode(email));
		}
		result.getFriendIds().add(friend.getUserId());
		result.getPlaylistIds().add(pl.getPlaylistId());
		result.setUpdated(now());
		MidasInviteDAO.save(result);
		return result;
	}

	public MidasFriendRequest createOrUpdateFriendRequest(MidasUser requestor, MidasUser requestee, MidasPlaylist pl) {
		MidasFriendRequest result = MidasFriendRequestDAO.retrieveByUsers(requestor.getUserId(), requestee.getUserId());
		if(result == null) {
			// New friend request
			result = new MidasFriendRequest();
			result.setRequestorId(requestor.getUserId());
			result.setRequesteeId(requestee.getUserId());
			result.setRequestCode(generateEmailCode(requestee.getEmail()));
		}
		result.getPlaylistIds().add(pl.getPlaylistId());
		result.setUpdated(now());
		MidasFriendRequestDAO.save(result);
		return result;
	}

	public MidasFriendRequest getFriendRequest(String requestCode) {
		return MidasFriendRequestDAO.retrieveByRequestCode(requestCode);
	}
	
	public String acceptFriendRequest(MidasFriendRequest req) {
		MidasUser requestor = MidasUserDAO.retrieveById(req.getRequestorId());
		if(requestor == null)
			return "Requesting user "+req.getRequestorId()+" not found";
		MidasUser requestee = MidasUserDAO.retrieveById(req.getRequesteeId());
		if(requestee == null)
			return "Requested user "+req.getRequesteeId()+" not found";
		for (String plId : req.getPlaylistIds()) {
			MidasPlaylist p = MidasPlaylistDAO.loadPlaylist(plId);
			p.getOwnerIds().add(requestee.getUserId());
			MidasPlaylistDAO.savePlaylist(p);
			requestee.getPlaylistIds().add(plId);
		}
		requestor.getFriendIds().add(requestee.getUserId());
		MidasUserDAO.save(requestor);
		requestee.getFriendIds().add(requestor.getUserId());
		MidasUserDAO.save(requestee);
		MidasFriendRequestDAO.delete(req);
		return null;
	}
	
	public void ignoreFriendRequest(MidasFriendRequest request) {
		MidasFriendRequestDAO.delete(request);
	}
	
	public List<MidasFriendRequest> getPendingFriendRequests(long userId) {
		return MidasFriendRequestDAO.retrieveByRequestee(userId);
	}
	
	public void deleteInvite(String inviteCode) {
		MidasInvite invite = MidasInviteDAO.retrieveByInviteCode(inviteCode);
		if(invite != null)
			MidasInviteDAO.delete(invite);
	}
	
	public MidasInvite getInvite(String inviteCode) {
		return MidasInviteDAO.retrieveByInviteCode(inviteCode);
	}
	
	@Override
	public MidasLibrary getLibrary(MidasUser u, Date since) {
		MidasLibrary lib = MidasLibraryDAO.getLibrary(u.getUserId());
		if(since != null && lib != null) {
			Iterator<Entry<String, Date>> it = lib.getTracks().entrySet().iterator();
			while(it.hasNext()) {
				Entry<String, Date> e = it.next();
				if(since.after(e.getValue()))
					it.remove();
			}
		}
		return lib;
	}
	
	@Override
	public void putLibrary(MidasLibrary lib) {
		MidasLibraryDAO.saveLibrary(lib);
	}
	
	private String generateEmailCode(String email) {
		MD5 hash = new MD5();
		hash.Update(email);
		hash.Update(getDateFormat().format(now()));
		return hash.asHex();
	}
}
