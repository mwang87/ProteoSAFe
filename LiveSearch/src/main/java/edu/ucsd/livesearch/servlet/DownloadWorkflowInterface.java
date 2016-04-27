package edu.ucsd.livesearch.servlet;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.xpath.XPathAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.ucsd.livesearch.account.AccountManager;
import edu.ucsd.livesearch.task.NullTask;
import edu.ucsd.livesearch.task.Task;
import edu.ucsd.livesearch.task.TaskManager;
import edu.ucsd.livesearch.util.FileIOUtils;
import edu.ucsd.livesearch.util.WorkflowParameterUtils;
import edu.ucsd.saint.commons.WebAppProps;
import edu.ucsd.saint.commons.http.HttpParameters;

@SuppressWarnings("serial")
public class DownloadWorkflowInterface
extends BaseServlet
{
	/*========================================================================
	 * Constants
	 *========================================================================*/
	private static final Logger logger =
		LoggerFactory.getLogger(DownloadWorkflowInterface.class);
	
	/*========================================================================
	 * Properties
	 *========================================================================*/
	private static Map<String, Document> interfaceCache =
		new HashMap<String, Document>();
	private static Map<String, Long> interfaceAccess =
		new HashMap<String, Long>();
	
	/*========================================================================
	 * Servlet methods
	 *========================================================================*/
	/**
	 * <p>Accepts and processes queries for workflow interface specification
	 * documents.
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
	protected void doGet(
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
		PrintWriter out = response.getWriter();
		
		// get the indicated workflow
		String workflow = parameters.getParameter("workflow");
		if (workflow == null) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST,
				"You must specify a valid workflow name " +
				"to download its specification files.");
			return;
		}
		
		// get the indicated specification type
		String type = parameters.getParameter("type");
		if (type == null) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST,
				"You must specify a valid workflow specification type " +
				"to download the appropriate file for this workflow.");
			return;
		}
		
		// get the indicated task, if present
		String task = parameters.getParameter("task");
		
		// retrieve indicated workflow interface specification document
		String document = null;
		try {
			document = FileIOUtils.printXML(
				getWorkflowSpecification(workflow, type, user, task));
		}
		// if the file was simply not found, report the proper error code
		catch (FileNotFoundException error) {
			response.sendError(
				HttpServletResponse.SC_NOT_FOUND, error.getMessage());
			return;
		}
		// otherwise, if the returned document is still null,
		// then some other error occurred
		if (document == null) {
			response.sendError(
				HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
				"There was an error retrieving your specified " +
				"workflow interface specification file.");
		} else {
			response.setContentType("application/xml");
			response.setContentLength(document.length());
			out.println(document);
		}
	}
	
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	public static final Document getWorkflowSpecification(
		String workflow, String type, String user, String taskID
	) throws FileNotFoundException {
		if (type == null)
			return null;
		
		// first search the task directory, if applicable
		if (taskID != null) {
			Task task = TaskManager.queryTask(taskID);
			if (task != null && task instanceof NullTask == false) {
				File taskSpecRoot = task.getPath("workflow/");
				if (taskSpecRoot != null && taskSpecRoot.isDirectory() &&
					taskSpecRoot.canRead()) try {
					Document spec =
						getSpecificationDocument(type, taskSpecRoot);
					if (spec != null)
						return spec;
				}
				// don't propagate this exception - it's normal for a task
				// to not have its own cached copy of result.xml!
				catch (FileNotFoundException error) {}
			}
		}
		
		// if a task was not specified, or a task-specific specification was
		// not found, retrieve the default specification of this type
		if (workflow == null)
			return null;
		
		// retrieve and verify global workflow specification directory
		File specRoot =
			new File(WebAppProps.getPath("livesearch.flow.spec.path"));
		if (specRoot == null || specRoot.isDirectory() == false ||
			specRoot.canRead() == false) {
			// TODO: report error
			return null;
		}
		
		// next search this user's personal workflow specs
		if (user != null) {
			File userSpecRoot =
				new File(specRoot, String.format("user/%s", user));
			if (userSpecRoot != null && userSpecRoot.isDirectory() &&
				userSpecRoot.canRead()) try {
				Document spec = getSpecificationDocument(
					type, new File(userSpecRoot, workflow.toLowerCase()));
				if (spec != null)
					return spec;
			}
			// don't propagate this exception - it's normal for a user
			// to not have his/her own cached copy of result.xml!
			catch (FileNotFoundException error) {}
		}
		
		// then search the global default workflows, but first verify that
		// the user has permission to access this global workflow -
		// unless it's result.xml, which any user can download.
		try {
			if (type.equalsIgnoreCase("result") ||
				AccountManager.getInstance().isWorkflowAccessible(
					workflow, user))
				return getSpecificationDocument(
					type, new File(specRoot, workflow.toLowerCase()));
			else throw new FileNotFoundException(String.format(
				"User \"%s\" does not have permission to access " +
				"workflow specification file \"%s/%s.xml\".", user,
				workflow.toLowerCase(), type));
		} catch (FileNotFoundException error) {
			throw error;
		} catch (Throwable error) {
			return null;
		}
	}
	
	public static final Map<String, String> getInstalledWorkflows(String user) {
		// retrieve and verify global workflow specification directory
		File specRoot =
			new File(WebAppProps.getPath("livesearch.flow.spec.path"));
		if (specRoot == null || specRoot.isDirectory() == false ||
			specRoot.canRead() == false)
			return null;
		
		// build workflow id -> label map
		Map<String, String> workflows =
			new TreeMap<String, String>(new WorkflowComparator());
		
		// first search for this user's personal workflow specs
		if (user != null) {
			File userSpecRoot =
				new File(specRoot, String.format("user/%s", user));
			if (userSpecRoot != null && userSpecRoot.isDirectory() &&
				userSpecRoot.canRead()) {
				for (File specFolder : userSpecRoot.listFiles()) {
					Document spec = null;
					try {
						spec = getSpecificationDocument("input", specFolder);
					} catch (FileNotFoundException error) {}
					if (spec != null) try {
						String workflow =
							spec.getElementsByTagName("workflow-id")
							.item(0).getTextContent().toUpperCase();
						String label =
							spec.getElementsByTagName("workflow-label")
							.item(0).getTextContent();
						workflows.put(workflow, label);
					} catch (Throwable error) {
						// TODO: report error
						continue;
					}
				}
			}
		}
		
		// then retrieve the global default workflows
		AccountManager manager = AccountManager.getInstance();
		// TODO: check for "tester" role as well, once that's added
		boolean fullAccess = false;
		if (user != null)
			fullAccess = manager.checkRole(user, "administrator");
		Map<String, Boolean> accessibility = null;
		if (fullAccess == false)
			accessibility = manager.getWorkflowAccessibility(user);
		for (File specFolder : specRoot.listFiles()) {
			// skip user directory
			if (specFolder.getName().equals("user"))
				continue;
			Document spec = null;
			try {
				spec = getSpecificationDocument("input", specFolder);
			} catch (FileNotFoundException error) {}
			if (spec != null) try {
				String workflow = spec.getElementsByTagName("workflow-id")
					.item(0).getTextContent().toUpperCase();
				// do not add this workflow to the workflows map if it's
				// already present from a user-defined specification
				if (workflows.get(workflow) != null)
					continue;
				// add this workflow to the workflows map if:
				// 1. the user has full access to all workflows due to role
				// 2. accessibility for this workflow is not restricted to
				//    any user (i.e. it's not in the accessibility map)
				// 3. accessibility is restricted, but this user is allowed
				else if (fullAccess || accessibility == null ||
					accessibility.containsKey(workflow) == false ||
					accessibility.get(workflow))
					workflows.put(workflow,
						spec.getElementsByTagName("workflow-label")
						.item(0).getTextContent());
			} catch (Throwable error) {
				// TODO: report error
				continue;
			}
		}
		if (workflows.size() < 1)
			return null;
		else return workflows;
	}
	
	public static final Collection<InputCollection> getCollections(
		String taskID, String user
	) {
		// retrieve indicated task
		Task task = TaskManager.queryTask(taskID);
		if (task == null)
			return null;
		// retrieve input specification for the indicated workflow
		Document input = null;
		try {
			input = getWorkflowSpecification(
				task.getFlowName(), "input", user, taskID);
		} catch (FileNotFoundException error) {}
		if (input == null)
			return null;
		// parse out all input collections - i.e. all <fileGenerator> elements 
		// having a "type" attribute of "input"
		NodeList collectionNodes = null;
		try {
			collectionNodes = XPathAPI.selectNodeList(input,
				"//fileGenerator[@type='upload']");
		} catch (Throwable error) {
			logger.error(String.format("Error retrieving input collections " +
				"for task \"%s\":", task.getID()), error);
			return null;
		}
		if (collectionNodes == null)
			return null;
		// for each collection found, add a collection record
		Collection<InputCollection> collections =
			new ArrayList<InputCollection>(collectionNodes.getLength());
		for (int i=0; i<collectionNodes.getLength(); i++) {
			Node collectionNode = collectionNodes.item(i);
			NamedNodeMap attributes = collectionNode.getAttributes();
			String purpose = attributes.getNamedItem("purpose").getNodeValue();
			String target = attributes.getNamedItem("target").getNodeValue();
			attributes = collectionNode.getParentNode().getAttributes();
			String name = attributes.getNamedItem("name").getNodeValue();
			String label = attributes.getNamedItem("label").getNodeValue();
			InputCollection collection =
				new InputCollection(name, purpose, label);
			// retrieve and record all of the files in this collection
			Collection<String> filenames =
				WorkflowParameterUtils.getUploadFilenames(
					WorkflowParameterUtils.extractParameters(task), target);
			if (filenames == null || filenames.isEmpty())
				continue;
			else for (String filename : filenames)
				if (filename != null && filename.trim().isEmpty() == false)
					collection.addFile(filename);
			// only add this collection if it is non-empty
			if (collection.hasFiles())
				collections.add(collection);
		}
		// return found collections
		if (collections.isEmpty())
			return null;
		else return collections;
	}
	
	/*========================================================================
	 * Convenience methods
	 *========================================================================*/
	private static Document getSpecificationDocument(
		String type, File directory
	) throws FileNotFoundException {
		if (type == null || directory == null)
			return null;
		File file = new File(directory, String.format("%s.xml", type));
		if (file.canRead() == false)
			throw new FileNotFoundException(String.format(
				"Could not find workflow specification \"%s.xml\" " +
				"in directory \"%s\".", type, directory.getAbsolutePath()));
		return FileIOUtils.parseXML(file);
	}
	
	@SuppressWarnings("unused")
	private static Document mergeSpecFiles(File source, File destination) {
		Document sourceXML = FileIOUtils.parseXML(source);
		Document destinationXML = FileIOUtils.parseXML(destination);
		return mergeSpecs(sourceXML, destinationXML);
	}
	
	private static Document mergeSpecs(Document source, Document destination) {
		if (destination == null)
			return null;
		else if (source == null)
			return destination;
		try {
			NodeList parameters = XPathAPI.selectNodeList(source,
				"/interface/parameters//parameter");
			for (int i=0; i<parameters.getLength(); i++)
				mergeParameter(parameters.item(i), destination);
		} catch (Throwable error) {
			logger.error("Error merging source and destination " +
				"interface specification documents:", error);
			return null;
		}
		// if the files were successfully merged, cache the result
		Node workflow = null;
		try {
			workflow = XPathAPI.selectSingleNode(destination, "//workflow");
		} catch (Throwable error) {
			logger.error("Error extracting workflow name from " +
				"interface specification document:", error);
		} finally {
			if (workflow != null) {
				String name = workflow.getTextContent();
				interfaceCache.put(name, destination);
				interfaceAccess.put(name, System.currentTimeMillis());
			}
		}
		return destination;
	}
	
	private static void mergeParameter(Node source, Document destination)
	throws Exception {
		if (source == null || destination == null)
			return;
		// get the source node's name
		String name =
			source.getAttributes().getNamedItem("name").getNodeValue();
		// get the source node's group, if present
		String group = null;
		Node sourceParent = source.getParentNode();
		if (sourceParent != null &&
			sourceParent.getNodeName().equals("parameterGroup"))
			group = sourceParent.getAttributes().getNamedItem("name")
				.getNodeValue();
		// get the parent node of this parameter in the destination document
		Node destinationParent = null;
		if (group != null) {
			destinationParent = XPathAPI.selectSingleNode(destination,
				String.format(
					"/interface/parameters/parameterGroup[@name='%s']", group));
		} else destinationParent =
			XPathAPI.selectSingleNode(destination, "/interface/parameters");
		// if the parent node could not be found, then this parameter is not
		// appropriate for the destination document, and should not be merged
		if (destinationParent == null)
			return;
		// try to find this parameter in the destination document
		Node parameter = XPathAPI.selectSingleNode(destinationParent,
			String.format("//parameter[@name='%s']", name));
		// if the destination document has already declared this parameter,
		// then this declaration overrides that of the source document,
		// and no merge should happen
		if (parameter != null)
			return;
		// merge the parameter from the source document into the destination
		parameter = destination.importNode(source, true);
		destinationParent.appendChild(parameter);
	}
	
	/*========================================================================
	 * Convenience classes
	 *========================================================================*/
	private static class WorkflowComparator
	implements Comparator<String>
	{
		/*====================================================================
		 * Public interface methods
		 *====================================================================*/
		public int compare(String workflow1, String workflow2) {
			if (workflow1 == null && workflow2 == null)
				return 0;
			else if (workflow1 == null)
				return -1;
			else if (workflow2 == null)
				return 1;
			else if (workflow1.equals(workflow2))
				return 0;
			else if (workflow1.equals("INSPECT"))
				return -1;
			else if (workflow2.equals("INSPECT"))
				return 1;
			else return workflow1.compareTo(workflow2);
		}
	}
	
	public static class InputCollection
	{
		/*====================================================================
		 * Properties
		 *====================================================================*/
		private String name;
		private String purpose;
		private String label;
		private Collection<String> files;
		
		/*====================================================================
		 * Constructors
		 *====================================================================*/
		public InputCollection(String name, String purpose, String label) {
			// validate collection name
			if (name == null)
				throw new NullPointerException(
					"Collection parameter name cannot be null.");
			else if (name.trim().isEmpty())
				throw new IllegalArgumentException("Collection parameter " +
					"name cannot be an empty string or whitespace.");
			this.name = name;
			// validate collection purpose
			if (purpose == null)
				throw new NullPointerException(
					"Collection purpose cannot be null.");
			else if (purpose.trim().isEmpty())
				throw new IllegalArgumentException("Collection purpose " +
					"cannot be an empty string or whitespace.");
			this.purpose = purpose;
			// validate collection label
			if (label == null)
				label = "";
			this.label = label;
			// initialize file collection
			files = new ArrayList<String>();
		}
		
		/*====================================================================
		 * Property accessor methods
		 *====================================================================*/
		public String getName() {
			return name;
		}
		
		public String getPurpose() {
			return purpose;
		}
		
		public String getLabel() {
			return label;
		}
		
		public Collection<String> getFiles() {
			return files;
		}
		
		public boolean hasFiles() {
			return (files != null && files.isEmpty() == false);
		}
		
		/*====================================================================
		 * Convenience methods
		 *====================================================================*/
		private void addFile(String file) {
			if (file != null)
				files.add(file);
		}
	}
}
