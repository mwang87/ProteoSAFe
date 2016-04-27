package edu.ucsd.livesearch.parameter.processors;

import java.util.List;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsd.livesearch.parameter.ParameterProcessor;
import edu.ucsd.livesearch.task.Task;
import edu.ucsd.livesearch.task.TaskBuilder;

public class SpectralArchiveProcessor
implements ParameterProcessor
{
	/*========================================================================
	 * Constants
	 *========================================================================*/
	@SuppressWarnings("unused")
	private static final Logger logger =
		LoggerFactory.getLogger(SpectralArchiveProcessor.class);
	
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	/**
	 * Stages the proper spectral archive file to the appropriate task
	 * directory.
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
		List<String> errors = new Vector<String>();
		
		// get user-selected archive name
		String archive = builder.getFirstParameterValue("spec_archive.archive");
		if (archive == null || archive.equals("None"))
			errors.add("Archive unspecified.");
		
		if (errors.size() < 1)
			return null;
		else return errors;
	}
}
