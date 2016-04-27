package edu.ucsd.ccms.flow.action;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import dapper.codelet.InputHandleResource;
import dapper.codelet.OutputHandleResource;

import edu.ucsd.saint.commons.SaintFileUtils;
import edu.ucsd.saint.commons.archive.Archive;
import edu.ucsd.saint.commons.archive.ArchiveUtils;
import edu.ucsd.saint.commons.archive.CompressionType;
import edu.ucsd.saint.commons.http.HttpGetAgent;
import edu.ucsd.saint.commons.http.SteadyRetry;
import edu.ucsd.saint.commons.xml.XmlUtils;
import edu.ucsd.saint.toolexec.FileHandle;
import edu.ucsd.saint.toolexec.Variable;
import edu.ucsd.saint.toolexec.VariableFactory;

public class DownloadAction extends Action{

	private static final Logger logger = LoggerFactory.getLogger(DownloadAction.class);
	
	private List<Element> downloads;
	private Element emtParams = null;

	private String baseURL = null;
	private String workflow = null;
	private CompressionType compression = CompressionType.GZIP;
	private List<Element> commonQueries = new LinkedList<Element>();
	
	private void initBinding(Element params){
		VariableFactory factory = environment.getFactory();
		workflow = params.getAttribute("workflow");
		downloads = new LinkedList<Element>();
		Map<String, Element> emtOut = XmlUtils.getNamedMap(params, "output", "port");
		Element emtBinding = XmlUtils.getElements(params, "bind").get(0);
		
		for(Element clause: XmlUtils.getChildElements(emtBinding)){
			String name = clause.getNodeName();
			if(name.equals("url"))
				baseURL = clause.getAttribute("value");
			else if(name.equals("compression"))
				compression = CompressionType.byTypeName(clause.getAttribute("type"));
			else if(name.equals("query"))
				commonQueries.add(clause);
		}
		for(Element clause: XmlUtils.getChildElements(emtBinding)){
			if(clause.getNodeName().equals("downloadParams"))
				emtParams = clause;
			else if(clause.getNodeName().equals("download")){
				String port = clause.getAttribute("port");
				Element stmt = mergeElements("download", clause, emtOut.get(port));
//				logger.info("Download statement [{}] added", port);
				downloads.add(stmt);
				factory.createOutputVariable(
					stmt, stmt.getAttribute("object"));
			}
		}
	}
	
	/*
	 * This "cache" refers to data cached in the computational back end,
	 * e.g. Proteogenomics databases.
	 */
	private Map<String, Element> cacheMap = new HashMap<String, Element>();
	
	private void initCache() {
		File configPath = new File(getConfigBase(), "shared.xml");
		if (configPath.isFile() == false)
			return;
		Document shared = XmlUtils.parseXML(configPath.getAbsolutePath());
		for (Element cache: XmlUtils.getElements(shared, "cache")) {
			String workflow = cache.getAttribute("workflow");
			String action = cache.getAttribute("action");
			String key = workflow + "." + action;
			cacheMap.put(key, cache);
			logger.info("Cache found: Key [{}], path [{}]", key, cache.getAttribute("path"));
		}
	}
	
	public DownloadAction(List<InputHandleResource> in,
			List<OutputHandleResource> out, Element params) {
		super(in, out, params);
		initBinding(params);
		initCache();
	}

	private boolean isDone(File doneFile){
		if(!doneFile.isFile()) return false;
		Scanner scanner = null;
		try{
			scanner = new Scanner(doneFile);
			String last = "";
			while(scanner.hasNextLine())
				last = scanner.nextLine();
			return last.equals(".done");
		} catch (Exception e) {
			logger.info("Failed to read done file: " + doneFile.getAbsolutePath(), e);
		}
		finally{
			if(scanner != null)
				scanner.close();
		}
		return false;
	}
	
	public void run()
	throws Exception {
		try {
			boolean done = isDone(new File(getGlobalFolder(".info"), nodeName));
			if (!done) {
				logger.info("{} Start retrieving", new Date());		
				File globalFolder = getGlobalFolder();
				if (globalFolder.isDirectory())
					FileUtils.cleanDirectory(globalFolder);
				doRetrieve();
			} else doMatch();
		} catch (Throwable error) {
			String message = "There was an error transferring this " +
				"workflow's input files to computation storage";
			logger.error(String.format("%s: %s", message, error.getMessage()));
			throw new Exception(String.format("%s.", message), error);
		}
	}

	private void doRetrieve()
	throws IOException {
		PrintWriter writer = null;
		try {
			File doneFile = new File(getGlobalFolder(".info"), nodeName);
			writer = new PrintWriter(doneFile);
			if (emtParams != null) {
				retrieveParams();
				loadParamsFile();
			}
			for (Element download: downloads)
				if (retrieveData(download) && matchContext(download))
					writer.println(download.getAttribute("object"));
			writer.println(".done");
		} catch (Throwable error) {
			throw new IOException(error);
		} finally {
			if (writer != null)
				writer.close();
		}
	}
	
	private void doMatch()
	throws FileNotFoundException {
		Scanner scanner = null;
		try {
			Set<String> downloaded = new HashSet<String>();
			// retrieve done file
			File doneFile = null;
			try {
				doneFile = new File(getGlobalFolder(".info"), nodeName);
			} catch (Throwable error) {
				logger.error(String.format(
					"Failed to retrieve .info/%s", nodeName), error);
				throw new FileNotFoundException(error.getMessage());
			}
			scanner = new Scanner(doneFile);
			while (scanner.hasNextLine())
				downloaded.add(scanner.nextLine());
			for (Element download: downloads)
				if (downloaded.contains(download.getAttribute("object")))
					matchContext(download);
		} finally {
			if (scanner != null)
				scanner.close();
		}		
	}
		
	private void retrieveParams()
	throws IOException {
		logger.info("Retrieving param file");
		File targetFolder = getGlobalFolder(".params");
		try {
			String url = composeURL(emtParams);
			logger.info("URL={}", url);
			downloadToFolder(url, targetFolder);
		} catch (Throwable error) {
			logger.error("Failed to Retrieve the parameter file", error);
			throw new IOException(error);
		}
	}
	
	private boolean retrieveData(Element stmt)
	throws IOException {
		String objName = stmt.getAttribute("object");
        File targetFolder = getGlobalFolder(objName);
        boolean needDownloading = true;
		try {
		    String key = workflow + "." + actionName;
		    logger.info("Downloading input data for workflow [{}], action [{}]",
		    	workflow, actionName);
		    if (cacheMap.containsKey(key)) {
		    	Element cache = cacheMap.get(key);
		    	String cacheBase = cache.getAttribute("path");
		    	File path = new File(cacheBase, objName);
		    	if (path.isDirectory()) {
		    		targetFolder.mkdirs();
		    		for (File file: path.listFiles())
		    			SaintFileUtils.makeLink(file,
		    				new File(targetFolder, file.getName()));
		    		logger.info("Found cache [{}] for [{}]",
		    			path.getAbsolutePath(), targetFolder.getAbsolutePath());
		    		needDownloading = false;
		    	}
		    }
		    if (needDownloading) {
		    	if (isLocal())
		    		linkToFolder(stmt, targetFolder);
		    	else downloadToFolder(composeURL(stmt), targetFolder);
		    }
		    return true;
		} catch (Throwable error) {
			logger.error("Failed to retrieve data to [{}]",
				targetFolder.getAbsolutePath(), error);
			throw new IOException(error);
		}
	}

	private boolean matchContext(Element stmt) {
		String objName = stmt.getAttribute("object");
		String portName = stmt.getAttribute("port");
		// retrieve target folder
		File targetFolder = null;
		try {
	        targetFolder = getGlobalFolder(objName);
		} catch (Throwable error) {
			logger.error(String.format(
				"Failed to retrieve global folder \"%s\"", objName), error);
			return false;
		}
		try {
	        Variable var = environment.getVariable(objName);
	        List<OutputHandleResource> outputs = outputHandles.get(portName);
	        var.matchContext(targetFolder);
			for (FileHandle id: var.getIdentifiers(""))
				for(OutputHandleResource resource: outputs)
					resource.put(id.getIndex() + ":" + id.getName());
			return true;
		} catch (Throwable error) {
			logger.info("Failed to match data to [{}]",
				targetFolder.getAbsolutePath(), error);
			return false;
		}
	}
	
	private String composeURL(Element emtDownload) {
		StringBuffer buffer = new StringBuffer(baseURL);
		buffer.append("?compression=");
		buffer.append(compression.toString());
		List<Element> queries = XmlUtils.getElements(emtDownload, "query");
		queries.addAll(commonQueries);
		for (Element query: queries) {
			String name = query.getAttribute("name");
			String valueRef = query.getAttribute("valueRef");
			String value = valueRef.isEmpty() ?
				query.getAttribute("value") : environment.evaluate(valueRef);
			// attempt to URL-encode both components of this parameter
			try {
				name = URLEncoder.encode(name, "UTF-8");
				value = URLEncoder.encode(value, "UTF-8");
			} catch (Throwable error) {}
			buffer.append(String.format("&%s=%s", name, value));
		}
		return buffer.toString();
	}
	
	private void downloadToFolder(String url, File target)
	throws IOException {
		try {
			CheckedInputStream input = null;
			HttpGetAgent agent = new HttpGetAgent(new SteadyRetry(120000, 30));
			try {
				HttpEntity entity = agent.execute(url);
				// compute checksum while downloading
				input =
					new CheckedInputStream(entity.getContent(), new CRC32());
				Archive archive =
					ArchiveUtils.loadArchive(compression, input);
				ArchiveUtils.extract(archive, target);
				input.close();
				long clientChecksum = input.getChecksum().getValue();
				logger.info(String.format("Computed client-side checksum " +
					"for archive [%s]: %s", url, clientChecksum));
				// compare to server checksum
				String serverChecksum = agent.getHeader("CRC32-Checksum");
				if (serverChecksum == null)
					logger.info("Server-side checksum is not present.");
				else try {
					if (Long.parseLong(serverChecksum) != clientChecksum)
						logger.info("Server-side checksum [{}] does not match.",
							serverChecksum);
				} catch (NumberFormatException error) {
					logger.error("Server-side checksum could not be parsed.",
						error);
				}
			} finally {
				if (input != null)
					input.close();
				agent.close();
			}
		} catch (Throwable error) {
        	logger.error(String.format(
        		"Failed to download archive from [%s]", url), error);
        	throw new IOException(error);
        }
	}
	
	private void linkToFolder(Element download, File target)
	throws IOException {
		// get the username and task ID
		String username = null;
		String taskID = null;
		for (Element query : commonQueries) {
			String name = query.getAttribute("name");
			if (name != null) {
				String valueRef = query.getAttribute("valueRef");
				if (valueRef != null) {
					String value = environment.evaluate(valueRef);
					if (name.equals("user"))
						username = value;
					else if (name.equals("task"))
						taskID = value;
				}
			}
		}
		// verify the username
		if (username == null)
			throw new IOException(
				"Could not find username in download binding.");
		// verify the task ID
		else if (taskID == null)
			throw new IOException(
				"Could not find task ID in download binding.");
		// retrieve and verify the front-end task directory
		// TODO: this is a bad hack
		String[] paths = {
			"/data/ccms-data/tasks",
			"/data/dev-data/dev1/tasks",
			"/data/dev-data/dev2/tasks",
			"/data/dev-data/internal/tasks",
			"data/tasks"
		};
		File taskFolder = null;
		for (String path : paths) {
			taskFolder = new File(path + "/"+ username + "/" + taskID);
			if (taskFolder.isDirectory())
				break;
		}
		if (taskFolder == null || taskFolder.isDirectory() == false)
			throw new IOException(String.format(
				"Could not find front-end folder for task \"%s\", " +
				"belonging to user \"%s\".", taskID, username));
		// link each download resource to the target folder
		try {
			List<Element> queries = XmlUtils.getElements(download, "query");
			for (Element query : queries) {
				String type = query.getAttribute("name");
				if (type != null && type.equals("resource")) {
					// find source folder for this resource
					String valueRef = query.getAttribute("valueRef");
					String source = valueRef.isEmpty() ?
						query.getAttribute("value") :
						environment.evaluate(valueRef);
					File sourceFolder = new File(taskFolder, source);
					// if this folder doesn't exist, then this task doesn't
					// have this resource - not necessarily an error
					if (sourceFolder.isDirectory() == false)
						break;
					// build map of source file -> target file links
					File[] sourceFiles = sourceFolder.listFiles();
					Map<File, File> fetches =
						new LinkedHashMap<File, File>(sourceFiles.length);
					for (File sourceFile : sourceFiles)
						fetches.put(sourceFile,
							new File(target, sourceFile.getName()));
					// make links for all the mapped files
					SaintFileUtils.makeLinks(fetches, getLocalFolder(nodeName));
				}
			}
		} catch (Throwable error) {
			throw new IOException(error);
		}
	}
}
