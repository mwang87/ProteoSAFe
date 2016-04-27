package edu.ucsd.livesearch.inspect;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringEscapeUtils;

import edu.ucsd.livesearch.inspect.InspectResult.Hit;
import edu.ucsd.livesearch.task.NullTask;
import edu.ucsd.livesearch.task.Task;
import edu.ucsd.livesearch.task.TaskManager.TaskStatus;
import edu.ucsd.livesearch.util.Commons;
import edu.ucsd.livesearch.util.FileIOUtils;

public class InspectUtils {

	private static Pattern errorPattern = Pattern.compile("\\[(E\\d{4}+)\\] (.*+)");
	private static Pattern warningPattern = Pattern.compile("\\{(W\\d{4}+)\\} (.*+)");
	private static Pattern filenamePattern = Pattern.compile("spec/(\\d++\\.\\S+)");

	public static class ErrorEntry {
		private String	code;
		private String	details;
		private int		count;

		public ErrorEntry(String code, String details){
			this.code = code;
			this.details = details;
			this.count = 0;
		}

		public int increaseCount()	{ return ++count; }
		public int getCount()		{ return count; }
		public String getDetails()	{ return details; }
		public String getCode()		{ return code; }
	}
	
	public static Collection<ErrorEntry> collectErrors(Task task, File errorFile){
		Map<String, ErrorEntry> errors = new TreeMap<String, ErrorEntry>();
		Scanner scanner = null;
		try{
			scanner = new Scanner(errorFile);
			while(scanner.hasNext()){
				String line = scanner.nextLine();
				Matcher errorMatcher = InspectUtils.errorPattern.matcher(line),
						warningMatcher = InspectUtils.warningPattern.matcher(line),
						matches = null;
				if(errorMatcher.matches()) matches = errorMatcher;
				if(warningMatcher.matches()) matches = warningMatcher;
				if(matches != null){
					String code = matches.group(1);
					String details = resolveFilename(task, matches.group(2));
					if(!errors.containsKey(code))
						errors.put(code, new ErrorEntry(code, details));
					errors.get(code).increaseCount();
				}
			}
		}
		catch(FileNotFoundException e){}
		finally{
			if(scanner != null) scanner.close();
		}
		return errors.values();
	}
	
	
	public static String resolveFilename(Task task, String desc){
		Matcher matcher = filenamePattern.matcher(desc);
		String result  = desc;
		Collection<String> found = new LinkedList<String>();
		while(matcher.find())
			found.add(matcher.group(1));
		for(String f: found){
			String realname = task.queryOriginalName(f);
			result = result.replace("spec/" + f, "[" + realname + "]");
		}
		return result;
	}

	public static String quote(String value){
		return (value == null)? "null":
			String.format("\'%s\'", StringEscapeUtils.escapeJson(value));
	}

//	public static String quote(Hit hit, String field){
//		String value = hit.getFieldValue(field);
//		return '\'' + StringEscapeUtils.escapeJavaScript(value) + '\'';
//	}
//	public static String integral(Hit hit, String field){
//		String value = hit.getFieldValue(field);
//		return FormatUtils.isInteger(value) ? value : "null"; 
//	}
//
//	public static String real(Hit hit, String field){
//		String value = hit.getFieldValue(field);		
//		return FormatUtils.isFloat(value) ? value : "null";
//	}

//	private static String JS_HIT_FIELDS[][] = {
//		{"ORIGINAL_NAME", "#SpectrumFile"},
//		{"INTERNAL_NAME", "InternalName"},
//	};

	public static void writeHitsJS(
			Writer writer, String variable, Task task){
		InspectResult result = null;		
		try{
			int counter = 0;
			result = new InspectResult(task);
			write(writer, "<script language='javascript' type='text/javascript'>%n");
			write(writer, "/* <![CDATA[ */%n");
			write(writer, "var %s = [%n", variable);
			boolean first = true;
			for(Hit hit: result){ 
				if(first){
					first = false;
					write(writer, "  {", variable);
				}
				else write(writer, " ,{", variable);
				for (String field : result.getFieldNames()) {
					String value = hit.getFieldValue(field);
					// TODO: this is a hack -- rather than explicitly
					// chopping off all extra proteins, there should be a
					// mechanism for listing them in the interface
					if (field.equals("Protein")) {
						String proteins[] = value.split("!");
						if (proteins != null && proteins.length > 0)
							value = proteins[0];
					}
					write(writer, "'%s':%s,", field, quote(value));
				}
				write(writer, "ProteinID:%s,", quote(hit.getProteinID()));
				write(writer, "Internal :%s,", quote(hit.getInternalName()));
				write(writer, "Position :%d,",   hit.getPosition());
				write(writer, "Counter  :'H%d'", counter++);
				write(writer, "  }%n");
			}
			write(writer, "];%n");
			write(writer, "/* ]]> */ %n");
			write(writer, "</script>%n");
		}
		catch(IOException e){
			e.printStackTrace();
		}
		finally{
			if (result != null)
				result.close();
		}
	}

	public static void writeHitsJS(
		Writer writer, String variable, Task task, String protein
	) {
		InspectResult result = null;
		try {
			write(writer, "<script language='javascript'>%n");
			write(writer, "var %s = [%n", variable);
			result = new InspectResult(task);
			Map<String, Collection<Hit>> groupedHits = groupHitsByProtein(task);
			if (groupedHits != null) {
				Collection<Hit> hits = groupedHits.get(protein);
				if (hits != null) {
					int counter = 0;
					boolean first = true;
					for (Hit hit : hits) {
						if (first) {
							first = false;
							write(writer, "  {", variable);
						} else write(writer, " ,{", variable);
						for (String field : result.getFieldNames())
							write(writer, "'%s':%s,", field,
								quote(hit.getFieldValue(field)));
						write(writer, "ProteinID:%s,",
							quote(hit.getProteinID()));
						write(writer, "Internal :%s,",
							quote(hit.getInternalName()));
						write(writer, "Position :%d,", hit.getPosition());
						write(writer, "Counter  :'H%d'", counter++);
						write(writer, "  }%n");
					}
				}
			}
			write(writer, "];%n");
			write(writer, "</script>%n");
		} catch(IOException error) {
			// TODO: report error
		} finally {
			if (result != null)
				result.close();
		}
	}
	
	public static void writeHitGroupsJS(
		Writer writer, String variable, Task task
	){
		try {
			write(writer, "<script language='javascript'>%n");
			write(writer, "var %s = [%n", variable);
			Map<String, Collection<Hit>> groupedHits = groupHitsByProtein(task);
			if (groupedHits != null) {
				int counter = 0;
				boolean first = true;
				for (String protein : groupedHits.keySet()) {
					Collection<Hit> hits = groupedHits.get(protein);
					if (hits == null || hits.size() < 1)
						continue;
					int hitCount = 0;
					Set<String> uniquePeptides = new TreeSet<String>();
					Set<String> uniqueModifiedPeptides = new TreeSet<String>();
					for (Hit hit : hits) {
						String peptide = clean(hit.getFieldValue("Annotation"));
						String plainPeptide = unmodify(peptide);
						if (plainPeptide != null) {
							uniquePeptides.add(plainPeptide);
							if (peptide != null && 
								peptide.equals(plainPeptide) == false)
								uniqueModifiedPeptides.add(peptide);
						}
						hitCount++;
					}
					// write protein details
					if (first) {
						first = false;
						write(writer, "  {", variable);
					} else write(writer, " ,{", variable);
					write(writer, "ProteinID :%s,",
						quote(InspectUtils.getProteinID(protein)));
					write(writer, "Protein :%s,", quote(protein));
					write(writer, "Comment :%s,",
						quote(InspectUtils.getProteinDescription(protein)));
					write(writer, "Hits    : %d,", hitCount);
					write(writer, "Counter :'P%d',", counter++);
					write(writer, "Peptides: %d,", uniquePeptides.size());
					write(writer, "Modified: %d",
						uniqueModifiedPeptides.size());
					write(writer, "  }%n");
				}
			}
			write(writer, "];%n");
			write(writer, "</script>%n");
		} catch(IOException error) {
			// TODO: report error
		}
	}
	
	public static Map<String, Collection<Hit>> groupHitsByProtein(Task task) {
		if (task == null || task instanceof NullTask)
			return null;
		Map<String, Collection<Hit>> groupedHits =
			new TreeMap<String, Collection<Hit>>();
		InspectResult result = null;
		try {
			result = new InspectResult(task);
			for (Hit hit : result) {
				String proteinField = hit.getFieldValue("Protein");
				if (proteinField == null)
					continue;
				String[] proteins = proteinField.split("!");
				// if there's only one protein in the string,
				// key this hit to that protein
				if (proteins == null || proteins.length < 1)
					proteins = new String[]{proteinField};
				// if there are many proteins in the string,
				// key this hit to the first one only
				else proteins = new String[]{proteins[0]};
				for (String protein : proteins) {
					Collection<Hit> hits = groupedHits.get(protein);
					if (hits == null)
						hits = new TreeSet<Hit>();
					hits.add(hit);
					groupedHits.put(protein, hits);
				}
			}
		} finally {
			if (result != null)
				result.close();
		}
		if (groupedHits.size() < 1)
			return null;
		else return groupedHits;
	}
	
	public static String getProteinID(String protein) {
		if (protein == null)
			return null;
		// extract the first protein, if there are many
		String proteins[] = protein.split("!");
		if (proteins != null && proteins.length > 0)
			protein = proteins[0];
		// split the protein string into parts, separated by pipe ("|")
		String start = null;
		String parts[] = protein.split("\\|");
		if (parts == null || parts.length < 1)
			start = protein;
		// TODO: remove the below clause when IPI ceases to be important
		else for (String part : parts) {
			if (part.startsWith("IPI:")) {
				start = part;
				break;
			}
		}
		if (start == null)
			start = parts[0];
		// if the first part contains a semicolon, then we assume that
		// the token before the semicolon is the database ID, and the
		// token after it is the ID of the protein itself
		if (start.contains(":")) {				
			parts = start.split(":");
			if (parts == null || parts.length < 2)
				return protein;
			else return parts[1];
		}
		// if the first part is the string "gi" or the string "pdb",
		// then we assume that the identifier is the next part
		else if (start.equals("gi") || start.equals("pdb")) {
			if (parts == null || parts.length < 2)
				return protein;
			else return parts[1];
		}
		// if the first part does not have one of the above recognized
		// formats, then we can't parse out a meaningful protein ID
		else return protein;
	}
	
	public static String getProteinDescription(String protein) {
		if (protein == null)
			return null;
		// extract the first protein, if there are many
		String proteins[] = protein.split("!");
		if (proteins != null && proteins.length > 0)
			protein = proteins[0];
		// attempt to look up the protein description
		String description = Commons.getDescription(getProteinID(protein));
		if (description == null)
			return protein;
		else return description;
	}
	
	public static String clean(String peptide) {
		if (peptide != null) {
			// remove enclosing dots and bordering residues
			int start = peptide.indexOf('.');
			// it's apparently legal for a peptide sequence to start with "[."
			if (start == 1 && peptide.charAt(0) != '[')
				peptide = peptide.substring(start + 1);
			int end = peptide.lastIndexOf('.');
			if (end == peptide.length() - 2)
				peptide = peptide.substring(0, end);
			// remove all whitespace in the sequence
			peptide = peptide.replaceAll("[ \t]+", "");
		}
		return peptide;
	}
	
	public static String unmodify(String peptide) {
		if (peptide != null)
			// remove all modifications, which are defined to be all
			// sequences of characters that are not upper-case letters
			peptide = peptide.replaceAll("[^A-Z]+", "");
		return peptide;
	}
	
	protected static void write(Writer writer, String format, Object ... args)
			throws IOException {
		writer.write(String.format(format, args));
	}

	public static String generateBrowseBlock(Task task, HttpServletRequest request){
		StringBuffer buffer = new StringBuffer();
		if(task.getStatus() == TaskStatus.DONE){
			String context = request.getContextPath();
			String format = 
				String.format("<a href=\"%s/%%s?task=%s&view=%%s\">%%s</a>", context, task.getID());
			buffer.append("[ ");
			buffer.append(String.format(format, "result.jsp", "peptide", "group by peptide")).append(" | \n");
			buffer.append(String.format(format, "result.jsp", "protein", "group by protein")).append(" ] \n");
		}
		return buffer.toString();
	}
	
	public static String generateDownloadBlock(Task task, HttpServletRequest request, String prefix){
		StringBuffer buffer = new StringBuffer();
		String context = request.getContextPath();
		String format = 
			String.format(
					"<a href='#' onclick=\"this.href='%s/inspect/Result?task=%s&target=%%s&compress='" +
					" + document.getElementById('%s_compress').value; return true;\">%%s</a>",
				context, task.getID(), prefix);
		buffer.append("[ ");
		buffer.append(String.format(format, "profile", "search profile")).append(" | \n");
		buffer.append(String.format(format, "result", "search result")).append(" | \n");
		buffer.append(String.format(format, "paris", "result images")).append(" | \n");
		buffer.append(String.format(format, "all", "all")).append(" ] \n");		
		buffer.append(String.format("<select id='%s_compress'/>", prefix));
		buffer.append("<option value='NONE'>Uncompressed</option>");
		buffer.append("<option value='ZIP' selected='selected'>ZIP</option>");
		buffer.append("<option value='GZIP'>GZIP</option>");
		buffer.append("<option value='BZIP2'>BZIP</option>");
		buffer.append("</select>");
		return buffer.toString();
	}

	public static File getResultPath(Task task){
		return FileIOUtils.getSingleFile(task.getPath("result/"));
	}

	public static File getLogPath(Task task){
		return FileIOUtils.getSingleFile(task.getPath("log/"));
	}
	
	public static File getSummaryPath(Task task){
		return FileIOUtils.getSingleFile(task.getPath("summary/"));
	}
	
	public static File getSummary(
		Task task, String proteinID, String protein, String filename
	) {
		if (task == null || proteinID == null || protein == null ||
			filename == null)
			return null;
		// check to see if file is already present, and if so, just return it
		File file = new File(filename);
		if (file.canRead())
			return file;
		else file.getParentFile().mkdirs();
		// build file content string
		InspectSummary summary = new InspectSummary(task, proteinID, protein);
		StringBuffer content = new StringBuffer();
		content.append("<h2>");
		content.append(summary.getProtein());
		content.append("</h2>\n");
		String description = summary.getDescription();
		if (description != null) {
			content.append("<h3>");
			content.append(description);
			content.append("</h3>\n");
		}
		content.append("<hr/>\n");
		if (summary.found())
			for (String line: summary.getDistribution()) {
				if (line.contains("coverage") == false) {
					content.append(line);
					content.append("\n");
				}
			}
		// write content to file
		if (FileIOUtils.writeFile(file, content.toString(), false) == false) {
			// TODO: report error
			return null;
		} else return file;
	}
}
