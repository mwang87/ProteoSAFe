package edu.ucsd.livesearch.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.fileupload.FileUploadException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsd.livesearch.account.AccountManager;
import edu.ucsd.livesearch.util.SessionListener;
import edu.ucsd.saint.commons.http.HttpParameters;

@SuppressWarnings("serial")
public abstract class BaseServlet
extends HttpServlet
{
	/*========================================================================
	 * Constants
	 *========================================================================*/
	private static final Logger logger =
		LoggerFactory.getLogger(BaseServlet.class);
	
	/*========================================================================
	 * Properties
	 *========================================================================*/
	private HttpParameters parameters;
	private HttpSession session;
	private String user;
	
	/*========================================================================
	 * Property accessor methods
	 *========================================================================*/
	/**
	 * Retrieves the correct logger instance associated with this class.
	 * 
	 * @return	the {@link Logger} object associated with this servlet class
	 */
	protected static final Logger getLogger() {
		return logger;
	}
	
	protected final HttpParameters getParameters() {
		return parameters;
	}
	
	protected final void setParameters(HttpParameters parameters) {
		this.parameters = parameters;
	}
	
	protected final HttpSession getSession() {
		return session;
	}
	
	protected final void setSession(HttpSession session) {
		this.session = session;
	}
	
	protected final String getUser() {
		return user;
	}
	
	protected final void setUser(String user) {
		this.user = user;
	}
	
	protected final boolean isAuthenticated() {
		String user = getUser();
		if (user == null)
			return false;
		else return AccountManager.isActiveUsername(user);
	}
	
	protected final boolean isAdministrator() {
		String user = getUser();
		if (user == null)
			return false;
		else return AccountManager.isAdministrator(user);
	}
	
	/*========================================================================
	 * Convenience methods
	 *========================================================================*/
	/**
	 * Initializes the servlet with the information stored in the provided
	 * HTTP request.
	 * 
	 * @param request			an {@link HttpServletRequest} object that
	 * 							contains the request the client has made of
	 * 							the servlet
	 * 
	 * @throws ServletException	if the request does not contain the valid
	 * 							information needed to initialize the servlet
	 */
	protected void initialize(HttpServletRequest request, boolean detached)
	throws ServletException {
		// retrieve request parameters
		try {
			setParameters(
				new HttpParameters(request, request.getParameter("uuid")));
		} catch (FileUploadException error) {
			getLogger().debug("Error retrieving HTTP file upload parameter",
				error);
			throw new ServletException(error);
		}
		
		// if this request's session has been detached, then consult the
		// session map to find the correct session and re-attach it
		HttpSession session = null;
		if (detached) {
			String targetId = parameters.getParameter("JSESSIONID");
			if (targetId != null) {
				session = SessionListener.getSessionFromId(targetId);
				setSession(session);
			}
		}
		// if no valid session was found, just use this request's session
		if (session == null) {
			session = request.getSession();
			setSession(session);
		}
		
		// retrieve authenticated user, if any
		setUser((String)session.getAttribute("livesearch.user"));
	}
	
	protected void reportError(
		int code, String message, HttpServletResponse response
	) throws IOException {
		reportError(code, message, response, null);
	}
	
	protected void reportError(
		int code, String message, HttpServletResponse response, Throwable error
	) throws IOException {
		// log error
		if (error != null)
			getLogger().error(message != null ? message : "", error);
		else if (message != null)
			getLogger().error(message);
		// send error in HTTP response
		if (response != null) {
			if (message != null)
				response.sendError(code, message);
			else response.sendError(code);
		}	
	}
}
