package edu.ucsd.ccms.flow.action;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.Map.Entry;
import java.util.TreeSet;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import dapper.codelet.InputHandleResource;
import dapper.codelet.OutputHandleResource;
import edu.ucsd.saint.commons.xml.XmlUtils;
import edu.ucsd.saint.toolexec.AbstractVariable;
import edu.ucsd.saint.toolexec.CommandLauncher;
import edu.ucsd.saint.toolexec.ExecEnvironment;
import edu.ucsd.saint.toolexec.FileHandle;
import edu.ucsd.saint.toolexec.Variable;
import edu.ucsd.saint.toolexec.VariableFactory;

public class ToolExecAction extends Action {

	private static final Logger logger = LoggerFactory.getLogger(ToolExecAction.class);

	private String toolName;
	private Set<Element> reqStmts;
	private Set<Element> prodStmts;
	private Set<String> skipCriteria;

	public ToolExecAction(List<InputHandleResource> in, List<OutputHandleResource> out, Element params) throws Exception {
		super(in, out, params);
		printDebug(in, out);
		Element action = XmlUtils.getFirstElement(params);
		toolName = action.getAttribute("tool");
		environment.setTool(toolName);
		logger.info("Tool name=[{}]", toolName);

		reqStmts  = getRequirements(params, toolName);
		prodStmts = getProductions(params);
		skipCriteria = getSkipCriteria(action);
	}

	private static Comparator<Element> ELEMENT_ID_COMP = new Comparator<Element>(){
		// assuming that member elements are non-empty; empty-check is the callers' responsibility  
		public int compare(Element x, Element y) {
			return x.getAttribute("id").compareTo(y.getAttribute("id"));
		}		
	};
	
	public Set<Element> getRequirements(Element params, String toolName) {
		Set<Element> result = new TreeSet<Element>(ELEMENT_ID_COMP);
		Map<String, Element> 
			inStmts   = XmlUtils.getNamedMap(params, "input", "port"),
			bindStmts = XmlUtils.getNamedMap(params, "inputAsRequirement", "requirement");
		List<Element>
			objBinds  = XmlUtils.getElements(XmlUtils.getElement(params, "objects"), "bind");

		for (Element bind : bindStmts.values()) {
			String port = bind.getAttribute("port");
			String req  = bind.getAttribute("requirement");
			logger.info("Input for port [{}] is not null [{}]", port, inStmts.get(port) != null);
			Element stmt = mergeElements(
				"require", req, bind, inStmts.get(port), environment.getRequirementStmt(req));
			stmt.setAttribute("bindingType", "action");
			result.add(stmt );
		}
		for (Element bind : objBinds) {
			String req = bind.getAttribute("requirement");
			if (bind.getAttribute("tool").equals(toolName)){
				Element stmt = mergeElements(
					req, "require", bind, environment.getRequirementStmt(req));
				stmt.setAttribute("bindingType", "object");
				result.add(stmt);
			}
		}
		return result;
	}

	public Set<Element> getProductions(Element params) {
		Set<Element> result = new TreeSet<Element>(ELEMENT_ID_COMP);
		Map<String, Element>
			outStmts  = XmlUtils.getNamedMap(params, "output", "port"),
			bindStmts = XmlUtils.getNamedMap(params, "productionToOutput", "production");
		for (Element bind : bindStmts.values()) {
			String port = bind.getAttribute("port");
			String prod = bind.getAttribute("production");
			logger.info("Output for port [{}] is not null [{}]", port, outStmts.get(port) != null);
			result.add(mergeElements(
				"produce", prod, bind, outStmts.get(port), environment.getProductionStmt(prod)));
		}
		return result;
	}

	private HashSet<String> getSkipCriteria(Element action) {
		HashSet<String> result = new HashSet<String>();
		String criteria = action.getAttribute("skipWhenMissing");
		for (String target : criteria.split(" ")) {
			result.add(target);
		}
		return result;
	}

	private boolean willSkip() {
		for (Entry<String, List<String>> entry : inputHandles.entrySet()) {
			if (skipCriteria.contains(entry.getKey())
					&& entry.getValue().isEmpty())
				return true;
		}
		return false;
	}

	public void run() throws Exception {
		preClean();
		try {
			if (!willSkip()) {
				File doneFile = new File(getGlobalFolder(".info"), nodeName);
				boolean done = isDone(doneFile);
				if (!done) {
					fetchInput();
					prepareOutput(done);
					launchTool();
					if (!collectOutput())
						throw new Exception(
							"Cannot collect result successfully.");
				}
				Map<String, List<String>> handlesMap = done ? 
					pickupOutputHandles(doneFile) : prepareOutputHandles(doneFile);
				writeOutputHandles(handlesMap);
			} else
				logger.info(
					"Action [{}] is skipped due to empty parameters", nodeName);
		} finally {
			postClean();
		}
	}

	private boolean isDone(File doneFile) {
		if (!doneFile.isFile())
			return false;
		Scanner scanner = null;
		try {
			scanner = new Scanner(doneFile);
			String last = "";
			while (scanner.hasNextLine())
				last = scanner.nextLine();
			return last.equals(".done");
		} catch (Exception e) {
			logger.info(
					"Failed to read done file: " + doneFile.getAbsolutePath(),
					e);
		} finally {
			if (scanner != null)
				scanner.close();
		}
		return false;
	}

	private void fetchInput()
	throws Exception {
		logger.info("Fetching workflow activity input...");
		boolean globalFetch = ExecEnvironment.getFetchMethod();
		VariableFactory factory = environment.getFactory();
		for(Element req: reqStmts){
			String bType = req.getAttribute("bindingType");
			String port = req.getAttribute("port");
			String name = req.getAttribute("name");
			String object = req.getAttribute("object");
			if(bType.equals("action")){
				List<String> identifiers = inputHandles.get(port);
				if (identifiers != null) {
					Variable var = factory.createInputVariable(req, object, identifiers);
					boolean contentRequired = !req.getAttribute("contentRequired").equals("false");
					boolean fetchMethod = !req.getAttribute("fetchMethod").equals("link") && globalFetch;
					logger.info(String.format("Workflow activity input file " +
						"\"%s\" will be %s to the node.", var.getName(),
						fetchMethod ? "copied" : "linked"));
					if (!contentRequired)
						logger.info("\tInput [{}.{}] is not fetched", nodeName, port);
					fetchParameter(var, contentRequired, fetchMethod);
					getLocalFolder(nodeName, object);
					logger.info("\tVariable [{}] = [{}]", var.getName(), environment.evaluate(var.getName()));
				} else
					logger.info(
						"\tVariable [{}] cannot be fetched due to empty identifiers", port);
			} else { // "i.e. when source.equals("object") is true"
				if (port != null && port.startsWith("@")) {
					String argName = req.getAttribute("requirement");
					String value = environment.evaluate(port);
					factory.createArgumentVariable(argName, value);
				} else {
					factory.createPlaceHolder(name, object);
				}
			}
		}
	}

	private void launchTool() throws Exception{
		String prefix = String.format("%s.%2$tY%2$tm%2$td_%2$tH%2$tM%2$tS_%2$tL", nodeName, new Date());
		File logFolder = getGlobalFolder(".logs");
		File cmdLog  = new File(logFolder, prefix + ".cmd");
		File execLog = new File(logFolder, prefix + ".log");
		CommandLauncher launcher = 
			environment.getCommandLauncher(getLocalFolder(nodeName));

		StringBuffer buffer = new StringBuffer();
		for(String token: launcher.getCommandTokens())
			buffer.append(' ').append(token);
		String cmdLine = buffer.toString().trim();

		FileWriter writer = new FileWriter(cmdLog);
		try {
			writer.write(cmdLine);
		} finally {
			writer.close();
		}
 
		if(!launcher.validateCommands())
			throw (new Exception("Invalid commands: " + cmdLine));

		OutputStream out = new FileOutputStream(execLog, true);
		try {
			launcher.execute(out);
		} finally {
			out.close();
		}		
	}
	
	private void prepareOutput(boolean pickup) {
		VariableFactory factory = environment.getFactory();
		for (Element prod: prodStmts) {
			String object = prod.getAttribute("object");
			String modifier = prod.getAttribute("folderModifier");

			AbstractVariable var = 
				factory.createOutputVariable(prod, object);
			if (pickup)
				var.setNamingRequired(true);
			
			try {
				if (StringUtils.isEmpty(modifier) == false)
					getLocalFolder(nodeName, modifier);
				else getLocalFolder(nodeName, object);
			} catch (Throwable error) {
				logger.error(String.format(
					"Failed to prepare local folder \"%s/%s\"", nodeName,
					StringUtils.isEmpty(modifier) ? object : modifier), error);
				throw new RuntimeException(error);
			}
		}
	}

	private boolean fetchParameter(
		Variable var, boolean contentRequired, boolean fetchFile
	) throws Exception {
		// TODO: mock-up. Assume there is a folder holding action results
		try {
			File working = getLocalFolder(nodeName);
			File source = getGlobalFolder();
			for (FileHandle handle : var.getIdentifiers(null)) {
				String id = handle.getName();
				String[] names = id.split(";");
				// build map of source and destination files
				Map<File, File> fetches =
					new LinkedHashMap<File, File>(names.length);
				// de-version the destination directory, so that the tool
				// sees all versions of the group as a single collection
				for (String name : names)
					fetches.put(new File(source, name),
						stripVersion(new File(working, name), true));
				// copy, link or create dummy file for each file mapping
				if (contentRequired) {
					if (fetchFile) {
						for (File srcFile : fetches.keySet()) {
							File destFile = fetches.get(srcFile);
							logger.info(String.format("Securely copying " +
								"workflow activity input file:\n\t%s ->\n\t%s",
								srcFile.getAbsolutePath(),
								destFile.getAbsolutePath()));
							copyFileWithRetries(srcFile, destFile);
						}
					} else linkFilesWithRetries(fetches, working);
				} else for (File srcFile : fetches.keySet()) {
					File destFile = fetches.get(srcFile);
					if (destFile.exists() == false) {
						destFile.getParentFile().mkdirs();
						destFile.createNewFile();
					}
				}
			}
		} catch (Throwable error) {
			logger.error(String.format(
				"Failed to retrieve parameter [%s]", var.getName()), error);
			throw new Exception(error);
		}
		return true;
	}

	private boolean collectOutput() throws Exception {
		logger.info("Collecting workflow activity output...");
		for (Element prod: prodStmts) {
			try {
				String portName = prod.getAttribute("port");
				String objName = prod.getAttribute("object");
				String paramName = prod.getAttribute("name");
				String folderModifier = prod.getAttribute("folderModifier");

				if (portName.startsWith("@"))
					continue;

				Variable var = environment.getVariable(paramName);
/**/
StringBuffer message = new StringBuffer(
	"Examining workflow activity output for production variable \"");
message.append(paramName);
message.append("\"...\n");
if (var == null)
	message.append("Variable was not found!");
else {
	message.append("Variable was found: Name = ");
	message.append(var.getName());
	message.append(", Type = ");
	message.append(var.getType().toString());
	message.append(", Datatype = ");
	message.append(var.getDatatype());
	message.append("\n");
	java.util.Collection<edu.ucsd.saint.toolexec.FileHandle> handles =
		var.getIdentifiers(null);
	if (handles == null || handles.isEmpty())
		message.append("No file identifiers were found.");
	else {
		message.append("File identifiers:");
		for (edu.ucsd.saint.toolexec.FileHandle handle : handles) {
			message.append("\n\t");
			message.append(handle.getIndex());
			message.append(": ");
			message.append(handle.getName());
		}
	}
}
logger.info(message.toString());
/**/
				// de-version the source directory, so that the tool
				// sees all versions of this group as a single collection
				File target = stripVersion(getLocalFolder(nodeName,
					StringUtils.isEmpty(folderModifier) ? objName :
					folderModifier), false);
				var.matchContext(target);
				if (!sendParameter(var, folderModifier))
					return false;
			} catch (Exception e) {
				logger.info("Failed to collect output", e);
				throw e;
			}
		}
		return true;
	}

	private Map<String, List<String>> prepareOutputHandles(File doneFile)
			throws Exception {
		Map<String, List<String>> result = new HashMap<String, List<String>>();
		PrintWriter writer = null;
		try {
			writer = new PrintWriter(doneFile);
			for (Element prod: prodStmts) {
				String portName = prod.getAttribute("port");
				if (portName.startsWith("@"))
					continue;
				String paramName = prod.getAttribute("name");
				logger.info("var [{}]", paramName);
				Variable var = environment.getVariable(paramName);
				List<String> handles = new LinkedList<String>();
				for (FileHandle fd : var.getIdentifiers("")) {
					String handle = fd.getIndex() + ":" + fd.getName();
					writer.println(portName + ':' + handle);
					handles.add(handle);
					logger.info("handle [{}] for var [{}]", handle, paramName);
				}
				result.put(portName, handles);
			}
			writer.println(".done");
		} finally {
			if (writer != null)
				writer.close();
		}
		return result;
	}

	private Map<String, List<String>> pickupOutputHandles(File doneFile)
			throws Exception {
		Map<String, List<String>> result = new HashMap<String, List<String>>();
		Scanner scanner = null;
		try {
			scanner = new Scanner(doneFile);
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				if (line.equals(".done"))
					break;
				String tokens[] = line.split(":", 2);
				String portName = tokens[0];
				String handle = tokens[1];
				if (!result.containsKey(portName))
					result.put(portName, new LinkedList<String>());
				result.get(portName).add(handle);
			}
		} finally {
			if (scanner != null)
				scanner.close();
		}
		return result;
	}

	private void writeOutputHandles(Map<String, List<String>> handlesMap) {
		for (String portName : handlesMap.keySet()) {
			List<OutputHandleResource> handles = outputHandles.get(portName);
			if (handles == null)
				continue;
			for (String fd : handlesMap.get(portName)) {
				for (OutputHandleResource handle : handles)
					handle.put(fd);
			}
		}
	}

	private boolean sendParameter(Variable var, String folderModifier)
	throws Exception {
		// TODO: mock-up. Assume there is a folder holding action results
		try {
			File working = getLocalFolder(nodeName, folderModifier);
			File target = getGlobalFolder();
			for (FileHandle handle : var.getIdentifiers(null)) {
				String id = handle.getName();
				for (String file : id.split(";")) {
					String srcName = StringUtils.isEmpty(folderModifier) ? 
						file :  FilenameUtils.getName(file);
					// de-version the source directory, so that the tool
					// sees all versions of this group as a single collection
					File srcFile =
						stripVersion(new File(working, srcName), true);
					File destFile = new File(target, file);
					logger.info(String.format("Securely copying workflow " +
						"activity output file:\n\t%s ->\n\t%s",
						srcFile.getAbsolutePath(), destFile.getAbsolutePath()));
					copyFileWithRetries(srcFile, destFile);
				}
			}
		} catch (IOException e) {
			logger.info("Failed to send parameter [" + var.getName() + "]", e);
			throw e;
		}
		return true;
	}

	private void preClean() throws Exception {
		clean(true);
	}

	private void postClean() throws Exception {
		clean(false);
	}

	private void clean(boolean forced) throws Exception {
		File folder = getLocalFolder(nodeName);
		try {
			if (forced || !isDebugMode())
				FileUtils.cleanDirectory(folder);
		} catch (Exception e) {
			String msg = String.format("Failed to clean folder [%s]", folder.getAbsolutePath());
			logger.info(msg, e);
			throw e;
		}
	}
	
	private File stripVersion(File file, boolean parent) {
		if (file == null)
			return null;
		// extract relevant filename
		String name = null;
		if (parent)
			name = file.getParentFile().getName();
		else name = file.getName();
		// remove version annotation, if any
		int dash = name.indexOf('-');
		if (dash > 0)
			name = name.substring(0, dash);
		// rebuild file path
		if (parent)
			return new File(
				new File(file.getParentFile().getParentFile(), name),
				file.getName());
		else return new File(file.getParentFile(), name);
	}
	
	public String toString() {
		return nodeName;
	}
}
