package edu.ucsd.livesearch.parameter.processors;

import java.util.List;

import edu.ucsd.livesearch.parameter.ParameterProcessor;
import edu.ucsd.livesearch.task.TaskBuilder;

public class BooleanProcessor
implements ParameterProcessor
{
	/*========================================================================
	 * Properties
	 *========================================================================*/
	private String parameter;
	
	/*========================================================================
	 * Constructors
	 *========================================================================*/
	public BooleanProcessor() {
		setParameter(null);
	}
	
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	/**
	 * Retrieves the appropriate boolean workflow parameter submitted by the
	 * user from the CCMS ProteoSAFe web application input form, and ensures
	 * that some value is present for this parameter, even if it was not
	 * selected by the user.
	 * 
	 * @param builder	an {@link TaskBuilder} object representing the building
	 * 					state of the task whose parameters are to be processed
	 * 
	 * @return			the {@link List} of error messages encountered
	 * 					during processing,
	 * 					null if processing completed successfully
	 */
	public final List<String> processParameters(TaskBuilder builder) {
		if (builder == null)
			return null;
		String parameter = getParameter();
		if (parameter == null)
			return null;
		// check to see if the specified parameter is present
		String value = builder.getFirstParameterValue(parameter);
		// if the parameter has not been specified,
		// assign a negative value to it
		if (value == null)
			builder.setParameterValue(parameter, "off");
		return null;
	}
	
	/*========================================================================
	 * Property accessor methods
	 *========================================================================*/
	/**
	 * Gets the input form name of the parameter that is to be processed
	 * by this processor.
	 * 
	 * @return	the name of the parameter to be processed,
	 * 			as specified in the workflow input form
	 */
	public String getParameter() {
		return parameter;
	}
	
	/**
	 * Sets the input form name of the parameter that is to be processed
	 * by this processor.
	 * 
	 * @param label	the name of the parameter to be processed,
	 * 				as specified in the workflow input form
	 */
	public void setParameter(String parameter) {
		this.parameter = parameter;
	}
}
