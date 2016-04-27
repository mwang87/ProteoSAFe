package edu.ucsd.livesearch.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsd.livesearch.storage.FileManager;
import edu.ucsd.saint.commons.http.HttpParameters;

@SuppressWarnings("serial")
public class SyncUserSpace
extends BaseServlet
{
	/*========================================================================
	 * Constants
	 *========================================================================*/
	@SuppressWarnings("unused")
	private static final Logger logger =
		LoggerFactory.getLogger(SyncUserSpace.class);
	
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	/**
	 * <p>Accepts and processes requests to synchronize a user's private upload
	 * directory on a remote server.  This service is used whenever a ProteoSAFe
	 * user account is created on a server that shares its user database, but
	 * not its file system, with another server.  In this case, the account will
	 * be known to both systems, but the user's upload directory will only be
	 * created on the remote system if this service is invoked.
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
	protected void doGet(
		HttpServletRequest request, HttpServletResponse response
	) throws ServletException, IOException {
		doPost(request, response);
	}
	
	/**
	 * <p>Accepts and processes requests to synchronize a user's private upload
	 * directory on a remote server.  This service is used whenever a ProteoSAFe
	 * user account is created on a server that shares its user database, but
	 * not its file system, with another server.  In this case, the account will
	 * be known to both systems, but the user's upload directory will only be
	 * created on the remote system if this service is invoked.
	 * 
	 * <p>By convention, a POST request to this servlet is assumed to be a
	 * request for data creation only.  No reading, update, or deletion of
	 * server resources is handled by this method.
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
	protected void doPost(
		HttpServletRequest request, HttpServletResponse response
	) throws ServletException, IOException {
		// initialize properties
		try {
			initialize(request, true);
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
		HttpParameters parameters = getParameters();
		
		// try to synchronize the user's private upload directory
		String user = parameters.getParameter("user");
		if (user == null) {
			reportError(HttpServletResponse.SC_BAD_REQUEST,
				"You must provide a username " +
				"to synchronize a user's private data space.", response);
			return;
		} else try {
			if (FileManager.syncFTPSpace(user) == false ||
				FileManager.syncUserSpace(user) == false) {
				reportError(HttpServletResponse.SC_BAD_REQUEST, String.format(
					"Could not synchronize private data space for user \"%s\".",
					user), response);
				return;
			} else response.getWriter().println(String.format(
				"Successfully synchronized private data space for user \"%s\".",
				user));
		} catch (Throwable error) {
			reportError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
				error.getMessage(), response);
			return;
		}
	}
}
