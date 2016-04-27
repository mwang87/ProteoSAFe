package edu.ucsd.livesearch.result;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import edu.ucsd.livesearch.result.parsers.Result;
import edu.ucsd.livesearch.servlet.BaseServlet;
import edu.ucsd.livesearch.task.NullTask;
import edu.ucsd.livesearch.task.Task;
import edu.ucsd.livesearch.task.TaskManager;
import edu.ucsd.livesearch.task.TaskManager.TaskStatus;
import edu.ucsd.livesearch.util.FileIOUtils;

@SuppressWarnings("serial")
public class DownloadBlock
extends BaseServlet
{
	/*========================================================================
	 * Servlet methods
	 *========================================================================*/
	/**
	 * <p>Accepts and processes queries for result view block specifications
	 * and data.
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
	@SuppressWarnings("unchecked")
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
		PrintWriter out = response.getWriter();
		
		// extract the request parameters into a simple map
		Map<String, String> parameters = new HashMap<String, String>();
		try {
			Enumeration<String> parameterNames =
				(Enumeration<String>)request.getParameterNames();
			while (parameterNames.hasMoreElements()) {
				String parameter = parameterNames.nextElement();
				parameters.put(parameter, request.getParameter(parameter));
			}
		} catch (Throwable error) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST,
				"You must specify valid parameters to download a " +
				"specification for one of a task's result blocks.");
			return;
		}
		
		// get the indicated task
		String task = parameters.get("task");
		if (task == null) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST,
				"You must specify a valid task ID to download the " +
				"specification for one of its result blocks.");
			return;
		}
		
		// get the indicated block
		String block = parameters.get("block");
		if (block == null) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST,
				"You must specify a valid block ID " +
				"to download its specification.");
			return;
		}
		
		// retrieve and print the block's XML document
		String document = FileIOUtils.printXML(
			getBlockSpecification(task, block, parameters));
		if (document == null) {
			response.sendError(
				HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
				"There was an error retrieving your specified " +
				"result block specification file.");
		} else {
			response.setContentType("application/xml");
			response.setContentLength(document.length());
			out.println(document);
		}
	}
	
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	public static final Document getBlockSpecification(
		String taskID, String block, Map<String, String> parameters
	) {
		if (taskID == null || block == null)
			return null;
		// retrieve task
		Task task = TaskManager.queryTask(taskID);
		if (task == null || task instanceof NullTask ||
			task.getStatus().equals(TaskStatus.NONEXIST))
			return null;
		// retrieve XML block specification
		Element blockSpec =
			ResultViewXMLUtils.getBlockSpecification(task, block);
		if (blockSpec == null) {
			// TODO: report error
			return null;
		}
		// retrieve data for this block
		Element dataSpec = ResultViewXMLUtils.getDataSpecification(blockSpec);
		Result result =
			ResultFactory.createResult(dataSpec, task, block, parameters);
		if (result == null) {
			// TODO: report error
			return null;
		}
		// create a new XML document
		Document document = null;
		Element rootNode = null;
		try {
			document = DocumentBuilderFactory.newInstance()
				.newDocumentBuilder().newDocument();
			rootNode = document.createElement("blockInstance");
			document.appendChild(rootNode);
		} catch (Throwable error) {
			// TODO: report error
			return null;
		}
		// write block specification to the new document
		try {
			Node blockNode = document.importNode(blockSpec, true);
			rootNode.appendChild(blockNode);
		} catch (Throwable error) {
			// TODO: report error
			return null;
		}
		// write data to the new document
		try {
			Element dataNode = document.createElement("blockData");
			dataNode.setTextContent(result.getData());
			rootNode.appendChild(dataNode);
		} catch (Throwable error) {
			// TODO: report error
			return null;
		}
		return document;
	}
}
