package edu.ucsd.livesearch.parameter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import edu.ucsd.livesearch.servlet.BaseServlet;
import edu.ucsd.livesearch.servlet.QuerySpecies;
import edu.ucsd.livesearch.servlet.QuerySpecies.Species;
import edu.ucsd.livesearch.util.Commons;
import edu.ucsd.livesearch.util.FileIOUtils;
import edu.ucsd.saint.commons.http.HttpParameters;

/**
 * This class contains static methods and web service interfaces for interacting
 * with ProteoSAFe "system resources".  These are generally defined as simple
 * server-side key-value mappings that are used to populate drop-down lists in
 * the ProteoSAFe workflow input form.
 * 
 * These mappings often correspond to actual files that will be linked to the
 * task directory (such as .fasta protein sequence database files), but in many
 * cases they are just controlled-vocabulary terms that will be recorded in
 * the task's parameter file.
 * 
 * @author Jeremy Carver
 */
@SuppressWarnings("serial")
public class ResourceManager
extends BaseServlet
{
	/*========================================================================
	 * Constants
	 *========================================================================*/
	private static final Logger logger =
		LoggerFactory.getLogger(ResourceManager.class);
	private static final Pattern MOD_NAME_PATTERN =
		Pattern.compile("\\[.*\\]\\s*\\[.*\\]\\s*(.*)");
	
	/*========================================================================
	 * Servlet methods
	 *========================================================================*/
	/**
	 * <p>Accepts and processes queries for name/label maps corresponding to
	 * specified system resources.
	 * 
	 * <p>By convention, a GET request to this servlet is assumed to be a
	 * request to read data only.  No creation, update, or deletion of
	 * server resources is handled by this method.
	 * 
	 * @param request	an {@link HttpServletRequest} object that contains
	 * 					the request the client has made of the servlet
	 * 
	 * @param response	an {@link HttpServletResponse} object that contains
	 * 					the response the servlet sends to the client
	 * 
	 * @throws IOException		if an input or output error is detected
	 * 							when the servlet handles the GET request
	 * 
	 * @throws ServletException	if the request for the GET could not be
	 * 							handled
	 */
	@Override
	protected void doGet(
		HttpServletRequest request, HttpServletResponse response
	) throws ServletException, IOException {
		// initialize properties
		try {
			initialize(request, false);
		} catch (ServletException error) {
			getLogger().error(
				"Error initializing servlet properties from request", error);
			response.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return;
		} catch (Throwable error) {
			getLogger().error(
				"Error initializing servlet properties from request", error);
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		}
		HttpParameters parameters = getParameters();
		PrintWriter out = response.getWriter();
		
		// get the indicated resource
		String resource = parameters.getParameter("resource");
		if (resource == null) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST,
				"You must specify a valid system resource name " +
				"to download a mapping of its installed content.");
			return;
		}
		
		// retrieve the specified resource data
		Map<String, String> resources = getResource(resource);
		if (resources == null || resources.isEmpty()) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND,
				"Could not find any installed content for resource \"" +
				resource + "\".");
			return;
		}
		
		// create the output XML document
		DocumentBuilderFactory factory =
			DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = null;
		try {
			builder = factory.newDocumentBuilder();
		} catch (ParserConfigurationException error) {
			logger.error("Error instantiating XML DocumentBuilder", error);
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
				"There was an error building the XML document for the " +
				"requested system resource information: " + error.getMessage());
			return;
		}
		Document document = builder.newDocument();

        // populate the XML document with the requested resource data
		Element root = document.createElement("resource");
		root.setAttribute("name", resource);
        document.appendChild(root);
        for (String name : resources.keySet()) {
        	Element item = document.createElement("item");
        	item.setAttribute("name", name);
        	item.setTextContent(resources.get(name));
        	root.appendChild(item);
        }
		
		// write the populated document to the servlet output stream
		String output = FileIOUtils.printXML(document);
		if (output == null) {
			response.sendError(
				HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
				"There was an error printing the XML document for the " +
				"requested system resource information.");
		} else {
			response.setContentType("application/xml");
			response.setContentLength(output.length());
			out.println(output);
		}
	}
	
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	public static final Map<String, String> getResource(String resource) {
		if (resource == null)
			return null;
		Map<String, String> resources = buildResourceMap(resource);
		if (resources.isEmpty())
			return null;
		else return resources;
	}
	
	public static final String getResourceName(String resourceItem) {
		if (resourceItem == null)
			return null;
		int pipe = resourceItem.indexOf('|');
		if (pipe < 0)
			return resourceItem;
		else return resourceItem.substring(0, pipe);
	}
	
	public static final String getModificationResourceName(
		String resourceItem
	) {
		String name = getResourceName(resourceItem);
		if (name == null)
			return null;
		Matcher matcher = MOD_NAME_PATTERN.matcher(name);
		if (matcher.matches())
			return matcher.group(1);
		else return name;
	}
	
	public static final Collection<String> getResourceLabels(
		String resourceItem
	) {
		if (resourceItem == null)
			return null;
		// everything after the first pipe is a label
		int pipe = resourceItem.indexOf('|');
		if (pipe < 0)
			return null;
		String label = resourceItem.substring(pipe + 1);
		// labels beyond the first are delimited by further pipes
		Collection<String> labels = null;
		String[] tokens = label.split("\\|");
		if (tokens == null || tokens.length < 1) {
			labels = new LinkedHashSet<String>(1);
			labels.add(label);
		} else {
			labels = new LinkedHashSet<String>(tokens.length);
			for (String token : tokens)
				labels.add(token);
		}
		if (labels == null || labels.isEmpty())
			return null;
		else return labels;
	}
	
	public static final String getResourceLabel(String resourceItem) {
		Collection<String> labels = getResourceLabels(resourceItem);
		if (labels == null || labels.isEmpty())
			return null;
		else return labels.iterator().next();
	}
	
	public static final String getCVResourceLabel(
		String value, String resource, Map<String, String> cache
	) {
		if (value == null)
			return null;
		else if (resource == null)
			return value;
		// handle database-backed resources separately
		if (resource.trim().equalsIgnoreCase("species"))
			return getSpeciesCVResourceLabel(value, cache);
		// make sure cache is fresh
		if (cache == null)
			cache = new LinkedHashMap<String, String>();
		if (cache.isEmpty()) {
			Map<String, String> map = getResource(resource);
			if (map != null)
				cache.putAll(map);
		}
		// attempt to resolve CV label from resource cache
		String label = null;
		if (cache != null && cache.containsKey(value)) {
			String resourceItem = cache.get(value);
			// "instrument" is special, since its "label" is the instrument
			// family it comes from - so what we really want here is its name
			if (resource.trim().equalsIgnoreCase("instrument"))
				label = getResourceName(resourceItem);
			else label = getResourceLabel(resourceItem);
			if (label == null)
				label = resourceItem;
		} else label = value;
		return label;
	}
	
	public static final Collection<String> getCVResourceLabels(
		String list, String resource, Map<String, String> cache
	) {
		if (list == null)
			return null;
		// initialize labels collection
		Collection<String> labels = null;
		String[] values = list.split(";");
		if (values == null || values.length < 1) {
			labels = new ArrayList<String>(1);
			labels.add(list);
			return labels;
		} else labels = new ArrayList<String>(values.length);
		// iterate over value items from the semicolon-delimited list
		for (String value : values) {
			String label = getCVResourceLabel(value, resource, cache);
			if (label != null)
				labels.add(label);
			else labels.add(value);
		}
		if (labels == null || labels.isEmpty())
			return null;
		else return labels;
	}
	
	public static final boolean updateResource(
		File updatedResource, String resourceType, String resource
	) {
		if (updatedResource == null || resourceType == null || resource == null)
			return false;
		else if (updatedResource.canRead() == false) {
			logger.error("Error updating resource: updated resource \"" +
				updatedResource.getAbsolutePath() + "\" cannot be accessed.");
			return false;
		}
		// retrieve the directory for this resource type
		File resourceFolder = new File(Commons.RESOURCE_PATH, resourceType);
		if (resourceFolder == null || resourceFolder.canRead() == false ||
			resourceFolder.isDirectory() == false) {
			logger.error("Error updating resource: resource type \"" +
				resourceType + "\" does not correspond to a valid resource " +
				"folder that is present and accessible on the server.");
			return false;
		}
		// retrieve the directory for this specific resource
		resourceFolder = new File(resourceFolder, resource);
		if (resourceFolder == null || resourceFolder.canRead() == false ||
			resourceFolder.isDirectory() == false) {
			logger.error("Error updating resource: resource \"" + resourceType +
				"/" + resource + "\" does not correspond to a valid resource " +
				"folder that is present and accessible on the server.");
			return false;
		}
		// copy the contents of the updated resource back to its directory
		try {
			if (updatedResource.isDirectory()) {
				for (File file : updatedResource.listFiles()) {
					if (file.isDirectory())
						FileUtils.copyDirectoryToDirectory(
							file, resourceFolder);
					else FileUtils.copyFileToDirectory(file, resourceFolder);
				}
			} else FileUtils.copyFileToDirectory(
				updatedResource, resourceFolder);
		} catch (Throwable error) {
			logger.error(
				"Error updating resource: could not copy updated resource \"" +
				updatedResource.getAbsolutePath() + "\".", error);
			return false;
		}
		return true;
	}

	/*========================================================================
	 * Convenience classes
	 *========================================================================*/
	/**
	 * Comparator to sort resource entries lexicographically
	 * by label, rather than name.
	 */
	private static class ResourceLabelComparator implements Comparator<String> {
		/*====================================================================
		 * Properties
		 *====================================================================*/
		private Map<String, String> resources;
		
		/*====================================================================
		 * Constructors
		 *====================================================================*/
		public ResourceLabelComparator(Map<String, String> resources) {
			if (resources == null)
				resources = new HashMap<String, String>();
			this.resources = resources;
		}
		
		/*====================================================================
		 * Comparator method
		 *====================================================================*/
		public int compare(String name1, String name2) {
			// compare mapped labels, rather than names
			String label1 = resources.get(name1);
			if (label1 == null)
				label1 = name1;
			String label2 = resources.get(name2);
			if (label2 == null)
				label2 = name2;
			return label1.compareTo(label2);
		}
	}
	
	/*========================================================================
	 * Convenience methods
	 *========================================================================*/
	private static Map<String, String> buildResourceMap(String resource) {
		// retrieve the resource directory
		if (resource == null)
			throw new NullPointerException(
				"Input resource name cannot be null.");
		File resourceFolder = new File(Commons.RESOURCE_PATH, resource);
		if (resourceFolder.isDirectory() == false)
			throw new IllegalArgumentException(
				String.format("Resource folder \"%s\" must be a directory.",
					resourceFolder.getAbsolutePath()));
		else if (resourceFolder.canRead() == false)
			throw new IllegalArgumentException(
				String.format("Resource folder \"%s\" must be readable.",
					resourceFolder.getAbsolutePath()));
		// if the resource contains a top-level "resource.list" file, then its
		// mapping is contained there, and we can ignore the remaining contents
		File resourceList = new File(resourceFolder, "resource.list");
		if (resourceList.canRead())
			return readResourceList(resourceList);
		// otherwise, build the mapping from the folders in this directory
		Set<File> folders = new HashSet<File>();
		for (File file : resourceFolder.listFiles())
			if (file != null && file.canRead() && file.isDirectory())
				folders.add(file);
		// iterate over all the entries in the resource folder
		// to build the resource entry name -> label map
		Map<String, String> resources =
			new HashMap<String, String>(folders.size());
		for (File folder : folders) {
			// read folder name to set up the default resource name
			String folderName = folder.getName();
			// retrieve and read the properties file for this resource
			Properties properties = new Properties();
			try {
				properties.load(new FileReader(
					new File(folder, "resource.properties")));
			}
			// if the properties file is not present or is somehow not
			// readable, just use the folder name as both the resource
			// name and label
			catch (Throwable error) {
				resources.put(folderName, folderName);
				continue;
			}
			// if this resource is meant to be hidden, skip it
			String hidden = properties.getProperty("resource.hidden");
			if (hidden != null && hidden.trim().equalsIgnoreCase("true"))
				continue;
			// retrieve resource name and label, use default if not present
			String name = properties.getProperty("resource.name");
			if (name == null || name.trim().equals(""))
				name = folderName;
			String label = properties.getProperty("resource.label");
			if (label == null || label.trim().equals(""))
				label = folderName;
			// add this resource to the map with proper labeling, if present
			resources.put(name, label);
		}
		// sort the resource map by label
		Map<String, String> sortedResources =
			new TreeMap<String, String>(new ResourceLabelComparator(resources));
		sortedResources.putAll(resources);
		// return the resource map
		if (sortedResources.size() < 1)
			return null;
		else return sortedResources;
	}
	
	/**
	 * Collects the resource mapping from a list file.  The lines of this plain
	 * text file are assumed to be simple key-value pairs in the format
	 * "key=value".
	 * 
	 * @param	resourceList	The File containing the resource list mapping
	 * @return	a Map of the key-value pairs contained in the argument file.
	 * 			This map is guaranteed to preserve the insertion order of its
	 * 			entries.
	 */
	private static Map<String, String> readResourceList(File resourceList) {
		Map<String, String> resources = new LinkedHashMap<String, String>();
		BufferedReader reader = null;
		try {
			// validate and open the resource list file for reading
			FileIOUtils.validateReadableFile(resourceList);
			reader = new BufferedReader(new FileReader(resourceList));
			// read the lines of the resource list file into a map
			String line = null;
			while (true) {
				line = reader.readLine();
				if (line == null)
					break;
				// parse the line into a key-value mapping
				String[] mapping = line.split("=");
				if (mapping.length != 2)
					continue;
				else resources.put(mapping[0], mapping[1]);
			}
		} catch (Throwable error) {
			logger.error("There was an error reading the resource list file",
				error);
			return null;
		} finally {
			try { reader.close(); }
			catch (Throwable error) {}
		}
		// return the resource mappings, or null if none were found
		if (resources.isEmpty())
			return null;
		else return resources;
	}
	
	private static final String getSpeciesCVResourceLabel(
		String value, Map<String, String> cache
	) {
		if (value == null)
			return null;
		// first check map cache, to prevent redundant database lookups
		String label = null;
		if (cache != null && cache.containsKey(value))
			label = cache.get(value);
		// otherwise look up the species in the database
		else {
			Species species = QuerySpecies.querySpecies(
				QuerySpecies.extractNCBITaxID(value));
			if (species != null) {
				label = species.getScientificName();
				// cache this found value in the map
				if (cache != null)
					cache.put(value, label);
			} else label = value;
		}
		return label;
	}
}
