package edu.ucsd.livesearch.notification;

/**
 * Interface for generically generating workflow notification emails.
 * 
 * @author Jeremy Carver
 */
public interface WorkflowNotification
{
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	/**
	 * Gets the subject line for this notification email.
	 * 
	 * @return	the subject line for this notification email
	 */
	public String getSubject();
	
	/**
	 * Gets the body content for this notification email.
	 * 
	 * @return	the body content for this notification email
	 */
	public String getContent();
}
