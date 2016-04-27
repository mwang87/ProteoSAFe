package edu.ucsd.livesearch.parameter.processors;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsd.livesearch.parameter.ParameterProcessor;
import edu.ucsd.livesearch.storage.ResourceAgent;
import edu.ucsd.livesearch.storage.ResourceManager;
import edu.ucsd.livesearch.task.Task;
import edu.ucsd.livesearch.task.TaskBuilder;

public class SpectrumProcessor
implements ParameterProcessor
{
	/*========================================================================
	 * Constants
	 *========================================================================*/
	@SuppressWarnings("unused")
	private static final Logger logger =
		LoggerFactory.getLogger(SpectrumProcessor.class);
	// TODO: Adding MS-Align's special format here is a hack
	private static final Set<String> LEGAL_SPECTRUM_EXTENSIONS =
		new HashSet<String>(Arrays.asList("mzxml", "mgf", "dta", "pkl",
			"msalign"));
	private static final String FLOAT_PATTERN =
		"(\\s*+[+-]?+\\d++(\\.\\d++)?+\\s*+)";
	private static final Pattern DTA_PATTERN =
		Pattern.compile(FLOAT_PATTERN + "{2}+");
	private static final Pattern PKL_PATTERN =
		Pattern.compile(FLOAT_PATTERN + "{2,3}+");
	
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	/**
	 * Validates the spectrum file-related parameters submitted by the user
	 * from the CCMS ProteoSAFe web application input form, and stages the
	 * associated files to the appropriate task directories.
	 * 
	 * @param builder	an {@link TaskBuilder} object representing the building
	 * 					state of the task whose parameters are to be processed
	 * 
	 * @return			the {@link List} of error messages encountered
	 * 					during processing,
	 * 					null if processing completed successfully
	 */
	public List<String> processParameters(TaskBuilder builder) {
		if (builder == null)
			return null;
		Task task = builder.getTask();
		if (task == null)
			return null;
		
		// get server-side files
		String specOnServer = builder.getFirstParameterValue("spec_on_server");
		ResourceAgent agent = ResourceManager.assignAgent(task);
		List<String> spectra = new ArrayList<String>();
		List<String> errors = new LinkedList<String>();
		try {
			spectra.addAll(agent.aquireOnServer(
				specOnServer, "spectrum", task.getPath("spec/")));
		} catch (Throwable error) {
			errors.add("There was a problem assigning your input files " +
				"of type [spectrum] to this workflow task.");
		}
		
		// validate all user-selected files
		if (spectra.isEmpty())
			errors.add("Spectrum file unspecified.");
		else {
			File spectrumFolder = task.getPath("spec/");
			for (String spectrum : spectra) {
				String realName = task.queryOriginalName(spectrum);
				String error = validateSpectrumFile(
					new File(spectrumFolder, spectrum), realName);
				if (error != null)
					errors.add(error);
				// add file mapping to parameters
				builder.setParameterValue(
					"spectrum_file_mapping", spectrum + "|" + realName);
			}
		}
		
		if (errors.size() < 1)
			return null;
		else return errors;
	}
	
	public static String validateSpectrumFile(File file, String realname) {
		String filename = file.getAbsolutePath();
		if (!file.exists())
			return String.format("File [%s] does not exist", realname);
		try {
			String code = null;
			String extension = FilenameUtils.getExtension(realname);
			if (extension.equalsIgnoreCase("dta") && !validateDTA(filename))
				code = String.format("File [%s] is corrupted", realname);
			if (extension.equalsIgnoreCase("pkl") && !validatePKL(filename))
				code = String.format("File [%s] is corrupted", realname);
			if (!LEGAL_SPECTRUM_EXTENSIONS.contains(extension.toLowerCase()))
				code = String.format("Unsupported spectrum file format [%s]",
					realname);
//			if(code != null) file.delete();
			return code;
		} catch (Throwable error) {
			return String.format(
				"Failed to validate [%s] due to unknown reason",
				file.getName());
		}
	}
	
	/*========================================================================
	 * Convenience methods
	 *========================================================================*/
	/**
	 * Validate .dta file, in which each line consists of two space-separated
	 * float numbers. See also:
	 * http://pfind.jdl.ac.cn/pfind/Help/first_user.HTML#dta
	 */
	private static boolean validateDTA(String filename) {
		Scanner scanner = null;
		try {
			scanner = new Scanner(new BufferedReader(new FileReader(filename)));
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				if (!DTA_PATTERN.matcher(line).matches())
					return false;
			}
			return true;			
		} catch (Throwable error) {
			return false;			
		} finally {
			try { scanner.close(); } catch (Throwable error) {}
		}
	}
	
	/**
	 * Validate .pkl format file, in which each line consists of two or three
	 * space-separated float numbers. See also:
	 * http://pfind.jdl.ac.cn/pfind/Help/first_user.HTML#pkl
	 */
	private static boolean validatePKL(String filename) {
		Scanner scanner = null;
		try {
			scanner = new Scanner(new BufferedReader(new FileReader(filename)));
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				if (!PKL_PATTERN.matcher(line).matches() && line.length() > 0)
					return false;
			}
			return true;
		} catch (Throwable error) {
			return false;			
		} finally {
			try { scanner.close(); } catch (Throwable error) {}
		}
	}
}
