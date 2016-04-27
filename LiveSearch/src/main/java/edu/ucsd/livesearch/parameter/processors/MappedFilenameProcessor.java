package edu.ucsd.livesearch.parameter.processors;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.ucsd.livesearch.dataset.mapper.ResultFileMapper;
import edu.ucsd.livesearch.parameter.ParameterProcessor;
import edu.ucsd.livesearch.task.TaskBuilder;

public class MappedFilenameProcessor
implements ParameterProcessor
{
	/*========================================================================
	 * Properties
	 *========================================================================*/
	private String parameter;
	private Map<String, String> fileMap;
	@SuppressWarnings("unused")
	private Set<String> mappedTargetFiles;
	
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	public List<String> processParameters(TaskBuilder builder) {
		if (builder == null)
			return null;
		String parameter = getParameter();
		if (parameter == null)
			return null;
		// retrieve mapping parameter
		String value = builder.getFirstParameterValue(parameter);
		if (value == null)
			return null;
		// parse mapping parameter, build map of target -> source filenames
		buildFilenameMap(value);
		// write "result_file_mapping" parameters
		for (String reference : fileMap.keySet())
			builder.addParameterValue("result_file_mapping",
				reference + "|" + fileMap.get(reference));
		// TODO: validate associated upload collection
		// against stored set of mapped target files
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
	public String getParameter() {
		return parameter;
	}
	
	/**
	 * Sets the input form name of the parameter that is to be processed
	 * by this processor.
	 * 
	 * @param label	the name of the parameter to be processed,
	 * 				as specified in the workflow input form
	 */
	public void setParameter(String parameter) {
		this.parameter = parameter;
	}
	
	/*========================================================================
	 * Convenience methods
	 *========================================================================*/
	private void buildFilenameMap(String value) {
		// parameter value mapping list should be semicolon-delimited
		String[] mappings = value.split(";");
		Map<String, String> fileMap =
			new HashMap<String, String>(mappings.length);
		// also collect the set of all target files that were mapped
		Set<String> mappedTargetFiles = new HashSet<String>(mappings.length);
		for (String mapping : mappings) {
			// each mapping should be a string with the following format:
			// <source_filename>|<target_filename>#<referenced_filename>,...
			String[] tokens = mapping.split("\\|");
			if (tokens == null || tokens.length != 2)
				throw new IllegalArgumentException(String.format(
					"\"file_mapping\" parameter value \"%s\" is invalid - " +
					"each file mapping (\"%s\") should contain two tokens " +
					"separated by a pipe (\"|\") character.", value, mapping));
			// split the mapped value to extract
			// the list of target file references
			String[] references = tokens[1].split(",");
			for (String reference : references) {
				// save this exact target:reference -> source mapping
				fileMap.put(reference, tokens[0]);
				// then also note this target file as having been mapped
				String[] files = reference.split(
					ResultFileMapper.EXTRACTED_FILE_DELIMITER);
				if (files == null || files.length != 2)
					throw new IllegalArgumentException(String.format(
						"\"result_file_mapping\" parameter value " +
						"\"%s\" is invalid - each result file reference " +
						"(\"%s\") should consist of two tokens separated " +
						"by a \"" + ResultFileMapper.EXTRACTED_FILE_DELIMITER +
						"\" character.", value, reference));
				mappedTargetFiles.add(files[0]);
			}
		}
		if (fileMap != null && fileMap.isEmpty() == false) {
			this.fileMap = fileMap;
			if (mappedTargetFiles != null &&
				mappedTargetFiles.isEmpty() == false)
				this.mappedTargetFiles = mappedTargetFiles;
		}
	}
}