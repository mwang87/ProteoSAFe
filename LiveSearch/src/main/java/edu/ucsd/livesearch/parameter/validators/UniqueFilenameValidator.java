package edu.ucsd.livesearch.parameter.validators;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;

public class UniqueFilenameValidator
extends ParameterValidator
{
	/*========================================================================
	 * Constants
	 *========================================================================*/
	private static final String NOT_UNIQUE =
		"Filenames assigned to parameter '%s' must be unique: " +
		"files '%s' and '%s' have the same base filename.";
	
	/*========================================================================
	 * Constructors
	 *========================================================================*/
	public UniqueFilenameValidator() {
		super();
		setMessage(NOT_UNIQUE);
	}
	
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	@Override
	public String validateParameter(String value) {
		if (value == null)
			return null;
		// file parameter values should be semicolon-delimited
		// lists of ProteoSAFe file descriptor strings
		String[] files = value.split(";");
		if (files == null || files.length < 1)
			return null;
		Map<String, String> filenames =
			new HashMap<String, String>(files.length);
		for (String file : files) {
			// only validate flat files; anything in a directory
			// is automatically collision-resistant
			if (file.startsWith("f.")) {
				file = file.substring(2);
				// extract the base filename of the descriptor
				String filename = FilenameUtils.getName(file);
				// if another file with this same base name was
				// already found, then the validation fails
				if (filenames.containsKey(filename))
					return String.format(getMessage(), getLabel(),
						filenames.get(filename), file);
				else filenames.put(filename, file);
			}
		}
		// if no duplicate file names were found, then the validation passes
		return null;
	}
	
}
