package edu.ucsd.livesearch.notification;

import edu.ucsd.livesearch.task.Task;
import edu.ucsd.saint.commons.WebAppProps;

public class GenericTaskCompletion
implements WorkflowNotification
{
	/*========================================================================
	 * Properties
	 *========================================================================*/
	private Task task;
	private String status;
	
	/*========================================================================
	 * Constructor
	 *========================================================================*/
	public GenericTaskCompletion(Task task, String status) {
		if (task == null)
			throw new NullPointerException("Task cannot be null.");
		else if (status == null)
			throw new NullPointerException("Task status cannot be null.");
		this.task = task;
		this.status = status;
	}
	
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	/**
	 * Gets the subject line for this notification email.
	 * 
	 * @return	the subject line for this notification email
	 */
	public String getSubject() {
		return String.format(
			"[ProteoSAFe] Job '%s' was %S", task.getID(), status);
	}
	
	/**
	 * Gets the body content for this notification email.
	 * 
	 * @return	the body content for this notification email
	 */
	public String getContent() {
		return String.format("The job you submitted was finished. " +
			"Follow the link for more information: %s/status.jsp?task=%s",
			WebAppProps.get("livesearch.site.url"), task.getID());
	}
}
