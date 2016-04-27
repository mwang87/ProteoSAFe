package edu.ucsd.livesearch.result;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import edu.ucsd.livesearch.result.DownloadResultFile.ResultType;
import edu.ucsd.livesearch.result.parsers.EmptyResult;
import edu.ucsd.livesearch.result.parsers.IterableResult;
import edu.ucsd.livesearch.result.parsers.Result;
import edu.ucsd.livesearch.result.processors.ResultProcessor;
import edu.ucsd.livesearch.task.NullTask;
import edu.ucsd.livesearch.task.Task;
import edu.ucsd.livesearch.task.TaskManager.TaskStatus;
import edu.ucsd.livesearch.util.FileIOUtils;

public class ResultFactory
{
	/*========================================================================
	 * Constants
	 *========================================================================*/
	private static final Logger logger =
		LoggerFactory.getLogger(ResultFactory.class);
	
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	public static final Result createResult(
		Element dataSpec, Task task, String block,
		Map<String, String> parameters
	) {
		if (dataSpec == null || task == null || task instanceof NullTask ||
			task.getStatus().equals(TaskStatus.NONEXIST))
			return null;
		// retrieve source result file
		Element source = ResultViewXMLUtils.getSourceSpecification(dataSpec);
		Result result = null;
		File resultFile = getSourceFile(source, task, parameters);
		// if there is no result file, it's not necessarily an error
		if (resultFile == null) {
			result = new EmptyResult();
			String value = getSourceValue(source, parameters);
			if (value != null)
				((EmptyResult)result).setData(value);
		} else {
			// run result file through parser chain
			List<Element> parsers =
				ResultViewXMLUtils.getParserSpecifications(dataSpec);
			result =
				parseResultFile(parsers, resultFile, task, block, parameters);
			// retrieve and handle global processor specifications;
			// these processors will be run on the final parser in the chain
			List<Element> processors =
				ResultViewXMLUtils.getProcessorSpecifications(
					ResultViewXMLUtils.getFirstChildElement(
						dataSpec, "processors"));
			result = processResult(result, processors, parameters);
		}
		if (result == null) {
			logger.error("Error creating and initializing result.");
			return null;
		}
		// attempt to load the result file
		try {
			result.load();
		} catch (Throwable error) {
			logger.error("Error loading result", error);
			return null;
		}
		return result;
	}
	
	public static final String getHTMLViewNavigator(Task task) {
		if (task == null || task instanceof NullTask)
			return null;
		TaskStatus status = task.getStatus();
		if (status == null || status.equals(TaskStatus.DONE) == false)
			return null;
		// build navigator HTML
		String link = String.format(
			"<a href=\"result.jsp?task=%s&view=%%s\">%%s</a>", task.getID());
		StringBuffer navigator = new StringBuffer();
		List<Element> views = ResultViewXMLUtils.getViewSpecifications(task);
		if (views == null || views.isEmpty()) {
			logger.error(String.format("Error generating task result view " +
				"navigator: no view specifications could be found " +
				"for task \"%s\".", task.getID()));
			return null;
		}
		// organize views into groups
		Map<String, List<Element>> groups_of_views =
			new HashMap<String, List<Element>>();
		for (Element view : views) {
			Map<String, String> attributes =
				ResultViewXMLUtils.getAttributes(view);
			// check to see if this view is hidden; if so, skip it
			String display = attributes.get("display");
			if (display != null && display.equals("hidden"))
				continue;
			// assign this view to the proper group; "Default" if none specified
			String group = attributes.get("group");
			if (group == null)
				group = "Default";
			if (groups_of_views.containsKey(group) == false)
				groups_of_views.put(group, new ArrayList<Element>());
			groups_of_views.get(group).add(view);
		}
		
		int group_view_current_count = 0;
		if(groups_of_views.containsKey("Default")){
			if(groups_of_views.get("Default").size() > 0){
				boolean added_default_link = false;
				StringBuffer default_navigator = new StringBuffer();
				default_navigator.append("[ ");
				for(Element view : groups_of_views.get("Default")){
					String navigator_addition = Generate_View_Link(view, link);
					if(navigator_addition != null){
						if(group_view_current_count > 0){
							default_navigator.append(" | ");
						}
						added_default_link = true;
						default_navigator.append(navigator_addition);
					}
					group_view_current_count++;
				}
				default_navigator.append(" ]");
				if(added_default_link){
					navigator.append(default_navigator.toString());
				}
			}
		}
		
		for(String group : groups_of_views.keySet()){
			if(group.equals("Default"))
				continue;
			navigator.append("<br/><br/><strong>");
			navigator.append(group);
			navigator.append("</strong><br/>[ ");
			group_view_current_count = 0;
			for (Element view : groups_of_views.get(group)) {
				String navigator_addition = Generate_View_Link(view, link);
				if (navigator_addition != null) {
					if (group_view_current_count > 0)
						navigator.append(" | ");
					navigator.append(navigator_addition);
				}
				group_view_current_count++;
			}
			navigator.append(" ]");
		}
		return navigator.toString();
	}
	
	public static final String Generate_View_Link(
		Element view, String formatting_string
	) {
		Map<String, String> attributes =
			ResultViewXMLUtils.getAttributes(view);
		if (attributes == null)
			return null;
		// only render a link to this view if it is not hidden
		String display = attributes.get("display");
		if (display != null && display.equals("hidden"))
			return null;
		// extract required attributes
		String id = attributes.get("id");
		if (id == null) {
			logger.error("Error generating task result view navigator: " +
				"\"id\" is a required attribute of element <view> in " +
				"the result view specification.");
			return null;
		}
		// extract optional attributes
		String label = attributes.get("label");
		if (label == null)
			label = id;
		// build link HTML
		return String.format(formatting_string, id, label);
	}
	
	public static final File getSourceFile(
		Element source, Task task, Map<String, String> parameters
	) {
		if (source == null)
			return null;
		// extract required attributes
		Map<String, String> attributes =
			ResultViewXMLUtils.getAttributes(source);
		if (attributes == null)
			return null;
		// get source type
		String type = resolveParameters(attributes.get("type"), parameters);
		if (type == null) {
			logger.error("Error retrieving task result file: \"type\" is a " +
				"required attribute of element <source> in the result view " +
				"specification.");
			return null;
		}
		ResultType sourceType = null;
		try {
			sourceType = ResultType.valueOf(type.toUpperCase());
		} catch (Throwable error) {
			logger.error("Error retrieving task result file: the \"type\" " +
				"attribute of element <source> in the result view " +
				"specification must have one of the following values: " +
				"\"file\", \"folder\", \"invoke\" or \"value\".");
			return null;
		}
		// get source name
		String name = resolveParameters(attributes.get("name"), parameters);
		if (name == null)
			return null;
		// get source parameters, if present (for plugin invocation)
		List<Element> parameterSpecs =
			ResultViewXMLUtils.getParameterSpecifications(source);
		// retrieve result file
		try {
			if (sourceType.equals(ResultType.INVOKE))
				return DownloadResultFile.invokePlugin(name,
					getPluginParameters(parameterSpecs, parameters), task);
			else return DownloadResultFile.fetchStaticFile(
				sourceType, name, task);
		} catch (Throwable error) {
			logger.error("Error retrieving task result file", error);
			return null;
		}
	}
	
	public static final String getSourceValue(
		Element source, Map<String, String> parameters
	) {
		if (source == null)
			return null;
		// extract required attributes
		Map<String, String> attributes =
			ResultViewXMLUtils.getAttributes(source);
		if (attributes == null)
			return null;
		// get source type
		String type = resolveParameters(attributes.get("type"), parameters);
		if (type == null) {
			logger.error("Error retrieving task result file: \"type\" is a " +
				"required attribute of element <source> in the result view " +
				"specification.");
			return null;
		}
		ResultType sourceType = null;
		try {
			sourceType = ResultType.valueOf(type.toUpperCase());
		} catch (Throwable error) {
			logger.error("Error retrieving task result file: the \"type\" " +
				"attribute of element <source> in the result view " +
				"specification must have one of the following values: " +
				"\"file\", \"folder\", \"invoke\" or \"value\".");
			return null;
		}
		// get value string
		if (sourceType.equals(ResultType.VALUE) == false)
			return null;
		else return resolveParameters(attributes.get("value"), parameters);
	}
	
	/*========================================================================
	 * Convenience methods
	 *========================================================================*/
	private static Result parseResultFile(
		List<Element> parsers, File file, Task task, String block,
		Map<String, String> parameters
	) {
		if (parsers == null || parsers.isEmpty())
			return null;
		// verify file
		if (file == null || file.canRead() == false) {
			logger.error("Error instantiating task result: a valid data " +
				"source file could not be retrieved.");
			return null;
		}
		// instantiate all parsers and run them to retrieve a final result
		Result result = null;
		for (Element parser : parsers) {
			result =
				parseResultFile(parser, result, file, task, block, parameters);
			if (result == null)
				return null;
		}
		return result;
	}
	
	private static Result parseResultFile(
		Element parser, Result previous, File file, Task task, String block,
		Map<String, String> parameters
	) {
		if (parser == null)
			return null;
		// extract required attributes
		Map<String, String> attributes =
			ResultViewXMLUtils.getAttributes(parser);
		if (attributes == null)
			return null;
		// get result type
		String type = resolveParameters(attributes.get("type"), parameters);
		if (type == null) {
			logger.error("Error instantiating task result: \"type\" is a " +
				"required attribute of element <parser> in the result view " +
				"specification.");
			return null;
		} else attributes.remove("type");
		// get result instance
		Result result = null;
		if (previous == null)
			result = getResultInstance(type, block, file, task);
		else result = getResultInstance(type, block, previous);
		if (result == null) {
			logger.error("Error obtaining result instance.");
			return null;
		}
		// set properties of the result from attributes
		for (String attribute : attributes.keySet()) {
			String value = resolveParameters(
				attributes.get(attribute), parameters);
			if (setObjectProperty(result, attribute, value, parameters)
				== false) {
				logger.error("Error setting result property \"" +
					attribute + "\".");
				return null;
			}
		}
		// get processors for this result
		List<Element> processors =
			ResultViewXMLUtils.getProcessorSpecifications(parser);
		result = processResult(result, processors, parameters);
		return result;
	}
	
	private static Result processResult(
		Result result, List<Element> processorSpecs,
		Map<String, String> parameters
	) {
		if (result == null)
			return null;
		else if (processorSpecs == null || processorSpecs.isEmpty())
			return result;
		else if (result instanceof IterableResult == false) {
			logger.error("Error initializing task result processors: " +
				"results of type \"" + result.getClass().getSimpleName() +
				"\" cannot have child <processor> elements in the result " +
				"view specification. <processor> elements are only " +
				"meaningful for iterable result types.");
			return null;
		}
		for (Element processorSpec : processorSpecs) {
			// extract required attributes
			Map<String, String> attributes =
				ResultViewXMLUtils.getAttributes(processorSpec);
			// get processor type
			String type = resolveParameters(attributes.get("type"), parameters);
			if (type == null) {
				logger.error("Error initializing task result processors: " +
					"\"type\" is a required attribute of element " +
					"<processor> in the result view specification.");
				return null;
			} else attributes.remove("type");
			// get processor instance
			ResultProcessor processor = getProcessorInstance(type);
			if (processor == null) {
				logger.error("Error obtaining result processor instance.");
				return null;
			}
			// set all remaining properties of the processor
			for (String attribute : attributes.keySet()) {
				String value = resolveParameters(
					attributes.get(attribute), parameters);
				if (setObjectProperty(
					processor, attribute, value, parameters) == false) {
					logger.error(
						"Error setting result processor property \"" +
						attribute + "\".");
					return null;
				}
			}
			// add the processor to the result
			((IterableResult)result).addProcessor(processor);
		}
		return result;
	}
	
	@SuppressWarnings("unchecked")
	private static Class<Result> getResultClass(String type) {
		if (type == null)
			return null;
		String className = "edu.ucsd.livesearch.result.parsers." +
			Character.toUpperCase(type.charAt(0)) + type.substring(1) +
			"Result";
		Class<Result> resultClass = null;
		try {
			resultClass = (Class<Result>)Class.forName(className);
		} catch (ClassNotFoundException error) {
			logger.error("Error loading result class", error);
			return null;
		}
		return resultClass;
	}
	
	private static Result getResultInstance(
		String type, String block, File file, Task task
	) {
		if (file == null || task == null)
			return null;
		// get result class
		Class<Result> resultClass = getResultClass(type);
		if (resultClass == null)
			return null;
		// instantiate result class
		Result result = null;
		try {
			result = resultClass.getConstructor(
				File.class, Task.class, String.class).newInstance(
				file, task, block);
		} catch (Throwable error) {
			logger.error("Error instantiating result class", error);
			return null;
		}
		return result;
	}
	
	private static Result getResultInstance(
		String type, String block, Result previous
	) {
		if (previous == null)
			return null;
		// get result class
		Class<Result> resultClass = getResultClass(type);
		if (resultClass == null)
			return null;
		// instantiate result class
		Result result = null;
		try {
			result = resultClass.getConstructor(
				Result.class, String.class).newInstance(previous, block);
		} catch (Throwable error) {
			logger.error("Error instantiating result class", error);
			return null;
		}
		return result;
	}
	
	@SuppressWarnings("unchecked")
	private static ResultProcessor getProcessorInstance(String type) {
		if (type == null)
			return null;
		String className = "edu.ucsd.livesearch.result.processors." +
			Character.toUpperCase(type.charAt(0)) + type.substring(1) +
			"Processor";
		Class<ResultProcessor> processorClass = null;
		try {
			processorClass = (Class<ResultProcessor>)Class.forName(className);
		} catch (ClassNotFoundException error) {
			logger.error("Error loading result processor class", error);
			return null;
		}
		ResultProcessor processor = null;
		try {
			processor = processorClass.getConstructor().newInstance();
		} catch (Throwable error) {
			logger.error("Error instantiating result class", error);
			return null;
		}
		return processor;
	}
	
	private static Map<String, String> getPluginParameters(
		List<Element> parameterSpecs, Map<String, String> parameters
	) {
		if (parameterSpecs == null)
			return null;
		Map<String, String> pluginParameters =
			new HashMap<String, String>(parameterSpecs.size());
		for (Element parameterSpec : parameterSpecs) {
			Map<String, String> parameter =
				ResultViewXMLUtils.getAttributes(parameterSpec);
			// get parameter name
			String name = resolveParameters(parameter.get("name"), parameters);
			if (name == null) {
				logger.error("Error instantiating task result: " +
					"\"name\" is a required attribute of element " +
					"<parameter> in the result view specification.");
				return null;
			}
			// first try to get parameter value as a literal value
			String value = resolveParameters(
				parameter.get("value"), parameters);
			// if it's not a literal value, then it is a file or folder path
			if (value == null) {
				value = resolveParameters(parameter.get("file"), parameters);
				if (value == null) {
					value = resolveParameters(
						parameter.get("folder"), parameters);
					if (value == null) {
						logger.error("Error instantiating task result: " +
							"could not determine the value of plugin " +
							"parameter \"" + name + "\".");
						return null;
					}
					// since this parameter value is a folder path,
					// prepend expected folder prefix
					else value = DownloadResultFile.FOLDER_PREFIX +
						FileIOUtils.escapeFilename(value);
				}
				// since this parameter value is a file path,
				// prepend expected file prefix
				else value = DownloadResultFile.FILE_PREFIX +
					FileIOUtils.escapeFilename(value);
			}
			pluginParameters.put(name, value);
		}
		if (pluginParameters == null || pluginParameters.isEmpty()) {
			logger.error("Error instantiating task result: a data source " +
				"of type \"invoke\" was specified, but no plugin " +
				"parameters could be found.");
			return null;
		} else return pluginParameters;
	}
	
	private static boolean setObjectProperty(
		Object object, String property, String value,
		Map<String, String> parameters
	) {
		if (object == null || property == null || value == null)
			return false;
		// convert the first character of the property to upper case
		property = Character.toUpperCase(property.charAt(0)) +
			property.substring(1);
		// get the property setter method
		Method method = null;
		try {
			method = object.getClass().getMethod(
				"set" + property, String.class);
		} catch (Throwable error) {
			logger.error("Error retrieving setter method for property \"" +
				property + "\" of class \"" +
				object.getClass().getName() + "\"", error);
		} finally {
			if (method == null)
				return false;
		}
		// invoke the property setter method
		try {
			method.invoke(object, value);
		} catch (Throwable error) {
			logger.error("Error invoking setter method for property \"" +
				property + "\" of class \"" +
				object.getClass().getName() + "\"", error);
			return false;
		}
		return true;
	}
	
	private static String resolveParameters(
		String value, Map<String, String> parameters
	) {
		if (value == null)
			return null;
		// parse out all parameter references
		StringBuffer copy = new StringBuffer(value);
		while (true) {
			// parse out parameter name
			String parameterName = null;
			int start = copy.indexOf("{");
			if (start < 0)
				break;
			int end = copy.indexOf("}");
			if (end <= start)
				break;
			else if (end == start + 1)
				parameterName = "";
			else parameterName = copy.substring(start + 1, end);
			// retrieve parameter value
			String parameterValue = null;
			if (parameters != null)
				parameterValue = parameters.get(parameterName);
			if (parameterValue == null)
				parameterValue = parameterName;
			int paramStart = parameterValue.indexOf("{");
			int paramEnd = parameterValue.indexOf("}");
			if (paramStart >= 0 && paramEnd > paramStart)
				throw new IllegalArgumentException(
					"Request parameter values should not themselves " +
					"be references to other parameters. You run the risk " +
					"of entering an infinite loop!");
			copy.delete(start, end + 1);
			copy.insert(start, parameterValue);
		}
		return copy.toString();
	}
}
