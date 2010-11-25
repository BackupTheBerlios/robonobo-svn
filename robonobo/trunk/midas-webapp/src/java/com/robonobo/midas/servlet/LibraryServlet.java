package com.robonobo.midas.servlet;

import java.io.IOException;
import java.util.Date;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.robonobo.common.exceptions.SeekInnerCalmException;
import com.robonobo.core.api.model.Library;
import com.robonobo.core.api.model.UserConfig;
import com.robonobo.core.api.proto.CoreApi.LibraryMsg;
import com.robonobo.midas.model.MidasLibrary;
import com.robonobo.midas.model.MidasUser;
import com.robonobo.remote.service.LocalMidasService;
import com.robonobo.remote.service.MidasService;

public class LibraryServlet extends MidasServlet {
	private MidasService service = LocalMidasService.getInstance();

	/**
	 * Gets a user's library url: /library/<user-id>?since=<ms-since-epoch> Returns a serialized LibraryMsg
	 */
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		MidasUser authUser = getAuthUser(req);
		if (authUser == null) {
			send401(req, resp);
			return;
		}
		long authUid = authUser.getUserId();
		long reqUid = getUserId(req);
		MidasUser reqUser = service.getUserById(reqUid);
		boolean allowed = (authUid == reqUid || authUser.getFriendIds().contains(reqUid));
		if (allowed) {
			resp.setContentType("application/data");
			// If the user has disabled library sharing, just send them an empty library
			UserConfig cfg = service.getUserConfig(reqUser);
			if (cfg != null && "false".equals(cfg.getItems().get("sharelibrary"))) {
				log.info("Returning blank library for " + reqUser.getEmail() + " to " + authUser.getEmail());
				Library lib = new Library();
				lib.setUserId(reqUid);
				writeToOutput(lib.toMsg(), resp);
			} else {
				log.info("Returning library for " + reqUser.getEmail() + " to " + authUser.getEmail());
				Date since = null;
				if (req.getParameter("since") != null)
					since = new Date(Long.parseLong(req.getParameter("since")));
				Library lib = service.getLibrary(reqUser, since);
				if (lib == null)
					lib = new MidasLibrary();
				writeToOutput(lib.toMsg(), resp);
			}
		} else {
			send401(req, resp);
			return;
		}
	}

	/**
	 * To add to the library: /library/<user-id>/add
	 * 
	 * To delete from the library: /library/<user-id>/del
	 * 
	 * Expects a serialized LibraryMsg in the request body with the streams being added/deleted
	 * 
	 * NB we do both these via PUT rather than using PUT & DELETE as tomcat doesn't pass through a request body in the
	 * DELETE
	 */
	@Override
	protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		MidasUser authUser = getAuthUser(req);
		if (authUser == null || authUser.getUserId() != getUserId(req)) {
			send401(req, resp);
			return;
		}
		LibraryMsg.Builder b = LibraryMsg.newBuilder();
		readFromInput(b, req);
		LibraryMsg msg = b.build();
		Library newLib = new MidasLibrary(msg);
		Library currentLib = service.getLibrary(authUser, null);
		String verb = getVerb(req);
		if ("add".equals(verb)) {
			if (currentLib == null) {
				newLib.setUserId(authUser.getUserId());
				service.putLibrary(newLib);
			} else {
				for (Entry<String, Date> entry : newLib.getTracks().entrySet()) {
					currentLib.getTracks().put(entry.getKey(), entry.getValue());
				}
				service.putLibrary(currentLib);
			}
			log.info("User " + authUser.getEmail() + " added " + newLib.getTracks().size() + " tracks to their library");
		} else if ("del".equals(verb)) {
			if (currentLib != null) {
				for (String sid : newLib.getTracks().keySet()) {
					currentLib.getTracks().remove(sid);
				}
				service.putLibrary(currentLib);
			}
			log.info("User " + authUser.getEmail() + " removed " + newLib.getTracks().size()
					+ " tracks from their library");
		} else
			send404(req, resp);
	}

	private long getUserId(HttpServletRequest req) {
		Pattern p = Pattern.compile("/([0-9a-eA-E]+).*");
		Matcher m = p.matcher(req.getPathInfo());
		if (!m.matches())
			throw new SeekInnerCalmException();
		return Long.parseLong(m.group(1), 16);
	}

	private String getVerb(HttpServletRequest req) {
		Pattern p = Pattern.compile("/\\d+/(\\w+)");
		Matcher m = p.matcher(req.getPathInfo());
		if (!m.matches())
			return null;
		return m.group(1);
	}
}
