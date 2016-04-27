package edu.ucsd.livesearch.parameter.processors;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsd.livesearch.parameter.ParameterProcessor;
import edu.ucsd.livesearch.task.Task;
import edu.ucsd.livesearch.task.TaskBuilder;
import edu.ucsd.livesearch.util.Commons;
import edu.ucsd.saint.commons.SaintFileUtils;

public class SequenceProcessor
implements ParameterProcessor
{
	/*========================================================================
	 * Constants
	 *========================================================================*/
	private static final Logger logger =
		LoggerFactory.getLogger(SequenceProcessor.class);
	
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	/**
	 * Validates the sequence file-related parameters submitted by the user
	 * from the CCMS ProteoSAFe web application input form, and stages the
	 * associated files to the appropriate task directories.
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
		List<String> errors = new LinkedList<String>();
		
		// stage common contaminants, if specified
		File fastaFolder = task.getPath("fasta/");
		String contaminants = builder.getFirstParameterValue("db.contaminants");
		if ("on".equals(contaminants)) {
			File source =
				new File(Commons.SEQUENCE_PATH, "CommonContaminants.fasta");
			try {
				SaintFileUtils.makeLink(source, fastaFolder);
			} catch(Exception error) {
				errors.add("Failed to generate link to " +
					"CommonContaminants database file.");
				logger.error("Error generating link to " +
					"CommonContaminants database file.", error);
			}
		}
		
		// ensure that at least one sequence database was selected
		String seqOnServer = builder.getFirstParameterValue("seq_on_server");
		String db = builder.getFirstParameterValue("db.DB");
		boolean dbPresent = (db != null && db.equals("None") == false);
		if (dbPresent == false && StringUtils.isEmpty(seqOnServer))
			errors.add("Sequence file/database unspecified.");
		
		if (errors.size() < 1)
			return null;
		else return errors;
	}
}
