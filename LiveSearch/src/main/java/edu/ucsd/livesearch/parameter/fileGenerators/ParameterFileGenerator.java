package edu.ucsd.livesearch.parameter.fileGenerators;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import edu.ucsd.livesearch.parameter.ParameterProcessor;
import edu.ucsd.livesearch.servlet.ManageParameters;
import edu.ucsd.livesearch.task.Task;
import edu.ucsd.livesearch.task.TaskBuilder;

public class ParameterFileGenerator
implements ParameterProcessor
{
	
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	/**
	 * Generates the final parameter file for the task currently being built.
	 * This processor should be invoked near the end of the processor chain,
	 * after all other parameters have been fully processed.
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
		Map<String, Collection<String>> parameters = builder.getParameters();
		if (parameters == null)
			return null;
		Task task = builder.getTask();
		if (task == null)
			return null;
		// write parameter file
		if (ManageParameters.writeParameterFile(parameters,
			task.getPath("params/params.xml")) == false) {
			List<String> errors = new Vector<String>(1);
			errors.add("Failed to write parameter file.");
			return errors;
		} else return null;
	}
}
