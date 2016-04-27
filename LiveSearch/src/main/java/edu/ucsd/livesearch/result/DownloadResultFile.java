package edu.ucsd.livesearch.result;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FilenameUtils;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsd.livesearch.dataset.Dataset;
import edu.ucsd.livesearch.dataset.DatasetManager;
import edu.ucsd.livesearch.inspect.InspectUtils;
import edu.ucsd.livesearch.result.plugin.DownloadPSMImage;
import edu.ucsd.livesearch.servlet.BaseServlet;
import edu.ucsd.livesearch.task.NullTask;
import edu.ucsd.livesearch.task.Task;
import edu.ucsd.livesearch.task.TaskManager;
import edu.ucsd.livesearch.task.TaskManager.TaskStatus;
import edu.ucsd.livesearch.util.FileIOUtils;
import edu.ucsd.saint.commons.IOUtils;

@SuppressWarnings("serial")
public class DownloadResultFile
extends BaseServlet
{
	/*========================================================================
	 * Constants
	 *========================================================================*/
	private static final Logger logger =
		LoggerFactory.getLogger(DownloadResultFile.class);
	public static final String FILE_PREFIX = "FILE->";
	public static final String FOLDER_PREFIX = "FOLDER->";
	// possible data sources for downloadable task result files
	public static enum ResultType {
		FILE, FOLDER, INVOKE, VALUE, DATASET;
		public String toString() {
			return this.name().toLowerCase();
		}
	}

	/*========================================================================
	 * Servlet methods
	 *========================================================================*/
	/**
	 * <p>Accepts and processes queries to download workflow result files.
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
	@SuppressWarnings("unchecked")
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
		ServletOutputStream out = response.getOutputStream();

		// extract the request parameters into a simple map
		Map<String, String> parameters = new HashMap<String, String>();
		try {
			Enumeration<String> parameterNames =
				(Enumeration<String>)request.getParameterNames();
			while (parameterNames.hasMoreElements()) {
				String parameter = parameterNames.nextElement();
				parameters.put(parameter, request.getParameter(parameter));
			}
		} catch (Throwable error) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST,
				"You must specify valid parameters to download " +
				"one of a task's result files.");
			return;
		}

		// get the indicated task
		Task task = null;
		try {
			task = TaskManager.queryTask(parameters.get("task"));
			if (task == null)
				throw new NullPointerException();
			else if (task instanceof NullTask ||
				task.getStatus().equals(TaskStatus.NONEXIST))
				throw new IllegalArgumentException();
		} catch (Throwable error) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST,
				"You must specify a valid task ID to download " +
				"one of its result files.");
			return;
		}

		// get this file's data source
		File file = null;
		ResultType type = ResultType.INVOKE;
		String source = parameters.get(type.toString());
		if (source != null)
			parameters.remove(type.toString());
		else {
			type = ResultType.FILE;
			source = parameters.get(type.toString());
			if (source == null) {
				type = ResultType.FOLDER;
				source = parameters.get(type.toString());
				if (source == null) {
					response.sendError(HttpServletResponse.SC_BAD_REQUEST,
						"You must specify either a \"file\", \"folder\" or " +
						"\"invoke\" parameter to download one of a task's " +
						"result files.");
					return;
				}
			}
		}
		// fetch file appropriately based on data source type
		try {
			if (type.equals(ResultType.INVOKE))
				file = invokePlugin(source, parameters, task);
			else file = fetchStaticFile(type, source, task);
		} catch (FileNotFoundException error) {
			response.sendError(HttpServletResponse.SC_GONE, error.getMessage());
			return;
		}
		if (file == null || file.canRead() == false) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
				"Error downloading task result file: " +
				"a valid file could not be found.");
			logger.error(String.format("There was an error downloading a " +
				"result file for task [%s]. This file is of type \"%s\", " +
				"with source string value of [%s]. Its path was determined " +
				"to be [%s]%s.", task.getID(), type.toString(), source,
				file == null ? "null" : file.getAbsolutePath(),
				file == null ? "" : String.format(
					" (canRead = %b)", file.canRead())));
			return;
		}

		// determine if the content of this file needs to be processed in
		// order to properly display it within the ProteoSAFe result view
		String block = parameters.get("block");

		// write file to servlet output stream appropriately based on extension
		// TODO: come up with some better assumptions here!
		String extension = FilenameUtils.getExtension(file.getName());
		if (extension.equalsIgnoreCase("png") ||
			extension.equalsIgnoreCase("gif") ||
			extension.equalsIgnoreCase("jpg")) {
			// if a block is provided, then this is a top-level streamed
			// data resource that is being managed by ProteoSAFe, and therefore
			// the output is assumed to be a properly formatted data URL
			if (block != null) {
				String dataURLContent = Base64.encodeBase64String(
					FileIOUtils.readBinaryFile(file));
				response.setContentLength(dataURLContent.length());
				out.print(dataURLContent);
			}
			// otherwise, this is assumed to be a
			// request for the literal image data
			else {
				String contentType = "image/";
				if (extension.equalsIgnoreCase("jpg"))
					contentType += "jpeg";
				else contentType += extension;
				response.setContentType(contentType);
				writeImage(file, out);
			}
		} else if (extension.equalsIgnoreCase("htm") ||
			extension.equalsIgnoreCase("html")) {
			String html = FileIOUtils.readFile(file);
			if (block != null)
				html = processHTML(
					html, task.getID(), block, FilenameUtils.getPath(source));
			response.setContentType("text/html");
			response.setContentLength(html.length());
			out.print(html);
		} else {
			//Determining whether to wrap this in json
			if(parameters.containsKey("json")){
				out.print("{ \"data\" : \"");
				out.print(JSONObject.escape(FileIOUtils.readFile(file)));
				out.print("\"}");
			}
			else if(parameters.containsKey("jsonp")){
				String callback = parameters.get("callback");
				out.print(callback + "(" );
				out.print("{ \"data\" : \"");
				out.print(JSONObject.escape(FileIOUtils.readFile(file)));
				out.print("\"}");
				out.print(")");
			}
			else{
				out.print(FileIOUtils.readFile(file));
			}
		}
	}

	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	public static File fetchStaticFile(
		ResultType type, String filename, Task task
	) throws IllegalArgumentException {
		if (type == null || filename == null ||
			task == null || task instanceof NullTask ||
			task.getStatus().equals(TaskStatus.NONEXIST))
			return null;
		// ensure that filename is valid
		filename = FileIOUtils.escapeFilenameWithSpaces(filename);
		// construct and verify file
		File file = null;
		// if the file comes from a dataset, look for it there
		if (type.equals(ResultType.DATASET)) {
			Dataset dataset = DatasetManager.queryDatasetByTaskID(task.getID());
			if (dataset == null)
				throw new IllegalArgumentException(String.format(
					"Source file [%s] of type \"%s\" was specified in " +
					"result.xml, but task [%s] (workflow \"%s\") does " +
					"not correspond to a valid dataset.",
					filename, type, task.getID(), task.getFlowName()));
			else try {
				file = dataset.getDatasetFile(filename);
			} catch (FileNotFoundException error) {
				throw new IllegalArgumentException(String.format(
					"Source file [%s] of type \"%s\" was specified in " +
					"result.xml, but could not be found in dataset [%s] " +
					"associated with task [%s] (workflow \"%s\").",
					filename, type, dataset.getDatasetIDString(),
					task.getID(), task.getFlowName()), error);
			}
		}
		// otherwise the file must come from the task
		else file = new File(task.getPath(""), filename);
		if (type.equals(ResultType.FOLDER) && filename.endsWith("/") == false)
			throw new IllegalArgumentException("\"folder\" attribute values " +
				"should end with a \"/\" character. If this is meant to " +
				"be a regular file, please use a \"file\" attribute instead.");
		else if (type.equals(ResultType.FILE) && filename.endsWith("/"))
			file = FileIOUtils.getSingleFile(file);
		// verify that file exists and is readable
		if (file.canRead() == false)
			throw new IllegalArgumentException(String.format(
				"Source file [%s] of type \"%s\" was specified in " +
				"result.xml, but the file could not be accessed.",
				filename, type));
		else return file;
	}

	public static File fetchDatasetFile(
			ResultType type, String filename, Dataset dataset
		) throws FileNotFoundException {
			if (type == null || filename == null || dataset == null)
				return null;
			// construct and verify file
			File file = dataset.getDatasetFile(filename);
			// if the file could not be found with the given name,
			// then try escaping the filename to find it
			if (file == null) {
				filename = FileIOUtils.escapeFilename(filename);
				file = dataset.getDatasetFile(filename);
			}
			// if the file was not found even after escaping
			// its name, then it's just not there
			if (file == null)
				return null;
			if (type.equals(ResultType.FOLDER) &&
				filename.endsWith("/") == false)
				throw new IllegalArgumentException("\"folder\" attribute " +
					"values should end with a \"/\" character. If this is " +
					"meant to be a regular file, please use a \"file\" " +
					"attribute instead.");
			else if (type.equals(ResultType.FILE) && filename.endsWith("/"))
				return FileIOUtils.getSingleFile(file);
			else return file;
		}

	public static File invokePlugin(
		String tool, Map<String, String> parameters, Task task
	) throws FileNotFoundException {
		if (tool == null)
			return null;
		// resolve all filename parameters
		else if (parameters != null) {
			// determine if source file should come from a dataset
			Dataset dataset = null;
			String datasetID = parameters.get("dataset");
			if (datasetID != null)
				dataset = DatasetManager.queryDatasetByID(datasetID);
			for (String parameter : parameters.keySet()) {
				String value = parameters.get(parameter);
				File file = null;
				if (value.startsWith(FILE_PREFIX)) {
					value = value.substring(FILE_PREFIX.length());
					// get file/folder from the proper source, task or dataset
					if (dataset != null)
						file = fetchDatasetFile(
							ResultType.FILE, value, dataset);
					else file = fetchStaticFile(ResultType.FILE, value, task);
				} else if (value.startsWith(FOLDER_PREFIX)) {
					value = value.substring(FOLDER_PREFIX.length());
					// get file/folder from the proper source, task or dataset
					if (dataset != null)
						file = fetchDatasetFile(
							ResultType.FOLDER, value, dataset);
					else file = fetchStaticFile(ResultType.FOLDER, value, task);
				} else continue;
				// if this is a filename parameter, update it
				// with the file's absolute path
				if (file == null) {
					// TODO: report error
					return null;
				} else parameters.put(parameter, file.getAbsolutePath());
			}
		}
		// TODO: this is a total hack
		if (parameters == null || parameters.isEmpty())
			return null;
		StringBuffer log = new StringBuffer("Attempting to invoke plugin \"");
		log.append(tool);
		log.append("\", using the following parameters:");
		for (String parameter : parameters.keySet()) {
			log.append("\n\t");
			log.append(parameter);
			log.append(" = \"");
			log.append(parameters.get(parameter));
			log.append("\"");
		}
		getLogger().info("\n" + log.toString());
		if (tool.equals("annotatedSpectrumImage")) {
			return DownloadPSMImage.generatePSMImage(task,
				parameters.get("file"),
				parameters.get("index"),
				parameters.get("scan"),
				parameters.get("spectrumid"),
				parameters.get("peptide"),
				parameters.get("fragmentation"),
				false, false, Boolean.parseBoolean(parameters.get("force")),
				Boolean.parseBoolean(parameters.get("trim")),
				Boolean.parseBoolean(
					parameters.get("annotation-style-inspect")));
		} else if (tool.equals("annotatedSpectrumImageThumbnail")) {
			return DownloadPSMImage.generatePSMImage(task,
				parameters.get("file"),
				parameters.get("index"),
				parameters.get("scan"),
				parameters.get("spectrumid"),
				parameters.get("peptide"),
				parameters.get("fragmentation"),
				true, false, Boolean.parseBoolean(parameters.get("force")),
				Boolean.parseBoolean(parameters.get("trim")),
				Boolean.parseBoolean(
					parameters.get("annotation-style-inspect")));
		} else if (tool.equals("annotatedSpectrumImageText")) {
			return DownloadPSMImage.generatePSMImage(task,
				parameters.get("file"),
				parameters.get("index"),
				parameters.get("scan"),
				parameters.get("spectrumid"),
				parameters.get("peptide"),
				parameters.get("fragmentation"),
				false, true, Boolean.parseBoolean(parameters.get("force")),
				Boolean.parseBoolean(parameters.get("trim")),
				Boolean.parseBoolean(
					parameters.get("annotation-style-inspect")));
		} else if (tool.equals("proteinSummaryExtractor")) {
			return InspectUtils.getSummary(task,
				parameters.get("proteinID"),
				parameters.get("protein"),
				parameters.get("output"));
		} else return null;
	}

	/*========================================================================
	 * Convenience methods
	 *========================================================================*/
	private static void writeImage(File image, ServletOutputStream output)
	throws IOException {
		BufferedInputStream input = null;
		try {
			input = new BufferedInputStream(new FileInputStream(image));
			IOUtils.copyStream(input, output);
		} finally {
			if (input != null)
				input.close();
		}
	}

	private static String processHTML(
		String html, String task, String block, String root
	) {
		if (html == null)
			return null;
		else if (task == null) {
			getLogger().error("Could not process streamed HTML, " +
				"because no task ID was provided to fetch linked files.");
			return html;
		} else if (block == null) {
			getLogger().error("Could not process streamed HTML, " +
				"because no block ID was provided to form proper hyperlinks.");
			return html;
		}
		if (root == null)
			root = "";
		// set up search/insert strings
		String[] attributes = new String[]{ "src", "href" };
		String absoluteURL = "[a-zA-Z]+://.*";
		String fetch = "DownloadResultFile?task=" + task + "&file=" + root;
		String link = "javascript:reloadBlock('" + block + "', '" + root;
		StringBuffer updated = new StringBuffer(html);
		// update specified search attributes
		for (String attribute : attributes) {
			String token = attribute + "=";
			int offset = 0;
			for (int i=html.indexOf(token); i>=0; i=html.indexOf(token, i+1)) {
				int index = i + token.length() + offset;
				String value = updated.substring(index);
				char quote = value.charAt(0);
				// only process this token if it's actually an attribute
				if (quote != '"' && quote != '\'')
					continue;
				// advance one character, to get past this attribute's opening
				// quotation, and then parse to the next newline if there is one
				index++;
				try {
					value = value.substring(1, value.indexOf('\n'));
				} catch (IndexOutOfBoundsException error) {
					value = value.substring(1);
				}
				// do not process absolute paths
				if (value.charAt(0) == '/' || value.matches(absoluteURL))
					continue;
				// do not process in-page anchor links
				else if (value.startsWith("#"))
					continue;
				// do not process data URLs
				else if (value.startsWith("data:"))
					continue;
				// if this attribute is valid, update it correctly
				else if (isLink(attribute, updated, index)) {
					// determine where this attribute ends
					int end = updated.indexOf(String.valueOf(quote), index);
					if (end >= 0) {
						// if this attribute is enclosed in single quotes,
						// update the js argument enclosures to double quotes
						String thisLink = new StringBuffer(link).toString();
						char thisQuote = '\'';
						if (quote == '\'') {
							thisQuote = '"';
							thisLink = thisLink.replaceAll("'", "\"");
						}
						// insert the link prefix, as well as its closure
						updated.insert(index, thisLink);
						end += thisLink.length();
						updated.insert(end, thisQuote + ");");
						offset += thisLink.length() + 3;
					}
				} else {
					updated.insert(index, fetch);
					offset += fetch.length();
				}
			}
			html = updated.toString();
		}
		return html;
	}

	private static boolean isLink(
		String attribute, StringBuffer content, int index
	) {
		if (attribute == null || attribute.equalsIgnoreCase("href") == false ||
			content == null || index < 1)
			return false;
		String tag = content.substring(0, index);
		int start = tag.lastIndexOf('<');
		if (start < 0)
			return false;
		else tag = tag.substring(start + 1);
		return tag.startsWith("a ") || tag.startsWith("A ");
	}
}
