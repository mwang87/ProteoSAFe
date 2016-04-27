package edu.ucsd.livesearch.servlet;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.xpath.XPathAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import edu.ucsd.livesearch.account.AccountManager;
import edu.ucsd.livesearch.parameter.LegacyParameterConverter;
import edu.ucsd.livesearch.storage.FileManager;
import edu.ucsd.livesearch.task.NullTask;
import edu.ucsd.livesearch.task.OnDemandLoader;
import edu.ucsd.livesearch.task.Task;
import edu.ucsd.livesearch.task.TaskManager;
import edu.ucsd.livesearch.util.FileIOUtils;
import edu.ucsd.saint.commons.WebAppProps;
import edu.ucsd.saint.commons.http.HttpParameters;

@SuppressWarnings("serial")
public class ManageParameters
extends BaseServlet
{
	/*========================================================================
	 * Constants
	 *========================================================================*/
	private static final Logger logger =
		LoggerFactory.getLogger(ManageParameters.class);
	
	private static final String system_protocol_user = "continuous";
	
	/*========================================================================
	 * HTTP servlet methods
	 *========================================================================*/
	/**
	 * <p>Accepts and processes queries for saved ProteoSAFe web application
	 * search protocols from the server.
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
	protected final void doGet(HttpServletRequest request,
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
		PrintWriter out = response.getWriter();
		
		// retrieve protocol XML
		String xml = null;
		String task = parameters.getParameter("task");
		if (task != null)
			xml = getTaskParameters(task);
		// verify authentication of user before attempting
		// to retrieve a user-owned search protocol
		else if (isAuthenticated() == false) {
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
				"You must be logged in to retrieve a search protocol.");
			return;
		} else {
			String protocol = parameters.getParameter("protocol");
			if (protocol == null || protocol.trim().equals("")) {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST,
					"You must specify a protocol name " +
					"to retrieve its details.");
			}
			xml = getProtocol(protocol, getUser());
		}
		// verify that protocol XML was successfully retrieved
		if (xml == null) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
				"The server was unable to retrieve your protocol details.");
			return;
		}
		
		// print protocol XML
		response.setContentType("application/xml");
		response.setContentLength(xml.length());
		out.print(xml);
		out.close();
	}
	
	/**
	 * <p>Accepts and processes requests to save new ProteoSAFe web application
	 * search protocols to the server or delete protocols, but deletion is only enabled by admins currently
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
	protected final void doPost(HttpServletRequest request,
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
		
		// verify authentication of user
		if (isAuthenticated() == false) {
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
				"You must be logged in to save a search protocol.");
			return;
		}
		
		// retrieve protocol name
		String protocol = parameters.getParameter("protocol");
		if (protocol == null || protocol.trim().equals("")) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST,
				"You must specify a protocol name to save a search protocol.");
		}
		
		
		String operation = parameters.getParameter("operation");
		if(operation == null || operation.compareTo("save") == 0){
			// retrieve protocol contents
			String contents = parameters.getParameter("contents");
			if (contents == null || contents.trim().equals("")) {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST,
					"You must provide file contents to save a search protocol.");
			}
			
			// save protocol
			if (saveProtocol(protocol, user, contents) == false)
				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
					"The server was unable to save your protocol.");
			else {
				// print saved protocol XML
				String xml = getProtocol(protocol, user);
				response.setContentType("application/xml");
				response.setContentLength(xml.length());
				PrintWriter out = response.getWriter();
				out.print(xml);
				out.close();
			}
			
			return;
		}
		
		if(operation != null || operation.compareTo("delete") == 0){
			AccountManager manager = AccountManager.getInstance();
			boolean isAdmin = manager.checkRole(user, "administrator");	
			if (isAdmin){
				if (removeProtocol(protocol, user) == false){
					response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
						"The server was unable to save your protocol.");
				}
			}
			else{
				response.sendError(HttpServletResponse.SC_FORBIDDEN,
				"The server was unable to remove your protocol.");
			}
		}
		
	}
	
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	/**
	 * Returns Protocols that the user can use, including system protocols
	 */
	public static final List<String> getProtocols(String user) {
		List<String> system_protocols = getOwnedProtocols(system_protocol_user);
		if (user == null)
			return null;
		String path = WebAppProps.getPath("livesearch.protocols.path");
		if (path == null) {
			logger.error("Error retrieving protocols for user \"" + user +
				"\": protocol directory path could not be found.");
			return null;
		}
		File root = new File(path, user);
		if (root.canRead() == false || root.isDirectory() == false) {
			// TODO: report error
			return system_protocols;
		} else {
			String[] files = root.list(new ProtocolFilter());
			List<String> filenames = new ArrayList<String>(files.length);
			for (String filename : files)
				// strip off ".xml" from end of filename
				filenames.add(filename.substring(0, (filename.length() - 4)));
			if (user.compareTo(system_protocol_user) != 0 &&
				system_protocols != null)
				filenames.addAll(system_protocols);
			return filenames;
		}
	}
	
	/**
	 * Returns Protocols User owns and can edit
	 * @param user
	 * @return
	 */
	public static final List<String> getOwnedProtocols(String user) {
		if (user == null)
			return null;
		String path = WebAppProps.getPath("livesearch.protocols.path");
		if (path == null) {
			logger.error("Error retrieving protocols for user \"" + user +
				"\": protocol directory path could not be found.");
			return null;
		}
		File root = new File(path, user);
		if (root.canRead() == false || root.isDirectory() == false) {
			// TODO: report error
			return null;
		} else {
			String[] files = root.list(new ProtocolFilter());
			List<String> filenames = new ArrayList<String>(files.length);
			for (String filename : files)
				// strip off ".xml" from end of filename
				filenames.add(filename.substring(0, (filename.length() - 4)));
			return filenames;
		}
	}
	
	public static final String getTaskParameters(Task task) {
		if (task == null) {
			logger.error(
				"Error retrieving task parameters: task is null.");
			return null;
		}
		File file = task.getPath("params/params.xml");
		if (file == null || file.canRead() == false) {
			// attempt to generate parameters file
			OnDemandLoader.load(new LegacyParameterConverter(task));
			file = task.getPath("params/params.xml");
			if (file == null || file.canRead() == false) {
				logger.error("Error retrieving task parameters: " +
					"parameter file for task with ID \"" + task.getID() +
					"\" is missing, and cannot be generated.");
				return null;
			}
		}
		return FileIOUtils.readFile(file);
	}
	
	public static final String getTaskParameters(String taskID) {
		if (taskID == null) {
			logger.error(
				"Error retrieving task parameters: task ID is null.");
			return null;
		}
		Task task = TaskManager.queryTask(taskID);
		if (task == null || task instanceof NullTask) {
			logger.error("Error retrieving task parameters: task with ID \"" +
				taskID + "\" could not be found.");
			return null;
		} else return getTaskParameters(task);
	}
	
	public static final Collection<String> getTaskParameter(
		Task task, String parameter
	) {
		if (task == null) {
			logger.error(
				"Error retrieving task parameter: task is null.");
			return null;
		} else if (parameter == null) {
			logger.error(
				"Error retrieving task parameter: parameter name is null.");
			return null;
		}
		String parameters = getTaskParameters(task);
		if (parameters == null) {
			logger.error("Error retrieving parameter \"" + parameter +
				"\" of task \"" + task.getID() +
				"\": task parameters could not be retrieved.");
			return null;
		}
		Document paramDoc = FileIOUtils.parseXML(parameters);
		if (paramDoc == null) {
			logger.error("Error retrieving parameter \"" + parameter +
				"\" of task \"" + task.getID() +
				"\": parameter document could not be parsed.");
			return null;
		}
		NodeList paramNodes = null;
		try {
			paramNodes = XPathAPI.selectNodeList(paramDoc,
				String.format("/parameters/parameter[@name='%s']", parameter));
		} catch (Throwable error) {
			logger.error("Error retrieving parameter \"" + parameter +
				"\" of task \"" + task.getID() + "\": parameter node list " +
				"could not be parsed from parameter document.", error);
			return null;
		} finally {
			if (paramNodes == null || paramNodes.getLength() < 1) {
				logger.error("Error retrieving parameter \"" + parameter +
					"\" of task \"" + task.getID() +
					"\": parameter not found in parameters file.");
				return null;
			}
		}
		try {
			Collection<String> params =
				new LinkedHashSet<String>(paramNodes.getLength());
			for (int i=0; i<paramNodes.getLength(); i++)
				params.add(paramNodes.item(i).getFirstChild().getNodeValue());
			return params;
		} catch (Throwable error) {
			logger.error("Error retrieving parameter \"" + parameter +
				"\" of task \"" + task.getID() +
				"\": parameter value could not be retrieved from parsed node.");
			return null;
		}
	}
	
	public static final Collection<String> getTaskParameter(
		String taskID, String parameter
	) {
		if (taskID == null) {
			logger.error(
				"Error retrieving task parameter: task ID is null.");
			return null;
		} else if (parameter == null) {
			logger.error(
				"Error retrieving task parameter: parameter name is null.");
			return null;
		}
		Task task = TaskManager.queryTask(taskID);
		if (task == null || task instanceof NullTask) {
			logger.error("Error retrieving task parameter: task with ID \"" +
				taskID + "\" could not be found.");
			return null;
		} else return getTaskParameter(task, parameter);
	}
	
	public static final String getFirstTaskParameter(
		Task task, String parameter
	) {
		if (task == null) {
			logger.error(
				"Error retrieving task parameter: task is null.");
			return null;
		} else if (parameter == null) {
			logger.error(
				"Error retrieving task parameter: parameter name is null.");
			return null;
		}
		Collection<String> parameters = getTaskParameter(task, parameter);
		if (parameters == null || parameters.isEmpty())
			return null;
		else return parameters.iterator().next();
	}
	
	public static final String getFirstTaskParameter(
		String taskID, String parameter
	) {
		if (taskID == null) {
			logger.error(
				"Error retrieving task parameter: task ID is null.");
			return null;
		} else if (parameter == null) {
			logger.error(
				"Error retrieving task parameter: parameter name is null.");
			return null;
		}
		Task task = TaskManager.queryTask(taskID);
		if (task == null || task instanceof NullTask) {
			logger.error("Error retrieving task parameter: task with ID \"" +
				taskID + "\" could not be found.");
			return null;
		} else return getFirstTaskParameter(task, parameter);
	}
	
	public static final String getProtocol(String protocol, String user) {
		if (protocol == null) {
			logger.error(
				"Error retrieving search protocol: protocol name is null.");
			return null;
		} else if (user == null) {
			logger.error(
				"Error retrieving search protocol: username is null.");
			return null;
		}
		String path = WebAppProps.getPath("livesearch.protocols.path");
		if (path == null) {
			logger.error("Error retrieving protocol \"" + protocol +
				"\" for user \"" + user + "\": protocol directory path " +
				"could not be found.");
			return null;
		}
		File file = new File(path, String.format("%s/%s.xml", user, protocol));
		if (file == null || file.canRead() == false) {
			logger.error("Error retrieving protocol \"" + protocol +
				"\" for user \"" + user + "\": protocol parameter file " +
				"is null or inaccessible.");
			
			if(system_protocol_user.compareTo(user) != 0){
				logger.info("Trying to see if its a system protocol instead");
				return getProtocol(protocol, system_protocol_user);
			}
			
			return null;
		} else return FileIOUtils.readFile(file);
	}
	
	public static final boolean saveProtocol(String protocol, String user,
		String xml) {
		if (protocol == null || user == null ||
			FileManager.syncUserSpace(user) == false || xml == null) {
			// TODO: report appropriate error
			return false;
		}
		String path = WebAppProps.getPath("livesearch.protocols.path");
		if (path == null) {
			logger.error("Error saving protocol \"" + protocol +
				"\" for user \"" + user + "\": protocol directory path " +
				"could not be found.");
			return false;
		}
		File root = new File(path, user);
		if (root.exists() &&
			(root.isDirectory() == false || root.canRead() == false)) {
			// TODO: report error
			return false;
		} else if (root.exists() == false && root.mkdir() == false) {
			// TODO: report error
			return false;
		}
		File file = new File(root, String.format("%s.xml", protocol));
		return writeParameterFile(xml, file);
	}
	
	public static final boolean removeProtocol(String protocol, String user) {
			if (protocol == null || user == null ||
				FileManager.syncUserSpace(user) == false) {
				// TODO: report appropriate error
				return false;
			}
			String path = WebAppProps.getPath("livesearch.protocols.path");
			if (path == null) {
				logger.error("Error removing protocol \"" + protocol +
					"\" for user \"" + user + "\": protocol directory path " +
					"could not be found.");
				return false;
			}
			File root = new File(path, user);
			if (root.exists() &&
				(root.isDirectory() == false || root.canRead() == false)) {
				// TODO: report error
				return false;
			} else if (root.exists() == false && root.mkdir() == false) {
				// TODO: report error
				return false;
			}
			File file = new File(root, String.format("%s.xml", protocol));
			
			//Removing file here
			getLogger().info("Removing protocol : " + file.toString());
			
			return file.delete();
		}
	
	public static final boolean writeParameterFile(
		Map<String, Collection<String>> parameters, File file
	) {
		if (parameters == null || file == null)
			return false;
		// build parameters XML document
		Document paramDocument =
			FileIOUtils.parseXML("<?xml version=\"1.0\" " +
				"encoding=\"ISO-8859-1\" ?>\n<parameters>\n</parameters>\n");
		// sort parameters alphabetically
		Collection<String> sortedParameters =
			new TreeSet<String>(parameters.keySet());
		// write sorted parameters to XML document
		for (String parameter : sortedParameters) {
			Collection<String> values = parameters.get(parameter);
			for (String value : values) {
				// create new parameter node
				Element paramNode = paramDocument.createElement("parameter");
				paramNode.setAttribute("name", parameter);
				paramNode.setTextContent(value);
				paramDocument.getDocumentElement().appendChild(paramNode);
			}
		}
		// write document to file
		String xml = FileIOUtils.printXML(paramDocument);
		return writeParameterFile(xml, file);
	}
	
	public static final boolean writeParameterFile(String xml, File file) {
		if (xml == null || file == null)
			return false;
		// write document to file
		PrintWriter writer = null;
		try {
			writer = new PrintWriter(file);
			writer.print(xml);
			return true;
		} catch (Exception error) {
			getLogger().error("Cannot create parameter file", error);
			return false;
		} finally {
			if (writer != null)
				writer.close();
		}
	}
	
	/*========================================================================
	 * Convenience classes
	 *========================================================================*/
	private static class ProtocolFilter
	implements FilenameFilter
	{
		public boolean accept(File directory, String name) {
			if (name == null)
				return false;
			else if (name.matches("(\\w|\\s)+\\.xml"))
				return true;
			else return false;
		}
	}
}
