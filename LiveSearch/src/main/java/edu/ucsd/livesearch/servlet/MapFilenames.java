package edu.ucsd.livesearch.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsd.livesearch.dataset.mapper.ResultFileMapper;
import edu.ucsd.saint.commons.http.HttpParameters;

@SuppressWarnings("serial")
public class MapFilenames
extends BaseServlet
{
	/*========================================================================
	 * Constants
	 *========================================================================*/
	@SuppressWarnings("unused")
	private static final Logger logger =
		LoggerFactory.getLogger(MapFilenames.class);
	
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	/**
	 * <p>Accepts and processes queries to transform a list of filenames
	 * selected in the web client UI into a correctly flattened and possibly
	 * parsed-out reference list.
	 * 
	 * <p>By convention, a GET request to this servlet is assumed to be a
	 * request to read data only.  No creation, update, or deletion of
	 * server resources is handled by this method.
	 * 
	 * <p>This method implements the <code>dojox.data.FileStore</code>
	 * protocol defined at
	 * <code>http://docs.dojocampus.org/dojox/data/FileStore/protocol</code>.
	 * 
	 * @param request	an {@link HttpServletRequest} object that contains
	 * 					the request the client has made of the servlet
	 * 
	 * @param response	an {@link HttpServletResponse} object that contains
	 * 					the response the servlet sends to the client
	 * 
	 * @throws IOException		if an input or output error is detected
	 * 							when the servlet handles the GET request
	 * 
	 * @throws ServletException	if the request for the GET could not be
	 * 							handled
	 */
	@Override
	protected void doGet(
		HttpServletRequest request, HttpServletResponse response
	) throws ServletException, IOException {
		// initialize properties
		try {
			initialize(request, false);
		} catch (ServletException error) {
			reportError(HttpServletResponse.SC_BAD_REQUEST,
				"Error initializing servlet properties from request",
				response, error);
			return;
		} catch (Throwable error) {
			reportError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
				"Error initializing servlet properties from request",
				response, error);
			return;
		}
		String user = getUser();
		HttpParameters parameters = getParameters();
		PrintWriter out = response.getWriter();
		
		// extract source and target filename groups
		String source = parameters.getParameter("source");
		if (source == null || source.trim().equals("")) {
			reportError(HttpServletResponse.SC_BAD_REQUEST,
				"You must specify a valid \"source\" file descriptor string " +
				"to map filenames.", response);
			return;
		}
		String target = parameters.getParameter("target");
		if (target == null || target.trim().equals("")) {
			reportError(HttpServletResponse.SC_BAD_REQUEST,
				"You must specify a valid \"target\" file descriptor string " +
				"to map filenames.", response);
			return;
		}
		
		// map files
		String output = null;
		try {
			ResultFileMapper mapper = new ResultFileMapper();
			mapper.setSpectrumFiles(source);
			mapper.setResultFiles(target, user);
			if (mapper.isMapped() == false)
				mapper.mapFiles();
			output = mapper.getMappedFileJSON();
		} catch (ParseException error) {
			reportError(HttpServletResponse.SC_BAD_REQUEST,
				"[:" + error.getMessage() + ":]", response);
			return;
		} catch (Throwable error) {
			reportError(HttpServletResponse.SC_BAD_REQUEST,
				error.getMessage(), response, error);
			return;
		}
		
		// write JSON result to servlet output stream
		response.setContentType("application/json");
		response.setContentLength(output.length());
		out.print(output);
	}
}
