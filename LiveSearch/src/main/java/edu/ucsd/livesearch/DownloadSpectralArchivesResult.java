package edu.ucsd.livesearch;

import java.io.File;
import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.ucsd.livesearch.task.Task;
import edu.ucsd.livesearch.task.TaskManager;
import edu.ucsd.saint.commons.archive.ArchiveEntry;
import edu.ucsd.saint.commons.archive.ArchiveUtils;
import edu.ucsd.saint.commons.archive.Archiver;
import edu.ucsd.saint.commons.archive.CompressionType;

@SuppressWarnings("serial")
public class DownloadSpectralArchivesResult
extends HttpServlet
implements Servlet
{
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	/**
	 * <p>Accepts and processes queries to download the result files of
	 * a specified Spectral Archives task.
	 * 
	 * <p>By convention, a POST request to this servlet is assumed to be a
	 * request for data creation only.  No reading, update, or deletion of
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
	protected void doPost(
		HttpServletRequest request, HttpServletResponse response
	) throws ServletException, IOException {
		String taskID = request.getParameter("task");
		Task task = TaskManager.queryTask(taskID);
		response.setContentType("application/zip");
		response.addHeader("Content-Disposition",
			"attachment; filename=\"result.zip\"");
		Archiver archiver = ArchiveUtils.createArchiver(
			response.getOutputStream(), CompressionType.ZIP);
		
		// add parameter file entry
		ArchiveEntry entry =
			new ArchiveEntry("params.xml", task.getPath("params/params.xml"));
		archiver.putNextEntry(entry);
		archiver.write(entry.getFile());
		archiver.closeEntry();
		
		// add result file entries
		File resultDirectory = task.getPath("result/");
		if (resultDirectory == null ||
			resultDirectory.isDirectory() == false) {
			// TODO: report error
		} else {
			File[] resultFiles = resultDirectory.listFiles();
			if (resultFiles == null || resultFiles.length < 1) {
				// TODO: report error
			} else for (File file: resultFiles) {
				if (file.isFile()) {
					entry = new ArchiveEntry(file.getName(), file);
					archiver.putNextEntry(entry);
					archiver.write(entry.getFile());
					archiver.closeEntry();
				}
			}
		}
		
		// write result archive to output
		archiver.close();
	}
}
