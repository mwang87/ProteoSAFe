package edu.ucsd.livesearch.parameter;

import java.util.List;

import edu.ucsd.livesearch.task.TaskBuilder;

/**
 * Interface for generically processing workflow parameters submitted
 * from the CCMS ProteoSAFe web application input form.
 * 
 * @author Jeremy Carver
 */
public interface ParameterProcessor
{
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	/**
	 * Processes one or more of the workflow parameters submitted by the user
	 * from the CCMS ProteoSAFe web application input form.
	 * 
	 * @param builder	an {@link TaskBuilder} object representing the building
	 * 					state of the task whose parameters are to be processed
	 * 
	 * @return			the {@link List} of error messages encountered
	 * 					during processing,
	 * 					null if processing completed successfully
	 */
	public List<String> processParameters(TaskBuilder builder);
}
