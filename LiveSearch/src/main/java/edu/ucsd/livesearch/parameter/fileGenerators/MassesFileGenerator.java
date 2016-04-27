package edu.ucsd.livesearch.parameter.fileGenerators;

import java.util.List;
import java.util.Vector;

import edu.ucsd.livesearch.parameter.GenerateMasses;
import edu.ucsd.livesearch.parameter.ParameterProcessor;
import edu.ucsd.livesearch.task.OnDemandLoader;
import edu.ucsd.livesearch.task.Task;
import edu.ucsd.livesearch.task.TaskBuilder;

public class MassesFileGenerator
implements ParameterProcessor
{
	
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	/**
	 * Generates the amino acid masses file for the task currently being built.
	 * This processor should be invoked near the end of the processor chain,
	 * after the final parameter file has been generated.
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
		Task task = builder.getTask();
		if (task == null)
			return null;
		// write amino acid masses file
		GenerateMasses massesLoader = new GenerateMasses(task);
		if (OnDemandLoader.load(massesLoader) == false) {
			List<String> errors = new Vector<String>(1);
			errors.add("Failed to write parameter file.");
			return errors;
		} else return null;
	}
}
