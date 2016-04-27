package edu.ucsd.livesearch.servlet;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsd.livesearch.storage.FileManager;
import edu.ucsd.livesearch.storage.UploadManager;
import edu.ucsd.livesearch.storage.UploadManager.PendingUpload;
import edu.ucsd.livesearch.task.NullTask;
import edu.ucsd.livesearch.task.Task;
import edu.ucsd.livesearch.task.TaskManager;
import edu.ucsd.livesearch.task.WorkflowUtils;
import edu.ucsd.livesearch.task.TaskManager.TaskStatus;
import edu.ucsd.saint.commons.http.HttpParameters;

@SuppressWarnings("serial")
public class QueueUploads
extends BaseServlet
{
	/*========================================================================
	 * Constants
	 *========================================================================*/
	private static final Logger logger =
		LoggerFactory.getLogger(QueueUploads.class);
	
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	/**
	 * <p>Accepts and processes queries for pending upload data from the server.
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
		// first poll the file upload queue to remove any stalled uploads,
		// and in turn clear out any stalled tasks that depend on them
		UploadManager.pollQueue();
		
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
		HttpParameters parameters = getParameters();
		
		// verify authentication of user
		if (isAuthenticated() == false) {
			reportError(HttpServletResponse.SC_UNAUTHORIZED,
				"You must be logged in to retrieve file details.", response);
			return;
		}
		
		// retrieve and verify ID of task whose
		// pending uploads are to be retrieved
		String taskId = parameters.getParameter("task");
		if (taskId == null || taskId.trim().equals("")) {
			reportError(HttpServletResponse.SC_BAD_REQUEST,
				"You must provide the ID of a valid queued task " +
				"to retrieve its pending upload details.", response);
			return;
		}
		
		// then retrieve this task's pending upload data
		Map<String, PendingUpload> pendingUploads =
			WorkflowUtils.getUploadsByTask(taskId);
		// only write to the output stream if there
		// are pending uploads for this task
		if (pendingUploads != null && pendingUploads.isEmpty() == false) {
			PrintWriter out = response.getWriter();
			StringBuffer output = new StringBuffer("[\n\t");
			// if an upload type was specified, only
			// return the details for those uploads
			String purpose = parameters.getParameter("purpose");
			if (purpose != null) {
				Task task = TaskManager.queryTask(taskId);
				if (task == null || task instanceof NullTask ||
					TaskStatus.UPLOADING.equals(task.getStatus()) == false) {
					reportError(HttpServletResponse.SC_BAD_REQUEST,
						"You must provide the ID of a valid uploading task " +
						"to retrieve its pending upload details.", response);
					return;
				}
				List<String> files = task.queryUploadsByPurpose(purpose);
				if (files == null || files.isEmpty()) {
					reportError(HttpServletResponse.SC_BAD_REQUEST, null,
						response);
					return;
				}
				for (String file : files) {
					PendingUpload upload = pendingUploads.get(file);
					if (upload == null)
						continue;
					output.append(printUpload(upload, file));
					output.append(",\n\t");
				}
			}
			// otherwise return the details for all of this task's uploads
			else for (String file : pendingUploads.keySet()){
				output.append(printUpload(pendingUploads.get(file), file));
				output.append(",\n\t");
			}
			if (output.length() >= 3)
				output.setLength(output.length() - 3);
			output.append("\n]");
			out.println(output.toString());
		}
	}
	
	/**
	 * <p>Accepts and processes requests to queue file uploads to the server.
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
		// first poll the file upload queue to remove any stalled uploads,
		// and in turn clear out any stalled tasks that depend on them
		UploadManager.pollQueue();
		
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
		String user = getUser();
		PrintWriter out = response.getWriter();
		
		// verify authentication of user
		if (isAuthenticated() == false) {
			reportError(HttpServletResponse.SC_UNAUTHORIZED,
				"You must be logged in to queue file uploads.", response);
			return;
		}
		
		// retrieve and verify target folder
		File folder = null;
		String path = parameters.getParameter("folder");
		if (path != null) {
			folder = getOwnedFile(path, user);
			if (folder != null) {
				if (folder.isDirectory() == false)
					folder = folder.getParentFile();
				if (folder.exists() == false && folder.mkdirs() == false)
					folder = null;
			}
			if (folder == null) {
				reportError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
					"There was an error retrieving your specified target " +
					"folder.", response);
				return;
			}
		}
		if (folder == null) {
			// if no target folder is selected,
			// just use the user's root folder
			folder = FileManager.getFile("f." + user);
			if (folder == null || folder.isDirectory() == false ||
				folder.canRead() == false) {
				reportError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
					"There was an error retrieving your root folder.",
					response);
				return;
			}
		}
		
		// retrieve upload filename
		List<String[]> uploads =
			extractUploads(parameters.getParameter("files"));
		if (uploads == null || uploads.isEmpty()) {
			reportError(HttpServletResponse.SC_BAD_REQUEST,
				"You must provide a list of valid file details " +
				"to queue files for upload.", response);
			return;
		}
		
		// generate a valid upload token for this batch of uploads
		String token = UploadManager.getUploadToken(user, getSession().getId());
		
		// queue each upload
		for (String[] upload : uploads) {
			File file = new File(folder, upload[1]);
			int fileSize;
			try {
				fileSize = Integer.parseInt(upload[2]);
			} catch (Throwable error) {
				reportError(HttpServletResponse.SC_BAD_REQUEST,
					"You must provide a valid file size in bytes " +
					"for each file queued for upload.", response);
				return;
				
			}
			// queue upload and create dummy file
			if (UploadManager.queueUpload(
				file, upload[0], token, user, fileSize) == false ||
				FileManager.createFile(user, file, null) == false) {
				StringBuffer failed =
					new StringBuffer("There was an error queuing file");
				if (file != null) {
					failed.append(" \"");
					failed.append(upload[1]);
					failed.append("\"");
				}
				failed.append(" for upload.");
				reportError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
					failed.toString(), response);
				return;
			}
		}
		// if the entire batch of files was successfully queued,
		// print the upload token, so the client can use it later
		out.print(token);
	}
	
	/**
	 * <p>Accepts and processes requests to update the progress of file
	 * uploads on the server.
	 * 
	 * <p>By convention, a PUT request to this servlet is assumed to be a
	 * request for data update only.  No creation, reading, or deletion of
	 * server resources is handled by this method.
	 * 
	 * @param request	an {@link HttpServletRequest} object that contains
	 * 					the request the client has made of the servlet
	 * 
	 * @param response	an {@link HttpServletResponse} object that contains
	 * 					the response the servlet sends to the client
	 * 
	 * @throws IOException		if an input or output error is detected
	 * 							when the servlet handles the PUT request
	 * 
	 * @throws ServletException	if the request for the PUT could not be
	 * 							handled
	 */
	@Override
	protected void doPut(
		HttpServletRequest request, HttpServletResponse response
	) throws ServletException, IOException {
		// first poll the file upload queue to remove any stalled uploads,
		// and in turn clear out any stalled tasks that depend on them
		UploadManager.pollQueue();
		
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
		HttpParameters parameters = getParameters();
		String user = getUser();
		
		// retrieve batch token of upload to be progressed
		String token = (String)parameters.getParameter("token");
		if (token == null || token.trim().equals("")) {
			reportError(HttpServletResponse.SC_BAD_REQUEST,
				"You must provide a valid file upload batch token " +
				"to update an upload's progress.", response);
			return;
		}
		
		// verify authentication of user
		if (isAuthenticated() == false) {
			// it's possible that the session may have timed out waiting
			// for a long upload, so try to get the user from the request
			user = UploadManager.getUploadUser(token);
			if (UploadManager.isTokenValid(token, user))
				setUser(user);
			// if no valid user was found in the request either, then
			// this request genuinely cannot be authenticated
			if (isAuthenticated() == false) {
				reportError(HttpServletResponse.SC_UNAUTHORIZED,
					"You must be logged in to update an upload's progress.",
					response);
				return;
			}
		}
		
		// retrieve id of upload to be progressed
		String id = parameters.getParameter("id");
		if (id == null || id.trim().equals("")) {
			reportError(HttpServletResponse.SC_BAD_REQUEST,
				"You must provide a valid file upload ID " +
				"to update the upload's progress.", response);
			return;
		}
		
		// retrieve bytes uploaded for upload to be progressed
		int bytesUploaded;
		try {
			bytesUploaded = Integer.parseInt(parameters.getParameter("bytes"));
		} catch (Throwable error) {
			reportError(HttpServletResponse.SC_BAD_REQUEST,
				"You must provide a valid number of bytes uploaded " +
				"to update an upload's progress.", response);
			return;
		}
		
		// update upload's progress
		if (UploadManager.progressUpload(id, token, bytesUploaded) == false) {
			reportError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
				"There was an error updating progress for upload \"" + id +
				"\", from batch \"" + token + "\".", response);
			return;
		}
	}
	
	/**
	 * <p>Accepts and processes requests to cancel queued uploads on the server.
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
		// first poll the file upload queue to remove any stalled uploads,
		// and in turn clear out any stalled tasks that depend on them
		UploadManager.pollQueue();
		
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
		HttpParameters parameters = getParameters();
		String user = getUser();
		
		// retrieve batch token of upload to be canceled
		String token = (String)parameters.getParameter("token");
		if (token == null || token.trim().equals("")) {
			reportError(HttpServletResponse.SC_BAD_REQUEST,
				"You must provide a valid file upload batch token " +
				"to cancel an upload.", response);
			return;
		}
		
		// verify authentication of user
		if (isAuthenticated() == false) {
			// it's possible that the session may have timed out waiting
			// for a long upload, so try to get the user from the request
			user = UploadManager.getUploadUser(token);
			if (UploadManager.isTokenValid(token, user))
				setUser(user);
			// if no valid user was found in the request either, then
			// this request genuinely cannot be authenticated
			if (isAuthenticated() == false) {
				reportError(HttpServletResponse.SC_UNAUTHORIZED,
					"You must be logged in to cancel an upload.", response);
				return;
			}
		}
		
		// retrieve id of upload to be canceled
		String id = parameters.getParameter("id");
		if (id == null || id.trim().equals("")) {
			reportError(HttpServletResponse.SC_BAD_REQUEST,
				"You must provide a valid file upload ID " +
				"to cancel the upload.", response);
			return;
		}
		
		// cancel upload
		StringBuffer message = new StringBuffer("The upload  having ID \"");
		message.append(id);
		message.append("\" and token \"");
		message.append(token);
		message.append("\" his being canceled due servlet request by user \"");
		message.append(user);
		message.append("\".");
		logger.info(message.toString());
		UploadManager.cancelUpload(id, token);
	}
	
	/*========================================================================
	 * Convenience methods
	 *========================================================================*/
	private static File getOwnedFile(String path, String user) {
		if (path == null || user == null)
			return null;
		// if specified file is not owned, then it can't be retrieved
		File file = FileManager.getFile("f." + path);
		if (FileManager.isOwned(file, user) == false)
			return null;
		else return file;
	}
	
	private static List<String[]> extractUploads(String files) {
		if (files == null)
			return null;
		// when queuing file uploads, the "files" parameter is assumed to
		// be a semicolon-delimited list of files in this batch
		String[] fileArray = files.split(";");
		if (fileArray == null || fileArray.length < 1)
			return null;
		List<String[]> uploads = new Vector<String[]>(fileArray.length);
		for (String file : fileArray) {
			// each file in the list is assumed to be a colon-delimited
			// list of file details in the following format: <id>:<name>:<size>
			String[] details = file.split(":");
			if (details == null || details.length != 3)
				return null;
			else uploads.add(details);
		}
		if (uploads.isEmpty())
			return null;
		else return uploads;
	}
	
	private static String printUpload(PendingUpload upload, String name) {
		if (upload == null)
			return null;
		StringBuffer output = new StringBuffer("{\"id\":\"");
		output.append(upload.getId());
		output.append("\", \"name\":\"");
		if (name != null)
			output.append(name);
		else output.append(upload.getName());
		output.append("\", \"percent\":");
		output.append(upload.getPercentUploaded());
		output.append(", \"elapsed\":\"");
		output.append(upload.getTimeSinceLastUpdate());
		output.append("\"}");
		return output.toString();
	}
}
