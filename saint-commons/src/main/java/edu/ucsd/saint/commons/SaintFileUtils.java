package edu.ucsd.saint.commons;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SaintFileUtils
{
	/*========================================================================
	 * Constants
	 *========================================================================*/
	private static final Logger logger =
		LoggerFactory.getLogger(SaintFileUtils.class);
	
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	public static void assertFileIsNotNull(File file, String description) {
		if (description == null)
			description = "File";
		if (file == null)
			throw new NullPointerException(
				String.format("%s is null.", description));
	}
	
	public static void assertFileIsReadable(File file, String description) {
		if (description == null)
			description = "File";
		assertFileIsNotNull(file, description);
		if (file.exists() == false)
			throw new IllegalArgumentException(
				String.format("%s (%s) does not exist.",
					description, file.getAbsolutePath()));
		else if (file.canRead() == false)
			throw new IllegalArgumentException(
				String.format("%s (%s) cannot be read.",
					description, file.getAbsolutePath()));
	}
	
	public static void makeLink(File source, File destination) {
		if (source == null || source.canRead() == false)
			throw new IllegalArgumentException(
				"Link source files must be valid readable files.");
		else if (destination == null)
			throw new NullPointerException(
				"Link destination files cannot be null.");
		else if (destination.isDirectory())
			destination = new File(destination, source.getName());
		// ensure that destination directory is present
		if (destination.getParentFile().exists() == false)
			destination.getParentFile().mkdirs();
		// build link command
		String[] command = getLinkCommand(source, destination);
		StringBuffer message = new StringBuffer();
		for (String token : command)
			message.append(token).append(" ");
		logger.info("{}", message.toString());
		// execute link command
		try {
			Process process = Runtime.getRuntime().exec(command);
			Helpers.runSimpleProcess(process);
		} catch (Throwable error) {
			throw new RuntimeException(
				"Could not execute requested link operation: " +
				error.getMessage(), error);
		}
	}
	
	public static void makeLinks(Map<File, File> links, File workingDirectory) {
		if (links == null || links.isEmpty())
			return;
		else if (workingDirectory == null)
			throw new NullPointerException(
				"Working directory argument cannot be null.");
		else if (workingDirectory.isDirectory() == false)
			throw new IllegalArgumentException(
				"Working directory argument must be an existing directory.");
		// create and execute batch file for link operations
		// TODO: implement Windows version
		File batch = null;
		boolean windows = System.getProperty("os.name").startsWith("Windows");
		if (windows)
			batch = new File(workingDirectory, "SAINT_links.bat");
		else batch = new File(workingDirectory, "SAINT_links.sh");
		PrintWriter output = null;
		try {
			// open batch file
			output = new PrintWriter(new BufferedWriter(
				new FileWriter(batch, false)));
			if (windows)
				output.println("@ECHO OFF");
			else output.println("#!/bin/bash");
			// write link operations into batch file
			for (File source : links.keySet()) {
				// validate source and destination files
				if (source == null)
					throw new NullPointerException(
						"Link source files cannot be null.");
				else if (source.canRead() == false)
					throw new IllegalArgumentException(String.format(
						"Link source file \"%s\" must be valid and readable.",
						source.getAbsolutePath()));
				File destination = links.get(source);
				if (destination == null)
					throw new NullPointerException(
						"Link destination files cannot be null.");
				else if (destination.isDirectory())
					destination = new File(destination, source.getName());
				// ensure that destination directory is present
				if (destination.getParentFile().exists() == false)
					destination.getParentFile().mkdirs();
				// build and execute link command
				String[] command = getLinkCommand(source, destination);
				// prepare link operation command line
				StringBuffer line = new StringBuffer();
				for (String token : command)
					line.append(token).append(" ");
				// truncate trailing space before writing to batch file
				line.setLength(line.length() - 1);
				output.println(line.toString());
			}
			// close batch file
			if (windows)
				output.print("EXIT /B");
			else output.print("exit 0");
			output.close();
			// execute link operations batch file
			batch.setExecutable(true);
			// must redirect the error stream to ensure that a simple stream
			// gobbler will prevent the process thread from deadlocking
			Process process = new ProcessBuilder(batch.getAbsolutePath())
				.redirectErrorStream(true).start();
			Helpers.runSimpleProcess(process);
		} catch (Throwable error) {
			throw new RuntimeException(
				"Could not execute requested link operations: " +
				error.getMessage(), error);
		} finally {
			try { output.close(); } catch (Throwable error) {}
			try { batch.delete(); } catch (Throwable error) {}
		}
	}
	
	public static void removeLink(File link) {
		// validate link file to be unlinked
		if (link == null)
			return;
		else if (link.canWrite() == false)
			throw new IllegalArgumentException(
				"Symbolic links must be writable to be unlinked.");
		else try {
			if (FileUtils.isSymlink(link) == false)
				throw new IllegalArgumentException(
					"Argument file must be a symbolic link to be unlinked.");
		} catch (Throwable error) {
			throw new RuntimeException(error);
		}
		// build unlink command
		String[] command = new String[]{"unlink", link.getAbsolutePath()};
		// execute unlink command
		try {
			Process process = Runtime.getRuntime().exec(command);
			Helpers.runSimpleProcess(process);
		} catch (Throwable error) {
			throw new RuntimeException(
				"Could not execute requested unlink operation: " +
				error.getMessage(), error);
		}
	}
	
	/*========================================================================
	 * Convenience methods
	 *========================================================================*/
	private static String[] getLinkCommand(File source, File destination) {
		if (source == null || destination == null)
			return null;
		String srcPath = source.getAbsolutePath();
		String dstPath = destination.getAbsolutePath();
		return System.getProperty("os.name").startsWith("Windows") ? 
			new String[]{"CMD", "/C", "mklink",
				source.isDirectory() ? "/J" : "/H", dstPath, srcPath} :
			new String[]{"ln", "-s", srcPath, dstPath};
	}
}
