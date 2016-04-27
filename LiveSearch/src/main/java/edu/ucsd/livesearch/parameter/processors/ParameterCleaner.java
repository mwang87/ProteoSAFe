package edu.ucsd.livesearch.parameter.processors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsd.livesearch.parameter.ParameterProcessor;
import edu.ucsd.livesearch.task.TaskBuilder;

public class ParameterCleaner
implements ParameterProcessor
{
	/*========================================================================
	 * Constants
	 *========================================================================*/
	@SuppressWarnings("unused")
	private static final Logger logger =
		LoggerFactory.getLogger(ParameterCleaner.class);
	
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	public List<String> processParameters(TaskBuilder builder) {
		if (builder == null)
			return null;
		Map<String, Collection<String>> parameters = builder.getParameters();
		if (parameters == null)
			return null;
		// clean out empty parameters
		Collection<String> parameterNames =
			new ArrayList<String>(parameters.keySet());
		for (String name : parameterNames) {
			// if the parameter name is empty, remove it
			if (name.trim().equals(""))
				builder.removeParameter(name);
			else {
				// if the parameter value is empty, remove it
				Collection<String> values = parameters.get(name);
				if (values == null || values.size() < 1 ||
					StringUtils.isEmpty(builder.getFirstParameterValue(name)))
					builder.removeParameter(name);
			}
		}
		// clean out specific parameters not needed any more
		builder.removeParameter("protocol");
		return null;
	}
}
