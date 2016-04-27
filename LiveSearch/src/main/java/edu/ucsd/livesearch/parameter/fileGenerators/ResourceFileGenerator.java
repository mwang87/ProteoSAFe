package edu.ucsd.livesearch.parameter.fileGenerators;

import java.io.File;
import java.io.FileReader;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsd.livesearch.parameter.ParameterProcessor;
import edu.ucsd.livesearch.task.Task;
import edu.ucsd.livesearch.task.TaskBuilder;
import edu.ucsd.livesearch.util.Commons;
import edu.ucsd.saint.commons.SaintFileUtils;

public class ResourceFileGenerator
implements ParameterProcessor
{
	/*========================================================================
	 * Constants
	 *========================================================================*/
	private static final Logger logger =
		LoggerFactory.getLogger(ResourceFileGenerator.class);
	
	/*========================================================================
	 * Properties
	 *========================================================================*/
	private String parameter;
	private String resource;
	private String target;
	
	/*========================================================================
	 * Constructors
	 *========================================================================*/
	public ResourceFileGenerator() {
		setParameter(null);
		setResource(null);
	}
	
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	/**
	 * Retrieves the appropriate resource file workflow parameter submitted
	 * by the user from the CCMS ProteoSAFe web application input form, and
	 * fetches the resource files associated with that parameter, storing
	 * them appropriately in the task's directory structure.
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
		Map<String, Collection<String>> parameters = builder.getParameters();
		if (parameters == null)
			return null;
		Task task = builder.getTask();
		if (task == null)
			return null;
		List<String> errors = new Vector<String>(1);
		// a parameter must be specified to fetch the correct resource
		// TODO: implement input specification exception to handle this
		String parameter = getParameter();
		if (parameter == null)
			throw new NullPointerException("Illegal input specification: " +
				"a resource file generator element must be a child of a " +
				"valid parameter element.");
		// check to see if the specified parameter is present
		Collection<String> values = parameters.get(parameter);
		// TODO: implement input specification exception to handle this
		if (values == null || values.isEmpty())
			throw new NullPointerException("Illegal input specification: " +
				"parameter \"" + parameter + "\" must be present in the " +
				"user's submitted input form to properly fetch the " +
				"specified resource.  Please ensure that this is a " +
				"required parameter.");
		// a resource type must be specified to fetch the correct resource
		// TODO: implement input specification exception to handle this
		String resource = getResource();
		if (resource == null)
			throw new NullPointerException("Illegal input specification: " +
				"resource type is a required attribute for all " +
				"resource file generator elements.");
		// fetch the specified resources
		// TODO: implement input specification exception to handle this
		File resources = new File(Commons.RESOURCE_PATH, resource);
		if (resources == null || resources.canRead() == false ||
			resources.isDirectory() == false)
			throw new NullPointerException("Illegal input specification: " +
				"resource type \"" + resource + "\" does not correspond " +
				"to a valid resource folder that is present and accessible " +
				"on the server.");
		// set up target folder
		File targetFolder = task.getPath("");
		String target = getTarget();
		if (target != null)
			targetFolder = new File(targetFolder, target);
		targetFolder.mkdirs();
		// fetch all specified resources
		for (String value : values) {
			// handle the special case of value "None"
			if (value.equalsIgnoreCase("None"))
				continue;
			// first try to find a resource having
			// a mapping with the specified value
			File resourceFolder = null;
			for (File folder : resources.listFiles()) {
				if (folder == null || folder.canRead() == false ||
					folder.isDirectory() == false)
					continue;
				// retrieve and read the properties file for this resource
				Properties properties = new Properties();
				try {
					properties.load(new FileReader(
						new File(folder, "resource.properties")));
				} catch (Throwable error) {
					continue;
				}
				// if this resource is meant to be hidden, skip it
				String hidden = properties.getProperty("resource.hidden");
				if (hidden != null && hidden.trim().equalsIgnoreCase("true"))
					continue;
				// if the mapped resource name is equal to the query value,
				// then the correct resource folder has been found
				if (value.equals(properties.getProperty("resource.name"))){
					resourceFolder = folder;
					break;
				}
			}
			// if no mapping could be found with the correct resource name,
			// then just assume that the name is equal to the directory name
			if (resourceFolder == null)
				resourceFolder = new File(resources, value);
			// ensure that the selected resource folder is actually readable
			if (resourceFolder == null || resourceFolder.canRead() == false ||
				resourceFolder.isDirectory() == false) {
				errors.add("Could not find resource of type \"" + resource +
					"\" with name \"" + value + "\".");
				return errors;
			}
			// create symbolic links to all selected resource files
			for (File file : resourceFolder.listFiles()) try {
				// do not copy the resource properties file
				if (file.getName().startsWith("resource.properties"))
					continue;
				SaintFileUtils.makeLink(file, targetFolder);
			} catch(Exception error) {
				logger.error(
					"Error creating soft links to resource files", error);
				errors.add("Could not create links to resource files of type " +
					"\"" + resource + "\" with name \"" + value + "\".");
				return errors;
			}
		}
		return null;
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
	
	public final String getResource() {
		return resource;
	}
	
	public final void setResource(String resource) {
		this.resource = resource;
	}
	
	public final String getTarget() {
		return target;
	}
	
	public final void setTarget(String target) {
		this.target = target;
	}
}
