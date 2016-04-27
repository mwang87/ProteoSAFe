package edu.ucsd.ccms.flow.action;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import dapper.codelet.InputHandleResource;
import dapper.codelet.OutputHandleResource;
import dapper.codelet.Resource;
import edu.ucsd.saint.commons.IOUtils;
import edu.ucsd.saint.commons.SaintFileUtils;
import edu.ucsd.saint.commons.xml.Wrapper;
import edu.ucsd.saint.commons.xml.XmlUtils;
import edu.ucsd.saint.toolexec.ExecEnvironment;

public class Action {
	
	private static final Logger logger = LoggerFactory.getLogger(Action.class); 

	protected String nodeName;
	protected String actionName;
	protected String taskName;
	protected Map<String, List<String>> inputHandles;
	protected Map<String, List<OutputHandleResource>> outputHandles;
	protected static ExecEnvironment environment;

	private static File localStorage;
	private static File globalStorage;
	private static File debugStorage;
	private static boolean debugMode = false;
	private static String fetchMethod;

	static{
		File toolsConfig = new File(getConfigBase(), "tool.xml");
		logger.info("toolConfig {}, toolBase {}", toolsConfig.getAbsolutePath(), getToolsBase().getAbsolutePath());
		if(toolsConfig.exists())
			ExecEnvironment.loadToolsConfig(toolsConfig, getToolsBase());
	}
	
	public Action(
		List<InputHandleResource> in, List<OutputHandleResource> out,
		Element params
	) {
		Element action = XmlUtils.getFirstElement(params);
		nodeName = params.getAttribute("id");
		actionName = action.getAttribute("name");
		environment = new ExecEnvironment();
		
		// incorporate workflow-specific tool specification, if present
		Element tool = XmlUtils.getElement(params, "toolset");
		if (tool != null)
			ExecEnvironment.loadToolsConfig(tool);
		
		loadArguments(params);
		taskName = environment.evaluate("@task");
		
		loadStorageSettings();
		inputHandles = getInputHandles(in);
		outputHandles = getOutputHandles(out);
		loadParamsFile();
	}

	private static void loadStorageSettings(){
		File     configPath = new File(getConfigBase(), "own.xml");
		Document doc   = XmlUtils.parseXML(configPath.getAbsolutePath());
		String   dbg   = XmlUtils.getElement(doc, "debug-mode").getAttribute("value");
		String   gpath = XmlUtils.getElement(doc, "global-storage").getAttribute("path");
		String   dpath = XmlUtils.getElement(doc, "debug-storage").getAttribute("path");
		String   lpath = XmlUtils.getElement(doc, "local-storage").getAttribute("path");
		Element  fetch = XmlUtils.getElement(doc, "fetchMethod");
		
		debugMode = (dbg == null) || (dbg.equals("true"));
		globalStorage = new File(gpath);
		debugStorage = new File(dpath);
		localStorage = debugMode ? debugStorage : new File(lpath);
		globalStorage.mkdirs();
		localStorage.mkdirs();
		if (fetch != null)
			fetchMethod = fetch.getAttribute("value");
		if (fetchMethod == null)
			fetchMethod = "copy";

		logger.info("\tDebugMode ={}", dbg);
		logger.info("\tLocal     ={}", localStorage);
		logger.info("\tGlobal    ={}", globalStorage);
	}


	protected static File getConfigBase(){
		File current = new File(System.getProperty("user.dir"));
		String path = System.getProperty("ccms.config.path");
		return (path != null) ? new File(path) : new File(current, "conf");
	}

	private static File getToolsBase() {
		// first check the system property, if specified
		String base = System.getProperty("ccms.tool.path");
		if (base != null)
			return new File(base);
		// then check the tools-base element of own.xml
		File configPath = new File(getConfigBase(), "own.xml");
		if (configPath.canRead()) {
			Document doc = XmlUtils.parseXML(configPath.getAbsolutePath());
			Element toolsBase = XmlUtils.getElement(doc, "tools-base");
			if (toolsBase != null)
				return new File(toolsBase.getAttribute("path"));
		}
		// finally just look under the parent directory of the delegate root
		File current = new File(System.getProperty("user.dir"));
		return new File(current.getParentFile(), "tools");
	}

	private void loadArguments(Element params) {
		for (Element emt : XmlUtils.getElements(params, "argument")) {
			String name = emt.getAttribute("name");
			String value = emt.getAttribute("value");
			environment.createArgument(name, value);
		}
	}

	private static HashMap<String, List<String>>
		getInputHandles(List<InputHandleResource> inputs) {
		HashMap<String, List<String>> result = new HashMap<String, List<String>>();
		String branchKnob = null;
		for(InputHandleResource resource: inputs){
			/* the resource name follows this convention:
			       activity_name: is_multiple ( source.port , destination.port )
			 */
			String tokens[] = resource.getName().split("[:\\(\\.,\\)]"); 
			String port = tokens[5];
			boolean multiplexed = Boolean.parseBoolean(tokens[1]);
			if(multiplexed){
				branchKnob = port;
				String handle = resource.getHandle(0);
				int pos = handle.indexOf(':');
				if(pos != -1){
					environment.createArgument("@counter", handle.substring(0, pos));
					logger.info("\targument @counter = [{}]", environment.getArgument("@counter"));
				}
			}
				
			if(!result.containsKey(port))
				result.put(port, new LinkedList<String>());
			List<String> list = result.get(port);
			for(String handle: resource)
				list.add(handle);
		}	
		for(Entry<String, List<String>> entry: result.entrySet()){
			if(entry.getKey().equals(branchKnob))
				continue;
			List<String> handles = entry.getValue();
			List<String> updated = new LinkedList<String>();
			int i = 0;
			for(String handle: handles){
				int pos = handle.indexOf(':');
				if(pos != -1)
					updated.add(i++ + ":" + handle.substring(pos + 1));
				else updated.add(i++ + ":" + handle);
			}
			handles.clear();
			handles.addAll(updated);
		}
		
		return result;
	}

	private static HashMap<String, List<OutputHandleResource>>
		getOutputHandles(List<OutputHandleResource> outputs) {
		HashMap<String, List<OutputHandleResource>> result = new HashMap<String, List<OutputHandleResource>>();
		for(OutputHandleResource resource: outputs){
			/* the resource name follows this convention:
		       activity_name: is_multiple ( source.port , destination.port )
			 */			
			String port = resource.getName().split("[:\\(\\.,\\)]")[3];
			if(!result.containsKey(port))
				result.put(port, new LinkedList<OutputHandleResource>());
			List<OutputHandleResource> list = result.get(port);
			list.add(resource);
		}
		return result;
	}

	protected void loadParamsFile() {
		// retrieve parameters file
		File paramFile = null;
		try {
			paramFile = new File(getGlobalFolder(".params"), "params.xml");
		} catch (Throwable error) {
			logger.error("Failed to retrieve .params/params.xml", error);
			throw new RuntimeException(error);
		}
		// parse and load parameters file
		if (paramFile.canRead()) try {
			Document doc = XmlUtils.parseXML(paramFile.getAbsolutePath());
			for (Element param : XmlUtils.getElements(doc, "parameter")) {
				String name = param.getAttribute("name");
				String value = param.getTextContent();
				environment.createArgument(name, value);
			}
		} catch (Throwable error) {
			logger.error("Failed to load .params/params.xml", error);
			throw new RuntimeException(error);
		}
	}

	private static File getFolder(File root, String ... args)
	throws IOException {
		File result = root;
		for (String arg : args)
			if (arg != null && !arg.isEmpty())
				result = new File(result, arg);
		if (result.exists() == false)
			createDirectoryWithRetries(result);
		return result;
	}

	protected static File getSharedFolder(String ... args)
	throws IOException {
		return getFolder(new File(globalStorage, "shared"), args);
	}

	protected File getGlobalFolder(String ... args)
	throws IOException {
		return getFolder(new File(globalStorage, taskName), args);
	}


	protected File getLocalFolder(String ... args)
	throws IOException {
		return getFolder(new File(localStorage, taskName), args);
	}
	
	public static boolean isDebugMode(){
		return debugMode;
	}
	
	public static boolean isLocal(){
		return fetchMethod != null && fetchMethod.equals("link");
	}
	
	private static Wrapper d = new Wrapper();

	protected static Element mergeElements(String tag, String id, Element ... elements) {
		Element result = mergeElements(tag, elements);
		result.setAttribute("id", id);
		return result;
	}

	protected static Element mergeElements(String tag, Element ... elements) {
		List<Node> nodes = new LinkedList<Node>();
		for(Element e: elements){
			NamedNodeMap map = e.getAttributes();
			for(int i = 0; i < map.getLength(); i++)
				nodes.add(map.item(i));
			NodeList list = e.getChildNodes();
			for(int i = 0; i < list.getLength(); i++)
				nodes.add(list.item(i));
		}
		Element result = d.E(tag, nodes.toArray(new Node[nodes.size()]));
		return result;
	}

	public void printDebug(List<InputHandleResource> in, List<OutputHandleResource> out) {
		StringBuffer buffer = new StringBuffer();
		buffer.append(String.format(
			"Node [%s], Action [%s], #in [%2d], #out=[%2d]", nodeName, actionName, in.size(), out.size()));
		for(Resource i: in){
			buffer.append(String.format("%n\tinput   [%10s]", i.getName()));
			for(String s: (InputHandleResource)i)
				buffer.append(String.format("%n\t\t\t%s", s));
		}
		for(Entry<String, List<String>> entry: inputHandles.entrySet())
			for(String handle: entry.getValue())
				buffer.append(
					String.format("%n\t %s --> %s", entry.getKey(), handle));
		for(Resource o: out)
			buffer.append(String.format("%n\toutput  [%10s]", o.getName()));
		for(Entry<String, List<OutputHandleResource>> entry: outputHandles.entrySet())
			for(OutputHandleResource handle: entry.getValue())
				buffer.append(
					String.format("%n\t %s --> %s", entry.getKey(), handle.getName()));
		
//		for(Entry<String, String> entry: data.arguments.entrySet())
//			buffer.append(String.format("%n\targument[%s]=[%s]", entry.getKey(), entry.getValue()));
//
//		for(Entry<String, Map<String, String>> entry: data.maps.entrySet()){
//			buffer.append(String.format("%n\tmap: %s", entry.getKey()));
//			for(Entry<String, String> mapping: entry.getValue().entrySet())
//				buffer.append(String.format(
//					"%n\t\t[%s]=>[%s]", mapping.getKey(), mapping.getValue()));
//		}
		logger.info(buffer.toString());
	}
	
	public void run() throws Exception {		
	}
	
	protected static void copyFileWithRetries(File source, File destination)
	throws IOException {
		int retries = 8;
		for (int i=1; i<=retries; i++) {
			try {
				IOUtils.copyFile(source, destination);
				break;
			} catch (Throwable error) {
				logger.error(String.format(
					"Error copying file (attempt %d)", i), error);
				if (i >= retries)
					throw new IOException(error);
			}
		}
	}
	
	protected static void linkFilesWithRetries(
		Map<File, File> links, File workingDirectory
	) throws IOException {
		int retries = 8;
		for (int i=1; i<=retries; i++) {
			try {
				SaintFileUtils.makeLinks(links, workingDirectory);
				break;
			} catch (Throwable error) {
				logger.error(String.format(
					"Error linking files (attempt %d)", i), error);
				if (i >= retries)
					throw new IOException(error);
			}
		}
	}
	
	protected static void createDirectoryWithRetries(File directory)
	throws IOException {
		// if the directory already exists, then we're done
		if (directory.isDirectory())
			return;
		// otherwise, create the directory
		int retries = 8;
		for (int i=1; i<=retries; i++) {
			try {
				if (directory.mkdirs())
					break;
				else throw new IOException(
					String.format("File[\"%s\"].mkdirs() returned false.",
						directory.getAbsolutePath()));
			} catch (Throwable error) {
				logger.error(String.format(
					"Error creating directory (attempt %d)", i), error);
				if (i >= retries)
					throw new IOException(error);
			}
		}
	}
}
