package edu.ucsd.livesearch.parameter.validators;

import java.util.List;
import java.util.Vector;

import edu.ucsd.livesearch.parameter.ParameterProcessor;
import edu.ucsd.livesearch.task.TaskBuilder;

public abstract class ParameterValidator
implements ParameterProcessor
{
	/*========================================================================
	 * Properties
	 *========================================================================*/
	private TaskBuilder builder;
	private String      parameter;
	private String      label;
	private String      message;
	
	/*========================================================================
	 * Constructors
	 *========================================================================*/
	public ParameterValidator() {
		setParameter(null);
		setLabel(null);
		setMessage(null);
	}
	
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	/**
	 * Retrieves the appropriate workflow parameters submitted by the user
	 * from the CCMS ProteoSAFe web application input form, and runs the
	 * validation defined by this class on them.
	 * 
	 * @param builder	an {@link TaskBuilder} object representing the building
	 * 					state of the task whose parameters are to be processed
	 * 
	 * @return			the {@link List} of error messages encountered
	 * 					during processing,
	 * 					null if processing completed successfully
	 */
	public List<String> processParameters(TaskBuilder builder) {
		if (builder == null)
			return null;
		this.builder = builder;
		String result = validateParameter(
			builder.getFirstParameterValue(getParameter()));
		if (result != null) {
			List<String> error = new Vector<String>(1);
			error.add(result);
			return error;
		} else return null;
	}
	
	/**
	 * Validates a single workflow parameter submitted by the user
	 * from the CCMS ProteoSAFe web application input form.
	 * 
	 * @param value	the value of the parameter to be validated
	 * 
	 * @return		the error message to be reported if the validation fails,
	 * 				null if the validation succeeded
	 */
	public abstract String validateParameter(String value);
	
	/*========================================================================
	 * Property accessor methods
	 *========================================================================*/
	/**
	 * Gets the TaskBuilder associated with the task whose parameter is to be
	 * validated by this validator.
	 * 
	 * @return	the TaskBuilder associated with this workflow task
	 */
	public TaskBuilder getTaskBuilder() {
		return builder;
	}
	
	/**
	 * Gets the input form name of the parameter that is to be validated
	 * by this validator.
	 * 
	 * @return	the name of the parameter to be validated,
	 * 			as specified in the workflow input form
	 */
	public String getParameter() {
		return parameter;
	}
	
	/**
	 * Sets the input form name of the parameter that is to be validated
	 * by this validator.
	 * 
	 * @param label	the name of the parameter to be validated,
	 * 				as specified in the workflow input form
	 */
	public void setParameter(String parameter) {
		this.parameter = parameter;
	}
	
	/**
	 * Gets the descriptive label for the parameter that is to be validated
	 * by this validator.
	 * 
	 * @return	the name of the parameter to be validated, formatted in
	 * 			the manner in which it should be presented to the user
	 */
	public String getLabel() {
		if (label == null)
			return parameter;
		else return label;
	}
	
	/**
	 * Sets the descriptive label for the parameter that is to be validated
	 * by this validator.
	 * 
	 * @param label	the name of the parameter to be validated, formatted in
	 * 				the manner in which it should be presented to the user
	 */
	public void setLabel(String label) {
		this.label = label;
	}
	
	/**
	 * Gets the message that will be presented to the user if this
	 * validation fails.
	 * 
	 * @return	the message that should be presented to the user
	 * 			if this validation fails
	 */
	public String getMessage() {
		return message;
	}
	
	/**
	 * Sets the message that will be presented to the user if this
	 * validation fails.  This message string should be parameterized
	 * according to the values expected in the validator implementation,
	 * as per the Java {@link Formatter} specification.
	 * 
	 * @param message	the message that should be presented to the user
	 * 					if this validation fails
	 */
	public void setMessage(String message) {
		if (message == null)
			this.message = "";
		else this.message = message;
	}
}
