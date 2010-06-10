package com.robonobo.midas.servlet;

import java.io.IOException;
import java.net.URLDecoder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.robonobo.core.api.model.User;
import com.robonobo.core.api.proto.CoreApi.SearchResponse;
import com.robonobo.midas.search.SearchFacade;

@SuppressWarnings("serial")
public class SearchServlet extends MidasServlet {
	SearchFacade searchFacade = new SearchFacade();
	
	@Override
	/**
	 * http://midas.robonobo.com/search?type=stream&q=foo&first=101
	 */
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		User u = getAuthUser(req);
		if(u == null) {
			send401(req, resp);
			return;
		}
		String searchType = req.getParameter("type");
		String query = URLDecoder.decode(req.getParameter("q"), "utf-8");
		int firstResult = (req.getParameter("first") == null) ? 0 : Integer.parseInt(req.getParameter("first"));
		try {
			SearchResponse response = searchFacade.search(searchType, query, firstResult);
			writeToOutput(response, resp);
			log.info("Returning "+response.getObjectIdCount()+" results to "+u.getEmail()+" for search '"+query+"'");
		} catch (Exception e) {
			throw new ServletException(e);
		}
	}
}
