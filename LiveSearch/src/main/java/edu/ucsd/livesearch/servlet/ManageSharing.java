package edu.ucsd.livesearch.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsd.livesearch.account.AccountManager;
import edu.ucsd.livesearch.dataset.Dataset;
import edu.ucsd.livesearch.dataset.DatasetManager;
import edu.ucsd.livesearch.task.NullTask;
import edu.ucsd.livesearch.task.Task;
import edu.ucsd.livesearch.task.TaskManager;
import edu.ucsd.saint.commons.ConnectionPool;
import edu.ucsd.saint.commons.WebAppProps;
import edu.ucsd.saint.commons.http.HttpParameters;

@SuppressWarnings("serial")
public class ManageSharing
extends BaseServlet
{
	/*========================================================================
	 * Constants
	 *========================================================================*/
	private static final Logger logger =
		LoggerFactory.getLogger(ManageSharing.class);
	private static final ConnectionPool pool = new ConnectionPool(
			"java:comp/env/" + WebAppProps.get("livesearch.jdbc.account"));
	// importable usernames - all users should be able to import these shares
	// TODO: this list should be encoded in an external configuration file
	private static final String[] IMPORTABLE_USERNAMES = new String[]{
		"speclibs", "public_data", "tremololibs"
	};

	/*========================================================================
	 * HTTP servlet methods
	 *========================================================================*/
	/**
	 * <p>Accepts and processes queries for the current user's file sharing
	 * data.
	 *
	 * <p>By convention, a GET request to this servlet is assumed to be a
	 * request to read data only.  No creation, update, or deletion of
	 * server resources is handled by this method.
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
	protected void doGet(HttpServletRequest request,
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
		String user = getUser();

		// verify authentication of user
		if (isAuthenticated() == false) {
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
				"You must be logged in to retrieve file sharing data.");
			return;
		}

		// retrieve file sharing state for the current user
		String shareState = dumpShareState(user);
		// write file sharing state to servlet output
		response.setContentType("text/xml");
		response.setContentLength(shareState.length());
		PrintWriter out = response.getWriter();
		out.print(shareState);
		out.close();
	}

	/**
	 * <p>Accepts and processes requests to persist file sharing state between
	 * two separate users of the ProteoSAFe web application.
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
		HttpParameters parameters = getParameters();
		String user = getUser();
		String owner = user;

		// verify authentication of user
		if (isAuthenticated() == false) {
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
				"You must be logged in to share files.");
			return;
		}

		// retrieve shared username or taskID
		String taskID = null;
		String gettingAccess = parameters.getParameter("sharedUser");
		if (gettingAccess == null || gettingAccess.trim().equals("")) {
			// check if this is a data import request
			String imported = parameters.getParameter("importUser");
			// if not, then a shared user parameter must be present
			if (imported == null || imported.trim().equals(""))
				response.sendError(HttpServletResponse.SC_BAD_REQUEST,
					"You must specify a username with whom " +
					"to share your files.");
			// otherwise, evaluate the import request
			else if (isValidImportUser(imported)) {
				owner = imported;
				gettingAccess = user;
			} else if (isValidImportTask(imported, user)) {
				owner = null;
				gettingAccess = user;
				taskID = imported;
			} else {
				response.sendError(HttpServletResponse.SC_FORBIDDEN,
					String.format("You cannot import data from user/task [%s].",
					imported));
				return;
			}
		}

		// save file sharing state
		if (owner != null && shareUser(owner, gettingAccess) == false)
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
				String.format("There was a problem making files owned by " +
				"user [%s] accessible to user [%s].", owner, gettingAccess));
		else if (taskID != null && importTask(taskID, gettingAccess) == false)
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
				String.format("There was a problem importing files from " +
				"task [%s] as a data share accessible to user [%s].",
				taskID, gettingAccess));
		else {
			// retrieve file sharing state for the current user
			String shareState = dumpShareState(user);
			// write file sharing state to servlet output
			response.setContentType("text/xml");
			response.setContentLength(shareState.length());
			PrintWriter out = response.getWriter();
			out.print(shareState);
			out.close();
		}
	}

	/**
	 * <p>Accepts and processes requests to clear file sharing state between
	 * two separate users of the ProteoSAFe web application.
	 *
	 * <p>By convention, a DELETE request to this servlet is assumed to be a
	 * request for data deletion only.  No creation, reading, or update of
	 * server resources is handled by this method.
	 *
	 * @param request	an {@link HttpServletRequest} object that contains
	 * 					the request the client has made of the servlet
	 *
	 * @param response	an {@link HttpServletResponse} object that contains
	 * 					the response the servlet sends to the client
	 *
	 * @throws IOException		if an input or output error is detected
	 * 							when the servlet handles the DELETE request
	 *
	 * @throws ServletException	if the request for the DELETE could not be
	 * 							handled
	 */
	@Override
	protected void doDelete(
		HttpServletRequest request, HttpServletResponse response
	) throws ServletException, IOException {
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
		HttpParameters parameters = getParameters();
		String user = getUser();
		String owner = user;

		// verify authentication of user
		if (isAuthenticated() == false) {
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
				"You must be logged in to unshare files.");
			return;
		}

		// retrieve shared username or taskID
		String taskID = null;
		String losingAccess = parameters.getParameter("sharedUser");
		if (losingAccess == null || losingAccess.trim().equals("")) {
			// check if this is a data import removal request
			String imported = parameters.getParameter("importUser");
			// if not, then a shared user parameter must be present
			if (imported == null || imported.trim().equals(""))
				response.sendError(HttpServletResponse.SC_BAD_REQUEST,
					"You must specify a username with whom " +
					"to unshare your files.");
			// otherwise, evaluate the import removal request
			else if (isUserDataAccessible(imported, user)) {
				owner = imported;
				losingAccess = user;
			} else if (isTaskImported(imported, user)) {
				owner = null;
				losingAccess = user;
				taskID = imported;
			} else {
				response.sendError(HttpServletResponse.SC_FORBIDDEN,
					String.format("Data resource [%s] does not correspond to " +
					"any user or task whose data is accessible to user [%s].",
					imported, user));
				return;
			}
		}

		// save file sharing state
		if (owner != null && unshareUser(owner, losingAccess) == false)
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
				String.format("There was a problem revoking access to files " +
				"owned by user [%s] from user [%s].", owner, losingAccess));
		else if (taskID != null && unimportTask(taskID, losingAccess) == false)
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
				String.format("There was a problem removing imported data " +
				"share for task [%s] from access by user [%s].",
				taskID, losingAccess));
		else {
			// retrieve file sharing state for the current user
			String shareState = dumpShareState(user);
			// write file sharing state to servlet output
			response.setContentType("text/xml");
			response.setContentLength(shareState.length());
			PrintWriter out = response.getWriter();
			out.print(shareState);
			out.close();
		}
	}

	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	/**
	 * Retrieves the set of users who have access to the argument user's data,
	 * i.e. the set of users with whom the argument user has shared his data.
	 */
	public static Set<String> getSharedUsers(String user) {
		if (user == null)
			return null;
		// set up query properties
		Set<String> sharedUsers = new LinkedHashSet<String>();
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		// perform query
		try {
			conn = pool.aquireConnection();
			stmt = conn.prepareStatement(
				"SELECT * FROM access_rights WHERE resource=?");
			stmt.setString(1, String.format("userdata:%s", user));
			rs = stmt.executeQuery();
			while (rs.next()) {
				String subject = rs.getString("subject");
				if (subject != null && subject.length() > 5)
					sharedUsers.add(subject.substring(5));
			}
		} catch (Throwable error) {
			logger.error(String.format(
				"Error querying users with whom user [%s] has shared data.",
				user), error);
		} finally {
			pool.close(stmt, conn, rs);
		}
		if (sharedUsers.isEmpty())
			return null;
		else return sharedUsers;
	}

	/**
	 * Retrieves the set of users whose data is accessible to the argument user,
	 * i.e. the set of users who have shared their data with the argument user.
	 */
	public static Set<String> getAccessibleUsers(String user) {
		if (user == null)
			return null;
		// first query this user's personal shares
		Set<String> accessibleUsers = getShares(user, "user");
		if (accessibleUsers == null)
			accessibleUsers = new LinkedHashSet<String>();
		// then query global shares accessible to all users
		Set<String> globalResources = getShares("registered", "user");
		if (globalResources != null)
			accessibleUsers.addAll(globalResources);
		if (accessibleUsers.isEmpty())
			return null;
		else return accessibleUsers;
	}

	public static Set<String> getImportedTasks(String user) {
		return getShares(user, "task");
	}

	public static boolean isUserDataAccessible(String owner, String user) {
		return isShareAccessible(owner, user, "user");
	}

	public static boolean shareUser(String owner, String gettingAccess) {
		return addShare(owner, gettingAccess, "user");
	}

	public static boolean unshareUser(String owner, String losingAccess) {
		return removeShare(owner, losingAccess, "user");
	}

	public static boolean isTaskImported(String taskID, String user) {
		return isShareAccessible(taskID, user, "task");
	}

	public static boolean importTask(String taskID, String user) {
		return addShare(taskID, user, "task");
	}

	public static boolean unimportTask(String taskID, String user) {
		return removeShare(taskID, user, "task");
	}

	/*========================================================================
	 * Convenience methods
	 *========================================================================*/
	private static boolean isValidImportUser(String user) {
		if (user == null)
			return false;
		// first validate just the username string itself
		boolean usernameValid = false;
		if (Dataset.isValidDatasetIDString(user))
			usernameValid = true;
		else for (String importable : IMPORTABLE_USERNAMES) {
			if (user.equalsIgnoreCase(importable)) {
				usernameValid = true;
				break;
			}
		}
		// if the username is valid, verify that it represents a real account
		if (usernameValid)
			return AccountManager.isRegistered(user);
		else return false;
	}

	private static boolean isValidImportTask(String taskID, String user) {
		if (taskID == null || user == null)
			return false;
		// only actual tasks can be imported
		Task task = TaskManager.queryTask(taskID);
		if (task == null || task instanceof NullTask)
			return false;
		// the task must be owned either by the user himself,
		// or by someone else who has shared his files with this user
		String taskUser = task.getUser();
		if (user.equals(taskUser))
			return true;
		else return isUserDataAccessible(taskUser, user);
	}

	private static Set<String> getShares(String user, String type) {
		if (user == null || type == null)
			return null;
		// set up query properties
		Set<String> shares = new LinkedHashSet<String>();
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		// perform query
		try {
			String prefix = String.format("%sdata:", type);
			conn = pool.aquireConnection();
			stmt = conn.prepareStatement(String.format(
				"SELECT * FROM access_rights " +
				"WHERE subject=? AND resource LIKE '%s%%'", prefix));
			stmt.setString(1, String.format("user:%s", user));
			rs = stmt.executeQuery();
			while (rs.next()) {
				String resource = rs.getString("resource");
				if (resource != null && resource.length() > prefix.length())
					shares.add(resource.substring(prefix.length()));
			}
		} catch (Throwable error) {
			logger.error(String.format(
				"Error querying resources accessible to user [%s].", user),
				error);
		} finally {
			pool.close(stmt, conn, rs);
		}
		if (shares.isEmpty())
			return null;
		else return shares;
	}

	private static boolean isShareAccessible(
		String owner, String testingAccess, String type
	) {
		if (owner == null || testingAccess == null || type == null)
			return false;
		// set up query properties
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		// perform query
		try {
			conn = pool.aquireConnection();
			stmt = conn.prepareStatement("SELECT * FROM access_rights " +
				"WHERE subject=? AND resource=?");
			stmt.setString(1, String.format("user:%s", testingAccess));
			stmt.setString(2, String.format("%sdata:%s", type, owner));
			rs = stmt.executeQuery();
			return rs.next();
		} catch (Throwable error) {
			logger.error(String.format("Error querying whether resource " +
				"[%s] (%s) is accessible to user [%s]:", owner, type,
				testingAccess), error);
			return false;
		} finally {
			pool.close(stmt, conn, rs);
		}
	}

	private static boolean addShare(
		String owner, String gettingAccess, String type
	) {
		if (owner == null || gettingAccess == null || type == null)
			return false;

        //Checking it already exists
        String share_to_insert = String.format("%sdata:%s", type, owner);
        Set<String> all_shares = getShares(gettingAccess, type);
        if(all_shares != null){
            for(String share_string : all_shares){
                if(share_string.compareTo(owner) == 0){
                    return true;
                }
            }
        }


		// set up query properties
		Connection conn = null;
		PreparedStatement stmt = null;
		// perform query
		try {
			conn = pool.aquireConnection();
			stmt = conn.prepareStatement("INSERT INTO access_rights" +
				"(subject, resource) VALUES(?, ?)");
			stmt.setString(1, String.format("user:%s", gettingAccess));
			stmt.setString(2, String.format("%sdata:%s", type, owner));
			return (stmt.executeUpdate() == 1);
		} catch (Throwable error) {
			logger.error(String.format("Error adding share for resource " +
				"[%s] (%s) to user [%s]:", owner, type, gettingAccess),
				error);
			return false;
		} finally {
			pool.close(stmt, conn);
		}
	}

	private static boolean removeShare(
		String owner, String losingAccess, String type
	) {
		if (owner == null || losingAccess == null || type == null)
			return false;
		// set up query properties
		Connection conn = null;
		PreparedStatement stmt = null;
		// perform query
		try {
			conn = pool.aquireConnection();
			stmt = conn.prepareStatement("DELETE FROM access_rights " +
				"WHERE subject=? AND resource=?");
			stmt.setString(1, String.format("user:%s", losingAccess));
			stmt.setString(2, String.format("%sdata:%s", type, owner));
			return (stmt.executeUpdate() == 1);
		} catch (Throwable error) {
			logger.error(String.format("Error removing share for resource " +
				"[%s] (%s) from user [%s]:", owner, type, losingAccess), error);
			return false;
		} finally {
			pool.close(stmt, conn);
		}
	}

	private static String dumpShareState(String user) {
		if (user == null)
			return null;
		StringBuffer document = new StringBuffer("<?xml version=\"1.0\" ");
		document.append("encoding=\"ISO-8859-1\" ?>\n<sharing>\n");
		// get set of accessible users
		Set<String> accessibleUsers = getAccessibleUsers(user);
		if (accessibleUsers != null && accessibleUsers.isEmpty() == false) {
			for (String accessibleUser : accessibleUsers) {
				document.append("\t<accessible id=\"")
					.append(accessibleUser).append("\">");
				if (Dataset.isValidDatasetIDString(accessibleUser)) {
					Dataset dataset =
						DatasetManager.queryDatasetByID(accessibleUser);
					if (dataset == null)
						document.append(accessibleUser);
					else document.append(StringEscapeUtils.escapeXml11(
						dataset.getSummaryString()));
				} else document.append(accessibleUser);
				document.append("</accessible>\n");
			}
		}
		// get set of imported tasks
		Set<String> importedTasks = getImportedTasks(user);
		if (importedTasks != null && importedTasks.isEmpty() == false) {
			for (String importedTask : importedTasks) {
				document.append("\t<accessible id=\"")
					.append(importedTask).append("\">");
				Task task = TaskManager.queryTask(importedTask);
				if (task == null || task instanceof NullTask)
					document.append(importedTask);
				else document.append(StringEscapeUtils.escapeXml11(
					task.getSummaryString()));
				document.append("</accessible>\n");
			}
		}
		// get set of shared users
		Set<String> sharedUsers = getSharedUsers(user);
		if (sharedUsers != null && sharedUsers.isEmpty() == false) {
			for (String sharedUser : sharedUsers) {
				document.append("\t<shared>");
				document.append(sharedUser);
				document.append("</shared>\n");
			}
		}
		document.append("</sharing>\n");
		return document.toString();
	}
}
