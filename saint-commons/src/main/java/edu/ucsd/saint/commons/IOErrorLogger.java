package edu.ucsd.saint.commons;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsd.saint.commons.IOUtils.FileCopyMethod;

public class IOErrorLogger
{
	/*========================================================================
	 * Constants
	 *========================================================================*/
	private static Logger logger = LoggerFactory.getLogger(IOErrorLogger.class);
	
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	public static void reportFileCopyError(
		File source, File destination, FileCopyMethod method, Throwable error
	) {
		logger.debug(String.format(
			"Error copying file:\n\t%s ->\n\t%s\n\t(Copy method \"%s\")",
			(source == null ? "NULL" : source.getAbsolutePath()),
			(destination == null ? "NULL" : destination.getAbsolutePath()),
			(method == null ? "NULL" : method.toString())), error);
	}
}
