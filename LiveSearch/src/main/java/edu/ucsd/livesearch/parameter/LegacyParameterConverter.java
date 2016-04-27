package edu.ucsd.livesearch.parameter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsd.livesearch.task.OnDemandOperation;
import edu.ucsd.livesearch.task.Task;

public class LegacyParameterConverter
implements OnDemandOperation
{
	/*========================================================================
	 * Constants
	 *========================================================================*/
	private final Logger logger =
		LoggerFactory.getLogger(LegacyParameterConverter.class);
	
	/*========================================================================
	 * Properties
	 *========================================================================*/
	private File parameters;
	private File commands;
	
	/*========================================================================
	 * Constructor
	 *========================================================================*/
	public LegacyParameterConverter(Task task) {
		parameters = task.getPath("params/params.xml");
		commands = task.getPath("params/commands.in");
	}
	
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	public boolean execute() {
		// generate parameters file
		try {
			parameters.createNewFile();
			FileWriter writer = new FileWriter(parameters);
			String contents = generateParametersFile();
			if (contents == null)
				throw new NullPointerException(
					"Parameter file content could not be generated.");
			writer.write(contents);
			writer.close();
			return true;
		} catch (Throwable error) {
			logger.error("Generation of task parameters file FAILED",
				error);
		}
		return false;
	}
	
	public boolean resourceExists() {
		return parameters.exists();
	}
	
	public boolean resourceDated() {
		if (parameters.exists() == false || commands.exists() == false)
			return false;
		else return parameters.lastModified() < commands.lastModified(); 
	}
	
	public String getResourceName() {
		return parameters.getAbsolutePath(); 
	}
	
	public File getParametersFile() {
		return parameters;
	}
	
	/*========================================================================
	 * Convenience methods
	 *========================================================================*/
	@SuppressWarnings("unchecked")
	private String generateParametersFile() {
		Map<String, Object> params = processFile(commands);
		if (params == null)
			return null;
		else if (params.size() < 2) {
			logger.error("There were not enough parameters extracted " +
				"from source file \"commands.in\" to successfully " +
				"generate parameter file content.");
			return null;
		} else if (params.get("cysteine_protease.cysteine") == null) {
			logger.error("Parameter \"cysteine_protease.cysteine\" " +
				"could not be extracted from source file \"commands.in\".");
			return null;
		} else if (params.get("cysteine_protease.protease") == null) {
			logger.error("Parameter \"cysteine_protease.protease\" " +
				"could not be extracted from source file \"commands.in\".");
			return null;
		} else {
			// generate parameter file contents
			StringBuffer contents = new StringBuffer(
				"<?xml version=\"1.0\" encoding=\"ISO-8859-1\" ?>" +
				"\n<parameters>\n");
			// add description
			contents.append("\t<parameter name=\"desc\">");
			contents.append("[[AUTO-GENERATED]]");
			contents.append("</parameter>\n");
			// add default FDR = 0.05
			contents.append("\t<parameter name=\"FDR.FDR\">");
			contents.append("0.05</parameter>\n");
			// add default FPR = 1.0
			contents.append("\t<parameter name=\"FPR.FPR\">");
			contents.append("1.0</parameter>\n");
			for (String parameter : params.keySet()) {
				Set<String> paramSet = null;
				if (parameter.equals("ptm.custom_PTM") == false) {
					paramSet = new HashSet<String>(1);
					paramSet.add((String)params.get(parameter));
				} else paramSet =
					(Set<String>)params.get(parameter);
				for (String value : paramSet) {
					contents.append("\t<parameter name=\"");
					contents.append(parameter);
					contents.append("\">");
					contents.append(value);
					contents.append("</parameter>\n");
				}
			}
			contents.append("</parameters>");
			// return generated contents
			return contents.toString();
		}
	}
	
	private Map<String, Object> processFile(File file) {
		if (file == null || file.exists() == false) {
			logger.error("Error generating XML parameter file: " +
				"source file \"commands.in\" could not be found.");
			return null;
		}
		Map<String, Object> params = new HashMap<String, Object>(3);
		params.put("cysteine_protease.cysteine", "None");
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(file));
			String line = null;
			while ((line = reader.readLine()) != null)
				processLine(line, params);
		} catch (Throwable error) {
			logger.error(String.format("Error reading file \"%s\": %s",
				file.getAbsolutePath(), error.getMessage()));
		} finally {
			if (reader != null) try {
				reader.close();
			} catch (IOException error) {}
		}
		if (params.size() < 1)
			return null;
		else return params;
	}
	
	private static void processLine(String line, Map<String, Object> params)
	throws Exception {
		if (line == null || params == null)
			throw new IllegalArgumentException(
				"processLine was invoked with null arguments.");
		// tolerance.PM_tolerance
		if (line.contains("PMTolerance,")) {
			String[] parts = line.split(",");
			if (parts.length != 2)
				throw new IllegalArgumentException(
					"Params file contains an illegal " +
					"\"PM_tolerance\" parameter.");
			else params.put("tolerance.PM_tolerance", parts[1]);
		}
		// tolerance.Ion_tolerance
		else if (line.contains("IonTolerance,")) {
			String[] parts = line.split(",");
			if (parts.length != 2)
				throw new IllegalArgumentException(
					"Params file contains an illegal " +
					"\"Ion_tolerance\" parameter.");
			else params.put("tolerance.Ion_tolerance", parts[1]);
		}
		// cysteine_protease.protease
		else if (line.contains("protease,")) {
			String[] parts = line.split(",");
			if (parts.length != 2)
				throw new IllegalArgumentException(
					"Params file contains an illegal " +
					"\"protease\" parameter.");
			else params.put("cysteine_protease.protease", parts[1]);
		}
		// instrument.instrument
		else if (line.contains("instrument,")) {
			String[] parts = line.split(",");
			if (parts.length != 2)
				throw new IllegalArgumentException(
					"Params file contains an illegal " +
					"\"instrument\" parameter.");
			else params.put("instrument.instrument", parts[1]);
		}
		// ptm.mods
		else if (line.contains("mods,")) {
			String[] parts = line.split(",");
			if (parts.length != 2)
				throw new IllegalArgumentException(
					"Params file contains an illegal \"mods\" parameter.");
			else params.put("ptm.mods", parts[1]);
		}
		// other PTMs
		// TODO: support PTMs other than fixed custom PTMS
		else if (line.contains("fix")) {
			String[] parts = line.split(",");
			if (parts.length != 4)
				throw new IllegalArgumentException(
					"Params file contains an illegal \"mod\" parameter.");
			else {
				String mass = Double.toString(Double.parseDouble(parts[1]));
				String aminoAcids = parts[2];
				if (aminoAcids.equals("C")) {
					double mod = Math.floor(Double.parseDouble(mass));
					if (mod == 57.0)
						params.put("cysteine_protease.cysteine", "c57");
					else if (mod == 58.0)
						params.put("cysteine_protease.cysteine", "c58");
					else if (mod == 99.0)
						params.put("cysteine_protease.cysteine", "c99");
					else addMod(aminoAcids, mass, params);
				} else {
					if (aminoAcids.contains("*"))
						addMod("*", mass, params);
					else for (int i=0; i<aminoAcids.length(); i++)
						addMod(
							Character.toString(aminoAcids.charAt(i)),
							mass, params
						);
				}
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	private static void addMod(String aminoAcid, String mass,
		Map<String, Object> params)
	throws Exception {
		if (aminoAcid == null || mass == null || params == null)
			throw new IllegalArgumentException(
				"addMod was invoked with null arguments.");
		Set<String> mods = (Set<String>)params.get("ptm.custom_PTM");
		if (mods == null)
			mods = new HashSet<String>();
		mods.add(String.format("%s,%s,fix", mass, aminoAcid));
		params.put("ptm.custom_PTM", mods);
	}
}
