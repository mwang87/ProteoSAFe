package edu.ucsd.livesearch.servlet;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsd.livesearch.storage.FileManager;
import edu.ucsd.saint.commons.http.HttpParameters;

public class SimpleUploadManager extends BaseServlet{

	private static final Logger logger =
		LoggerFactory.getLogger(SimpleUploadManager.class);
	
	
	@Override
	protected void doPost(
		HttpServletRequest request, HttpServletResponse response
	) throws ServletException, IOException {
		logger.info("Uploading Simple");
		// initialize properties
		try {
			initialize(request, true);
		} catch (ServletException error) {
			reportError(HttpServletResponse.SC_BAD_REQUEST,
				"Error initializing servlet properties from request",
				response, error);
			return;
		} catch (Throwable error) {
			reportError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
				"Error initializing servlet properties from request",
				response, error);
			return;
		}
		HttpParameters parameters = getParameters();
		
		String user = getUser();
		PrintWriter out = response.getWriter();
		out.write("RUNNING\n");
		
		// verify authentication of user
		if (isAuthenticated() == false) {
			reportError(HttpServletResponse.SC_UNAUTHORIZED,
				"You must be logged in to queue file uploads.", response);
			out.write("LOGIN\n");
			return;
		}
		out.write(user + "\n");
		logger.info("Uploading Simple from user " + user);
		// retrieve and verify target folder
		File folder = null;
		String path = parameters.getParameter("folder");
		if (path != null) {
			out.write(path);
			logger.info("to folder " + path);
			folder = getOwnedFile(path, user);
			if (folder != null) {
				if (folder.isDirectory() == false)
					folder = folder.getParentFile();
				if (folder.exists() == false && folder.mkdirs() == false)
					folder = null;
			}
			if (folder == null) {
				reportError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
					"There was an error retrieving your specified target " +
					"folder.", response);
				out.write("NOT OWNED FOLDER\n");
				return;
			}
		}
		else{
			out.write("NO FOLDER\n");
			return;
		}
		
		//Getting Actual File
		for (String fileParam : parameters.getFileParamNames()) {
			out.write("Uploading SHIZ " + fileParam);
			FileItem upload = parameters.getFile(fileParam);
			File file = new File(folder, upload.getName());
			logger.info("writing file " + upload.getName());
			out.write(upload.getName() + "\n");
			boolean uploaded = FileManager.createFile(user, file, upload);
			// if the upload was successful, inform the upload queue
			if (uploaded)
				logger.info("SUCCESSFUL UPLOAD");
		}
	}
	
	private static File getOwnedFile(String path, String user) {
		if (path == null || user == null)
			return null;
		// if specified file is not owned, then it can't be retrieved
		File file = FileManager.getFile("f." + path);
		if (FileManager.isOwned(file, user) == false)
			return null;
		else return file;
	}
}

