package com.robonobo.midas.servlet;

import java.io.IOException;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.robonobo.common.persistence.PersistenceManager;
import com.robonobo.core.api.model.Stream;
import com.robonobo.core.api.proto.CoreApi.StreamMsg;
import com.robonobo.midas.model.MidasStream;
import com.robonobo.remote.service.LocalMidasService;
import com.robonobo.remote.service.MidasService;

public class StreamServlet extends MidasServlet {
	private static final long serialVersionUID = 1L;
	private MidasService service = LocalMidasService.getInstance();

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String streamId = getStreamId(req);
		// check to see if they want the stream gfx
		if(streamId.endsWith(".png")) {
			streamId = streamId.replaceFirst(".png", "");
			getThumbs().writeThumbToResponse(streamId, resp);
		} else {
			MidasStream stream = service.getStreamById(streamId);
			if("test".equals(streamId)) {
				stream = new MidasStream();
				stream.setDescription("A Test stream");
				stream.setTitle("Test");
				stream.setMimeType("audio/mpeg");
				stream.setStreamId("test");
				stream.setThumbnail(getThumbs().getImageForId("test"));
			}
			if(stream == null) {
				send404(req, resp);
				return;
			}
			resp.setContentType("application/data");
			log.debug("Returning stream " + stream.getStreamId());
			writeToOutput(stream.toMsg(), resp);
		}
 	}

	@Override
	protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		StreamMsg.Builder smb = StreamMsg.newBuilder();
		readFromInput(smb, req);
		Stream stream = new Stream(smb.build());
		updateStream(stream, req, resp);
	}

	private String getStreamId(HttpServletRequest req) {
		String uuid = req.getPathInfo().replaceFirst("/", "");
		return uuid;
	}

	private void updateStream(Stream newStream, HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		if(PersistenceManager.exists(MidasStream.class, newStream.getStreamId())) {
			// TODO Check for fingerprinty stuff - for now just return ok
		} else if(getAuthUser(req) != null) {
			MidasStream s = new MidasStream();
			s.copyFrom(newStream);
			Date nowTime = new Date();
			s.setPublished(nowTime);
			s.setModified(nowTime);
			log.info("Creating new stream " + newStream.getStreamId());
			service.saveStream(s);
		} else {
			log.info("No or invalid user creds provided for creation of stream " + newStream.getStreamId() + " from "
					+ req.getRemoteAddr());
			resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "No, or invalid, authentication details were sent");
			return;
		}
	}
}
