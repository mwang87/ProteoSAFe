package edu.ucsd.livesearch.storage;

import java.io.File;
import java.util.Collection;
import java.util.Set;

import org.apache.commons.fileupload.FileItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsd.livesearch.account.AccountManager;
import edu.ucsd.livesearch.servlet.ManageSharing;
import edu.ucsd.livesearch.task.NullTask;
import edu.ucsd.livesearch.task.Task;
import edu.ucsd.livesearch.task.TaskManager;
import edu.ucsd.saint.commons.WebAppProps;

/**
 * Utility class providing managed access to the CCMS web application
 * server's file system.
 * 
 * @author To-ju Huang
 * @author Jeremy Carver
 */
public class FileManager
{
	/*========================================================================
	 * Constants
	 *========================================================================*/
	public static final String USER_SPACE;
	public static final String GROUP_SPACE;
	public static final String FTP_SPACE;
	public static final String DATASET_SPACE;
	static {
		USER_SPACE  = WebAppProps.getPath("livesearch.user.path", "");
		GROUP_SPACE = WebAppProps.getPath("livesearch.group.path", "");
		FTP_SPACE = WebAppProps.getPath("livesearch.ftp.path", "");
		DATASET_SPACE = WebAppProps.getPath("livesearch.massive.path", "");
	}
	private static final Logger logger =
		LoggerFactory.getLogger(FileManager.class);
	
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	public static final boolean syncFTPSpace(String user){
		if (user == null || AccountManager.isActiveUsername(user) == false)
			return false;
		File path = new File(FTP_SPACE, user);
		if (path.exists())
			return path.isDirectory();
		else return path.mkdir();
	}
	
	public static final boolean syncUserSpace(String user){
		if (user == null || AccountManager.isActiveUsername(user) == false)
			return false;
		File path = new File(USER_SPACE, user);
		if (path.exists())
			return path.isDirectory();
		else return path.mkdir();
	}
	
	public static final File getFile(String path) {
		if (path == null)
			return null;
		String tokens[] = path.split("/");
		if (tokens.length <= 0)
			return null;
		String root = tokens[0];
		if (!root.matches("[gutf]\\..*"))
			return null;
		String ref = root.substring(2);

		File file = null;
		switch(root.charAt(0)) {
			case 'g':
				file = new File(GROUP_SPACE, ref);
				break;
			case 'u':
				file = new File(USER_SPACE, ref);
				break;
			case 'f':
				file = new File(FTP_SPACE, ref);
				break;
			case 't':
				file = new File(USER_SPACE);
				Task task = TaskManager.queryTask(ref);
				if (task instanceof NullTask)
					file = null;
				else file = new File(file, task.getUser() + "/" + ref);
		}

		for (int i=1; i<tokens.length; i++)
			file = new File(file, tokens[i]);
		if (file != null)
			logger.debug(String.format("FileManager.getPath(): " +
				"path=[%s], file=[%s], existing=[%b], %s",
				path, file.getAbsolutePath(), file.exists(),
				file.isDirectory()? "dir" : "file"));
		else logger.debug(
			String.format("FileManager.getPath(): path=[%s]", path));
		return file;
	}
	
	public static final File getFile(String user, String task, String path) {
		File file = new File(USER_SPACE);
		file = new File(file, user);
		file = new File(file, task);
		for (String segment: path.split("/"))
			file = new File(file, segment);
		if (path.endsWith("/"))
			file.mkdirs();
		else file.getParentFile().mkdirs();
		return file;
	}
	
	public static final String getOwner(String path) {
		if (path == null)
			return null;
		String tokens[] = path.split("/");
		if (tokens.length <= 0)
			return null;
		String root = tokens[0];
		if (root.matches("[df]\\..*") == false)
			return null;
		else return root.substring(2);
	}
	
	public static final boolean isOwned(File file, String user) {
		if (file == null || file.canRead() == false ||
			user == null || syncFTPSpace(user) == false)
			return false;
		// get user's root uploads folder
		File root = getFile("f." + user);
		if (root == null || root.isDirectory() == false ||
			root.canRead() == false)
			return false;
		// the specified file must be a descendant of the user's root folder
		String path = file.getAbsolutePath();
		if (path.startsWith(root.getAbsolutePath()))
			return true;
		// if the folder isn't accessible as an upload path,
		// then try a task path
		root = new File(USER_SPACE, user);
		if (root == null || root.isDirectory() == false ||
			root.canRead() == false)
			return false;
		else return path.startsWith(root.getAbsolutePath());
	}
	
	public static final boolean isAccessible(File file, String user) {
		if (file == null || file.canRead() == false ||
			user == null || syncFTPSpace(user) == false)
			return false;
		else if (isOwned(file, user))
			return true;
		else {
			Set<String> accessibleUsers =
				ManageSharing.getAccessibleUsers(user);
			if (accessibleUsers == null || accessibleUsers.isEmpty())
				return false;
			else {
				for (String accessibleUser: accessibleUsers)
					if (isOwned(file, accessibleUser))
						return true;
				return false;
			}
		}
	}
	
	public static File getOwnedFile(String path, String user) {
		if (path == null || user == null)
			return null;
		// if specified file is not owned, then it can't be retrieved
		File file = FileManager.getFile("f." + path);
		if (FileManager.isOwned(file, user) == false)
			return null;
		else return file;
	}
	
	public static File getAccessibleFile(String path, String user) {
		if (path == null || user == null)
			return null;
		// if specified file is not accessible, then it can't be retrieved
		File file = FileManager.getFile("f." + path);
		if (FileManager.isAccessible(file, user))
			return file;
		// if the folder isn't accessible as an upload path,
		// then try a task path
		else {
			file = FileManager.getFile("t." + path);
			if (FileManager.isAccessible(file, user) )
				return file;
			else {
				file = FileManager.getFile("u." + user + "/" + path);
				if (FileManager.isAccessible(file, user) )
					return file;
			}
		}
		return null;
	}
	
	public static final boolean createFile(
		String user, File file, FileItem contents
	) {
		if (user == null || syncFTPSpace(user) == false || file == null)
			return false;
		// verify ownership of parent folder
		File parent = file.getParentFile();
		if (FileManager.isOwned(parent, user) == false) {
			logger.error("Error creating file \"" + file.getAbsolutePath() +
				"\": Parent directory is not owned by user \"" + user + "\".");
			return false;
		}
		// create placeholder file
		try {
			if (file.exists())
				file.delete();
			file.createNewFile();
		} catch (Throwable error) {
			logger.error("Error creating file \"" + file.getAbsolutePath() +
				"\"", error);
			return false;
		}
		// write file contents, if present
		if (contents != null) try {
			contents.write(file);
		} catch (Throwable error) {
			logger.error("Error writing file \"" + file.getAbsolutePath() +
				"\"", error);
			return false;
		}
		logger.debug("File \"" + file.getAbsolutePath() +
			"\" was successfully created.");
		return true;
	}
	
	public static final boolean createFolder(String user, File folder) {
		if (user == null || syncFTPSpace(user) == false || folder == null)
			return false;
		// verify ownership
		File parent = folder.getParentFile();
		if (FileManager.isOwned(parent, user) == false) {
			logger.error("Error creating folder \"" + folder.getAbsolutePath() +
				"\": Parent directory is not owned by user \"" + user + "\".");
			return false;
		}
		// verify that this folder can be created
		if (folder.exists()) {
			// TODO: these should be checked exceptions, with proper
			// handling in the doPost() method
			if (folder.isDirectory()) {
				logger.error("Error creating folder \"" +
					folder.getAbsolutePath() +
					"\": This folder already exists.");
				return false;
			} else {
				logger.error("Error creating folder \"" +
					folder.getAbsolutePath() + "\": There is already a file " +
					"with this name in the same directory.");
				return false;
			}
		}
		try {
			folder.mkdir();
		} catch (Throwable error) {
			logger.error("Error creating folder \"" + folder.getAbsolutePath() +
				"\"", error.getMessage());
			return false;
		}
		logger.debug("Folder \"" + folder.getAbsolutePath() +
			"\" was successfully created.");
		return true;
	}
	
	public static final boolean renameFile(
		String user, File file, String newName
	) {
		if (user == null || syncFTPSpace(user) == false ||
			file == null || newName == null || newName.trim().equals(""))
			return false;
		String oldPath = file.getAbsolutePath();
		// verify ownership
		File parent = file.getParentFile();
		if (FileManager.isOwned(parent, user) == false) {
			logger.error("Error renaming file \"" + oldPath + "\" to \"" +
				newName + "\": File is not owned by user \"" + user + "\".");
			return false;
		}
		// verify that the user is not trying to rename his root folder
		File root = FileManager.getFile("f." + user);
		if (root == null ||
			root.getAbsolutePath().equals(oldPath)) {
			logger.error("Error renaming file \"" + oldPath + "\" to \"" +
				newName + "\": File cannot be renamed, " +
				"because it is the user's root folder.");
			return false;
		}
		// verify validity of new filename
		if (newName.contains(File.pathSeparator)) {
			logger.error("Error renaming file \"" + oldPath + "\" to \"" +
				newName + "\": New filename is a path that would " +
				"place the renamed file in a new directory.");
			return false;
		}
		// rename file
		try {
			// create new file
			File newFile = new File(parent, newName);
			file.renameTo(newFile);
		} catch (Throwable error) {
			logger.error("Error renaming file \"" + oldPath + "\" to \"" +
				newName + "\"", error.getMessage());
			return false;
		}
		logger.debug("File \"" + oldPath + "\" was successfully renamed to \"" +
			newName + "\".");
		return true;
	}
	
	public static final boolean deleteFile(String user, File file) {
		if (user == null || syncFTPSpace(user) == false || file == null)
			return false;
		String path = file.getAbsolutePath();
		// verify ownership
		File parent = file.getParentFile();
		if (FileManager.isOwned(parent, user) == false) {
			logger.error("Error deleting file \"" + path +
				"\": File is not owned by user \"" + user + "\".");
			return false;
		}
		// verify that the user is not trying to delete his root folder
		File root = FileManager.getFile("f." + user);
		if (root == null ||
			root.getAbsolutePath().equals(path)) {
			logger.error("Error deleting file \"" + path +
				"\": File cannot be deleted, because " +
				"it is the user's root folder.");
			return false;
		}
		// verify that no child files are present, if this is a folder
		if (file.isDirectory()) {
			String[] children = file.list();
			if (children != null && children.length > 0) {
				logger.error("Error deleting file \"" + path +
					"\": File cannot be deleted, because " +
					"it is a directory containing files.");
				return false;
			}
		}
		// delete file
		try {
			file.delete();
		} catch (Throwable error) {
			logger.error("Error deleting file \"" + path + "\"",
				error.getMessage());
			return false;
		}
		logger.debug("File \"" + path + "\" was successfully deleted.");
		return true;
	}
	
	
	public static final String resolvePath(
		String path, Collection<String> users
	) {
		if (path == null || users == null || users.size() < 1)
			return null;
		// find which accessible user this file belongs to
		for (String user : users) {
			// get this user's root folder
			File root = getFile("f." + user);
			// if the root folder is somehow invalid, then just skip over it
			if (root == null || root.canRead() == false ||
				root.isDirectory() == false)
				continue;
			String rootPath = root.getPath();
			// if this file is this user's root folder, then it's good
			if (path.equals(rootPath))
				return user.replace('\\', '/');
			// otherwise it has to be a descendant of this user's root folder
			else if (path.startsWith(rootPath))
				return (user +
					path.substring(rootPath.length())).replace('\\', '/');
			// if the folder isn't accessible as an upload path,
			// then try a task path
			root = new File(USER_SPACE, user);
			if (root == null || root.canRead() == false ||
				root.isDirectory() == false)
				continue;
			rootPath = root.getPath();
			// if this file is this user's task root folder, then it's good
			if (path.equals(rootPath))
				return user.replace('\\', '/');
			// otherwise it has to be a descendant
			// of this user's task root folder
			else if (path.startsWith(rootPath)) {
				// task paths should not be prefixed with the username
				String resolved = path.substring(rootPath.length());
				// trim leading slash, if present
				if (resolved.startsWith(File.separator))
					resolved = resolved.substring(File.separator.length());
				return resolved;
			}
		}
		// the file was found to not belong to any accessible user
		return null;
	}
	
	
	public static final String resolvePath(String path) {
		if (path == null)
			return null;
		// the argument path should start with the ProteoSAFe upload space
		else if (path.startsWith(FTP_SPACE))
			return path.substring(FTP_SPACE.length());
		// the file was found to not belong to any user
		else return null;
	}
}
