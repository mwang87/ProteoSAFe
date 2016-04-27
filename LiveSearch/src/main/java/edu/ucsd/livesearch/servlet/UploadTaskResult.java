package edu.ucsd.livesearch.servlet;

import java.io.File;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsd.livesearch.task.NullTask;
import edu.ucsd.livesearch.task.Task;
import edu.ucsd.livesearch.task.TaskManager;
import edu.ucsd.saint.commons.http.HttpParameters;
import edu.ucsd.saint.commons.archive.Archive;
import edu.ucsd.saint.commons.archive.ArchiveEntry;
import edu.ucsd.saint.commons.archive.ArchiveUtils;

/**
 * Servlet implementation class for Servlet: UploadResult
 *
 */
 public class UploadTaskResult extends javax.servlet.http.HttpServlet implements javax.servlet.Servlet {
    /**
	 * 
	 */
	private static final long serialVersionUID = -2253377550356166099L;
	private final Logger logger = LoggerFactory.getLogger(UploadTaskResult.class);	

	/* (non-Java-doc)
	 * @see javax.servlet.http.HttpServlet#HttpServlet()
	 */
	public UploadTaskResult() {
		super();
	}   	 	
	
	/* (non-Java-doc)
	 * @see javax.servlet.http.HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		boolean badRequest = false;		
		try{
			HttpParameters params = new HttpParameters(request, null);
			String taskID = params.getParameter("task");
			Task task = TaskManager.queryTask(taskID);
			String resource = params.getParameter("resource");
			boolean cleanup = !"false".equals(params.getParameter("cleanup"));
			logger.info(String.format(
				"Upload resource [%s] for task [%s]", resource, task));
			if(task instanceof NullTask || StringUtils.isEmpty(resource))
				badRequest = true;
			else{
				File folder = task.getPath(resource + "/");
				if(cleanup && folder.exists() && folder.isDirectory()){
					logger.info(String.format("Clean task resource %s: %s", resource, folder.getAbsolutePath()));
					FileUtils.cleanDirectory(folder);
				}
				FileItem content = params.getFile("content");
				Archive arch = ArchiveUtils.loadArchive(content);

				ArchiveEntry entry = arch.getNextEntry();				
				while(entry != null){				
					File file = new File(folder, entry.getFilename());
					arch.read(file); // this is resource related
					arch.closeEntry();
					entry = arch.getNextEntry();
				}
			}
		}
		catch(FileUploadException e){
			badRequest = true;
			logger.error("Failed to upload task resource", e);
		}
		finally{
			if(badRequest) response.sendError(HttpServletResponse.SC_BAD_REQUEST);
		}
	}

//	private static File getStorageBase(){
//		return new File("/usr/local/tomcat/webapps/test/"); 
//	}
}
