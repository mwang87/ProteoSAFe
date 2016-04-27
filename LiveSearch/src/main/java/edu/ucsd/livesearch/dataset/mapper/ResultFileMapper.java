package edu.ucsd.livesearch.dataset.mapper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringEscapeUtils;

import edu.ucsd.livesearch.storage.FileManager;

public class ResultFileMapper
{
	/*========================================================================
	 * Constants
	 *========================================================================*/
	private static final Comparator<String> FILE_PATH_COMPARATOR =
		new FilePathComparator();
	public static final String EXTRACTED_FILE_DELIMITER = "#";
	public static final Pattern FILE_URI_PROTOCOL_PATTERN =
		Pattern.compile("file:(?:[/]{2})?(.*)");
//		Pattern.compile("[^/]+:(?:[/]{1,2})?(.*)");
	
	/*========================================================================
	 * Properties
	 *========================================================================*/
	private Set<String> spectrumFiles;
	private Set<String> resultFiles;
	private Map<String, Collection<String>> fileMap;
	private boolean isMapped;
	
	/*========================================================================
	 * Constructors
	 *========================================================================*/
	public ResultFileMapper() {
		spectrumFiles = new TreeSet<String>(FILE_PATH_COMPARATOR);
		resultFiles = new TreeSet<String>(FILE_PATH_COMPARATOR);
		fileMap =
			new TreeMap<String, Collection<String>>(FILE_PATH_COMPARATOR);
		isMapped = false;
	}
	
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	public void setSpectrumFiles(String fileDescriptor)
	throws ParseException {
		// reset spectrum files collection
		spectrumFiles = new TreeSet<String>(FILE_PATH_COMPARATOR);
		// if file descriptor is null, leave collection empty and unmapped
		if (fileDescriptor == null) {
			isMapped = false;
			return;
		}
		// otherwise, parse it and add its files to the collection
		for (String file : fileDescriptor.split(";")) {
			// if the user selected a directory, then its paths
			// need to be preserved from the point of selection
			String root = FilenameUtils.getName(file.substring(2));
			if (file.startsWith("d.")) {
				// convert directory descriptor to plain file,
				// since FileManager handles those the same
				file = "f." + file.substring(2);
				// recurse down into directories to get all descendant files
				Set<String> directory = new HashSet<String>();
				flattenFiles(file, directory);
				for (String descendant : directory)
					addSetElementUniquely(
						descendant.substring(descendant.indexOf(root)),
						spectrumFiles);
			}
			// otherwise, just add the flat file name
			else addSetElementUniquely(root, spectrumFiles);
		}
		// verify that all added spectrum files are of the correct type
		for (String file : spectrumFiles) {
			String extension = FilenameUtils.getExtension(file);
			if (extension == null ||
				(extension.equalsIgnoreCase("mgf") == false &&
				extension.equalsIgnoreCase("mzxml") == false &&
				extension.equalsIgnoreCase("mzml") == false))
					throw new ParseException(String.format(
						"File \"%s\" is invalid. Only properly-formatted " +
						"files of type .mgf, .mzXML or .mzML can be " +
						"assigned to the \"Peak List\" file category.",
						file), -1);
		}
		// if result files are not also set, then mapping cannot occur yet
		if (resultFiles == null || resultFiles.isEmpty())
			isMapped = false;
		// otherwise try to do the mapping now
		else mapFiles();
	}
	
	public void setResultFiles(String fileDescriptor, String user)
	throws IOException, ParseException {
		// reset result files collection
		resultFiles = new TreeSet<String>(FILE_PATH_COMPARATOR);
		// if file descriptor is null, leave collection empty and unmapped
		if (fileDescriptor == null) {
			isMapped = false;
			return;
		}
		// otherwise, parse it and add its files to an intermediate collection,
		// which will be a map of original path -> flattened path
		Map<String, String> parentResultFiles = new HashMap<String, String>();
		for (String file : fileDescriptor.split(";")) {
			// if the user selected a directory, then its paths
			// need to be preserved from the point of selection
			String root = FilenameUtils.getName(file.substring(2));
			if (file.startsWith("d.")) {
				// convert directory descriptor to plain file,
				// since FileManager handles those the same
				file = "f." + file.substring(2);
				// recurse down into directories to get all descendant files
				Set<String> directory = new HashSet<String>();
				flattenFiles(file, directory);
				for (String descendant : directory)
					addMapElementUniquely(descendant,
						descendant.substring(descendant.indexOf(root)),
						parentResultFiles);
			}
			// otherwise, just add the flat file name
			else addMapElementUniquely(
				file.substring(2), root, parentResultFiles);
		}
		// then traverse intermediate collection to parse out file references
		for (String file : parentResultFiles.keySet()) {
			Collection<String> referencedFiles =
				extractReferencedFilenames(file, user);
			for (String referencedFile : referencedFiles)
				// add this file reference prefixed with parent result file name
				resultFiles.add(
					parentResultFiles.get(file) + EXTRACTED_FILE_DELIMITER +
					referencedFile);
		}
		// if spectrum files are not also set, then mapping cannot occur yet
		if (spectrumFiles == null || spectrumFiles.isEmpty())
			isMapped = false;
		// otherwise try to do the mapping now
		else mapFiles();
	}
	
	public boolean isMapped() {
		return isMapped;
	}
	
	public void mapFiles()
	throws IllegalStateException {
		// reset file mapping
		fileMap =
			new TreeMap<String, Collection<String>>(FILE_PATH_COMPARATOR);
		isMapped = false;
		// verify that mapping can be done
		if (spectrumFiles == null || spectrumFiles.isEmpty())
			throw new IllegalStateException(
				"Cannot map files with an empty spectrum file collection.");
		else if (resultFiles == null || resultFiles.isEmpty())
			throw new IllegalStateException(
				"Cannot map files with an empty result file collection.");
		// compare each referenced result file to each spectrum file
		for (String uniqueResultFile : resultFiles) {
			// cut off result file prefix
			String resultFile = uniqueResultFile;
			int separator = resultFile.indexOf(EXTRACTED_FILE_DELIMITER);
			if (separator >= 0)
				resultFile = resultFile.substring(separator + 1);
			// look for the best match among all the spectrum files
			int bestComparison = 0;
			String bestMatch = null;
			for (String spectrumFile : spectrumFiles) {
				int comparison =
					FILE_PATH_COMPARATOR.compare(spectrumFile, resultFile);
				// the paths are a perfect match if the comparator returns 0
				if (comparison == 0) {
					bestMatch = spectrumFile;
					break;
				}
				// the paths don't match at all if the
				// comparator returns either 1 or -1
				else if (comparison == 1 || comparison == -1)
					continue;
				// otherwise the magnitude is proportional to the number of
				// path elements that matched; meaning higher is better
				else if (bestMatch == null || comparison > bestComparison) {
					bestMatch = spectrumFile;
					bestComparison = comparison;
				}
			}
			// if no match was found at all, then the user will have to decide;
			// otherwise, default to the best match found
			if (bestMatch != null) {
				Collection<String> mappedFiles = fileMap.get(bestMatch);
				if (mappedFiles == null)
					mappedFiles = new TreeSet<String>(FILE_PATH_COMPARATOR);
				mappedFiles.add(uniqueResultFile);
				fileMap.put(bestMatch, mappedFiles);
			}
		}
		// once all files have been considered, set the files as mapped
		isMapped = true;
	}
	
	public String getMappedFileJSON()
	throws IllegalStateException {
		if (isMapped() == false)
			throw new IllegalStateException("Files are not yet mapped!");
		// write out pre-mapped file assignments first,
		// so that we can also keep track of mapped result files
		Collection<String> mappedResultFiles =
			new HashSet<String>(resultFiles.size());
		StringBuffer mappings = new StringBuffer("\"mappings\":{");
		for (String spectrumFile : fileMap.keySet()) {
			mappings.append("\"");
			mappings.append(StringEscapeUtils.escapeJson(spectrumFile));
			mappings.append("\":[");
			for (String resultFile : fileMap.get(spectrumFile)) {
				mappedResultFiles.add(resultFile);
				mappings.append("\"");
				mappings.append(StringEscapeUtils.escapeJson(resultFile));
				mappings.append("\",");
			}
			// chomp trailing comma
			if (mappings.charAt(mappings.length() - 1) == ',')
				mappings.setLength(mappings.length() - 1);
			mappings.append("],");
		}
		// chomp trailing comma
		if (mappings.charAt(mappings.length() - 1) == ',')
			mappings.setLength(mappings.length() - 1);
		mappings.append("}");
		// write out spectrum file list
		StringBuffer json = new StringBuffer("{\"spectrum\":{");
		for (String spectrumFile : spectrumFiles) {
			json.append("\"");
			json.append(StringEscapeUtils.escapeJson(spectrumFile));
			json.append("\":true");	// spectrum files are always shown
			json.append(",");
		}
		// chomp trailing comma
		if (json.charAt(json.length() - 1) == ',')
			json.setLength(json.length() - 1);
		// write out result file list
		json.append("},\"result\":{");
		for (String resultFile : resultFiles) {
			json.append("\"");
			json.append(StringEscapeUtils.escapeJson(resultFile));
			json.append("\":");
			if (mappedResultFiles.contains(resultFile))
				json.append("false");
			else json.append("true");
			json.append(",");
		}
		// chomp trailing comma
		if (json.charAt(json.length() - 1) == ',')
			json.setLength(json.length() - 1);
		// write out pre-mapped file assignments
		json.append("},");
		json.append(mappings.toString());
		json.append("}");
		return json.toString();
	}
	
	/*========================================================================
	 * Convenience methods
	 *========================================================================*/
	private void flattenFiles(String filename, Collection<String> files) {
		File file = FileManager.getFile(filename);			
		if (file == null || file.exists() == false)
			return;
		else if (file.isDirectory())
			for (File child : file.listFiles())
				flattenFiles(
					filename + File.separatorChar + child.getName(), files);
		else files.add(filename.substring(2));
	}
	
	private Collection<String> extractReferencedFilenames(
		String filename, String user
	) throws IOException, ParseException {
		File file = FileManager.getAccessibleFile(filename, user);			
		if (file == null || file.exists() == false)
			throw new FileNotFoundException(String.format(
				"Cannot find file \"%s\" to parse filename references.",
				filename));
		// delegate parsing based on filename extension
		ResultFileListParser parser = null;
		String extension = FilenameUtils.getExtension(filename);
		if (extension == null)
			throw new ParseException(String.format(
				"Cannot determine filename extension of file " +
				"\"%s\" to parse filename references.", filename), -1);
		else if (extension.equalsIgnoreCase("mzid"))
			parser = new MzIdentMLFileListParser(file);
		else if (extension.equalsIgnoreCase("mztab"))
			parser = new MzTabFileListParser(file);
		else throw new ParseException(String.format("File \"%s\" is invalid. " +
			"Only properly-formatted files of type mzIdentML (.mzid) or " +
			"mzTab (.mztab) can be assigned to the \"Result\" file category.",
			filename), -1);
		// once parser is instantiated, invoke it
		parser.parse();
		return parser.getParsedFilenames();
	}
	
	private void addSetElementUniquely(String element, Set<String> set) {
		if (element == null || set == null)
			return;
		else if (set.contains(element))
			throw new IllegalStateException(String.format(
				"Could not add element [%s] to set, " +
				"since it was already found there.", element));
		else set.add(element);
	}
	
	private void addMapElementUniquely(
		String key, String value, Map<String, String> map
	) {
		if (key == null || value == null || map == null)
			return;
		else if (map.containsKey(key))
			throw new IllegalStateException(String.format(
				"Could not add element [%s] to map, " +
				"since it was already found there.", key));
		else map.put(key, value);
	}
	
	/*========================================================================
	 * Convenience classes
	 *========================================================================*/
	/**
	 * Comparator to sort file paths lexicographically in reverse path element
	 * order.
	 */
	private static class FilePathComparator implements Comparator<String> {
		/*====================================================================
		 * Comparator method
		 *====================================================================*/
		public int compare(String path1, String path2) {
			// first normalize all path separators
			path1 = FilenameUtils.separatorsToSystem(path1);
			path2 = FilenameUtils.separatorsToSystem(path2);
			// then split paths into arrays of path elements
			String[] elements1 =
				path1.split(StringEscapeUtils.escapeJava(File.separator));
			String[] elements2 =
				path2.split(StringEscapeUtils.escapeJava(File.separator));
			// then traverse paths in reverse order,
			// differentiating only when a path element differs
			int steps = 0;
			for (int i=elements1.length-1; i>=0; i--) {
				String element1 = elements1[i];
				// try grabbing the element from the second
				// path array that is this many steps back
				int index2 = elements2.length - 1 - steps;
				// if the second path doesn't have this many elements, then the
				// first path is longer and therefore should compare higher
				if (index2 < 0)
					return steps + 1;
				else {
					String element2 = elements2[index2];
					int comparison = element1.compareTo(element2);
					if (comparison > 0)
						return steps + 1;
					else if (comparison < 0)
						return (steps + 1) * -1;
				}
				steps++;
			}
			// if all elements of the first path have been exhausted, and the
			// second path still has more, then the second path is longer and
			// therefore should compare higher
			if (elements2.length > steps)
				return (steps + 1) * -1;
			// otherwise both paths are completely identical
			else return 0;
		}
	}
	
	public static void main(String[] args) {
		String[] paths = {
			"b/d.txt",
			"a/b/c.txt",
			"a/b/c/d.txt"
		};
		FilePathComparator comparator = new FilePathComparator();
		java.util.SortedSet<String> sortedPaths =
			new java.util.TreeSet<String>(comparator);
		for (String path : paths)
			sortedPaths.add(path);
		System.out.println("Sorted paths:");
		int i = 1;
		for (String path : sortedPaths) {
			System.out.println(String.format("\tPath %d = [%s]", i, path));
			i++;
		}
	}
}
