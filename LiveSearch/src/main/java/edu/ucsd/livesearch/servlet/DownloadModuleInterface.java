package edu.ucsd.livesearch.servlet;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsd.livesearch.util.FileIOUtils;
import edu.ucsd.saint.commons.WebAppProps;
import edu.ucsd.saint.commons.http.HttpParameters;

@SuppressWarnings("serial")
public class DownloadModuleInterface
extends BaseServlet
{
	/*========================================================================
	 * Constants
	 *========================================================================*/
	@SuppressWarnings("unused")
	private static final Logger logger =
		LoggerFactory.getLogger(DownloadModuleInterface.class);
	
	/*========================================================================
	 * Servlet methods
	 *========================================================================*/
	/**
	 * <p>Accepts and processes queries for workflow interface specification
	 * documents.
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
		
		// get the indicated module name
		String module = parameters.getParameter("module");
		if (module == null) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST,
				"You must specify a valid interface module " +
				"name to download its content files.");
			return;
		}
		
		// get the indicated module ID
		String id = parameters.getParameter("id");
		if (id == null) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST,
				"You must specify a valid interface module " +
				"ID to download its content files.");
			return;
		}
		
		// get the indicated module type
		String type = parameters.getParameter("type");
		if (type == null) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST,
				"You must specify a valid interface module type " +
				"to download the appropriate file for this module.");
			return;
		}
		
		// get the indicated module file name or extension
		String filename = parameters.getParameter("file");
		String extension = parameters.getParameter("extension");
		if (filename == null && extension == null) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST,
				"You must specify a valid interface module file name or " +
				"extension to download the appropriate file for this module.");
			return;
		}
		
		// build properties map
		Map<String, String> properties = new HashMap<String, String>();
		properties.put("id", id);
		for (String parameter : parameters.getParameterNames()) {
			if (parameter.startsWith("property:"))
				properties.put(
					parameter.substring(9), parameters.getParameter(parameter));
		}
		
		// retrieve indicated module file
		String file = null;
		try {
			if (filename != null)
				file = FileIOUtils.readFile(
					getInterfaceModuleFileByName(type, module, filename));
			else file = FileIOUtils.readFile(
				getInterfaceModuleFileByExtension(type, module, extension));
		} catch (FileNotFoundException error) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		
		// output module file contents
		if (file == null) {
			response.sendError(
				HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
				"There was an error retrieving your " +
				"specified interface module file.");
		} else {
			// substitute module properties into output file content
			file = substituteProperties(file, properties);
			// write processed file content to output stream
			if (extension.equals("inc"))
				response.setContentType("text/html");
			else if (extension.equals("js"))
				response.setContentType("application/javascript");
			else if (extension.equals("css"))
				response.setContentType("text/css");
			response.setContentLength(file.length());
			out.println(file);
		}
	}
	
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	public static final File getInterfaceModuleFileByName(
		String type, String module, String filename
	) throws FileNotFoundException {
		if (module == null || type == null || filename == null)
			return null;
		// retrieve module directory
		File moduleDirectory = getModuleDirectory(type, module);
		if (moduleDirectory == null)
			throw new FileNotFoundException();
		// retrieve indicated module file
		else {
			File file = new File(moduleDirectory, filename);
			if (file == null || file.canRead() == false)
				throw new FileNotFoundException();
			else return file;
		}
	}
	
	public static final File getInterfaceModuleFileByExtension(
		String type, String module, String extension
	) throws FileNotFoundException {
		if (module == null || type == null || extension == null)
			return null;
		// retrieve module directory
		File moduleDirectory = getModuleDirectory(type, module);
		if (moduleDirectory == null)
			throw new FileNotFoundException();
		// retrieve indicated module file
		else for (File file : moduleDirectory.listFiles()) {
			if (extensionsMatch(
				extension, FilenameUtils.getExtension(file.getName())))
				return file;
		}
		throw new FileNotFoundException();
	}
	
	/*========================================================================
	 * Convenience methods
	 *========================================================================*/
	private static File getModuleDirectory(String type, String module) {
		if (module == null || type == null)
			return null;
		// retrieve and verify global interface module directory
		File moduleRoot =
			new File(WebAppProps.getPath("livesearch.plugins.path"));
		if (moduleRoot == null || moduleRoot.isDirectory() == false ||
			moduleRoot.canRead() == false) {
			// TODO: report error
			return null;
		}
		// retrieve module type directory
		File moduleDirectory = new File(moduleRoot, type);
		if (moduleDirectory == null || moduleDirectory.isDirectory() == false ||
			moduleDirectory.canRead() == false) {
			// TODO: report error
			return null;
		}
		// retrieve directory for the indicated module
		moduleDirectory = new File(moduleDirectory, module);
		if (moduleDirectory == null || moduleDirectory.isDirectory() == false ||
			moduleDirectory.canRead() == false) {
			// TODO: report error
			return null;
		} else return moduleDirectory;
	}
	
	private static boolean extensionsMatch(
		String extension1, String extension2
	) {
		if (extension1 == null || extension2 == null)
			return false;
		Set<String> extensions = new HashSet<String>(2);
		extensions.add(extension1);
		extensions.add(extension2);
		if (extensions.size() == 1)
			return true;
		else if (extensions.contains("inc") && extensions.contains("html"))
			return true;
		else return false;
	}
	
	private static String substituteProperties(
		String content, Map<String, String> properties
	) {
		if (content == null || properties == null || properties.isEmpty())
			return content;
		for (String property : properties.keySet())
			content = content.replaceAll(
				Pattern.quote("${" + property + "}"), properties.get(property));
		return content;
	}
}
