package edu.ucsd.livesearch.servlet;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.ucsd.livesearch.storage.FileManager;
import edu.ucsd.saint.commons.IOUtils;

public class DownloadTaskFile extends HttpServlet{
	/**
	 * 
	 */
	private static final long serialVersionUID = -1562748671493794191L;

	public void doGet(HttpServletRequest req, HttpServletResponse res)
		throws ServletException, IOException {
		String URI = req.getRequestURI();
		String path = URI.replace(req.getContextPath() + "/download/task/", "t.");
		File file = FileManager.getFile(path);
		if(file.exists() && file.isFile()){
			res.setContentType("application/octet-stream");
			res.addHeader("Content-Disposition","attachment; filename=\"" + file.getName() + "\"");
			if(!IOUtils.appendFileTo(file, res.getOutputStream()))
				throw new IOException("Failed to write file to outputstream: " + file.getAbsolutePath());
		}
	}
	  
	public void doPost(HttpServletRequest req, HttpServletResponse res)
			throws ServletException, IOException {
		doGet(req, res);
	}
}
