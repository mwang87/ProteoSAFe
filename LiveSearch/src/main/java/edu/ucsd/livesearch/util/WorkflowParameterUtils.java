package edu.ucsd.livesearch.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import org.apache.xpath.XPathAPI;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.ucsd.livesearch.result.DownloadResultFile;
import edu.ucsd.livesearch.result.DownloadResultFile.ResultType;
import edu.ucsd.livesearch.task.Task;
import edu.ucsd.saint.commons.http.HttpParameters;

public class WorkflowParameterUtils
{
	/*========================================================================
	 * Constants
	 *========================================================================*/
	public static final String UPLOAD_FILE_MAPPING_PARAMETER =
		"upload_file_mapping";
	
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	public static final Map<String, Collection<String>> extractParameters(
		HttpParameters parameters
	) {
		if (parameters == null)
			return null;
		Map<String, Collection<String>> extracted =
			new LinkedHashMap<String, Collection<String>>();
		for (String parameter : parameters.getParameterNames()) {
			Collection<String> values = parameters.getMultiParams(parameter);
			if (values != null && values.isEmpty() == false)
				extracted.put(parameter, values);
		}
		if (extracted.isEmpty())
			return null;
		else return extracted;
	}
	
	public static final Map<String, Collection<String>> extractParameters(
		Document parameters
	) {
		if (parameters == null)
			return null;
		Map<String, Collection<String>> extracted =
			new LinkedHashMap<String, Collection<String>>();
		try {
			NodeList nodes = XPathAPI.selectNodeList(parameters, "//parameter");
			for (int i=0; i<nodes.getLength(); i++) {
				Node node = nodes.item(i);
				addParameterValue(extracted,
					getAttribute(node, "name"), node.getTextContent());
			}
		} catch (Throwable error) {
			return null;
		}
		if (extracted.isEmpty())
			return null;
		else return extracted;
	}
	
	public static final Map<String, Collection<String>> extractParameters(
		File paramsXML
	) {
		return extractParameters(FileIOUtils.parseXML(paramsXML));
	}
	
	public static final Map<String, Collection<String>> extractParameters(
		Task task
	) {
		return extractParameters(DownloadResultFile.fetchStaticFile(
			ResultType.FILE, "params/params.xml", task));
	}
	
	public static final String getParameter(Task task, String parameter) {
		return getParameter(extractParameters(task), parameter);
	}
	
	public static final String getParameter(
		Map<String, Collection<String>> parameters, String parameter
	) {
		if (parameters == null || parameter == null)
			return null;
		Collection<String> values = parameters.get(parameter);
		if (values == null || values.isEmpty())
			return null;
		else return values.toArray(new String[values.size()])[0];
	}
	
	public static final Collection<String> getUploadFilenames(
		Task task, String target
	) {
		return getUploadFilenames(extractParameters(task), target);
	}
	
	public static final Collection<String> getUploadFilenames(
		Map<String, Collection<String>> parameters, String target
	) {
		if (parameters == null || target == null)
			return null;
		Collection<String> uploadMappings =
			parameters.get(UPLOAD_FILE_MAPPING_PARAMETER);
		if (uploadMappings == null || uploadMappings.isEmpty())
			return null;
		// traverse upload mappings, return original filenames
		// for all uploads belonging to target collection
		Collection<String> uploadFilenames = new LinkedHashSet<String>();
		for (String mapping : uploadMappings) {
			String[] mappingTokens = mapping.split("\\|");
			if (mappingTokens == null || mappingTokens.length != 2)
				throw new IllegalArgumentException(String.format(
					"ProteoSAFe upload file mapping parameter value \"%s\" " +
					"does not adhere to the expected format of " +
					"<target>-<mangledFilename>|<originalFilePath>", mapping));
			String[] mangledTokens = mappingTokens[0].split("-");
			if (mangledTokens == null)
				throw new IllegalArgumentException(String.format(
					"ProteoSAFe upload descriptor \"%s\" does not adhere " +
					"to the expected format of <target>-<mangledFilename>",
					mappingTokens[0]));
			// legacy upload mappings are not prepended with "<target>-"
			else if (mangledTokens.length != 2)
				continue;
			else if (mangledTokens[0].equals(target))
				uploadFilenames.add(mappingTokens[1]);
		}
		if (uploadFilenames.isEmpty())
			return null;
		else return uploadFilenames;
	}
	
	public static final String getFirstParameterValue(
		Map<String, Collection<String>> parameters, String parameter
	) {
		if (parameters == null || parameter == null)
			return null;
		Collection<String> values = parameters.get(parameter);
		if (values == null || values.size() < 1)
			return null;
		else return new ArrayList<String>(values).get(0);
	}
	
	public static final void setParameterValue(
		Map<String, Collection<String>> parameters,
		String parameter, String value
	) {
		if (parameters == null || parameter == null || value == null)
			return;
		Collection<String> values = new LinkedHashSet<String>(1);
		values.add(value);
		parameters.put(parameter, values);
	}
	
	public static final void addParameterValue(
		Map<String, Collection<String>> parameters,
		String parameter, String value
	) {
		if (parameters == null || parameter == null || value == null)
			return;
		Collection<String> values = parameters.get(parameter);
		if (values == null)
			values = new LinkedHashSet<String>(1);
		values.add(value);
		parameters.put(parameter, values);
	}
	
	public static final Collection<String> removeParameter(
		Map<String, Collection<String>> parameters, String parameter
	) {
		if (parameters == null || parameter == null)
			return null;
		else return parameters.remove(parameter);
	}
	
	/*========================================================================
	 * Convenience methods
	 *========================================================================*/
	private static String getAttribute(Node node, String attribute) {
		if (node == null || attribute == null)
			return null;
		try {
			return node.getAttributes().getNamedItem(attribute).getNodeValue();
		} catch (Throwable error) {
			return null;
		}
	}
}
