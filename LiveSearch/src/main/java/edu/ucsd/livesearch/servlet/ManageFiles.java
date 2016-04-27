package edu.ucsd.livesearch.servlet;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsd.livesearch.dataset.Dataset;
import edu.ucsd.livesearch.dataset.DatasetManager;
import edu.ucsd.livesearch.storage.FileManager;
import edu.ucsd.livesearch.storage.UploadManager;
import edu.ucsd.livesearch.task.NullTask;
import edu.ucsd.livesearch.task.Task;
import edu.ucsd.livesearch.task.TaskManager;
import edu.ucsd.saint.commons.http.HttpParameters;

@SuppressWarnings("serial")
public class ManageFiles
extends BaseServlet
{
	/*========================================================================
	 * Constants
	 *========================================================================*/
	@SuppressWarnings("unused")
	private static final Logger logger =
		LoggerFactory.getLogger(ManageFiles.class);
	
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	/**
	 * <p>Accepts and processes queries for file and folder data from the
	 * server.
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
		HttpParameters parameters = getParameters();
		String user = getUser();
		PrintWriter out = response.getWriter();
		
		// verify authentication of user
		if (isAuthenticated() == false) {
			reportError(HttpServletResponse.SC_UNAUTHORIZED,
				"You must be logged in to retrieve file details.", response);
			return;
		}
		
		// if "path" parameter is present, simply provide file details
		String path = parameters.getParameter("path");
		if (path != null) {
			File file = FileManager.getAccessibleFile(path, user);
			if (file != null && file.canRead()) {
				Set<String> users = ManageSharing.getAccessibleUsers(user);
				if (users == null)
					users = new HashSet<String>();
				users.add(user);
				out.println(encodeJSON(file, users));
			} else {
				reportError(HttpServletResponse.SC_BAD_REQUEST,
					"You must specify a valid file path " +
					"to retrieve its details.", response);
				return;
			}
		}
		
		// otherwise, execute the supplied query
		else {
			// retrieve query
			String query = parameters.getParameter("query");
			
			// retrieve query options
			boolean deep = false;
			boolean ignoreCase = false;
			Map<String, String> queryOptions =
				parseJSON(parameters.getParameter("queryOptions"));
			if (queryOptions != null) {
				deep = Boolean.parseBoolean(queryOptions.get("deep"));
				ignoreCase =
					Boolean.parseBoolean(queryOptions.get("ignoreCase"));
			}
			
			// retrieve result options
			boolean dirsOnly = false;
			String options = parameters.getParameter("options");
			if (options != null && options.contains("dirsOnly"))
				dirsOnly = true;
			
			// execute query
			String items =
				executeQuery(user, query, deep, ignoreCase, dirsOnly);
			if (items == null)
				reportError(HttpServletResponse.SC_BAD_REQUEST,
					"There was a problem with your file query.", response);
			
			// write JSON string to page
			else out.print(items);
		}
	}
	
	/**
	 * <p>Accepts and processes requests to upload new files or folders
	 * to the server.  It is assumed that these files were properly queued
	 * for upload in advance of this request.
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
		String user = getUser();
		
		// verify authentication of user
		if (isAuthenticated() == false) {
			// it's possible that the session may have timed out waiting
			// for a long upload, so try to get the user from the request
			String token = (String)parameters.getParameter("token");
			user = UploadManager.getUploadUser(token);
			if (UploadManager.isTokenValid(token, user))
				setUser(user);
			// if no valid user was found in the request either, then
			// this request genuinely cannot be authenticated
			if (isAuthenticated() == false) {
				reportError(HttpServletResponse.SC_UNAUTHORIZED,
					"You must be logged in to create or upload files.",
					response);
				return;
			}
		}
		
		// retrieve and verify target folder
		File folder = null;
		String path = parameters.getParameter("folder");
		if (path != null) {
			folder = FileManager.getOwnedFile(path, user);
			if (folder != null) {
				if (folder.isDirectory() == false)
					folder = folder.getParentFile();
				if (folder.exists() == false && folder.mkdirs() == false)
					folder = null;
			}
			if (folder == null) {
				reportError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
					"There was an error retrieving " +
					"your specified target folder.", response);
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
		
		// retrieve intended target filename, if present
		String name = parameters.getParameter("name");
		
		// retrieve and execute intended action
		String action = parameters.getParameter("action");
		
		// 1. -- Upload files
		if (action == null || action.trim().equalsIgnoreCase("upload")) {
			List<String> failedFiles = new ArrayList<String>();
			for (String fileParam : parameters.getFileParamNames()) {
				FileItem upload = parameters.getFile(fileParam);
				File file = new File(folder, upload.getName());
				boolean uploaded = FileManager.createFile(user, file, upload);
				// if the upload was successful, inform the upload queue
				if (uploaded)
					UploadManager.finishUpload(file);
				else failedFiles.add(upload.getName());
			}
			if (failedFiles != null && failedFiles.size() > 0) {
				StringBuffer failed =
					new StringBuffer("There was an error uploading ");
				if (failedFiles.size() == 1) {
					failed.append("file ");
					failed.append(failedFiles.get(0));
				} else {
					failed.append("the following files:");
					for (String failedFile : failedFiles) {
						failed.append("<br/>");
						failed.append(failedFile);
					}
				}
				reportError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
					failed.toString(), response);
				return;
			}
		}
		
		// 2. -- Create new folder
		else if (action.trim().equalsIgnoreCase("create")) {
			if (name == null || name.trim().equals("")) {
				reportError(HttpServletResponse.SC_BAD_REQUEST,
					"You must provide a valid folder name " +
					"to create a new folder.", response);
				return;
			}
			File file = new File(folder, name);
			if (FileManager.createFolder(user, file) == false) {
				StringBuffer failed =
					new StringBuffer("There was an error creating folder");
				if (file != null) {
					failed.append(" ");
					failed.append(file.getName());
				}
				failed.append(".");
				reportError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
					failed.toString(), response);
				return;
			}
		}
		// TODO: handle checked exceptions properly
	}
	
	/**
	 * <p>Accepts and processes requests to update files or folders
	 * on the server.
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
		
		// verify authentication of user
		if (isAuthenticated() == false) {
			reportError(HttpServletResponse.SC_UNAUTHORIZED,
				"You must be logged in to update files.", response);
			return;
		}
		
		// retrieve and verify path of file to be renamed
		String path = parameters.getParameter("path");
		if (path == null || path.trim().equals("")) {
			reportError(HttpServletResponse.SC_BAD_REQUEST,
				"You must provide a path to a valid existing file " +
				"to rename it.", response);
			return;
		}
		
		// retrieve and verify new name of file to be renamed
		String newName = parameters.getParameter("newName");
		if (newName == null || newName.trim().equals("")) {
			reportError(HttpServletResponse.SC_BAD_REQUEST,
				"You must provide a valid new filename to rename a file.",
				response);
			return;
		}
		
		// rename file
		File file = FileManager.getOwnedFile(path, user);
		if (FileManager.renameFile(user, file, newName) == false) {
			StringBuffer failed =
				new StringBuffer("There was an error renaming file");
			if (file != null) {
				failed.append(" ");
				failed.append(file.getName());
			}
			failed.append(".");
			reportError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
				failed.toString(), response);
			return;
		}
	}
	
	/**
	 * <p>Accepts and processes requests to delete files or folders
	 * on the server.
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
		
		// verify authentication of user
		if (isAuthenticated() == false) {
			reportError(HttpServletResponse.SC_UNAUTHORIZED,
				"You must be logged in to delete files.", response);
			return;
		}
		
		// retrieve and verify path of file to be deleted
		String path = parameters.getParameter("path");
		if (path == null || path.trim().equals("")) {
			reportError(HttpServletResponse.SC_BAD_REQUEST,
				"You must provide a path to a valid existing file " +
				"to delete it.", response);
			return;
		}
		
		// delete file
		File file = FileManager.getOwnedFile(path, user);
		if (FileManager.deleteFile(user, file) == false) {
			StringBuffer failed =
				new StringBuffer("There was an error deleting file");
			if (file != null) {
				failed.append(" ");
				failed.append(file.getName());
			}
			failed.append(".");
			reportError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
				failed.toString(), response);
			return;
		}
	}
	
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	public static final String executeQuery(
		String user, String query,
		boolean deep, boolean ignoreCase, boolean dirsOnly
	) {
		if (user == null || FileManager.syncFTPSpace(user) == false)
			return null;
		// construct a sorted list of users whose files are
		// accessible to this user, starting with the user himself
		List<String> users = new ArrayList<String>();
		users.add(user);
		// retrieve the set of other accessible users
		Set<String> accessibleUsers = ManageSharing.getAccessibleUsers(user);
		if (accessibleUsers != null && accessibleUsers.isEmpty() == false) {
			// sort the set of users
			String[] sortedUsers =
				accessibleUsers.toArray(new String[accessibleUsers.size()]);
			Arrays.sort(sortedUsers);
			// add the sorted set of users
			users.addAll(Arrays.asList(sortedUsers));
		}
		// retrieve the set of imported tasks
		List<String> tasks = new ArrayList<String>();
		Set<String> importedTasks = ManageSharing.getImportedTasks(user);
		if (importedTasks != null && importedTasks.isEmpty() == false) {
			// sort the set of tasks
			String[] sortedTasks =
				importedTasks.toArray(new String[importedTasks.size()]);
			Arrays.sort(sortedTasks);
			// add the sorted set of tasks
			tasks.addAll(Arrays.asList(sortedTasks));
		}
		// parse and perform query
		Map<String, String> queryTerms = parseJSON(query);
		List<File> results = null;
		// if the query is empty, just return the root nodes
		if (queryTerms == null || queryTerms.isEmpty()) {
			results = new ArrayList<File>(users.size());
			// add root folders belonging to all accessible users
			for (String accessibleUser : users) {
				File folder = FileManager.getFile("f." + accessibleUser);
				if (folder != null && folder.canRead() && folder.isDirectory())
					results.add(folder);
			}
			// add root folders belonging to all imported tasks
			for (String importedTask : tasks) {
				File folder = FileManager.getFile("t." + importedTask);
				if (folder != null && folder.canRead() && folder.isDirectory())
					results.add(folder);
			}
		} else {
			File folder = null;
			// if "parentDir" is one of the query attributes,
			// use the selected folder as the root folder
			String parentDir = queryTerms.get("parentDir");
			if (parentDir != null)
				folder = FileManager.getAccessibleFile(parentDir, user);
			// otherwise just use the user's root folder
			else folder = FileManager.getFile("f." + user);
			if (folder != null && folder.canRead() && folder.isDirectory())
				results = query(folder, queryTerms, deep, ignoreCase, users);
			
		}
		// report results of query
		StringBuffer items = new StringBuffer("\t\"items\":[");
		int total = 0;
		if (results != null && results.isEmpty() == false) {
			// sort results into folders, then files
			List<File> folders = new ArrayList<File>();
			List<File> files = new ArrayList<File>();
			for (File file : results) {
				if (file.isDirectory())
					folders.add(file);
				else files.add(file);
			}
			// only add files if "dirsOnly" was not specified
			if (dirsOnly == false)
				folders.addAll(files);
			// sort the results alphabetically
			Collections.sort(folders);
			// write the results to the output string
			for (File file : folders) {
				items.append("\n\t\t");
				items.append(encodeJSON(file, users));
				items.append(",");
				total++;
			}
			// remove trailing comma
			if (items.charAt(items.length() - 1) == ',')
				items.delete(items.length() - 1, items.length());
		}
		items.append("\n\t]");
		// write complete output string
		StringBuffer output =
			new StringBuffer(String.format("{\n\t\"total\":%d,\n", total));
		output.append(items);
		output.append("\n}");
		return output.toString();
	}
	
	/*========================================================================
	 * Convenience methods
	 *========================================================================*/
	private static Map<String, String> parseJSON(String json) {
		// confirm that JSON string is present
		if (json == null)
			return null;
		// confirm that JSON string is correctly formatted -- it should
		// consist of a comma-delimited list of Javascript object properties,
		// enclosed by curly braces
		json = json.trim();
		if (json.startsWith("{") == false || json.endsWith("}") == false)
			return null;
		json = json.substring(1, json.length() - 1).trim();
		String[] properties = json.split(",");
		if (properties == null || properties.length < 1)
			return null;
		// parse JSON properties
		Map<String, String> parsedProperties = new HashMap<String, String>();
		for (String property : properties) {
			// confirm that JSON property is correctly formatted -- it should
			// consist of two strings, representing the property name and the
			// property value respectively, delimited by a colon
			String[] parts = property.trim().split(":");
			if (parts == null || parts.length < 2)
				return null;
			// if there are any ":" characters in the specified value string,
			// then Java has split them up -- we'll have to assume that all
			// such characters were in fact part of the value string and not
			// the name string, and recombine them.
			else if (parts.length > 2) {
				for (int i=2; i<parts.length; i++)
					parts[1] += ":" + parts[i];
			}
			String name = clean(parts[0]);
			String value = clean(parts[1]);
			if (StringUtils.isEmpty(name) || StringUtils.isEmpty(value))
				return null;
			parsedProperties.put(name, value);
		}
		// return parsed query terms
		if (parsedProperties.size() < 1)
			return null;
		else return parsedProperties;
	}
	
	private static String clean(String value) {
		if (value == null)
			return null;
		if (value.startsWith("\""))
			value = value.substring(1);
		if (value.endsWith("\""))
			value = value.substring(0, value.length() - 1);
		return value.trim();
	}
	
	private static boolean isTaskDirectory(File file) {
		if (file == null || file.canRead() == false)
			return false;
		// a task directory should be two directories down from the
		// root directory of the system-wide task space
		String path = file.getAbsolutePath();
		if (path.startsWith(FileManager.USER_SPACE) == false)
			return false;
		else path = path.substring(FileManager.USER_SPACE.length());
		// trim leading slash, if present
		if (path.startsWith(File.separator))
			path = path.substring(File.separator.length());
		String[] directories = path.split(File.separator);
		return directories != null && directories.length == 2 &&
			directories[1] != null && directories[1].trim().equals("") == false;
	}
	
	private static boolean isDatasetDirectory(File file) {
		if (file == null || file.canRead() == false)
			return false;
		else return Dataset.isValidDatasetIDString(file.getName());
	}
	
	private static String encodeJSON(File file, Collection<String> users) {
		if (file == null || file.canRead() == false)
			return null;
		String name = file.getName();
		// the "guest user" rule
		if (name.matches("Guest\\.(\\w)+-(\\d)+"))
			name = name.substring(0, name.indexOf("."));
		// treat task and dataset root directories specially
		else if (isTaskDirectory(file)) {
			Task task = TaskManager.queryTask(name);
			if (task != null && task instanceof NullTask == false)
				name = JSONObject.escape(task.getSummaryString());
		} else if (isDatasetDirectory(file)) {
			Dataset dataset = DatasetManager.queryDatasetByID(name);
			if (dataset != null)
				name = JSONObject.escape(dataset.getSummaryString());
		}
		StringBuffer json = new StringBuffer(
			String.format("{\"name\":\"%s\", ", name));
		json.append(String.format("\"parentDir\":\"%s\", ",
			FileManager.resolvePath(file.getParent(), users)));
		json.append(String.format("\"size\":%d, ", file.length()));
		json.append(String.format("\"modified\":%d, ", file.lastModified()));
		json.append(String.format("\"directory\":%b, ", file.isDirectory()));
		json.append(String.format("\"path\":\"%s\"",
			FileManager.resolvePath(file.getPath(), users)));
		json.append("}");
		return json.toString();
	}
	
	private static List<File> query(File folder, Map<String, String> query,
		boolean deep, boolean ignoreCase, Collection<String> users) {
		if (folder == null || folder.isDirectory() == false)
			return null;
		List<File> files = new ArrayList<File>();
		// check folder against query parameters
		if (matchFile(folder, query, ignoreCase, users))
			files.add(folder);
		// examine contents of folder
		File[] contents = folder.listFiles();
		if (contents != null && contents.length > 0) {
			for (File file : contents) {
				// if deep search is specified, recurse
				if (deep && file.isDirectory()) {
					List<File> descendants =
						query(file, query, deep, ignoreCase, users);
					if (descendants != null && descendants.isEmpty() == false)
						files.addAll(descendants);
				}
				// otherwise just check this file against query parameters
				else if (matchFile(file, query, ignoreCase, users))
					files.add(file);
			}
		}
		if (files.isEmpty())
			return null;
		else return files;
	}
	
	private static boolean matchFile(File file, Map<String, String> query,
		boolean ignoreCase, Collection<String> users
	) {
		if (file == null || file.canRead() == false)
			return false;
		else if (query == null || query.size() < 1)
			return true;
		// check file against all specified query attributes
		for (String attribute : query.keySet()) {
			// get specified attribute value to be checked from file
			if (attribute == null)
				continue;
			String value = null;
			if (attribute.equals("name")) {
				value = file.getName();
			} else if (attribute.equals("parentDir")) {
				value = FileManager.resolvePath(file.getParent(), users);
			} else if (attribute.equals("size")) {
				value = Long.toString(file.length());
			} else if (attribute.equals("modified")) {
				value = Long.toString(file.lastModified());
			} else if (attribute.equals("directory")) {
				value = Boolean.toString(file.isDirectory());
			} else if (attribute.equals("path")) {
				value = FileManager.resolvePath(file.getPath(), users);
			}
			if (value == null)
				return false;
			// check attribute value against specified reference value
			// TODO: provide support for wildcards
			String reference = query.get(attribute);
			if ((ignoreCase == false && value.equals(reference) == false) ||
				(ignoreCase && value.equalsIgnoreCase(reference) == false))
				return false;
		}
		// we got through every query attribute, and the file matched them all
		return true;
	}
}
