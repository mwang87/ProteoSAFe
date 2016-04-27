package edu.ucsd.saint.toolexec;


import java.io.File;
import java.io.OutputStream;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import edu.ucsd.saint.commons.xml.XmlUtils;
import edu.ucsd.saint.toolexec.CommandLauncher;
import edu.ucsd.saint.toolexec.ExecEnvironment;
import edu.ucsd.saint.toolexec.FileHandle;

public class CommandLauncher {
	public static Logger logger = LoggerFactory.getLogger(CommandLauncher.class);
	
	private Element toolExecStmt;
	private File executableFolder, dataFolder;
	private List<String> commandTokens;
	private ExecEnvironment environment;
	/* when the tool is executed where the binary is located, it is unsafe if full paths of data parameters are unspecified */
	private boolean appendDataFolder;
	

	CommandLauncher(ExecEnvironment environment, File dataFolder) {
		this.environment = environment;
		toolExecStmt = environment.getExecStatement();
		String path = toolExecStmt.getAttribute("directory");
		this.executableFolder = resolveFolder(path);
		this.dataFolder = resolveFolder(dataFolder);
		this.appendDataFolder = (executableFolder != null) && (dataFolder != null);
		logger.info("ExecutableFolder=[{}], DataFolder=[{}]", executableFolder, dataFolder );

		commandTokens = new LinkedList<String>();
		commandTokens.addAll(getEnvTokens());
		commandTokens.addAll(prepareToolTokens());
	}
	
	private List<String> getEnvTokens() {
		LinkedList<String> result = new LinkedList<String>();
		String executableEnv = toolExecStmt.getAttribute("env");
		String executablePath = toolExecStmt.getAttribute("path");
		if (executableEnv.matches("python|perl|bash")) {
			result.add(executableEnv);
		} else if (executableEnv.matches("java")) {
			File jvmExecutable = ExecEnvironment.getJavaExecutable();
			logger.info("jvm at [{}]", jvmExecutable);
			result.add((jvmExecutable != null) ? jvmExecutable.getAbsolutePath() : "java");
		}
		for (Element arg : XmlUtils.getElements(toolExecStmt, "envArg")) {
			String value = arg.getAttribute("value");
			String option = arg.getAttribute("option");
			if (!option.isEmpty())
				result.add('-' + option);
			if (!value.isEmpty())
				result.add(value);
		}
		if (executableEnv.equals("java")) {
			result.add("-cp");
			result.add(executablePath);
			Element mainClass = XmlUtils.getElement(toolExecStmt, "mainClass");
			if (mainClass != null)
				result.add(mainClass.getAttribute("name"));
		}
		else result.add(executablePath); // else it's not java, so we can just add the executable/scripts to the token list		
		return result;
	}

	public List<String> prepareToolTokens() {
		LinkedList<String> result = new LinkedList<String>();
		for (Element arg : XmlUtils.getElements(toolExecStmt, "arg")) {
			String valueRef = arg.getAttribute("valueRef");
			String pathRef = arg.getAttribute("pathRef");
			String value = arg.getAttribute("value");
			String option = arg.getAttribute("option");
			String forEach = arg.getAttribute("foreach");
			String mapAgainst = arg.getAttribute("mapAgainst");
			String ifEmpty = arg.getAttribute("ifEmpty");

			Collection<String> tokens = new LinkedList<String>();
			if (!forEach.isEmpty())
				tokens.addAll(evaluateForEach(forEach));
			else if (!value.isEmpty())
				tokens.add(value);
			else if (!valueRef.isEmpty())
				tokens.add(evaluateReference(valueRef));

			if (!mapAgainst.isEmpty())
				tokens = mapAgainst(mapAgainst, tokens);
			if (hasEmptyValues(tokens) && ifEmpty.equals("skip"))
				continue;
			// get argument convention
			String argConvention = toolExecStmt.getAttribute("argConvention");
			// process all parameter value tokens
			for (String t : tokens) {
				// if "concatenated" argConvention is specified,
				// concatenate values to options
				if (argConvention != null &&
					argConvention.equalsIgnoreCase("concatenated"))
					result.add(option.isEmpty() ? t : '-' + option + t);
				// otherwise, add option and value separately as normal
				else {
					if (!option.isEmpty())
						result.add('-' + option);
					result.add(t);
				}
			}
			if (tokens.isEmpty() && !option.isEmpty())
				result.add('-' + option);
			if (!pathRef.isEmpty()) {
				result.add(evaluatePath(pathRef));
			}
		}
		return result;
	}

	private List<String> evaluateForEach(String foreach) {
		List<String> values = new LinkedList<String>();
		for (FileHandle handle : environment.getIdentifiers(foreach)) {
			String val = handle.getName();
			logger.info("FOREACH [{}]: [{}]", foreach , val);
			if (appendDataFolder)
				val = FilenameUtils.concat(dataFolder.getAbsolutePath(), val);
			values.add(val);
		}
		return values;
	}

	private List<String> mapAgainst(String mapAgainst,
			Collection<String> values) {
		List<String> mapsTo = new LinkedList<String>();
		Map<String, String> map = ExecEnvironment.getMap(mapAgainst);
		for (String key : values)
			mapsTo.add(map.get(key));
		return mapsTo;
	}

	private String evaluateReference(String ref) {
		if (ref == null)
			return null;
		// split reference string into tokens separated by plus signs ("+")
		String[] tokens = ref.split("\\+");
		// concatenate tokens together to form a dereferenced value string
		StringBuffer concatenated = new StringBuffer();
		for (String token : tokens) {
			String value = environment.evaluate(token);
			if (executableFolder != null && dataFolder != null)
				value = FilenameUtils.concat(
					dataFolder.getAbsolutePath(), value);
			if (value != null)
				concatenated.append(value);
		}
		return concatenated.toString();
	}
	
	private String evaluatePath(String path) {
		if (path == null)
			return null;
		// determine system file path separator token
		char separator;
		try {
			separator = System.getProperty("file.separator").charAt(0);
		} catch (Throwable error) {
			separator = '/';
		}
		// split path string into tokens separated by plus signs ("+")
		String[] tokens = path.split("\\+");
		// concatenate tokens together to form a file path
		StringBuffer concatenated = new StringBuffer();
		for (String token : tokens) {
			if (token.startsWith("@") || token.startsWith("#"))
				concatenated.append(environment.evaluate(token));
			else concatenated.append(environment.getPathVar(token));
			if (concatenated.charAt(concatenated.length() - 1) != separator)
				concatenated.append(separator);
		}
		// remove trailing path separator
		concatenated.setLength(concatenated.length() - 1);
		return concatenated.toString();
	}

	public String[] getCommandTokens(){
		return commandTokens.toArray(new String[commandTokens.size()]);
	}
	
	public boolean validateCommands() throws Exception {
		for (String token : commandTokens)
			if (token == null)
				return false;
		return true;
	}

	public void execute(OutputStream out) throws Exception {
		ProcessBuilder builder = new ProcessBuilder(commandTokens);
		builder.redirectErrorStream(true);
		if(executableFolder != null)
			builder.directory(executableFolder);
		else if(dataFolder != null)
			builder.directory(dataFolder);
		Process proc = builder.start();
		Scanner scanner = new Scanner(proc.getInputStream());
		StringBuffer buffer = new StringBuffer();
			buffer = new StringBuffer();
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
					if (out != null) {
						out.write(line.getBytes());
						out.write('\n');
					}
				buffer.append(line).append('\n');
			}
			proc.waitFor();
		int code = proc.exitValue();
		buffer.append(
				"Tool execution terminates abnormally with exit code [")
				.append(code).append("]");
		logger.info("Tool execution terminates with exit code [{}]", code);
		if (code != 0)
			throw new Exception(buffer.toString());
	}

	private static boolean hasEmptyValues(Collection<String> values) {
		boolean result = false;
		for (String v : values)
			result = result || StringUtils.isEmpty(v);
		return result;
	}

	private static File resolveFolder(String path){
		if(StringUtils.isEmpty(path)) return null;
		File result = new File(path);
		return result.isDirectory() ? result : null;
	}

	private static File resolveFolder(File path){
		if(path == null) return null;
		return path.isDirectory() ? path : null;
	}
}
