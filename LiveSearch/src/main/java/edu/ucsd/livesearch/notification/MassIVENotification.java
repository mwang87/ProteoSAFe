package edu.ucsd.livesearch.notification;

import edu.ucsd.livesearch.dataset.Dataset;
import edu.ucsd.livesearch.dataset.DatasetManager;
import edu.ucsd.livesearch.task.Task;
import edu.ucsd.livesearch.util.WorkflowParameterUtils;
import edu.ucsd.saint.commons.WebAppProps;

public class MassIVENotification
implements WorkflowNotification
{
	/*========================================================================
	 * Properties
	 *========================================================================*/
	private Task task;
	private Dataset dataset;
	private String status;
	
	/*========================================================================
	 * Constructor
	 *========================================================================*/
	public MassIVENotification(Task task, String status) {
		if (task == null)
			throw new NullPointerException("Task cannot be null.");
		else if (status == null)
			throw new NullPointerException("Task status cannot be null.");
		this.task = task;
		this.status = status;
		// get dataset details from the database
		dataset = DatasetManager.queryDatasetByTaskID(this.task.getID());
		if (dataset == null)
			throw new IllegalArgumentException(String.format(
				"Task \"%s\" does not correspond to a valid MassIVE dataset.",
				this.task.getID()));
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
		return String.format("MassIVE Dataset Submission \"%s\" is %S",
			dataset.getDatasetIDString(), status);
	}
	
	/**
	 * Gets the body content for this notification email.
	 * 
	 * @return	the body content for this notification email
	 */
	public String getContent() {
		// display basic dataset information
		StringBuffer content =
			new StringBuffer("Your dataset was successfully submitted.\n\n");
		// MassIVE dataset ID
		content.append("MassIVE ID: ");
		content.append(dataset.getDatasetIDString());
		// task title
		String title = task.getDescription();
		if (title != null) {
			content.append("\nTitle: ");
			content.append(title);
		}
		// submission date
		content.append("\nDate Created: ");
		content.append(dataset.getCreatedString());
		// dataset description
		String description =
			WorkflowParameterUtils.getParameter(task, "dataset.comments");
		if (description != null) {
			content.append("\nDescription: ");
			content.append(description);
		}
		// file count and total size
		content.append("\nFiles Uploaded: ");
		content.append(dataset.getFileCountString());
		content.append(" (");
		content.append(dataset.getFileSizeString());
		content.append(")");
		// dataset password, if present
		String password =
			WorkflowParameterUtils.getParameter(task, "dataset.password");
		if (password != null) {
			content.append("\n\nDataset Password: ");
			content.append(password);
		}
		// display link to dataset status page
		content.append(String.format(
			"\n\nFollow this link for more information: %s/status.jsp?task=%s",
			WebAppProps.get("livesearch.site.url"), task.getID()));
		return content.toString();
	}
}
