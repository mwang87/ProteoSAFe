package edu.ucsd.livesearch.parameter.processors;

import java.util.List;
import java.util.Vector;

import edu.ucsd.livesearch.parameter.ParameterProcessor;
import edu.ucsd.livesearch.task.TaskBuilder;

public class MsAlignProcessor
implements ParameterProcessor
{
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	/**
	 * Processes MS-Align-related workflow parameters submitted by the user
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
		List<String> errors = new Vector<String>();
		// TODO: this functionality should be handled by a more generic
		// XML parameter specification mechanism
		// process minimum modification mass
		Integer minimum = null;
//		IntegerValidator minValidator = new IntegerValidator();
//		minValidator.setParameter("msalign.minmodmass");
//		minValidator.setLabel("Minimum modification mass");
//		minValidator.setMinimum("-200");
//		minValidator.setMaximum("200");
//		List<String> minErrors = minValidator.processParameters(builder);
//		if (minErrors != null && minErrors.size() > 0)
//			errors.addAll(minErrors);
//		else
		try {
			minimum = Integer.parseInt(
				builder.getFirstParameterValue("msalign.minmodmass"));
		} catch (Throwable error) {}
		// process minimum modification mass
		Integer maximum = null;
//		IntegerValidator maxValidator = new IntegerValidator();
//		maxValidator.setParameter("msalign.maxmodmass");
//		maxValidator.setLabel("Maximum modification mass");
//		maxValidator.setMinimum("-200");
//		maxValidator.setMaximum("200");
//		List<String> maxErrors = maxValidator.processParameters(builder);
//		if (maxErrors != null && maxErrors.size() > 0)
//			errors.addAll(maxErrors);
//		else
		try {
			maximum = Integer.parseInt(
				builder.getFirstParameterValue("msalign.maxmodmass"));
		} catch (Throwable error) {}
		// confirm that minimum is less than or equal to maximum
		if (minimum != null && maximum != null && (maximum - minimum) < 0)
			errors.add("\"Minimum modification mass\" must be less than " +
				"or equal to \"Maximum modification mass\".");
		if (errors.size() < 1)
			return null;
		else return errors;
	}
}
