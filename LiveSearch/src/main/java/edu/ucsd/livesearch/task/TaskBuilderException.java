package edu.ucsd.livesearch.task;

@SuppressWarnings("serial")
public class TaskBuilderException
extends Exception
{
	/*========================================================================
	 * Constructors
	 *========================================================================*/
	public TaskBuilderException() {
		super();
	}
	
	public TaskBuilderException(String message) {
		super(message);
	}
	
	public TaskBuilderException(Throwable error) {
		super(error);
	}
	
	public TaskBuilderException(String message, Throwable error) {
		super(message, error);
	}
}
