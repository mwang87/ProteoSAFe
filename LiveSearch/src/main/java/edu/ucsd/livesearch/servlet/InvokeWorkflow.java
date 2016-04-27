package edu.ucsd.livesearch.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsd.livesearch.account.AccountManager;
import edu.ucsd.livesearch.task.NullTask;
import edu.ucsd.livesearch.task.Task;
import edu.ucsd.livesearch.task.TaskFactory;
import edu.ucsd.livesearch.task.WorkflowUtils;
import edu.ucsd.livesearch.task.TaskManager.TaskStatus;
import edu.ucsd.saint.commons.http.HttpParameters;

@SuppressWarnings("serial")
public class InvokeWorkflow
extends BaseServlet
{
	/*========================================================================
	 * Constants
	 *========================================================================*/
	private static final Logger logger =
		LoggerFactory.getLogger(InvokeWorkflow.class);
	
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	@Override
	protected void doPost(
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
		PrintWriter out = response.getWriter();
		
		// build and launch task
		String workflow = parameters.getParameter("workflow");
		Task task = null;
		try {
			// verify that the user has permission
			// to launch the selected workflow
			String user = getUser();
			if (AccountManager.getInstance().isWorkflowAccessible(
				workflow, user) == false) {
				out.print(String.format(
					"User [%s] does not have permission to run workflow [%s].",
					user, workflow));
				return;
			}
			// create and initialize task
			task = TaskFactory.createTask(
				(String)request.getSession().getAttribute("livesearch.user"),
				parameters);
			// if task creation failed for any reason, report errors
			if (task == null) {
				out.print("Task creation failed due to an unknown error.");
				return;
			} else if (task instanceof NullTask) {
				out.print(task.getMessage());
				return;
			}
			// otherwise, launch task
			else if (TaskStatus.FAILED.equals(task.getStatus()) == false)
				WorkflowUtils.queueUploadingTask(task);
			// report success by printing launched task ID
			response.getWriter().print(task.getID());
		} catch (Throwable error) {
			logger.error(String.format("Error invoking workflow [%s]",
				workflow), error);
			try {
				out.print(error.getMessage());
			} catch (Throwable innerError) {}
		}
	}
}
