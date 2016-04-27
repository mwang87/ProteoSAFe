package edu.ucsd.livesearch.servlet;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsd.saint.commons.WebAppProps;

@SuppressWarnings("serial")
public class TestSystemHealth
extends BaseServlet
{
	/*========================================================================
	 * Constants
	 *========================================================================*/
	@SuppressWarnings("unused")
	private static final Logger logger =
		LoggerFactory.getLogger(TestSystemHealth.class);
	private static final String TEST_FILE_NAME = "ProteoSAFe_test_file.txt";
	private static final String TEST_FILE_CONTENT = "TEST";
	
	/*========================================================================
	 * Servlet methods
	 *========================================================================*/
	/**
	 * <p>Accepts and processes queries to run general ProteoSAFe system
	 * health tests.
	 * 
	 * <p>By convention, a GET request to this servlet is assumed to be a
	 * request to read data only.  No creation, update, or deletion of
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
	protected void doGet(
		HttpServletRequest request, HttpServletResponse response
	) throws ServletException, IOException {
		// initialize properties
		try {
			initialize(request, false);
		} catch (ServletException error) {
			getLogger().error(
				"Error initializing servlet properties from request", error);
			response.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return;
		} catch (Throwable error) {
			getLogger().error(
				"Error initializing servlet properties from request", error);
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		}
		
		// test NFS file system root directory
		try {
			testDirectory(
				new File(WebAppProps.get("livesearch.ftp.public.path")), true);
		} catch (Throwable error) {
			reportError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
				error.getMessage(), response, error);
		}
		
		// test NFS MassIVE repository root directory (but don't test writing,
		// since the repository should be moread-only from the web servers)
		try {
			testDirectory(
				new File(WebAppProps.get("livesearch.massive.path")), false);
		} catch (Throwable error) {
			reportError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
				error.getMessage(), response, error);
		}
	}
	
	/*========================================================================
	 * Convenience methods
	 *========================================================================*/
	private void testDirectory(File directory, boolean write)
	throws Exception {
		// verify directory
		if (directory.isDirectory() == false)
			throw new IllegalArgumentException(
				String.format("Test directory \"%s\" is not a valid directory.",
					directory.getAbsolutePath()));
		else if (directory.canRead() == false)
			throw new IllegalArgumentException(
				String.format("Test directory \"%\" cannot be read.",
					directory.getAbsolutePath()));
		else if (write && directory.canWrite() == false)
			throw new IllegalArgumentException(
				String.format("Test directory \"%\" cannot be written.",
					directory.getAbsolutePath()));
		
		// if this is a write test, set up test file
		if (write) {
			File testFile = new File(directory, TEST_FILE_NAME);
			PrintWriter output = null;
			try {
				output = new PrintWriter(
					new BufferedWriter(new FileWriter(testFile, false)));
				output.print(TEST_FILE_CONTENT);
			} catch (Throwable error) {
				throw new RuntimeException(
					String.format("Could not write test file \"%s\".",
						testFile.getAbsolutePath()), error);
			} finally {
				try { output.close(); } catch (Throwable ignored) {}
			}
			
			// test written file
			if (testFile.canRead() == false)
				throw new RuntimeException(String.format(
					"Test file \"%s\" was not readable after being written.",
					testFile.getAbsolutePath()));
			else if (testFile.canWrite() == false)
				throw new RuntimeException(String.format("Test file \"%s\" " +
					"was no longer writable after being written.",
					testFile.getAbsolutePath()));
			else if (testFile.length() != TEST_FILE_CONTENT.length())
				throw new RuntimeException(String.format("Test file \"%s\" " +
					"had an incorrect file size after being written " +
					"(expected %d bytes, found %d bytes).",
					testFile.getAbsolutePath(), TEST_FILE_CONTENT.length(),
					testFile.length()));
			
			// delete test file
			if (testFile.delete() == false)
				throw new RuntimeException(
					String.format("Could not delete test file \"%s\".",
						testFile.getAbsolutePath()));
		}
	}
	
	protected void reportError(
		int code, String message, HttpServletResponse response, Throwable error
	) throws IOException {
		// wrap message for system logs
		String logMessage = null;
		if (message != null)
			logMessage = "\n\n***[SYSTEM_TEST_ERROR_BEGIN]***" + message +
				"\n\n***[SYSTEM_TEST_ERROR_END]\n";
		// log error
		if (error != null)
			getLogger().error(logMessage != null ? logMessage : "", error);
		else if (logMessage != null)
			getLogger().error(logMessage);
		// send error in HTTP response
		if (response != null) {
			if (message != null)
				response.sendError(code, message);
			else response.sendError(code);
		}	
	}
}
