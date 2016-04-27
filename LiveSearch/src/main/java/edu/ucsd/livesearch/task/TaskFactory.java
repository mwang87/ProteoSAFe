package edu.ucsd.livesearch.task;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.xpath.XPathAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.ucsd.livesearch.parameter.ParameterProcessor;
import edu.ucsd.livesearch.parameter.fileGenerators.MassesFileGenerator;
import edu.ucsd.livesearch.parameter.fileGenerators.ParameterFileGenerator;
import edu.ucsd.livesearch.parameter.processors.ParameterCleaner;
import edu.ucsd.livesearch.parameter.processors.ProvenanceProcessor;
import edu.ucsd.livesearch.servlet.DownloadWorkflowInterface;
import edu.ucsd.livesearch.util.WorkflowParameterUtils;
import edu.ucsd.saint.commons.http.HttpParameters;

public class TaskFactory
{
	/*========================================================================
	 * Constants
	 *========================================================================*/
	private static final Logger logger =
		LoggerFactory.getLogger(TaskFactory.class);

	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	public static Task createTask(String user, HttpParameters parameters) {
		if (parameters == null)
			return null;
		// convert HttpParameters to a generic key-value string map
		Map<String, Collection<String>> parameterMap =
			WorkflowParameterUtils.extractParameters(parameters);
		if (parameterMap == null)
			return createTask(
				user, new LinkedHashMap<String, Collection<String>>(0));
		else return createTask(user, parameterMap);
	}

	public static Task createTask(
		String user, Map<String, Collection<String>> parameters
	) {
		if (parameters == null)
			return null;
		String workflow = WorkflowParameterUtils.getFirstParameterValue(
			parameters, "workflow");
		if (workflow == null)
			return null;
		// retrieve and read workflow interface specification
		Document spec = null;
		try {
			spec = DownloadWorkflowInterface.getWorkflowSpecification(
				workflow, "input", user, null);
		} catch (Throwable error) {
            if(workflow.equals("MAKE-DATASET-PUBLIC") || workflow.equals("ADD-DATASET-PUBLICATION") || workflow.equals("UPDATE-DATASET-METADATA") || workflow.equals("DELETE-DATASET") || workflow.equals("ADD-SPECTRUM-TAG") || workflow.equals("REMOVE-SPECTRUM-TAG")){
            }
            else{
                return null;
            }
		}
		// set up validator and processor lists
		List<ParameterProcessor> validators = null;
		List<ParameterProcessor> processors =
			new ArrayList<ParameterProcessor>();
		// not all workflows have an input.xml; if this one doesn't, then that
		// just means it's a ProteoSAFe "system" workflow, so we can skip the
		// usual processor and validator instantiation
		if (spec != null) {
			// set up parameter validators
			try {
				validators = setupProcessors(spec, workflow, "validator");
			} catch (TaskBuilderException error) {
				logger.error("Error initializing parameter validators:", error);
				return new NullTask();
			}
			// set up parameter processors
			processors.add(new ParameterCleaner());
			List<ParameterProcessor> remaining = null;
			try {
				remaining = setupProcessors(spec, workflow, "processor");
			} catch (TaskBuilderException error) {
				logger.error("Error initializing parameter processors:", error);
				return new NullTask();
			} finally {
				if (remaining != null)
					processors.addAll(remaining);
			}
			// set up file generators
			List<ParameterProcessor> fileGenerators = null;
			try {
				fileGenerators = setupProcessors(spec, workflow, "fileGenerator");
			} catch (TaskBuilderException error) {
				logger.error("Error initializing file generators:", error);
				return new NullTask();
			} finally {
				if (fileGenerators != null)
					processors.addAll(fileGenerators);
			}
			// set up final processors, that must appear after everything else
			processors.add(new ProvenanceProcessor());
		}
		// all workflow tasks need a params.xml
		processors.add(new ParameterFileGenerator());
		// all cluster-executed workflow tasks are assumed to need a masses.txt
		if (spec != null)
			processors.add(new MassesFileGenerator());
		// instantiate TaskBuilder
		TaskBuilder builder =
			new TaskBuilder(user, parameters, validators, processors);
		// build task
		Task task = builder.buildTask();
		if (task == null)
			return new NullTask();
		else return task;
	}

	/*========================================================================
	 * Convenience methods
	 *========================================================================*/
	private static List<ParameterProcessor> setupProcessors(
		Document spec, String workflow, String nodeType
	) throws TaskBuilderException {
		if (spec == null || workflow == null || nodeType == null)
			return null;
		// traverse processors defined in the interface specification
		NodeList processorNodes = null;
		try {
			processorNodes = XPathAPI.selectNodeList(spec, "//" + nodeType);
		} catch (Throwable error) {
			throw new TaskBuilderException("Workflow parameter processors " +
				"could not be initialized, because there was a problem " +
				"retrieving processor information from the specification " +
				"document for workflow \"" + workflow + "\".", error);
		} finally {
			// if there are no processors, return null
			if (processorNodes == null || processorNodes.getLength() < 1)
				return null;
		}
		// build and collect processors
		List<ParameterProcessor> processors =
			new ArrayList<ParameterProcessor>(processorNodes.getLength());
		for (int i=0; i<processorNodes.getLength(); i++) {
			// set up processor
			Node processorNode = processorNodes.item(i);
			NamedNodeMap attributes = processorNode.getAttributes();
			// instantiate processor by type
			String type = null;
			ParameterProcessor processor = null;
			try {
				type = attributes.getNamedItem("type").getNodeValue();
				processor = getProcessor(type, nodeType);
			} catch (Throwable error) {
			} finally {
				if (processor == null)
					throw new TaskBuilderException("There was a problem " +
						"instantiating a workflow parameter processor of " +
						"type \"" + type + "\" from the specification " +
						"document for workflow \"" + workflow + "\".");
			}
			// configure processor with parameter attributes
			Node parent = processorNode.getParentNode();
			String parentType = parent.getNodeName();
			if (parentType.equals("parameter")) {
				// get parameter name
				String parameter = null;
				try {
					parameter = parent.getAttributes().getNamedItem("name")
						.getNodeValue();
					setProcessorProperty(processor, "parameter", parameter);
				} catch (Throwable error) {
					throw new TaskBuilderException("There was a problem " +
						"initializing a workflow parameter processor of " +
						"type \"" + type + "\", because there was a problem " +
						"retrieving the parameter name from the " +
						"specification document for workflow \"" + workflow +
						"\".", error);
				}
				// get parameter label, if present
				String label = null;
				try {
					label = parent.getAttributes().getNamedItem("label")
						.getNodeValue();
					if (label != null)
						setProcessorProperty(processor, "label", label);
				} catch (Throwable error) {}
				// get option values, if present
				NodeList options = null;
				try {
					options = XPathAPI.selectNodeList(parent, ".//option");
					for (int j=0; j<options.getLength(); j++) {
						String value = options.item(j).getAttributes()
							.getNamedItem("value").getNodeValue();
						if (value != null)
							setProcessorProperty(processor, "option", value);
					}
				} catch (Throwable error) {}
			} else if (parentType.equals("processors")) {
				// TODO: do the right thing
			} else throw new TaskBuilderException("There was a problem " +
				"initializing a workflow parameter processor of type \"" +
				type + "\", because of a syntax error in the specification " +
				"document for workflow \"" + workflow + "\": a <" + nodeType +
				"> element was found as a child of a <" + parentType +
				"> element, which is not allowed.");
			// initialize processor with attributes
			for (int j=0; j<attributes.getLength(); j++) {
				Node attribute = attributes.item(j);
				String property = attribute.getNodeName();
				if (property.equals("type"))
					continue;
				String value = attribute.getNodeValue();
				if (setProcessorProperty(processor, property, value) == false)
					throw new TaskBuilderException("There was a problem " +
						"initializing a workflow parameter processor of " +
						"type \"" + type + "\", because there was a problem " +
						"assigning value \"" + value + "\" to property \"" +
						property + "\".");
			}
			// get property values, if present
			NodeList properties = null;
			try {
				properties =
					XPathAPI.selectNodeList(processorNode, ".//property");
				for (int j=0; j<properties.getLength(); j++) {
					Node property = properties.item(j);
					String name = property.getAttributes()
						.getNamedItem("name").getNodeValue();
					String value = property.getTextContent();
					if (name != null && value != null)
						setProcessorProperty(processor, name, value);
				}
			} catch (Throwable error) {}
			// add processor to list
			processors.add(processor);
		}
		if (processors.size() < 1)
			return null;
		else return processors;
	}

	@SuppressWarnings("rawtypes")
	private static ParameterProcessor getProcessor(
		String processorType, String nodeType
	) {
		if (processorType == null || nodeType == null)
			return null;
		// get the processor class
		String className = "edu.ucsd.livesearch.parameter." + nodeType + "s." +
			Character.toUpperCase(processorType.charAt(0)) +
				processorType.substring(1) +
			Character.toUpperCase(nodeType.charAt(0)) +
				nodeType.substring(1);
		Class processorClass = null;
		try {
			processorClass = Class.forName(className);
		} catch (ClassNotFoundException error) {
			logger.error("Error loading processor class:", error);
			return null;
		}
		// instantiate and return the processor object
		ParameterProcessor processor = null;
		try {
			processor = (ParameterProcessor)processorClass.newInstance();
		} catch (Throwable error) {
			return null;
		}
		return processor;
	}

	private static boolean setProcessorProperty(
		ParameterProcessor processor, String property, String value
	) {
		if (processor == null || property == null || value == null)
			return false;
		// convert the first character of the property to upper case
		property =
			Character.toUpperCase(property.charAt(0)) + property.substring(1);
		// get the property setter method
		Method method = null;
		try {
			method =
				processor.getClass().getMethod("set" + property, String.class);
		} catch (Throwable error) {
		} finally {
			if (method == null)
				return false;
		}
		// invoke the property setter method
		try {
			method.invoke(processor, value);
		} catch (Throwable error) {
			error.printStackTrace();
			return false;
		}
		return true;
	}
}
