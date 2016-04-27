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

public class DatasetLicenseProcessor
implements ParameterProcessor
{
	/*========================================================================
	 * Constants
	 *========================================================================*/
	private static final Logger logger =
		LoggerFactory.getLogger(DatasetLicenseProcessor.class);
	private static final String DEFAULT_LICENSE = "CC0-1.0";
	
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	public List<String> processParameters(TaskBuilder builder) {
		if (builder == null)
			return null;
		Task task = builder.getTask();
		if (task == null)
			return null;
		List<String> errors = new LinkedList<String>();
		
		// stage default license file, if specified
		File licenseFolder = task.getPath("license/");
		String defaultLicense =
			builder.getFirstParameterValue("default.license");
		if ("on".equals(defaultLicense)) {
			String licensePath = String.format(
				"licenses/%s/%s_license.txt", DEFAULT_LICENSE, DEFAULT_LICENSE);
			File resource = new File(Commons.RESOURCE_PATH, licensePath);
			// TODO: implement input specification exception to handle this
			if (resource == null || resource.canRead() == false)
				throw new NullPointerException(String.format(
					"Could not retrieve default license file, since system " +
					"resource file \"%s\" could not be found.", licensePath));
			try {
				SaintFileUtils.makeLink(resource, licenseFolder);
			} catch(Exception error) {
				errors.add("Failed to generate link to default license file.");
				logger.error("Error generating link to default license file.",
					error);
			}
		}
		
		// ensure that at least one license file was selected
		if (StringUtils.isEmpty(
				builder.getFirstParameterValue("license_files")) &&
			StringUtils.isEmpty(
				builder.getFirstParameterValue("license_on_server")) &&
			"on".equals(defaultLicense) == false)
			errors.add("You must select a license file for this dataset.");
		
		if (errors.size() < 1)
			return null;
		else return errors;
	}
}
