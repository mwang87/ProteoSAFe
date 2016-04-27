package edu.ucsd.livesearch.parameter.processors;

import java.util.List;

import edu.ucsd.livesearch.parameter.ParameterProcessor;
import edu.ucsd.livesearch.parameter.validators.IntegerValidator;
import edu.ucsd.livesearch.task.TaskBuilder;

public class PepNovoProcessor
implements ParameterProcessor
{
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	/**
	 * Processes PepNovo-related workflow parameters submitted by the user
	 * from the CCMS ProteoSAFe web application input form.
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
		// TODO: this functionality should be handled by a more generic
		// XML parameter specification mechanism
		String mode = builder.getFirstParameterValue("pepnovo.pepsolmode");
		if (mode != null && mode.equals("tags")) {
			IntegerValidator tagValidator = new IntegerValidator();
			tagValidator.setParameter("pepnovo.tag_length");
			tagValidator.setLabel("Tags with length");
			tagValidator.setMinimum("3");
			tagValidator.setMaximum("6");
			return tagValidator.processParameters(builder);
		} else {
			builder.removeParameter("pepnovo.tag_length");
			return null;
		}
	}
}
