package edu.ucsd.livesearch.parameter.fileGenerators;

import java.io.File;
import java.util.List;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsd.livesearch.parameter.ParameterProcessor;
import edu.ucsd.livesearch.storage.ResourceAgent;
import edu.ucsd.livesearch.storage.ResourceManager;
import edu.ucsd.livesearch.task.Task;
import edu.ucsd.livesearch.task.TaskBuilder;

public class UploadFileGenerator
implements ParameterProcessor
{
	/*========================================================================
	 * Constants
	 *========================================================================*/
	private static final Logger logger =
		LoggerFactory.getLogger(UploadFileGenerator.class);
	
	/*========================================================================
	 * Properties
	 *========================================================================*/
	private String parameter;
	private String target;
	private String purpose;
	
	/*========================================================================
	 * Constructors
	 *========================================================================*/
	public UploadFileGenerator() {
		setParameter(null);
		setTarget(null);
		setPurpose(null);
	}
	
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	/**
	 * Retrieves the appropriate user upload file workflow parameter submitted
	 * by the user from the CCMS ProteoSAFe web application input form, and
	 * fetches the uploaded files associated with that parameter, storing
	 * them appropriately in the task's directory structure.
	 * 
	 * @param builder	an {@link TaskBuilder} object representing the building
	 * 					state of the task whose parameters are to be processed
	 * 
	 * @return			the {@link List} of error messages encountered
	 * 					during processing,
	 * 					null if processing completed successfully
	 */
	@Override
	public final List<String> processParameters(TaskBuilder builder) {
		if (builder == null)
			return null;
		Task task = builder.getTask();
		if (task == null)
			return null;
		List<String> errors = new Vector<String>(1);
		// a parameter must be specified to fetch the correct uploads
		// TODO: implement input specification exception to handle this
		String parameter = getParameter();
		if (parameter == null)
			throw new NullPointerException("Illegal input specification: " +
				"an upload file generator element must be a child of a " +
				"valid parameter element.");
		// check to see if the specified parameter is present
		String value = builder.getFirstParameterValue(parameter);
		if (value == null || value.trim().isEmpty())
			return null;
		// clean file value
		value = cleanFileParameter(value);
		// a target folder name must be specified to copy the
		// uploaded files to the correct task subfolder
		// TODO: implement input specification exception to handle this
		String target = getTarget();
		if (target == null)
			throw new NullPointerException("Illegal input specification: " +
				"target folder is a required attribute for all " +
				"upload file generator elements.");
		// ensure that target folder exists and is ready to accept the files
		File targetFolder = new File(task.getPath(""), target);
		targetFolder.mkdirs();
		// if upload purpose is specified, use that for the upload's
		// database record, otherwise use the default purpose of "database"
		String purpose = getPurpose();
		if (purpose == null)
			purpose = "database";
		// copy user-uploaded files to the specified target task folder
		ResourceAgent agent = ResourceManager.assignAgent(task);
		List<String> uploads = null;
		try {
			uploads = agent.aquireOnServer(value, purpose, targetFolder);
		} catch (Throwable error) {
			String message = String.format(
				"There was a problem assigning your input files of type [%s] " +
				"(purpose = [%s]) to this workflow task.", parameter, purpose);
			errors.add(message);
			logger.error(message, error);
		}
		// ensure that at least one user upload is present
		if (uploads == null || uploads.isEmpty())
			errors.add(String.format("A required user upload of type [%s] " +
				"(purpose = [%s]) is not present.", parameter, purpose));
		// add file mappings to parameters
		else for (String upload : uploads) {
			String name = task.queryOriginalName(upload);
			if (name != null)
				builder.addParameterValue(
					"upload_file_mapping", upload + "|" + name);
		}
		if (errors.size() < 1)
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
	public final String getParameter() {
		return parameter;
	}
	
	/**
	 * Sets the input form name of the parameter that is to be processed
	 * by this processor.
	 * 
	 * @param parameter	the name of the parameter to be processed,
	 * 					as specified in the workflow input form
	 */
	public final void setParameter(String parameter) {
		this.parameter = parameter;
	}
	
	public final String getTarget() {
		return target;
	}
	
	public final void setTarget(String target) {
		this.target = target;
	}
	
	public final String getPurpose() {
		return purpose;
	}
	
	public final void setPurpose(String purpose) {
		this.purpose = purpose;
	}
	
	/*========================================================================
	 * Convenience methods
	 *========================================================================*/
	private String cleanFileParameter(String value) {
		if (value == null)
			return null;
		StringBuffer cleaned = new StringBuffer();
		for (String token : value.split(";")) {
			// convert all directory descriptors to plain files,
			// since ResourceAgent handles those the same
			if (token.startsWith("d.")) {
				cleaned.append("f.");
				cleaned.append(token.substring(2));
			} else cleaned.append(token);
			cleaned.append(";");
		}
		return cleaned.toString();
	}
}
