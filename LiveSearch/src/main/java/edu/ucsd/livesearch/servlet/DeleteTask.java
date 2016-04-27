package edu.ucsd.livesearch.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsd.livesearch.dataset.Dataset;
import edu.ucsd.livesearch.dataset.DatasetManager;
import edu.ucsd.livesearch.task.NullTask;
import edu.ucsd.livesearch.task.Task;
import edu.ucsd.livesearch.task.TaskManager;

@SuppressWarnings("serial")
public class DeleteTask
extends BaseServlet
{
	/*========================================================================
	 * Constants
	 *========================================================================*/
	private Logger logger = LoggerFactory.getLogger(DeleteTask.class);
	
	/*========================================================================
	 * Servlet methods
	 *========================================================================*/
	/**
	 * <p>Accepts and processes queries to delete tasks.
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
		
		// retrieve and verify task
		Task task = TaskManager.queryTask(request.getParameter("task"));
		if (task instanceof NullTask)
			return;
		boolean isOwner =
			ServletUtils.sameIdentity(request.getSession(), task.getUser());
		boolean isAdmin = ServletUtils.isAdministrator(request.getSession());
		
		// determine if this task represents a MassIVE dataset
		Dataset dataset = DatasetManager.queryDatasetByTaskID(task.getID());
		boolean isDataset = false;
		boolean isPrivate = false;
		if (dataset != null) {
			isDataset = true;
			isPrivate = dataset.isPrivate();
		}
		
		// delete this task, if permissions allow
		if (isOwner || isAdmin) try {
			// only delete if this is not a dataset, or if it's private;
			// not even owners can delete a public dataset
			if (isDataset == false)
				TaskManager.deleteTask(task);
			else if (isPrivate || isAdmin) {
				TaskManager.deleteTask(task);
				DatasetManager.deleteDataset(dataset, getUser());
			}
		} catch (Exception error) {
			logger.error("Failed to delete task [" + task.getID() + "]", error);
		}
		
		// redirect back to status page
		response.sendRedirect("status.jsp?task=" + task.getID());
	}  	
	
	protected void doPost(
		HttpServletRequest request, HttpServletResponse response
	) throws ServletException, IOException {
		doGet(request, response);
	}
}
