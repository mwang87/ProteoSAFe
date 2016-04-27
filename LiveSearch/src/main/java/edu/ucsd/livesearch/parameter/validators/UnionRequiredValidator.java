package edu.ucsd.livesearch.parameter.validators;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;

import edu.ucsd.livesearch.storage.FileManager;
import edu.ucsd.livesearch.storage.ResourceAgent;
import edu.ucsd.livesearch.task.TaskBuilder;

public class UnionRequiredValidator
extends ParameterValidator
{
	/*========================================================================
	 * Constants
	 *========================================================================*/
	private static final String UNION_REQUIRED =
		"Files must be assigned to at least one " +
		"of the following file categories: [%s].";
	
	/*========================================================================
	 * Properties
	 *========================================================================*/
	private Set<String> collections;
	private String user;
	
	/*========================================================================
	 * Constructors
	 *========================================================================*/
	public UnionRequiredValidator() {
		super();
		setMessage(UNION_REQUIRED);
		collections = new LinkedHashSet<String>();
	}
	
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	@Override
	public String validateParameter(String value) {
		// get sibling file collection parameters to check against
		TaskBuilder builder = getTaskBuilder();
		if (builder == null)
			return null;
		// get this task's user to validate file accessibility
		user = builder.getUser();
		// gather all relevant parameter values
		Collection<String> values =
			new ArrayList<String>(collections.size() + 1);
		if (value != null)
			values.add(value);
		for (String collection : collections) {
			String parameterValue =
				builder.getFirstParameterValue(collection);
			if (parameterValue != null)
				values.add(parameterValue);
		}
		// validate each string parameter value until something is found
		boolean allEmpty = true;
		for (String parameterValue : values) {
			if (validateParameterValue(parameterValue)) {
				allEmpty = false;
				break;
			}
		}
		if (allEmpty)
			return String.format(getMessage(), getParameterListString());
		else return null;
	}
	
	/*========================================================================
	 * Property accessor methods
	 *========================================================================*/
	public void setCollection(String value) {
		if (value != null)
			collections.add(value);
	}
	
	/*========================================================================
	 * Convenience methods
	 *========================================================================*/
	private boolean validateParameterValue(String value) {
		if (value == null)
			return false;
		// this validator can be used to compare standard file selector
		// parameters, system resource parameters, and checkboxes (such
		// as "Common Contaminants"), so all need to be accounted for
		else if (value.startsWith("f.") || value.startsWith("d."))
			return validateFileParameterValue(value);
		// if this is not a standard file selector parameter, then we can't
		// really validate it for true accessibility of the underlying file,
		// since it might be anything; so just make sure it's not empty
		else if (value.trim().equals("") || value.equalsIgnoreCase("None") ||
			value.equalsIgnoreCase("off"))
			return false;
		else return true;
	}
	
	private boolean validateFileParameterValue(String value) {
		if (value == null || value.trim().equals(""))
			return false;
		// recurse directories to get the flat list of file descriptors
		Collection<String> descriptors = new LinkedList<String>();
		for (String descriptor : value.split(";")) {
			// convert all directory descriptors to plain files,
			// since ResourceAgent handles those the same
			if (descriptor.startsWith("d."))
				descriptor = "f." + descriptor.substring(2);
			try {
				ResourceAgent.resolveResources(descriptor, user, descriptors);
			} catch (FileNotFoundException error) {
				return false;
			}
		}
		// check all file descriptors for accessibility
		if (descriptors.isEmpty())
			return false;
		else for (String descriptor : descriptors)
			if (FileManager.isAccessible(FileManager.getFile(descriptor), user)
				== false)
				return false;
		return true;
	}
	
	private String getParameterListString() {
		StringBuffer list = new StringBuffer(getParameter());
		for (String collection : collections)
			list.append(", ").append(collection);
		return list.toString();
	}
}
