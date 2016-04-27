package edu.ucsd.saint.toolexec;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import edu.ucsd.saint.commons.Helpers;
import edu.ucsd.saint.commons.xml.XmlUtils;
import edu.ucsd.saint.toolexec.CommandLauncher;
import edu.ucsd.saint.toolexec.ExecEnvironment;
import edu.ucsd.saint.toolexec.FileHandle;
import edu.ucsd.saint.toolexec.Variable;
import edu.ucsd.saint.toolexec.VariableFactory;

public class ExecEnvironment {
	private static Logger logger = LoggerFactory.getLogger(ExecEnvironment.class);

	private static Map<String, Map<String, String>> valueMaps = new HashMap<String, Map<String, String>>();
	private static Map<String, String> pathVariables = new HashMap<String, String>();
	private static Map<String, Element> tools = new HashMap<String, Element>() ;
	private static Map<String, Element> commands = new HashMap<String, Element> ();
	private static Map<String, Element> datatypes = new HashMap<String, Element>() ;
	private static boolean fetchMethod = true;
	private static File javaPath = null;
	private static File toolsBase = null;

	private Map<String, String> arguments;
	private Map<String, Variable> variables;
	private VariableFactory factory;
	private Element toolStmt;
	private Element execStmt;
	private Map<String, Element> reqStmts, prodStmts;

	public static void loadToolsConfig(File toolsConfig, File base) {
		logger.info("Initializing this node's tool environment " +
			"from tool config file [{}].", toolsConfig.getAbsolutePath());
		Document tool = XmlUtils.parseXML(toolsConfig.getAbsolutePath());
		Element  root = tool.getDocumentElement();
		loadToolsConfig(root, base);
	}

	public static void loadToolsConfig(Element root) {
		// dump merged tool config to the log
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		XmlUtils.prettyPrint(root, stream);
		logger.info("Merging workflow-specific tool config " +
			"into this node's tool environment:\n{}", stream.toString());
		loadToolsConfig(root, null);
	}

	public static void loadToolsConfig(Element root, File base) {
		tools = mergeMaps(tools, XmlUtils.getNamedMap(root, "tool"));
		datatypes =
			mergeMaps(datatypes, XmlUtils.getNamedMap(root, "datatype"));
		List<Element> pathSets = XmlUtils.getElements(root, "pathSet");
		List<Element> valueMaps = XmlUtils.getElements(root, "valueMap");
		Element emtFetch = XmlUtils.getElement(root, "fetchMethod");
		if (emtFetch != null)
			fetchMethod = !emtFetch.getAttribute("value").equals("link");
		if (base != null)
			toolsBase = base;
		loadValueMaps(valueMaps);
		loadCommands();
		loadPathSets(pathSets);
	}
	
	private static void loadValueMaps(List<Element> m) {
		if (valueMaps == null)
			valueMaps = new HashMap<String, Map<String, String>>();
		for (Element mapEmt : m) {
			String mapName = mapEmt.getAttribute("name");
			Map<String, String> map = new HashMap<String, String>();
			valueMaps.put(mapName, map);
			for (Element mapping: XmlUtils.getElements(mapEmt, "mapping")) {
				String key = mapping.getAttribute("key");
				String value = mapping.getAttribute("value");
				map.put(key, value);
			}
			logger.debug("map [{}] size [{}]", mapName, valueMaps.size());
		}
	}
	
	private static void loadCommands() {
		if (commands == null)
			commands = new HashMap<String, Element>();		
		for (Entry<String, Element> toolEntry : tools.entrySet()) {
			String name = toolEntry.getKey();
			Element command =
				XmlUtils.getElement(toolEntry.getValue(), "execution");
			// if this tool command is already present, overwrite it with
			// the new command, but preserve its "path" and "directory"
			// attributes, since they might have been set previously
			if (commands.containsKey(name)) {
				Element oldCommand = commands.get(name);
				String path = oldCommand.getAttribute("path");
				if (path != null && path.equals("") == false)
					command.setAttribute("path", path);
				String directory = oldCommand.getAttribute("directory");
				if (directory != null && directory.equals("") == false)
					command.setAttribute("directory", directory);
			}
			commands.put(name, command);
		}
	}
	
	private static void loadPathSets(List<Element> pathSets) {
		javaPath = null;
		if (pathVariables == null)
			pathVariables = new HashMap<String, String>();
		for (Element set : pathSets) {
			String defaultBase = toolsBase.getAbsolutePath();
			String actualBase =
				FilenameUtils.concat(defaultBase, set.getAttribute("base"));
			for (Element toolPath: XmlUtils.getElements(set, "toolPath")) {
				String tool = toolPath.getAttribute("tool");
				String path = toolPath.getAttribute("path");
				String dirOption = toolPath.getAttribute("execDirectory");
				path = updateToolPath(actualBase, path);
				assertContaining(commands, tool,
					"Command for tool [{}] cannot be found");
				Element command = commands.get(tool);
				if (command != null) {
					command.setAttribute("path", path);
					if (dirOption.equals("tool"))
						command.setAttribute(
							"directory", new File(path).getParent());
				}
			}
			Element emtJavaPath = XmlUtils.getElement(set, "javaPath");
			if (emtJavaPath != null) {
				String path = emtJavaPath.getAttribute("path");
				if (!path.isEmpty())
					javaPath = new File(FilenameUtils.concat(actualBase, path));
				if (javaPath.isDirectory())
					javaPath = new File(javaPath, "java");
				if (!javaPath.isFile())
					javaPath = null;
			}
			for (Element pathVar : XmlUtils.getElements(set, "pathVar")) {
				String name = pathVar.getAttribute("name");
				String path = pathVar.getAttribute("path");
				path = FilenameUtils.concat(actualBase, path);
				pathVariables.put(name, path);
			}
		}
	}
	
	private static String updateToolPath(String base, String path) {
		if (base == null || path == null)
			return path;
		// in the event of Java-style classpath separators, the tool base
		// must be prepended separately to each tool in the classpath
		String separator = null;
		if (path.contains(":"))
			separator = ":";
		else if (path.contains(";"))
			separator = ";";
		if (separator == null)
			return FilenameUtils.concat(base, path);
		else {
			String[] parts = path.split(separator);
			StringBuffer updatedPath = new StringBuffer();
			for (String part : parts) {
				updatedPath.append(FilenameUtils.concat(base, part));
				updatedPath.append(separator);
			}
			// strip off superfluous final separator character
			updatedPath.setLength(updatedPath.length() - 1);
			return updatedPath.toString();
		}
	}
	
	private static <T> Map<String, T> mergeMaps(
		Map<String, T> destination,
		Map<String, T> source
	) {
		if (source == null)
			return destination;
		else if (destination == null)
			destination = new LinkedHashMap<String, T>(source.size());
		// source values will overwrite the values in the destination map
		for (String key : source.keySet())
			destination.put(key, source.get(key));
		return destination;
	}
	
	public static File getJavaExecutable(){
		return javaPath;
	}

	public static boolean getFetchMethod(){
		return fetchMethod;
	}

	public static Map<String, String> getMap(String mapAgainst){
		return valueMaps.get(mapAgainst);
	}

	public ExecEnvironment() {
		arguments = new HashMap<String, String>();
		variables = new HashMap<String, Variable>();
		factory = new VariableFactory(this);
	}
	
	public void setTool(String toolName) throws Exception{
		toolStmt = getToolStatement(toolName);
		execStmt = getExecStatement(toolName);
		if(toolStmt == null || execStmt == null)
			throw new Exception(String.format("Cannot find tool [%s]", toolName));
		reqStmts  = XmlUtils.getNamedMap(toolStmt, "require");
		prodStmts = XmlUtils.getNamedMap(toolStmt, "produce");		
	}

	void putVariable(String name, Variable var){
		variables.put(name, var);
		logger.info(String.format(
				"%s variable [%s] of type [%s] is created", var.getType(), var.getName(), var.getClass().getName()));		
	}
	
	public Variable getVariable(String name){
		assertContaining(variables, name, "Variable [{}] cannot be found");
		return variables.get(name);
	}
	
	public void createArgument(String name, String argument){
		arguments.put(name, argument);
	}
	
	public String getArgument(String name){
		return arguments.get(name);
	}
	
	public String getPathVar(String name){
		assertContaining(pathVariables, name,
				"Path variable [{}] cannot be found");
		return pathVariables.get(name);
	}
	
	private Element getToolStatement(String tool) {
		assertContaining(tools, tool, "Tool statement for tool [{}] cannot be found");
		return tools.get(tool);
	}


	private Element getExecStatement(String tool){
		assertContaining(commands, tool, "Execution statement for tool [{}] cannot be found");
		return commands.get(tool);
	}

	Element getDataTypeDecl(String typeName) {
		assertContaining(datatypes, typeName, "Datatype [{}] cannot be found");
		return datatypes.get(typeName);
	}

	Element getToolStatement(){
		return toolStmt;
	}
	
	Element getExecStatement(){
		return execStmt;
	}

	public Collection<FileHandle> getIdentifiers(String token) { 
		int first = token.indexOf('.');
		String id = (first == -1)? token : token.substring(0, first);
		String remaining = (first == -1)? "" : token.substring(first + 1);
		Variable var = getVariable(id);
		logger.info("TOKEN [{}], ID [{}]", token, id);
		logger.info("var is not null [{}]", var != null);
		return (var == null) ? 
				new LinkedList<FileHandle>() :
				var.getIdentifiers(remaining);
	}

	public String evaluate(String id) {
		String result = null;
		if (id.equals("@@random"))
			result = Helpers.getUUID(false);
		// valueRef tokens starting with "#" refer to literal strings
		else if (id.startsWith("#"))
			result = id.substring(1);
		// valueRef tokens starting with "@" refer to parameters from params.xml
		else if (id.startsWith("@"))
			result = getArgument(id.substring(1));
		// valueRef tokens not starting with a special character
		// refer to the tool's declared require/produce variables
		else {
			int first = id.indexOf('.');
			String token = (first == -1) ? id : id.substring(0, first);
			String remaining = (first == -1) ? "" : id.substring(first + 1);
			Variable var = getVariable(token);
			result = (var == null) ? "" : var.evaluate(remaining);
		}
		logger.info("\tEVAL({})={}", id, result);
		return result; 
	}


	public String evaluate(String id, String base) {
		String result = evaluate(id);
		if(!id.startsWith("@") && base != null)
			return base + "/" + result;		
		return result;
	}

	public VariableFactory getFactory(){
		return factory;
	}
	
	public CommandLauncher getCommandLauncher(File dataFolder){
		return new CommandLauncher(this, dataFolder);
	}
	
	public Element getRequirementStmt(String req){
		assertContaining(reqStmts, req, "Requirement [{}] cannot be found");		
		return reqStmts.get(req);
	}
	
	public Element getProductionStmt(String prod){
		assertContaining(prodStmts, prod, "Production [{}] cannot be found");
		return prodStmts.get(prod);
	}
	
	protected static boolean assertContaining(Map<String, ? > map, String key, String assertion){
		boolean result = map.containsKey(key);
		if(!result)
			logger.error(assertion, key);
		return result; 
	}
}
