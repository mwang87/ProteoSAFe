package edu.ucsd.livesearch.dataset.mapper;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.regex.Matcher;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.apache.commons.lang3.StringEscapeUtils;

import edu.ucsd.livesearch.storage.FileManager;

public class MzIdentMLFileListParser
implements ResultFileListParser
{
	/*========================================================================
	 * Properties
	 *========================================================================*/
	private File file;
	private boolean isParsed;
	private Collection<String> filenames;
	
	/*========================================================================
	 * Constructors
	 *========================================================================*/
	public MzIdentMLFileListParser() {
		this(null);
	}
	
	public MzIdentMLFileListParser(File file) {
		setFile(file);
		isParsed = false;
		filenames = null;
	}
	
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	/**
	 * Sets the mzIdentML result file that will be parsed to retrieve the list
	 * of referenced filenames contained within that file.
	 * 
	 * @param file	a {@link File} object representing the mzIdentML result file
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
		// parse mzIdentML file, collect referenced filenames
		Collection<String> filenames = new LinkedHashSet<String>();
		XMLEventReader reader = null;
		try {
			// set up streamed XML file parser
			reader = XMLInputFactory.newInstance().createXMLEventReader(
				new FileReader(file));
			// read through mzIdentML file
			while (reader.hasNext()) {
				XMLEvent event = reader.nextEvent();
				int type = event.getEventType();
				if (type == XMLStreamConstants.START_ELEMENT) {
					// get basic element information
					StartElement element = event.asStartElement();
					String name = element.getName().getLocalPart();
					// if this is a <SpectraData> element, extract the filename
					if (name.equals("SpectraData")) {
						String filename = getAttribute(element, "location");
						if (filename == null)
							throw new ParseException(String.format(
								"Error parsing input mzIdentML file \"%s\" " +
								"(line %d):\n\"location\" is a required " +
								"attribute of element <SpectraData>.",
								FileManager.resolvePath(file.getAbsolutePath()),
								event.getLocation().getLineNumber()), -1);
						else {
							String cleaned = resolveFilename(filename);
							if (cleaned != null &&
								cleaned.trim().equals("") == false)
								filenames.add(cleaned);
							else throw new ParseException(String.format(
								"Error parsing input mzIdentML file \"%s\" " +
								"(line %d):\nAttribute \"location\" of " +
								"element <SpectraData> contains an empty " +
								"file reference%s.",
								FileManager.resolvePath(file.getAbsolutePath()),
								event.getLocation().getLineNumber(),
								filename.equals("") ? "" :
									String.format(" (\"%s\")", filename)), -1);
						}
					}
				}
			}
		} catch (XMLStreamException error) {
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
				"mzIdentML file in order to retrieve its list of referenced " +
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
	// retrieves an attribute value from an opening element tag,
	// as per the Java XML stream API
	private static String getAttribute(StartElement element, String name) {
		if (element == null || name == null)
			return null;
		Attribute attribute = element.getAttributeByName(new QName(name));
		if (attribute == null)
			return null;
		else return attribute.getValue();
	}
	
	private static String resolveFilename(String filename) {
		if (filename == null)
			return null;
		// if this is a file URI, clean it
		Matcher matcher =
			ResultFileMapper.FILE_URI_PROTOCOL_PATTERN.matcher(filename);
		if (matcher.matches())
			filename = matcher.group(1);
		// TODO: convert to mapped filename
		return filename;
	}
}
