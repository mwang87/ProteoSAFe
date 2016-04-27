package edu.ucsd.livesearch.dataset.mapper;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.ParseException;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringEscapeUtils;

import edu.ucsd.livesearch.storage.FileManager;

public class MzTabFileListParser
implements ResultFileListParser
{
	/*========================================================================
	 * Constants
	 *========================================================================*/
	private static final Pattern FILE_LINE_PATTERN =
		Pattern.compile("^MTD\\s+ms_run\\[(\\d+)\\]-location\\s+([^\\s]+)$");
	
	/*========================================================================
	 * Properties
	 *========================================================================*/
	private File file;
	private boolean isParsed;
	private Collection<String> filenames;
	
	/*========================================================================
	 * Constructors
	 *========================================================================*/
	public MzTabFileListParser() {
		this(null);
	}
	
	public MzTabFileListParser(File file) {
		setFile(file);
		isParsed = false;
		filenames = null;
	}
	
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	/**
	 * Sets the mzTab result file that will be parsed to retrieve the list
	 * of referenced filenames contained within that file.
	 * 
	 * @param file	a {@link File} object representing the mzTab result file
	 * 				to be parsed
	 */
	@Override
	public void setFile(File file) {
		// if this is a new file, then it needs to be parsed
		if (file == null || file.equals(this.file) == false)
			isParsed = false;
		// set this file as the new file to be parsed
		this.file = file;
	}
	
	/**
	 * Returns <code>true</code> if a result file has been assigned to this
	 * parser, and has been correctly parsed.
	 * 
	 * @return <code>true</code> if a result file has been set and parsed;
	 *         <code>false</code> otherwise
	 */
	@Override
	public boolean isParsed() {
		return isParsed;
	}
	
	/**
	 * Parses the associated result file to retrieve its encoded list of
	 * referenced files.
	 * 
	 * @throws	IllegalStateException	if no result file has been set yet
	 * @throws	IOException				if there is a problem reading the file
	 * @throws	ParseException			if there is an error parsing the file
	 */
	@Override
	public void parse()
	throws IllegalStateException, IOException, ParseException {
		if (file == null)
			throw new IllegalStateException(
				"You must assign a valid mzIdentML file in order to parse it.");
		// set parsed status to false, in case there's a parsing error
		isParsed = false;
		this.filenames = null;
		// parse mzTab file, collect referenced filenames
		Collection<String> filenames = new LinkedHashSet<String>();
		RandomAccessFile reader = null;
		try {
			reader = new RandomAccessFile(file, "r");
			int lineCount = 0;
			long position = -1;
			String line = null;
			while (true) {
				position = reader.getFilePointer();
				line = reader.readLine();
				if (line == null)
					break;
				lineCount++;
				// don't keep reading if we're past the metadata section
				if (line.startsWith("MTD") == false &&
					line.startsWith("COM") == false &&
					line.trim().equals("") == false)
					break;
				// get spectrum file data, if this is a file location line
				Matcher matcher = FILE_LINE_PATTERN.matcher(line);
				if (matcher.matches()) {
					String filename = matcher.group(2);
					String cleaned = resolveFilename(filename);
					if (cleaned != null && cleaned.trim().equals("") == false)
						filenames.add(cleaned);
					else throw new ParseException(String.format("Error " +
						"parsing input mzTab file \"%s\" (line %d):" +
						"\nSpectrum file line contains an empty file " +
						"reference%s.",
						FileManager.resolvePath(file.getAbsolutePath()),
						lineCount, filename.equals("") ? "" :
							String.format(" (\"%s\")", filename)),
						(int)position);
				}
			}
		} catch (ParseException error) {
			throw error;
		} catch (Throwable error) {
			throw new IOException(error);
		} finally {
			try { reader.close(); } catch (Throwable error) {}
		}
		// save parsed filenames, mark file as parsed
		this.filenames = filenames;
		isParsed = true;
	}
	
	/**
	 * Returns the list of referenced filenames encoded in the parsed result
	 * file.
	 * 
	 * @return 	a {@link Collection} of {@link String}s representing the list
	 * 			of referenced files encoded in the parsed result file.
	 *         
	 * @throws	IllegalStateException	if the result file has not been set or
	 * 									parsed yet
	 */
	@Override
	public Collection<String> getParsedFilenames()
	throws IllegalStateException {
		if (isParsed() == false)
			throw new IllegalStateException("You must parse a valid " +
				"mzTab file in order to retrieve its list of referenced " +
				"filenames.");
		else return filenames;
	}
	
	/**
	 * Returns the list of referenced filenames encoded in the parsed result
	 * file, as a JSON string.
	 * 
	 * @return 	a {@link String} representing the list of referenced files
	 * 			encoded in the parsed result file, in JSON format.
	 *         
	 * @throws	IllegalStateException	if the result file has not been set or
	 * 									parsed yet
	 */
	@Override
	public String getParsedFilenamesJSON()
	throws IllegalStateException {
		Collection<String> filenames = getParsedFilenames();
		StringBuffer json = new StringBuffer("[");
		for (String filename : filenames) {
			json.append("\"");
			json.append(StringEscapeUtils.escapeJson(filename));
			json.append("\",");
		}
		// chomp trailing comma
		if (json.charAt(json.length() - 1) == ',')
			json.setLength(json.length() - 1);
		json.append("]");
		return json.toString();
	}
	
	/*========================================================================
	 * Convenience methods
	 *========================================================================*/
	private static String resolveFilename(String filename) {
		if (filename == null)
			return null;
		// account for buggy mzidentml-lib implementation
		Pattern pattern = Pattern.compile("^[^:/]+:/{2,3}([^:/]+://.*)$");
		Matcher matcher = pattern.matcher(filename);
		if (matcher.matches())
			filename = matcher.group(1);
		// if this is a file URI, clean it
		matcher = ResultFileMapper.FILE_URI_PROTOCOL_PATTERN.matcher(filename);
		if (matcher.matches())
			filename = matcher.group(1);
		// TODO: convert to mapped filename
		return filename;
	}
}
