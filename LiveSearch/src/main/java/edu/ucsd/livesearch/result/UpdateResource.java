package edu.ucsd.livesearch.result;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsd.livesearch.account.AccountManager;
import edu.ucsd.livesearch.parameter.ResourceManager;
import edu.ucsd.livesearch.result.DownloadResultFile.ResultType;
import edu.ucsd.livesearch.servlet.BaseServlet;
import edu.ucsd.livesearch.task.NullTask;
import edu.ucsd.livesearch.task.Task;
import edu.ucsd.livesearch.task.TaskManager;
import edu.ucsd.livesearch.task.TaskManager.TaskStatus;
import edu.ucsd.livesearch.util.WorkflowParameterUtils;

@SuppressWarnings("serial")
public class UpdateResource
extends BaseServlet
{
	/*========================================================================
	 * Constants
	*========================================================================*/
	@SuppressWarnings("unused")
	private static final Logger logger =
		LoggerFactory.getLogger(UpdateResource.class);
	
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	/**
	 * <p>Accepts and processes requests to update system resource files.
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
	@SuppressWarnings("unchecked")
	@Override
	protected void doPut(
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
				"You must specify valid parameters to update a system " +
				"resource with one of a task's result files.");
			return;
		}
		
		// get the indicated task
		Task task = null;
		try {
			task = TaskManager.queryTask(parameters.get("task"));
			if (task == null)
				throw new NullPointerException();
			else if (task instanceof NullTask ||
				task.getStatus().equals(TaskStatus.NONEXIST))
				throw new IllegalArgumentException();
		} catch (Throwable error) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST,
				"You must specify a valid task ID to update a system " +
				"resource with one of its result files.");
			return;
		}
		
		// ensure that this user is authenticated to update system resources
		if (canUpdateResources(task.getFlowName()) == false) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN,
				"You are not authenticated to update the specified resource.");
			return;
		}
		
		// get this file's data source
		File file = null;
		ResultType type = ResultType.INVOKE;
		String source = parameters.get(type.toString());
		if (source != null)
			parameters.remove(type.toString());
		else {
			type = ResultType.FILE;
			source = parameters.get(type.toString());
			if (source == null) {
				type = ResultType.FOLDER;
				source = parameters.get(type.toString());
				if (source == null) {
					response.sendError(HttpServletResponse.SC_BAD_REQUEST,
						"You must specify either a \"file\", \"folder\" or " +
						"\"invoke\" parameter to update a system resource " +
						"with one of a task's result files.");
					return;
				}
			}
		}
		// fetch file appropriately based on data source type
		try {
			if (type.equals(ResultType.INVOKE))
				file = DownloadResultFile.invokePlugin(
					source, parameters, task);
			else file = DownloadResultFile.fetchStaticFile(type, source, task);
		} catch (FileNotFoundException error) {
			response.sendError(HttpServletResponse.SC_GONE, error.getMessage());
			return;
		}
		if (file == null || file.canRead() == false) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
				"Error updating a system resource with a task result file: " +
				"a valid file could not be found.");
			return;
		}
		
		// get the resource type to be updated
		String resourceType = parameters.get("resource");
		if (resourceType == null) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST,
				"You must specify a valid resource type to update a " +
				"system resource with one of a task's result files.");
			return;
		}
		
		// get the resource name to be updated
		String resource = WorkflowParameterUtils.getParameter(
			WorkflowParameterUtils.extractParameters(task),
			parameters.get("parameter"));
		if (resource == null) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST,
				"You must specify a valid resource parameter to update a " +
				"system resource with one of a task's result files.");
			return;
		}
		
		// update resource
		boolean updated = ResourceManager.updateResource(
			file, resourceType, resource.toLowerCase());
		if (updated == false)
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
				"Error updating system resource \"" + resourceType + "/" +
				resource + "\" with task result file \"" + task.getID() + "/" +
				source + "\".");
	}
	
	/*========================================================================
	 * Convenience methods
	 *========================================================================*/
	private boolean canUpdateResources(String workflow) {
		if (isAdministrator())
			return true;
		else try {
			if (AccountManager.getInstance().isWorkflowAccessible(
				workflow, getUser()))
				return true;
		} catch (Throwable error) {
			return false;
		}
		return false;
	}
}
