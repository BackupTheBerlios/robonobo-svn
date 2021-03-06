package com.robonobo.remote.service;

import java.util.List;

import com.robonobo.midas.model.MidasFriendRequest;
import com.robonobo.midas.model.MidasInvite;
import com.robonobo.midas.model.MidasPlaylist;
import com.robonobo.midas.model.MidasStream;
import com.robonobo.midas.model.MidasUser;

public interface MidasService {
	public List<MidasUser> getAllUsers();
	
	public MidasUser getUserByEmail(String email);

	public MidasUser getUserById(long userId);

	public MidasUser createUser(MidasUser user);
	
	public void saveUser(MidasUser user);

	/**
	 * Returns the target user, but only the bits that are allowed to be seen by
	 * the requesting user
	 */
	public MidasUser getUserAsVisibleBy(MidasUser target, MidasUser requestor);

	public void deleteUser(long userId);
	
	public MidasPlaylist getPlaylistById(String playlistId);

	public void savePlaylist(MidasPlaylist playlist);

	public void deletePlaylist(MidasPlaylist playlist);

	public MidasStream getStreamById(String streamId);

	public void saveStream(MidasStream stream);

	public void deleteStream(MidasStream stream);
	
	/**
	 * This is for monitoring, ensures db connection is ok
	 */
	public Long countUsers();
	
	public MidasFriendRequest createOrUpdateFriendRequest(MidasUser requestor, MidasUser requestee, MidasPlaylist pl);
	
	public MidasFriendRequest getFriendRequest(String requestCode);
	
	/**
	 * Returns error message, null if no error
	 */
	public String acceptFriendRequest(MidasFriendRequest request);
	
	public void ignoreFriendRequest(MidasFriendRequest request);
	
	public List<MidasFriendRequest> getPendingFriendRequests(long userId);
	
	public MidasInvite createOrUpdateInvite(String email, MidasUser friend, MidasPlaylist pl);
	
	public MidasInvite getInvite(String inviteCode);
	
	public void deleteInvite(String inviteCode);
}
