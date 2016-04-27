package edu.ucsd.livesearch.notification;

import edu.ucsd.livesearch.dataset.Dataset;
import edu.ucsd.livesearch.dataset.DatasetManager;
import edu.ucsd.saint.commons.WebAppProps;

public class DatasetSubscriberNotification
implements WorkflowNotification
{
	/*========================================================================
	 * Properties
	 *========================================================================*/
	private Dataset dataset;
	private String operation;
	private String message;
	
	/*========================================================================
	 * Constructor
	 *========================================================================*/
	public DatasetSubscriberNotification(
		String datasetID, String operation, String message
	) {
		this(DatasetManager.queryDatasetByID(datasetID), operation, message);
	}
	
	public DatasetSubscriberNotification(
		Dataset dataset, String operation, String message
	) {
		if (dataset == null)
			throw new NullPointerException("Dataset cannot be null.");
		this.dataset = dataset;
		if (operation == null)
			this.operation = "has been updated";
		else this.operation = operation;
		if (message == null)
			this.message = "";
		else this.message = message;
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
			"Your subscribed MassIVE dataset [%s] %s",
			dataset.getDatasetIDString(), operation);
	}
	
	/**
	 * Gets the body content for this notification email.
	 * 
	 * @return	the body content for this notification email
	 */
	public String getContent() {
		return String.format("%s\n\nSee the dataset for more information: " +
			"%s/result.jsp?task=%s&view=advanced_view", message,
			WebAppProps.get("livesearch.site.url"), dataset.getTaskID());
	}
}
