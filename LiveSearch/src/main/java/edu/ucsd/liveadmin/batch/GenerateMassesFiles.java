package edu.ucsd.liveadmin.batch;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsd.liveadmin.Cleanup;
import edu.ucsd.livesearch.servlet.BaseServlet;

@SuppressWarnings("serial")
public class GenerateMassesFiles
extends BaseServlet
{
	/*========================================================================
	 * Constants
	 *========================================================================*/
	@SuppressWarnings("unused")
	private static final Logger logger =
		LoggerFactory.getLogger(GenerateMassesFiles.class);
	
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	/**
	 * <p>Accepts and processes requests to generate amino acid masses files
	 * for all tasks on the server.
	 * 
	 * @param request	an {@link HttpServletRequest} object that contains
	 * 					the request the client has made of the servlet
	 * 
	 * @param response	an {@link HttpServletResponse} object that contains
	 * 					the response the servlet sends to the client
	 * 
	 * @throws IOException		if an input or output error is detected
	 * 							when the servlet handles the POST request
	 * 
	 * @throws ServletException	if the request for the POST could not be
	 * 							handled
	 */
	@Override
	protected void doPost(HttpServletRequest request,
		HttpServletResponse response)
	throws ServletException, IOException {
		// initialize properties
		try {
			initialize(request, false);
		} catch (ServletException error) {
			getLogger().error(
				"Error initializing servlet properties from request", error);
			response.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return;
		} catch (Throwable error) {
			getLogger().error(
				"Error initializing servlet properties from request", error);
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		}
		
		// verify authentication of user
		if (isAuthenticated() == false) {
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
				"You must be logged in to generate amino acid masses files.");
			return;
		}
		
		// generate amino acid masses files for all tasks
		String result = Cleanup.GenerateMassesFiles();
		if (result == null) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
				"There was an error generating amino acid masses files. " +
				"Please consult the server log for more details.");
			return;
		} else {
			response.getWriter().println(result);
		}
	}
}
