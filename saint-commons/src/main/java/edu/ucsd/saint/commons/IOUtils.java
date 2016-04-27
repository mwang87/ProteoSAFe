package edu.ucsd.saint.commons;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IOUtils
{
	/*========================================================================
	 * Constants
	 *========================================================================*/
	private static Logger logger = LoggerFactory.getLogger(IOUtils.class);
	private static final int STREAM_COPY_BUFFER_SIZE = 1024;
	// constructs to control file copy parameters
	public static enum FileCopyMethod {
		STREAM, APACHE;
		public void copyFile(File source, File destination)
		throws IOException {
			try {
				if (this.equals(STREAM))
					copyFileByStream(source, destination);
				else if (this.equals(APACHE))
					FileUtils.copyFile(source, destination);
				else throw new IllegalStateException(String.format(
					"Unrecognized file copy method: \"%s\"", this.toString()));
			} catch (Throwable error) {
				// report the exception
				IOErrorLogger.reportFileCopyError(
					source, destination, this, error);
				// re-throw the exception
				if (error instanceof IOException)
					throw (IOException)error;
				else throw new IOException(error);
			}
		}
	}
	private static final boolean USE_SECURE_FILE_COPY = true;
	private static final boolean RANDOMIZE_FILE_COPY_METHOD = false;
	private static final FileCopyMethod DEFAULT_FILE_COPY_METHOD =
		FileCopyMethod.APACHE;
	
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	public static boolean dumpToFile(InputStream input, File file) {
		BufferedOutputStream output = null;
		try {
	        output = new BufferedOutputStream(new FileOutputStream(file));	        
	        copyStream(input, output);
	        return true;
		} catch (Throwable error) {
			logger.error(String.format("Failed to dump to file:\n\t%s",
				file.getAbsolutePath()), error);
		} finally { 
			try { output.close(); }
			catch (Throwable error) {}
		}
		return false;
	}
	
	public static boolean dumpToFile(InputStream input, String savedAs) {
		return dumpToFile(input, new File(savedAs));
	}
	
	public static boolean appendFileTo(File file, OutputStream output) {
		BufferedInputStream input = null;
		try {
			input = new BufferedInputStream(new FileInputStream(file));
			copyStream(input, output);
			return true;
		} catch (Throwable error) {
			logger.error(String.format("Failed to append to file:\n\t%s",
				file.getAbsolutePath()), error);
		} finally {
			try { input.close(); }
			catch (Throwable error) {}			
		}
		return false;
	}
	
	public static void copyStream(InputStream input, OutputStream output)
	throws IOException {
		byte[] buffer = new byte[STREAM_COPY_BUFFER_SIZE];
		while (true) {
			int bytes = input.read(buffer, 0, STREAM_COPY_BUFFER_SIZE);
			if (bytes <= 0)
				break;
			output.write(buffer, 0, bytes);
		}
	}
	
	public static void copyFile(File source, File destination)
	throws IOException {
		// use either random or default file copy method, based on settings
		FileCopyMethod method;
		if (RANDOMIZE_FILE_COPY_METHOD) {
			FileCopyMethod[] methods = FileCopyMethod.values();
			method = methods[
				new Random(System.currentTimeMillis()).nextInt(methods.length)];
		} else method = DEFAULT_FILE_COPY_METHOD;
		// use either basic or secure file copy, based on settings
		if (USE_SECURE_FILE_COPY)
			copyFileSecurely(source, destination, method);
		else method.copyFile(source, destination);
	}
	
	public static void copyFileByStream(File source, File destination)
	throws IOException {
		BufferedInputStream input = null;
		BufferedOutputStream output = null;
		try {
			input = new BufferedInputStream(new FileInputStream(source));
	        output =
	        	new BufferedOutputStream(new FileOutputStream(destination));	
			copyStream(input, output);
		} catch (Throwable error) {
			// report the exception
			logger.error(String.format("Failed to copy file:\n\t%s ->\n\t%s",
				source.getAbsolutePath(), destination.getAbsolutePath()),
				error);
			// re-throw the exception
			if (error instanceof IOException)
				throw (IOException)error;
			else throw new IOException(error);
		} finally {
			try { input.close(); }
			catch (Throwable error) {}
			try { output.close(); }
			catch (Throwable error) {}	
		}
	}
	
	public static void copyFileSecurely(
		File source, File destination, FileCopyMethod method
	) throws IOException {
		// verify file variables
		SaintFileUtils.assertFileIsReadable(source, "Source file");
		SaintFileUtils.assertFileIsNotNull(destination, "Destination file");
		if (method == null)
			method = FileCopyMethod.APACHE;
		// collect source file statistics
		long sourceSize = source.length();
		long sourceChecksum = FileUtils.checksumCRC32(source);
		// copy source file to destination
		method.copyFile(source, destination);
		// collect destination statistics after copy
		SaintFileUtils.assertFileIsReadable(destination, "Destination file");
		long destinationSize = destination.length();
		long destinationChecksum = FileUtils.checksumCRC32(destination);
		// verify statistics
		if (destinationSize != sourceSize)
			throw new IOException(String.format(
				"File sizes did not match after copy:" +
				"\n\tsource (%s) [%d bytes]\n\tdestination (%s) [%d bytes]",
				source.getAbsolutePath(), sourceSize,
				destination.getAbsolutePath(), destinationSize));
		else if (destinationChecksum != sourceChecksum)
			throw new IOException(String.format(
				"File checksums did not match after copy:" +
				"\n\tsource (%s) [%d]\n\tdestination (%s) [%d]",
				source.getAbsolutePath(), sourceChecksum,
				destination.getAbsolutePath(), destinationChecksum));
	}
}
