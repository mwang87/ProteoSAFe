package edu.ucsd.livesearch.result;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import edu.ucsd.livesearch.parameter.LegacyParameterConverter;
import edu.ucsd.livesearch.result.parsers.IterableResult;
import edu.ucsd.livesearch.result.parsers.Result;
import edu.ucsd.livesearch.result.parsers.ResultHit;
import edu.ucsd.livesearch.servlet.BaseServlet;
import edu.ucsd.livesearch.task.NullTask;
import edu.ucsd.livesearch.task.OnDemandLoader;
import edu.ucsd.livesearch.task.Task;
import edu.ucsd.livesearch.task.TaskManager;
import edu.ucsd.livesearch.task.TaskManager.TaskStatus;
import edu.ucsd.saint.commons.archive.ArchiveEntry;
import edu.ucsd.saint.commons.archive.ArchiveUtils;
import edu.ucsd.saint.commons.archive.Archiver;
import edu.ucsd.saint.commons.archive.CompressionType;
import edu.ucsd.livesearch.util.FileIOUtils;

@SuppressWarnings("serial")
public class DownloadResult
extends BaseServlet
{
	/*========================================================================
	 * Constants
	 *========================================================================*/
	@SuppressWarnings("unused")
	private static final Logger logger =
		LoggerFactory.getLogger(DownloadResult.class);
	
	/*========================================================================
	 * Servlet methods
	 *========================================================================*/
	/**
	 * <p>Accepts and processes requests to download workflow result file
	 * packages.
	 * 
	 * @param request	an {@link HttpServletRequest} object that contains
	 * 					the request the client has made of the servlet
	 * 
	 * @param response	an {@link HttpServletResponse} object that contains
	 * 					the response the servlet sends to the client
	 * 
	 * @throws IOException		if an input or output error is detected
	 * 							when the servlet handles the POST request
	 * 
	 * @throws ServletException	if the request for the POST could not be
	 * 							handled
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected void doPost(
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
		
		// extract the request parameters
		Map<String, String> parameters = new HashMap<String, String>();
		try {
			Enumeration<String> parameterNames =
				(Enumeration<String>)request.getParameterNames();
			while (parameterNames.hasMoreElements()) {
				String parameter = parameterNames.nextElement();
				parameters.put(parameter, request.getParameter(parameter));
			}
		} catch (Throwable error) {
			parameters = null;
		}
		
		// get the ID of the task whose results are to be downloaded
		String taskID = request.getParameter("task");
		if (taskID == null)
			throw new ServletException(
				"Please specify the ID of a valid task to see its results.");
		
		// retrieve the specified task, and verify that it can display results
		Task task = TaskManager.queryTask(taskID);
		if (task == null || task instanceof NullTask ||
			task.getStatus().equals(TaskStatus.NONEXIST))
			throw new ServletException(
				"No valid task could be found for task ID \"" + taskID + "\".");
		else if (task.getStatus().equals(TaskStatus.DONE) == false)
			throw new IllegalStateException();
		
		// retrieve the proper view specification
		String view = request.getParameter("view");
		if (view == null)
			throw new ServletException("Please specify the name " +
				"of a valid result view for workflow type \"" +
				task.getFlowName() + "\" to see the results of this task.");
		String archiveName = task.getFlowName() + "-" +
			task.getID().substring(0, 8) + "-" + view;
		
		// retrieve the proper data specifications for the specified view
		Map<String, Element> dataSpecs =
			ResultViewXMLUtils.getDataSpecifications(task, view);
		if (dataSpecs == null || dataSpecs.isEmpty())
			throw new ServletException(
				"No valid data specifications could be found for view \"" +
				view + "\" of workflow type \"" + task.getFlowName() + "\".");
		
		// iterate over the data specifications, set up results
		Map<String, Result> results =
			new HashMap<String, Result>(dataSpecs.size());
		for (String block : dataSpecs.keySet()) {
			String name = archiveName + "-" + block;
			String blockID = view + "-" + block;
			Result result = ResultFactory.createResult(
				dataSpecs.get(block), task, blockID, parameters);
			if (result == null)
				throw new ServletException("There was an error retrieving " +
					"the result data for block \"" + block + "\" of " +
					"workflow type \"" + task.getFlowName() + "\".");
			else results.put(name, result);
		}
		
		// retrieve remaining download parameters
		String option = request.getParameter("option");
		String content = request.getParameter("content");
		String entries = request.getParameter("entries");
		if ("paris".equals(option))
			entries = null;
		
		// prepare output for result file archive
		response.setContentType("application/zip");
		response.addHeader("Content-Disposition",
			"attachment; filename=\"ProteoSAFe-" + archiveName + ".zip\"");
		Archiver archiver = ArchiveUtils.createArchiver(
			response.getOutputStream(), CompressionType.ZIP);
		
		// write main (filtered) result file
		boolean hasEntries = entries != null &&
			("filtered".equals(content) || "checked".equals(content));
		writeBlockResults(archiver, results, hasEntries, entries);
		
		// write secondary result files
		Collection<ArchiveEntry> secondaryResults =
			collectSecondaryResults(task, option);
		if (secondaryResults != null) {
			for (ArchiveEntry entry : secondaryResults) {
				File fileToCompress = entry.getFile();
				if(fileToCompress.exists()){
					archiver.putNextEntry(entry);
					archiver.write(entry.getFile());
					archiver.closeEntry();
				}
				else{
					//getLogger().error("File does not exist: " + entry.getFile());
				}
							
			}
		}
		
		// close and send archive
		archiver.close();
	}
	
	/*========================================================================
	 * Convenience methods
	 *========================================================================*/
	private void writeBlockResults(
		Archiver archiver, Map<String, Result> results,
		boolean hasEntries, String entries
	) {
		if (archiver == null || results == null || results.isEmpty())
			return;
		for (String resultName : results.keySet()) {
			Result result = results.get(resultName);
			try {
				// if this result is iterable, iterate over it
				if (result instanceof IterableResult) {
					ArchiveEntry entry = new ArchiveEntry(resultName + ".tsv");
					archiver.putNextEntry(entry);
					IterableResult iterable = (IterableResult)result;
					// get first hit, to ensure attributes are populated
					ResultHit hit = null;
					if (iterable.hasNext())
						hit = iterable.next();
					// write header line
					StringBuffer header = new StringBuffer();
					List<String> fields = hit.getFieldNames();
					if (fields != null)
						for (String field : fields)
							header.append(field + "\t");
					List<String> attributes = hit.getAttributeNames();
					if (attributes != null)
						for (String attribute : attributes)
							header.append(attribute + "\t");
					// truncate trailing tab, if necessary
					if (header.charAt(header.length() - 1) == '\t')
						header.setLength(header.length() - 1);
					archiver.print(header.toString());
					archiver.println();
					// if this result needs to be filtered, do so
					if (hasEntries) {
						List<String> ids = Arrays.asList(entries.split(";"));
						int counter = 0;
						do {
							if (ids.contains(Integer.toString(counter))) {
								archiver.print(hit.toString());
								archiver.println();
							}
							counter++;
						} while (iterable.hasNext() &&
							(hit = iterable.next()) != null);
					}
					// otherwise just write all hits
					else do {
						archiver.print(hit.toString());
						archiver.println();
					} while (iterable.hasNext() &&
						(hit = iterable.next()) != null);
					archiver.closeEntry();
				}
				// if this result is not iterable, just write the file
				else if (OnDemandLoader.load(result)) {
					File file = result.getFile();
					String extension =
						FilenameUtils.getExtension(file.getName());
					if (extension == null || extension.trim().equals(""))
						extension = "result";
					ArchiveEntry entry =
						new ArchiveEntry(resultName + "." + extension);
					archiver.putNextEntry(entry);
					archiver.write(file);
					archiver.closeEntry();
				} else {
					// TODO: report error
					continue;
				}
			} catch (Throwable error) {
				// TODO: report error
				continue;
			} finally {
				result.close();
			}
		}
	}
	
	private Collection<ArchiveEntry> collectSecondaryResults(
		Task task, String option
	) {
		Collection<ArchiveEntry> results = new Vector<ArchiveEntry>();
		ArchiveEntry parameters = getResultParameters(task);
		if (parameters != null)
			results.add(parameters);
		// TODO: read view specification, get files indicated there
		// retrieve the extra files that need to be downloaded for this task
		List<Element> sources = ResultViewXMLUtils.getChildElements(
			ResultViewXMLUtils.getDownloadSpecification(task), "source");
		if (sources != null && sources.isEmpty() == false) {
			for (Element source : sources) {
				// TODO: implement parameter specification for <download>
				File sourceFile =
					ResultFactory.getSourceFile(source, task, null);
				if (sourceFile == null)
					continue;
				else if (sourceFile.isDirectory())
					addResultsRecursively(sourceFile, "", results);
				else results.add(
					new ArchiveEntry(sourceFile.getName(), sourceFile));
			}
		}
		// TODO: handle MCP "option" appropriately
		return results;
	}
	
	private void addResultsRecursively(
		File file, String parentPath, Collection<ArchiveEntry> results
	) {
		if (file == null || file.canRead() == false || results == null)
			return;
		else if (file.isDirectory() == false)
			results.add(new ArchiveEntry(parentPath + file.getName(), file));
		else {
			parentPath = file.getName() + "/";
			File[] files = file.listFiles();
			if (files != null && files.length > 0)
				for (File child : files)
					addResultsRecursively(child, parentPath, results);
		}
	}
	
	private ArchiveEntry getResultParameters(Task task) {
		if (task == null)
			return null;
		else {
			// ensure that parameters file is present
			LegacyParameterConverter paramsLoader =
				new LegacyParameterConverter(task);
			if (OnDemandLoader.load(paramsLoader))
				return new ArchiveEntry(
					"params.xml", task.getPath("params/params.xml"));
			else return null;
		}
	}
}
