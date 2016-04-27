package edu.ucsd.livesearch.parameter.processors;

import java.util.ArrayList;
import java.util.List;

import edu.ucsd.livesearch.parameter.ParameterProcessor;
import edu.ucsd.livesearch.task.TaskBuilder;

public class ConcatenateProcessor
implements ParameterProcessor
{
	/*========================================================================
	 * Properties
	 *========================================================================*/
	private String parameter;
	private String suffix;

	/*========================================================================
	 * Constructors
	 *========================================================================*/
	public ConcatenateProcessor() {
		setParameter(null);
		setSuffix(null);
	}
	
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	/**
	 * Retrieves the appropriate pair of workflow parameters submitted by the
	 * user from the CCMS ProteoSAFe web application input form, and
	 * concatenates the "suffix" parameter onto the end of the main parameter.
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
		List<String> errors = new ArrayList<String>();
		String parameter = getParameter();
		if (parameter == null)
			errors.add("Illegal input.xml specification: " +
				"a <processor> element of type \"concatenate\" must be " +
				"a child of a valid <parameter> element.");
		String suffix = getSuffix();
		if (suffix == null)
			errors.add("Illegal input.xml specification: " +
				"\"suffix\" is a required attribute for all " +
				"<processor> elements of type \"concatenate\".");
		// check to see if the specified parameters are present
		String value = builder.getFirstParameterValue(parameter);
		if (value == null)
			errors.add(String.format("Could not concatenate the values " +
				"of parameters \"%s\" and \"%s\": no value could be found " +
				"for prefix parameter \"%s\".", parameter, suffix, parameter));
		String suffixValue = builder.getFirstParameterValue(suffix);
		if (suffixValue == null)
			errors.add(String.format("Could not concatenate the values " +
				"of parameters \"%s\" and \"%s\": no value could be found " +
				"for suffix parameter \"%s\".", parameter, suffix, suffix));
		// if the parameters have been specified, concatenate them
		if (value != null && suffixValue != null) {
			value = value + suffixValue;
			// write the concatenated value to a new special parameter
			parameter = parameter + "_" + suffix;
			builder.setParameterValue(parameter, value);
		}
		if (errors.isEmpty())
			return null;
		else return errors;
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
	 * @param parameter	the name of the parameter to be processed,
	 * 					as specified in the workflow input form
	 */
	public void setParameter(String parameter) {
		this.parameter = parameter;
	}
	
	/**
	 * Gets the input form name of the parameter whose value is to be
	 * concatenated to the end of the main parameter by this processor.
	 * 
	 * @return	the name of the parameter to be concatenated,
	 * 			as specified in the workflow input form
	 */
	public String getSuffix() {
		return suffix;
	}
	
	/**
	 * Sets the input form name of the parameter whose value is to be
	 * concatenated to the end of the main parameter by this processor.
	 * 
	 *  @param parameter	the name of the parameter to be concatenated,
	 * 						as specified in the workflow input form
	 */
	public void setSuffix(String suffix) {
		this.suffix = suffix;
	}
}
