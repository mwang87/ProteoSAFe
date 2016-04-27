package edu.ucsd.livesearch.storage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsd.livesearch.servlet.ManageSharing;
import edu.ucsd.livesearch.task.Task;
import edu.ucsd.livesearch.util.Commons;
import edu.ucsd.saint.commons.ConnectionPool;
import edu.ucsd.saint.commons.SaintFileUtils;
import edu.ucsd.saint.commons.archive.Archive;
import edu.ucsd.saint.commons.archive.ArchiveEntry;
import edu.ucsd.saint.commons.archive.ArchiveUtils;

public class ResourceAgent {
	private static final Logger logger = LoggerFactory.getLogger(ResourceAgent.class);
	
	private Task task;
	private int counter;
	//private boolean isGuest;

	ResourceAgent(Task task){
		this.task = task;
		this.counter = 0;
	}
	
	public List<String> aquireOnServer(
		String resources, String purpose, File folder
	) throws IOException {
		List<String> result = new LinkedList<String>();
		if (resources == null)
			return result;
		Connection conn = null;
		PreparedStatement stmt = null;
		ConnectionPool pool = null;
		try {
			pool = Commons.getConnectionPool();
			conn = pool.aquireConnection();
			stmt = conn.prepareStatement(
				"INSERT INTO uploads" +
				"(user_id, task_id, purpose, original_name, saved_as, upload_time, source) " +
				"VALUES(?, ?, ?, ?, ?, NOW(), 'server-side')");
			stmt.setString(1, task.getUser());
			stmt.setString(2, task.getID());
			stmt.setString(3, purpose);
			
			Collection<String> resourceNames = new LinkedHashSet<String>();
			for (String name : resources.split(";")) try {
				resolveResources(name, task.getUser(), resourceNames);
			} catch (FileNotFoundException error) {
				continue;
			}
			
			String type = folder.getName();
			for (String resource : resourceNames) {
				File target = FileManager.getFile(resource);
				String namedAs = String.format(type + "-%05d.%s", counter++,
					FilenameUtils.getExtension(target.getName()));
				stmt.setString(4, resource.substring(2));
				stmt.setString(5, namedAs);
				if (stmt.executeUpdate() == 1) {
					SaintFileUtils.makeLink(target, new File(folder, namedAs));
					result.add(namedAs);
				}
			}
		} catch (Throwable error) {
			String message = String.format(
				"Failed to acquire resources [%s] on server for purpose [%s]",
				resources, purpose);
			logger.error(message, error);
			throw new IOException(message, error);
		} finally {
			pool.close(stmt, conn);
		}
		return result;
	}
	
	public List<String> aquireFromRequest(FileItem item, String purpose, File folder){
		List<String> result = new LinkedList<String>();
		if(item == null) return result;
		Connection conn = null;
		PreparedStatement stmt = null;
		ConnectionPool pool = null;
		try{
			pool = Commons.getConnectionPool();
			conn = pool.aquireConnection();
			stmt = conn.prepareStatement(
				"INSERT INTO uploads" +
				"(user_id, task_id, purpose, original_name, saved_as, upload_time, source) " +
				"VALUES(?, ?, ?, ?, ?, NOW(), 'uploaded')");
			stmt.setString(1, task.getUser());
			stmt.setString(2, task.getID());
			stmt.setString(3, purpose);
			
			Archive ai = ArchiveUtils.loadArchive(item);
			ArchiveEntry ae = null;
			while((ae = ai.getNextEntry()) != null){
				String savedAs = String.format("%05d.%s", counter++, ae.getExtension());
				stmt.setString(4, ae.getFilename());
				stmt.setString(5, savedAs);
				if(stmt.executeUpdate() == 1){
					// exactly ONE record is inserted, i.e. successful insertion 
					ai.read(new File(folder, savedAs));
					result.add(savedAs);
				}
				ai.closeEntry();
			}
		}
		catch(Throwable th){
			logger.error(
				String.format(
					"Failed to aquire resources from request for [%s]", purpose), th);
		}
		finally{
			pool.close(stmt, conn);
		}
		return result;
	}
	
	/*========================================================================
	 * Public static interface methods
	 *========================================================================*/
	public static void resolveResources(
		String filename, String user, Collection<String> files
	) throws FileNotFoundException {
		if (filename == null || user == null || files == null)
			return;
		String processedFilename = filename;
		File target = FileManager.getFile(processedFilename);
		// if the folder isn't accessible as an upload path,
		// then try a task path
		if (target == null || target.exists() == false) {
			processedFilename = "u." + user + "/" + filename.substring(2);
			target = FileManager.getFile(processedFilename);
			// if this isn't a task file owned by this user,
			// try the rest of the user's accessible users
			if (target == null || target.exists() == false) {
				Collection<String> users =
					ManageSharing.getAccessibleUsers(user);
				if (users != null && users.isEmpty() == false) {
					for (String accessible : users) {
						processedFilename =
							"u." + accessible + "/" + filename.substring(2);
						target = FileManager.getFile(processedFilename);
						if (target != null && target.exists())
							break;
					}
				}
				if (target == null || target.exists() == false)
					throw new FileNotFoundException(
						String.format("Could not find file [%s].", filename));
			}
		}
		if (target.isDirectory()) {
			for (File file : target.listFiles())
				resolveResources(
					processedFilename + "/" + file.getName(), user, files);
		} else files.add(processedFilename);
	}
}
