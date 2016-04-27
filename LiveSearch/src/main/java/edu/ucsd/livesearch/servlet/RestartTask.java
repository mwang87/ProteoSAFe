package edu.ucsd.livesearch.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.ucsd.livesearch.task.NullTask;
import edu.ucsd.livesearch.task.Task;
import edu.ucsd.livesearch.task.TaskManager;
import edu.ucsd.livesearch.task.WorkflowUtils;
import edu.ucsd.livesearch.task.TaskManager.TaskStatus;

@SuppressWarnings("serial")
public class RestartTask
extends BaseServlet
{
	@Override
	protected void doGet(
		HttpServletRequest request, HttpServletResponse response
	) throws ServletException, IOException {
		boolean	isAdmin = ServletUtils.isAdministrator(request.getSession());
		boolean isOwner =
			ServletUtils.sameIdentity(request.getSession(), getUser());
		if (isAdmin == false && isOwner == false)
			return;
		Task task = TaskManager.queryTask(request.getParameter("task"));
		if (task != null && task instanceof NullTask == false) {
			TaskStatus status = task.getStatus();
			if (TaskStatus.SUSPENDED.equals(status) ||
				TaskStatus.DONE.equals(status) ||
				TaskStatus.FAILED.equals(status)) try {
				TaskManager.setRunning(task);
				WorkflowUtils.launchWorkflow(task);
			} catch (Exception error) {
				error.printStackTrace();
			}
			// if the task is currently queued, re-queue it to
			// force an evaluation of its pending uploads
			else if (TaskStatus.UPLOADING.equals(status))
				WorkflowUtils.queueUploadingTask(task);
		}
		response.sendRedirect("status.jsp?task=" + task.getID());
	}  	
	
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}
}
