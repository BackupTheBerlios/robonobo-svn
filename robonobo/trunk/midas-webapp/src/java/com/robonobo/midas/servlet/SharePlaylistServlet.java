package com.robonobo.midas.servlet;

import static com.robonobo.common.util.TextUtil.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.robonobo.core.api.model.Playlist;
import com.robonobo.core.api.model.User;
import com.robonobo.midas.model.MidasFriendRequest;
import com.robonobo.midas.model.MidasInvite;
import com.robonobo.midas.model.MidasPlaylist;
import com.robonobo.midas.model.MidasUser;
import com.robonobo.remote.service.LocalMidasService;
import com.robonobo.remote.service.MailService;
import com.robonobo.remote.service.MailServiceImpl;
import com.robonobo.remote.service.MidasService;

@SuppressWarnings("serial")
public class SharePlaylistServlet extends MidasServlet {
	private MidasService service = LocalMidasService.getInstance();
	private MailService mailService;
	private String roboLaunchUrl;
	private String inviteUrlBase;
	private String friendReqUrlBase;
	private String fromRoboName;
	private String fromRoboEmail;

	@Override
	public void init() throws ServletException {
		super.init();
		Enumeration<String> paramNames = getServletContext().getInitParameterNames();
		List<String> paramList = new ArrayList<String>();
		while (paramNames.hasMoreElements()) {
			paramList.add(paramNames.nextElement());
		}
		mailService = new MailServiceImpl(getInitParameter("smtpServer"));
		roboLaunchUrl = getInitParameter("roboLaunchUrl");
		inviteUrlBase = getInitParameter("inviteUrlBase");
		friendReqUrlBase = getInitParameter("friendReqUrlBase");
		fromRoboName = getInitParameter("fromRoboName");
		fromRoboEmail = getInitParameter("fromRoboEmail");
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		long playlistId;
		try {
			playlistId = Long.parseLong(req.getParameter("plid"), 16);
		} catch (NumberFormatException e) {
			send404(req, resp);
			return;
		}
		MidasPlaylist p = service.getPlaylistById(playlistId);
		if (p == null) {
			send404(req, resp);
			return;
		}

		MidasUser u = getAuthUser(req);
		if (u == null || !p.getOwnerIds().contains(u.getUserId())) {
			send401(req, resp);
			return;
		}
		String localPath = req.getRequestURI();
		String opName = localPath.substring(localPath.lastIndexOf("/") + 1);
		if (opName.equals("share"))
			doShare(p, u, req, resp);
		else
			send404(req, resp);
	}

	/**
	 * share-playlist/share?plid=1c0d&friendids=45ed,34567,45678[&emails=foo@bar.com,baz@quuz.com]
	 * 
	 * @throws IOException
	 * @throws ServletException
	 */
	private void doShare(MidasPlaylist p, MidasUser u, HttpServletRequest req, HttpServletResponse resp)
			throws IOException, ServletException {
		String fidParam = req.getParameter("friendids");
		List<String> friendIds = isNonEmpty(fidParam) ? Arrays.asList(fidParam.split(",")) : new ArrayList<String>();
		String eParam = req.getParameter("emails");
		List<String> newFriendEmailParams = isNonEmpty(eParam) ? Arrays.asList(eParam.split(","))
				: new ArrayList<String>();

		// We'll have three categories of users that might need updating:
		// 1. Existing friends of this user who need to be added to this
		// playlist
		Set<MidasUser> newPlaylistUsers = new HashSet<MidasUser>();
		// 2. Existing robonobo users who are not friends of this user - send
		// them a friend request
		Set<MidasUser> newFriends = new HashSet<MidasUser>();
		// 3. Non-users of robonobo - send them invite emails
		Set<String> inviteEmails = new HashSet<String>();

		// Go through our parameters, figuring out which users are in our 3
		// categories
		for (String emailParam : newFriendEmailParams) {
			String email = urlDecode(emailParam);
			MidasUser shareUser = service.getUserByEmail(email);
			if (shareUser == null) {
				inviteEmails.add(email);
			} else if (u.getFriendIds().contains(shareUser.getUserId())) {
				// They are already a friend, deal with them in a moment...
				friendIds.add(Long.toString(shareUser.getUserId()));
			} else {
				// Friend request
				newFriends.add(shareUser);
			}
		}
		// Check we have enough invites
		if (inviteEmails.size() > 0) {
			// Client should have checked this already, so just chuck an error
			log.error("User " + u.getEmail() + " tried to share playlist " + p.getTitle()
					+ ", but has insufficient invites");
			if (inviteEmails.size() > u.getInvitesLeft())
				throw new ServletException("Not enough invites left!");
			u.setInvitesLeft(u.getInvitesLeft() - inviteEmails.size());
			service.saveUser(u);
		}
		// Users specified via user id must already be friends
		for (String friendIdStr : friendIds) {
			long friendId = Long.parseLong(friendIdStr, 16);
			if (!u.getFriendIds().contains(friendId)) {
				send401(req, resp);
				return;
			}
			MidasUser friend = service.getUserById(friendId);
			if (friend == null) {
				send404(req, resp);
				return;
			}
			if (friend.getPlaylistIds().contains(p.getPlaylistId()))
				continue;
			newPlaylistUsers.add(friend);
		}

		// Existing friends - add them as playlist owners, and send them a
		// notification
		for (MidasUser friend : newPlaylistUsers) {
			if (log.isDebugEnabled())
				log.debug("User " + u.getEmail() + " sharing playlist " + p.getTitle() + " with existing friend "
						+ friend.getEmail());
			friend.getPlaylistIds().add(p.getPlaylistId());
			friend.setUpdated(getUpdatedDate(friend.getUpdated()));
			p.getOwnerIds().add(friend.getUserId());
			service.saveUser(friend);
			sendNotifyPlaylistShare(u, friend, p);
		}
		p.setUpdated(getUpdatedDate(p.getUpdated()));
		service.savePlaylist(p);
		// New friends
		for (MidasUser newFriend : newFriends) {
			if (log.isDebugEnabled())
				log.debug("User " + u.getEmail() + " sharing playlist " + p.getTitle() + " with new friend "
						+ newFriend.getEmail());
			MidasFriendRequest friendReq = service.createOrUpdateFriendRequest(u, newFriend, p);
			sendFriendRequest(friendReq, u, newFriend, p);
		}
		// Invites
		for (String invitee : inviteEmails) {
			if (log.isDebugEnabled())
				log.debug("User " + u.getEmail() + " sharing playlist " + p.getTitle() + " with invited robonobo user "
						+ invitee);
			MidasInvite invite = service.createOrUpdateInvite(invitee, u, p);
			sendInvite(invite, u, p);
		}
	}

	protected void sendNotifyPlaylistShare(User fromUser, User toUser, Playlist p) throws IOException {
		String subject = fromUser.getFriendlyName() + " shared a playlist with you: " + p.getTitle();
		StringBuffer bod = new StringBuffer();
		bod.append(fromUser.getFriendlyName()).append("(").append(fromUser.getEmail()).append(")");
		bod.append(" shared a robonobo playlist with you.\n\n");
		bod.append("Title: ").append(p.getTitle()).append("\n");
		if (isNonEmpty(p.getDescription()))
			bod.append("Description: ").append(p.getDescription());
		bod.append("\n\nTo launch robonobo and see this playlist, go to " + roboLaunchUrl);
		bod.append("\n\n(from robonobo mailmonkey)\n");
		mailService.sendMail(fromRoboName, fromRoboEmail, toUser.getFriendlyName(), toUser.getEmail(),
				fromUser.getFriendlyName(), fromUser.getEmail(), subject, bod.toString());
	}

	protected void sendFriendRequest(MidasFriendRequest req, User fromUser, User toUser, Playlist p) throws IOException {
		String subject = fromUser.getFriendlyName() + " would like to be your friend on robonobo";
		StringBuffer bod = new StringBuffer();
		bod.append(fromUser.getFriendlyName()).append("(").append(fromUser.getEmail()).append(")");
		bod.append(" has shared a playlist with you, and would like to become your friend on robonobo.  This means that they will see playlists that you have made public, and you will see theirs.\n\n");
		bod.append("Playlist title: ").append(p.getTitle()).append("\n");
		if (isNonEmpty(p.getDescription()))
			bod.append("Description: ").append(p.getDescription());
		bod.append("\n\nTo add ").append(fromUser.getFriendlyName()).append(" as a friend, click this link:\n\n");
		bod.append(friendReqUrlBase).append(req.getRequestCode());
		bod.append("\n\nCopy and paste this into your browser if clicking does not work.  To ignore this request, just delete this email.");
		bod.append("\n\n(from robonobo mailmonkey)\n");
		mailService.sendMail(fromRoboName, fromRoboEmail, toUser.getFriendlyName(), toUser.getEmail(),
				fromUser.getFriendlyName(), fromUser.getEmail(), subject, bod.toString());
	}

	protected void sendInvite(MidasInvite invite, User fromUser, Playlist p) throws IOException {
		String subject = fromUser.getFriendlyName() + " has invited you to robonobo";
		StringBuffer bod = new StringBuffer();
		bod.append(fromUser.getFriendlyName()).append("(").append(fromUser.getEmail()).append(")");
		bod.append(" has invited you to robonobo, the social music application that lets you share your music with friends while supporting artists.  As a welcome present, they have sent you a playlist:\n\n");
		bod.append("Title: ").append(p.getTitle()).append("\n");
		if (isNonEmpty(p.getDescription()))
			bod.append("Description: ").append(p.getDescription()).append("\n");
		bod.append("\nTo accept the invitation and start using robonobo, click this link:\n\n");
		bod.append(inviteUrlBase).append(invite.getInviteCode());
		bod.append("\n\nCopy and paste this into your browser if clicking does not work.  To ignore this invitation, just delete this email.");
		bod.append("\n\n(from robonobo mailmonkey)\n");
		mailService.sendMail(fromRoboName, fromRoboEmail, null, invite.getEmail(), fromUser.getFriendlyName(),
				fromUser.getEmail(), subject, bod.toString());
	}
}
