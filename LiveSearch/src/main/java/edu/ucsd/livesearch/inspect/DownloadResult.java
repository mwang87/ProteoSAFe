package edu.ucsd.livesearch.inspect;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.math.NumberUtils;

import edu.ucsd.livesearch.inspect.InspectResult.Hit;
import edu.ucsd.livesearch.parameter.LegacyParameterConverter;
import edu.ucsd.livesearch.task.OnDemandLoader;
import edu.ucsd.livesearch.task.Task;
import edu.ucsd.livesearch.task.TaskManager;
import edu.ucsd.saint.commons.archive.ArchiveEntry;
import edu.ucsd.saint.commons.archive.ArchiveUtils;
import edu.ucsd.saint.commons.archive.Archiver;
import edu.ucsd.saint.commons.archive.CompressionType;

public class DownloadResult extends javax.servlet.http.HttpServlet implements javax.servlet.Servlet {	
	private static final long serialVersionUID = -9058782014932478168L;
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String taskID = request.getParameter("task");
		String entries = request.getParameter("entries");
		String content = request.getParameter("content");
		String option = request.getParameter("option");
		String type = request.getParameter("type");

		if("paris".equals(option)) entries = "all";
			
		Task task = TaskManager.queryTask(taskID);

		response.setContentType("application/zip");
		response.addHeader("Content-Disposition","attachment; filename=\"result.zip\"");
		Archiver archiver = ArchiveUtils.createArchiver(
			response.getOutputStream(), CompressionType.ZIP);
		for(ArchiveEntry entry: collectArchiveEntries(task, option)){
			archiver.putNextEntry(entry);
			archiver.write(entry.getFile());
			archiver.closeEntry();			
		}
		boolean withEntries =
			"filtered".equals(content) || "checked".equals(content);
		if (type == null || type.equals("protein") == false) {
			writeResultEntry(archiver, task, withEntries, entries);
			writeProteinEntry(archiver, task, false, null);
		} else {
			writeResultEntry(archiver, task, false, null);
			writeProteinEntry(archiver, task, withEntries, entries);
		}
		archiver.close();
	}

	private ArchiveEntry getParamsEntry(Task task){
		//OnDemandLoader.load(new GenerateProfile(task));
		if (task == null)
			return null;
		else {
			// ensure that parameters file is present
			LegacyParameterConverter paramsLoader =
				new LegacyParameterConverter(task);
			if (OnDemandLoader.load(paramsLoader))
				return new ArchiveEntry(
					"params.xml", task.getPath("params/params.xml"));
			else return null;
		}
	}

	private Collection<ArchiveEntry> getParisEntries(Task task){
		Collection<ArchiveEntry> entries = new LinkedList<ArchiveEntry>(); 		
		if(task == null) return entries;
		GenerateForParis paris = new GenerateForParis(task);
		if(!OnDemandLoader.load(paris)) return entries;
		File folder = paris.getFolder();
		for(File file: folder.listFiles()){
			String name = "paris/" + file.getName();
			entries.add(new ArchiveEntry(name, file));
		}
		return entries;
	}

	
	private Collection<ArchiveEntry> collectArchiveEntries(
			Task task, String option){
		Collection<ArchiveEntry> E = new LinkedList<ArchiveEntry>();
		ArchiveEntry profile = getParamsEntry(task);
		if (profile != null)
			E.add(profile);
		if("paris".equals(option)) E.addAll(getParisEntries(task));
		return E;
	}
	
	private void writeResultEntry(
		Archiver archiver, Task task, boolean withEntries, String entries
	) throws IOException {
		InspectResult result = null;
		try {
			result = new InspectResult(task);
			ArchiveEntry ae = new ArchiveEntry("result.txt");
			archiver.putNextEntry(ae);
			String fields[] = result.getFieldNames();
			for(String field: fields) archiver.print(field + "\t");
			archiver.println();
			if(withEntries)
				for(String s: entries.split(";")){
					long pos = NumberUtils.toLong(s, -1);
					if(pos < 0) continue;
					result.seek(pos);
					for(String value: result.next().getFieldValues())
						archiver.print(value + "\t");
					archiver.println();
				}
			else
				while(result.hasNext()){
					for(String value: result.next().getFieldValues())
						archiver.print(value + "\t");
					archiver.println();
				}
			archiver.closeEntry();
		} finally {
			if (result != null)
				result.close();
		}
	}
	
	private void writeProteinEntry(
		Archiver archiver, Task task, boolean withEntries, String entries
	) throws IOException {
		// set up archive entry for this file
		InspectResult result = null;
		try {
			result = new InspectResult(task);
			ArchiveEntry ae = new ArchiveEntry("proteins.txt");
			archiver.putNextEntry(ae);
			// print header line
			archiver.print("ProteinID\tProtein\tComment\tPosition\t" +
				"Hits\tCounter\tPeptides\tModified");
			archiver.println();
			// print protein rows
			int counter = 0;
			Hit hit = result.next();
			while (hit != null) {
				int hits = 0;
				Hit current = hit;
				String protein = hit.getFieldValue("Protein");
				String description = hit.getDescription();
				// don't include this hit if it's not in the specified
				// set of proteins
				// TODO: a simple string "contains" check may not be adequate,
				// since it will return true if a specified entry has a name
				// that contains the protein name, but is not equal
				if (withEntries && entries.contains(protein) == false) {
					hit = result.next();
					continue;
				}
				Set<String> uniquePeptides = new HashSet<String>();
				Set<String> uniqueModifiedPeptides = new HashSet<String>();
				while (hit != null &&
					hit.getFieldValue("Protein").equals(protein)) {
					String peptide =
						InspectUtils.clean(hit.getFieldValue("Annotation"));
					String plainPeptide = InspectUtils.unmodify(peptide);
					if (plainPeptide != null) {
						uniquePeptides.add(plainPeptide);
						if (peptide != null && !peptide.equals(plainPeptide))
							uniqueModifiedPeptides.add(peptide);
					}
					hit = result.next();
					hits++;
				}
				archiver.print(current.getProteinID() + "\t");
				archiver.print(protein + "\t");
				if (protein.equals(description))
					archiver.print("N/A\t");
				else archiver.print(description + "\t");
				archiver.print(current.getPosition() + "\t");
				archiver.print(hits + "\t");
				archiver.print(counter++ + "\t");
				archiver.print(uniquePeptides.size() + "\t");
				archiver.print(uniqueModifiedPeptides.size() + "\t");
				archiver.println();
			}
			archiver.closeEntry();
		} finally {
			if (result != null)
				result.close();
		}
	}
}

/*	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
String taskID = request.getParameter("task");
String target = request.getParameter("target");
String compress = request.getParameter("compress");

target = correctTarget(target);
Task task = TaskManager.queryTask(taskID);
GenerateReadable readable = new GenerateReadable(task);
if(!FileManager.loadOnDemand(readable) || target == null)
	return;

CompressionType type = decideCompression(target, compress);		
String filename = decideFilename(target, type);

String mime = type.getMime();
response.setContentType(mime);
response.addHeader("Content-Disposition","attachment; filename=\"" + filename + '\"');
Archiver archiver = ArchiveUtils.createArchiver(response.getOutputStream(), type);

for(ArchiveEntry entry: decideEntries(target, task)){
	archiver.putNextEntry(entry);
	archiver.write(entry.getFile());
	archiver.closeEntry();			
}
archiver.close();
}

private ArchiveEntry getResultEntry(Task task){
FileManager.loadOnDemand(new GenerateReadable(task));		
return task != null ? new ArchiveEntry("result.txt", task.getPath("inspect.txt")) : null;
}

private String correctTarget(String target){
if(target == null) return null;
if(target.equals("result") || target.equals("profile") ||
	target.equals("all") || target.equals("paris"))
	return target;
return null;
}

private CompressionType decideCompression(String target, String comp){
CompressionType type = CompressionType.recognize(comp);
if(target.equals("result") || target.equals("profile"))
	return type == CompressionType.NONE ? CompressionType.TEXT : type;
if(!type.isArchiveType()) return CompressionType.ZIP;
return type;
}

private String decideFilename(String target, CompressionType type){
String base = "inspect_" + target;
switch(type){
case BZIP2: return base + ".tbz";
case GZIP:	return base + ".tgz";
case ZIP:	return base + ".zip";
case TEXT:	return base + ".txt";
default:	return  base + ".out";
}
}
*/	
