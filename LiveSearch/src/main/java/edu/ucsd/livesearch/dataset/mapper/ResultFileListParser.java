package edu.ucsd.livesearch.dataset.mapper;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Collection;

public interface ResultFileListParser
{
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	/**
	 * Sets the result file that will be parsed to retrieve the list of
	 * referenced filenames contained within that file.
	 * 
	 * @param file	a {@link File} object representing the result file to be
	 * 				parsed
	 */
	public void setFile(File file);
	
	/**
	 * Returns <code>true</code> if a result file has been assigned to this
	 * parser, and has been correctly parsed.
	 * 
	 * @return <code>true</code> if a result file has been set and parsed;
	 *         <code>false</code> otherwise
	 */
	public boolean isParsed();
	
	/**
	 * Parses the associated result file to retrieve its encoded list of
	 * referenced files.
	 * 
	 * @throws	IllegalStateException	if no result file has been set yet
	 * @throws	IOException				if there is a problem reading the file
	 * @throws	ParseException			if there is an error parsing the file
	 */
	public void parse()
	throws IllegalStateException, IOException, ParseException;
	
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
	public Collection<String> getParsedFilenames()
	throws IllegalStateException;
	
	/**
	 * Returns the list of referenced filenames encoded in the parsed result
	 * file, as a JSON string.
	 * 
	 * @return 	a {@link String}s representing the list of referenced files
	 * 			encoded in the parsed result file, in JSON format.
	 *         
	 * @throws	IllegalStateException	if the result file has not been set or
	 * 									parsed yet
	 */
	public String getParsedFilenamesJSON()
	throws IllegalStateException;
}
