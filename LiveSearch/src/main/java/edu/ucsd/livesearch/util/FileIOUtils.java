package edu.ucsd.livesearch.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class FileIOUtils
{
	/*========================================================================
	 * Constants
	 *========================================================================*/
	private static final Logger logger =
		LoggerFactory.getLogger(FileIOUtils.class);

	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	public static final void validateFile(File file)
	throws IOException {
		if (file == null)
			throw new FileNotFoundException("File is null.");
		else if (file.exists() == false)
			throw new FileNotFoundException("File does not exist.");
	}

	public static final void validateReadableFile(File file)
	throws IOException {
		validateFile(file);
		if (file.canRead() == false)
			throw new FileNotFoundException("File cannot be read.");
	}

	public static final void validateWritableFile(File file)
	throws IOException {
		validateFile(file);
		if (file.canWrite() == false)
			throw new FileNotFoundException("File cannot be written.");
	}

	public static final void validateDirectory(File directory)
	throws IOException {
		validateFile(directory);
		if (directory.isDirectory() == false)
			throw new FileNotFoundException("File is not a directory.");
	}

	public static final File getSingleFile(File folder) {
		File files[] = folder.listFiles();
		if (files != null && files.length > 0)
			return files[0];
		else return null;
	}

	public static final String readFile(File file) {
		if (file == null)
			return null;
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(file));
			StringBuffer contents = new StringBuffer();
			String line = null;
			while ((line = reader.readLine()) != null) {
				contents.append(line);
				contents.append("\n");
			}
			return contents.toString();
		} catch (IOException error) {
			logger.error(String.format("Error reading file \"%s\"",
				file.getAbsolutePath()), error);
			return null;
		} finally {
			if (reader != null) try {
				reader.close();
			} catch (IOException error) {}
		}
	}

	public static final byte[] readBinaryFile(File file) {
		if (file == null)
			return null;
		FileInputStream reader = null;
		try {
			reader = new FileInputStream(file);
			byte[] bytes = new byte[(int)file.length()];
			int offset = 0;
			int bytesRead = 0;
			while (offset < bytes.length &&
				(bytesRead =
					reader.read(bytes, offset, bytes.length - offset)) >= 0) {
				offset += bytesRead;
			}
			if (offset < bytes.length)
				throw new IOException(
					String.format("File \"%s\" was not completely read",
						file.getAbsolutePath()));
			else return bytes;
		} catch (IOException error) {
			logger.error(String.format("Error reading file \"%s\"",
				file.getAbsolutePath()), error);
			return null;
		} finally {
			if (reader != null) try {
				reader.close();
			} catch (IOException error) {}
		}
	}

	public static boolean writeFile(File file, String contents,
		boolean append) {
		if (file == null || contents == null)
			return false;
		// write document to file
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(file, append));
			writer.write(contents);
			return true;
		} catch (IOException error) {
			logger.error(String.format("Error writing to file \"%s\"",
				file.getAbsolutePath()), error);
			return false;
		} finally {
			if (writer != null) try {
				writer.close();
			} catch (IOException error) {}
		}
	}

	public static final Document parseXML(File file) {
		if (file == null || file.canRead() == false)
			return null;
		else {
			// read XML file contents into string
			String contents = readFile(file);
			// build XML document from parameters file
			return parseXML(contents);
		}
	}

	public static final Document parseXML(String contents) {
		if (StringUtils.isEmpty(contents))
			return null;
		else {
			// get document builder
			DocumentBuilderFactory factory =
				DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = null;
			try {
				builder = factory.newDocumentBuilder();
			} catch (ParserConfigurationException error) {
				logger.error("Error instantiating XML DocumentBuilder", error);
				return null;
			}
			// parse XML string into document
			Document document = null;
			try {
				// TODO: try to determine correct encoding from XML file
				document = builder.parse(
					new ByteArrayInputStream(contents.getBytes()));
			} catch (IOException error) {
				logger.error("Error parsing XML document", error);
				return null;
			} catch (SAXException error) {
				logger.error("Error parsing XML document", error);
				return null;
			}
			return document;
		}
	}

	public static final String printXML(Document document) {
		if (document == null)
			return null;
		else {
			Transformer transformer = null;
			try {
				transformer =
					TransformerFactory.newInstance().newTransformer();
			} catch (TransformerConfigurationException error) {
				logger.error("Error instantiating XML Transformer", error);
				return null;
			}
			transformer.setOutputProperty(
				OutputKeys.OMIT_XML_DECLARATION, "no");
			transformer.setOutputProperty(OutputKeys.STANDALONE, "yes");
			transformer.setOutputProperty(OutputKeys.METHOD, "xml");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			// necessary to actually indent the XML for true pretty-printing;
			// temporarily disabled since MS-GFDB's parser
			// apparently breaks due to the added whitespace
//			transformer.setOutputProperty(
//				"{http://xml.apache.org/xslt}indent-amount", "2");
			StreamResult result = new StreamResult(new StringWriter());
			DOMSource source = new DOMSource(document);
			try {
				transformer.transform(source, result);
			} catch (TransformerException error) {
				logger.error("Error transforming XML", error);
				return null;
			}
			return result.getWriter().toString();
		}
	}

	public static final String escapeFilename(String filename) {
		if (filename == null)
			return null;
		// legal filename characters are assumed to be: letters (a-zA-Z),
		// digits (0-9), underscores (_), dots (.), dashes (-), and slashes (/)
		Pattern legalCharacters = Pattern.compile("\\w|\\.|-|/");
		// examine each character for legality, escape any illegal ones found
		StringBuffer escapedFilename = new StringBuffer();
		boolean escaped = false;
		for (int i=0; i<filename.length(); i++) {
			String character = Character.toString(filename.charAt(i));
			// if this character was preceded by a single backslash,
			// then it's escaped and is acceptable
			if (escaped)
				escaped = false;
			// if a backslash is found, and the previous character was not
			// also a backslash, then this is the beginning of an escape
			// sequence and the next character should be passed accordingly
			else if (character.equals("\\"))
				escaped = true;
			// otherwise, if this character is illegal and has not
			// already been escaped, then it must be escaped now
			else if (legalCharacters.matcher(character).matches() == false)
				character = "\\" + character;
			escapedFilename.append(character);
		}
		return escapedFilename.toString();
	}

	public static final String escapeFilenameWithSpaces(String filename) {
		if (filename == null)
			return null;
		// legal filename characters are assumed to be: letters (a-zA-Z),
		// digits (0-9), underscores (_), dots (.), dashes (-), and slashes (/)
		Pattern legalCharacters = Pattern.compile("\\w|\\.|-|/| ");
		// examine each character for legality, escape any illegal ones found
		StringBuffer escapedFilename = new StringBuffer();
		boolean escaped = false;
		for (int i=0; i<filename.length(); i++) {
			String character = Character.toString(filename.charAt(i));
			// if this character was preceded by a single backslash,
			// then it's escaped and is acceptable
			if (escaped)
				escaped = false;
			// if a backslash is found, and the previous character was not
			// also a backslash, then this is the beginning of an escape
			// sequence and the next character should be passed accordingly
			else if (character.equals("\\"))
				escaped = true;
			// otherwise, if this character is illegal and has not
			// already been escaped, then it must be escaped now
			else if (legalCharacters.matcher(character).matches() == false)
				character = "\\" + character;
			escapedFilename.append(character);
		}
		return escapedFilename.toString();
	}
}
