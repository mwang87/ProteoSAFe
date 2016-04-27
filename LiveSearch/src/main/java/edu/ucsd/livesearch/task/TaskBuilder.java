package edu.ucsd.livesearch.task;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsd.livesearch.parameter.ParameterProcessor;
import edu.ucsd.livesearch.parameter.validators.ParameterValidator;
import edu.ucsd.livesearch.result.ResultViewXMLUtils;
import edu.ucsd.livesearch.task.TaskManager.TaskStatus;
import edu.ucsd.livesearch.util.WorkflowParameterUtils;
import edu.ucsd.saint.commons.WebAppProps;

public class TaskBuilder
{
	/*========================================================================
	 * Constants
	 *========================================================================*/
	private static final Logger logger =
		LoggerFactory.getLogger(TaskBuilder.class);
	
	/*========================================================================
	 * Properties
	 *========================================================================*/
	private String user;
	private Map<String, Collection<String>> parameters;
	private Task task;
	private List<ParameterValidator> validators;
	private List<ParameterProcessor> processors;
	
	/*========================================================================
	 * Constructor
	 *========================================================================*/
	public TaskBuilder(
		String user, Map<String, Collection<String>> parameters
	) {
		this(user, parameters, null, null);
	}
	
	public TaskBuilder(
		String user, Map<String, Collection<String>> parameters,
		List<ParameterProcessor> validators,
		List<ParameterProcessor> processors
	) {
		this.user = user;
		this.parameters = parameters;
		// add validators
		if (validators != null) {
			for (ParameterProcessor validator : validators) {
				if (validator instanceof ParameterValidator == false)
					throw new IllegalArgumentException("All processors " +
						"passed as members of the \"validators\" argument " +
						"collection must be instances of " +
						ParameterValidator.class + ".");
				else addValidator((ParameterValidator)validator);
			}
		}
		// add processors
		if (processors != null)
			for (ParameterProcessor processor : processors)
				addProcessor(processor);
	}
	
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	public final Task buildTask() {
		// first validate parameters, to prevent spam
		// tasks from cluttering up the database
		List<String> errors = validateParameters();
		// report validation errors, if any
		if (errors != null && errors.size() > 0) {
			task = new NullTask();
			reportErrors(errors, "Task parameter validation");
			return task;
		}
		// once parameters have passed validation, create and initialize task
		if (task == null) {
			String site = WebAppProps.get("livesearch.site.name");
			task =
				TaskManager.createTask((user == null) ? "guest" : user, site);
			initializeTask();
		}
		// now run parameter processors
		errors = processParameters();
		// report errors, if any
		if (errors != null && errors.size() > 0) {
			task.setStatus(TaskStatus.FAILED);
			task.setFailures(errors);
			reportErrors(errors, "Task parameter processing");
		}
		return task;
	}
	
	/*========================================================================
	 * Property accessor methods
	 *========================================================================*/
	public final String getUser() {
		return user;
	}
	
	public final Map<String, Collection<String>> getParameters() {
		if (parameters == null)
			return null;
		else return new LinkedHashMap<String, Collection<String>>(parameters);
	}
	
	public final Collection<String> getParameterValues(String parameter) {
		if (parameter == null || parameters == null)
			return null;
		Collection<String> values = parameters.get(parameter);
		if (values == null || values.isEmpty())
			return null;
		else return values;
	}
	
	public final String getFirstParameterValue(String parameter) {
		return WorkflowParameterUtils.getFirstParameterValue(
			parameters, parameter);
	}
	
	public final void setParameterValue(String parameter, String value) {
		WorkflowParameterUtils.setParameterValue(parameters, parameter, value);
	}
	
	public final void addParameterValue(String parameter, String value) {
		WorkflowParameterUtils.addParameterValue(parameters, parameter, value);
	}
	
	public final Collection<String> removeParameter(String parameter) {
		return WorkflowParameterUtils.removeParameter(parameters, parameter);
	}
	
	public final Task getTask() {
		return task;
	}
	
	public final List<ParameterValidator> getValidators() {
		if (validators == null)
			return null;
		else return new ArrayList<ParameterValidator>(validators);
	}
	
	public final List<ParameterProcessor> getProcessors() {
		if (processors == null)
			return null;
		else return new ArrayList<ParameterProcessor>(processors);
	}
	
	public final void addValidator(ParameterValidator validator) {
		if (validator == null)
			return;
		else {
			if (validators == null)
				validators = new ArrayList<ParameterValidator>();
			validators.add(validator);
		}
	}
	
	public final void addProcessor(ParameterProcessor processor) {
		if (processor == null)
			return;
		else {
			if (processors == null)
				processors = new ArrayList<ParameterProcessor>();
			processors.add(processor);
		}
	}
	
	/*========================================================================
	 * Convenience methods
	 *========================================================================*/
	private void initializeTask() {
		if (task != null && parameters != null) {
			// set workflow name
			String workflow = getFirstParameterValue("workflow");
			if (StringUtils.isEmpty(workflow) == false)
				task.setFlowName(workflow);
			// set workflow version, if applicable
			String version = ResultViewXMLUtils.getInterfaceVersion(task);
			if (version != null)
				task.setFlowVersion(version);
			// set description
			String desc = getFirstParameterValue("desc");
			if (desc == null)
				desc = "";
			task.setComment(desc);
			// set email
			String email = getFirstParameterValue("email");
			if (StringUtils.isEmpty(email) == false)
				TaskManager.setNotification(task, email);
			// add basic task info to parameters
			setParameterValue("user", task.getUser());
			setParameterValue("task", task.getID());
		}
	}
	
	private List<String> validateParameters() {
		// get parameter validators
		if (validators == null || validators.size() < 1)
			return null;
		// ensure that there is a valid workflow parameter
		List<String> errors = new ArrayList<String>();
		String workflow = getFirstParameterValue("workflow");
		if (workflow == null || workflow.trim().isEmpty())
			errors.add("The \"workflow\" parameter must be non-empty.");
		// validate the parameters
		for (ParameterValidator validator : validators) {
			List<String> theseErrors = validator.processParameters(this);
			if (theseErrors != null && theseErrors.size() > 0)
				errors.addAll(theseErrors);
		}
		if (errors.size() < 1)
			return null;
		else return errors;
	}
	
	private List<String> processParameters() {
		// get parameter processors
		if (processors == null || processors.size() < 1)
			return null;
		// process all the parameters
		List<String> errors = new ArrayList<String>();
		for (ParameterProcessor processor : processors) {
			List<String> theseErrors = processor.processParameters(this);
			if (theseErrors != null && theseErrors.size() > 0)
				errors.addAll(theseErrors);
		}
		if (errors.size() < 1)
			return null;
		else return errors;
	}
	
	private void reportErrors(List<String> errors, String context) {
		if (errors == null || errors.size() < 1)
			return;
		else if (context == null || context.trim().equals(""))
			context = "Task creation";
		StringBuffer errorMessage = new StringBuffer(context);
		errorMessage.append(" failed due to ");
		errorMessage.append(errors.size());
		errorMessage.append(" error");
		if (errors.size() > 1)
			errorMessage.append("s:");
		else errorMessage.append(":\n");
		for (String error : errors) {
			errorMessage.append("\n  ");
			errorMessage.append(error);
		}
		logger.error(errorMessage.toString());
		if (task != null)
			task.setMessage(errorMessage.toString());
	}
}
