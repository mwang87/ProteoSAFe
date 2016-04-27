package edu.ucsd.livesearch.parameter.validators;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import edu.ucsd.livesearch.task.TaskBuilder;

public class ProteomeXchangeValidator
extends ParameterValidator
{
	/*========================================================================
	 * Constants
	 *========================================================================*/
	private static final String NO_RESULTS =
		"ProteomeXchange datasets must include files in either the " +
		"\"Result Files\" or \"Search Engine Files\" categories.";
	
	/*========================================================================
	 * Properties
	 *========================================================================*/
	private String result;
	private String search;
	
	/*========================================================================
	 * Constructors
	 *========================================================================*/
	public ProteomeXchangeValidator() {
		super();
		setMessage(NO_RESULTS);
	}
	
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	@Override
	public final List<String> processParameters(TaskBuilder builder) {
		if (builder == null)
			return null;
		result = builder.getFirstParameterValue("result_files");
		search = builder.getFirstParameterValue("search_files");
		return super.processParameters(builder);
	}
	
	@Override
	public String validateParameter(String value) {
		if (value == null)
			return null;
		else if (value.trim().equalsIgnoreCase("on") == false)
			return null;
		// PX datasets must have files in either "result" or "search"
		else if (StringUtils.isEmpty(result) && StringUtils.isEmpty(search))
			return getMessage();
		else return null;
	}
}
