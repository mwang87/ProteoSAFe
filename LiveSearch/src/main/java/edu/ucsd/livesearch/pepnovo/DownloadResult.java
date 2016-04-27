package edu.ucsd.livesearch.pepnovo;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.math.NumberUtils;

import edu.ucsd.livesearch.task.Task;
import edu.ucsd.livesearch.task.TaskManager;
import edu.ucsd.saint.commons.archive.ArchiveEntry;
import edu.ucsd.saint.commons.archive.ArchiveUtils;
import edu.ucsd.saint.commons.archive.Archiver;
import edu.ucsd.saint.commons.archive.CompressionType;

public class DownloadResult extends javax.servlet.http.HttpServlet implements javax.servlet.Servlet {
	/**
	 * 
	 */
	private static final long serialVersionUID = 5815828129815238079L;

/*	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String taskID = request.getParameter("task");
		Task task = TaskManager.queryTask(taskID);
		GenerateReadable readable = new GenerateReadable(task);
		if(!FileManager.loadOnDemand(readable))
			return;

		CompressionType compression = CompressionType.recognize(request.getParameter("compress")); 
		String filename = "pepnovo.txt";
		switch(compression){
		case ZIP:	filename = "pepnovo.zip"; break;
		case GZIP:	filename = "pepnovo.tgz"; break;
		case BZIP2:	filename = "pepnovo.tbz"; break;
		}
		String mime = compression.getMime();
		response.setContentType(mime);
		response.addHeader("Content-Disposition","attachment; filename=\"" + filename + '\"');
		Archiver archiver = ArchiveUtils.createArchiver(response.getOutputStream(), compression);
		File result = task.getPath("pepnovo.txt");		
		archiver.putNextEntry(new ArchiveEntry("pepnovo.txt", result));
		archiver.write(result);
		archiver.closeEntry();
		archiver.close();
	}*/

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String taskID = request.getParameter("task");
		String entries = request.getParameter("entries");
		String content = request.getParameter("content");
		//boolean params = "on".equals(request.getParameter("params"));
			
		Task task = TaskManager.queryTask(taskID);

		response.setContentType("application/zip");
		response.addHeader("Content-Disposition","attachment; filename=\"result.zip\"");
		Archiver archiver = ArchiveUtils.createArchiver(
			response.getOutputStream(), CompressionType.ZIP);

		boolean withEntries = "filtered".equals(content) || "checked".equals(content);
		writeResultEntry(archiver, task, withEntries, entries);

		archiver.close();
	}
	
	private void writeResultEntry(Archiver archiver, Task task, boolean withEntries, String entriesStr)
		throws IOException{
		
		Set<Long> entries = new HashSet<Long>();
		if(withEntries)
			for(String s: entriesStr.split(";")){
				long index = NumberUtils.toLong(s, -1);
				if(index != -1)
					entries.add(index);
			}
		
		PepnovoResult result = new PepnovoResult(task);
		ArchiveEntry ae = new ArchiveEntry("pepnovo.txt");
		archiver.putNextEntry(ae);
	
		result = new PepnovoResult(task);
		for(PepnovoScan scan: result.getScans()){
			if(withEntries && !entries.contains(scan.getIndex())) continue;
			if(scan.getScanNumber() < 0)
				archiver.print(String.format(
					">> %s %s%n", scan.getSpectrumFile(), scan.getTitle()));
			else archiver.print(String.format(
					">> %s %d %s%n", scan.getSpectrumFile(), scan.getScanNumber(), scan.getTitle()));
			archiver.print("#Index\tRnkScr\tPnvScr\tN-Gap\tC-Gap\t[M+H]\tCharge\tSequence\n");
			for(PepnovoHit hit: scan.getHits()){
				archiver.print(String.format("%d\t%f\t%f\t%f\t%f\t%f\t%f\t%s%n",
					hit.getIndex(),
					hit.getProb(),
					hit.getScore(),
					hit.getNGap(),
					hit.getCGap(),
					hit.getMH(),
					hit.getCharge(),
					hit.getSequence()));
			}
		}
		archiver.closeEntry();
	}
}
