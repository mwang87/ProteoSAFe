package edu.ucsd.livesearch.servlet;

import java.io.File;
import java.io.IOException;
import java.util.zip.CRC32;
import java.util.zip.CheckedOutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsd.livesearch.task.NullTask;
import edu.ucsd.livesearch.task.Task;
import edu.ucsd.livesearch.task.TaskManager;
import edu.ucsd.saint.commons.archive.ArchiveEntry;
import edu.ucsd.saint.commons.archive.ArchiveUtils;
import edu.ucsd.saint.commons.archive.Archiver;
import edu.ucsd.saint.commons.archive.CompressionType;

/**
 * Servlet implementation class for Servlet: DownloadResource
 *
 */
 @SuppressWarnings("serial")
public class DownloadTaskResources
extends javax.servlet.http.HttpServlet
implements javax.servlet.Servlet
{
	/*========================================================================
	 * Constants
	 *========================================================================*/
	private static final Logger logger =
		LoggerFactory.getLogger(DownloadTaskResources.class);
		
	/*========================================================================
	 * Servlet methods
	 *========================================================================*/
	protected void doGet(
		HttpServletRequest request, HttpServletResponse response
	) throws ServletException, IOException {
		// retrieve and verify task
		Task task = TaskManager.queryTask(request.getParameter("task"));
		if (task instanceof NullTask)
			return;
		
		// retrieve indicated task resource directory, list out its contents
		String resource = request.getParameter("resource");
		if (StringUtils.isEmpty(resource))
			return;
		File path = task.getPath(resource + "/");
		File files[] = path.isDirectory() ? path.listFiles() : new File[]{path}; 
		if (files.length == 0)
			return;
		
		// package task resource files into an archive
		CompressionType compression =
			CompressionType.byTypeName(request.getParameter("compression"));
		CheckedOutputStream checksumStream = new CheckedOutputStream(
			response.getOutputStream(), new CRC32());
		Archiver archiver =
			ArchiveUtils.createArchiver(checksumStream, compression);
		try {
			response.setContentType(compression.getMime());
			response.addHeader(
				"Content-Disposition","attachment; filename=\"result." +
				compression.getExtension());
			for (File file : files) {
				if (file.isFile() == false)
					continue;
				ArchiveEntry entry = new ArchiveEntry(file.getName(), file);
				archiver.putNextEntry(entry);
				archiver.write(entry.getFile());
				archiver.closeEntry();
			}
		} finally {
			archiver.close();
			// add file integrity data to HTTP response footers
			String checksum =
				Long.toString(checksumStream.getChecksum().getValue());
			logger.info(String.format("Computed server-side checksum for " +
				"archive [%s:%s]: %s", task.getID(), resource, checksum));
			response.addHeader("CRC32-Checksum", checksum);
		}
	}  	
	
	protected void doPost(
		HttpServletRequest request, HttpServletResponse response
	) throws ServletException, IOException {
		doGet(request, response);
	}
	
//	private static File getStorageBase() { 
//		return new File("/usr/local/tomcat/webapps/test/"); 
//	}
}
