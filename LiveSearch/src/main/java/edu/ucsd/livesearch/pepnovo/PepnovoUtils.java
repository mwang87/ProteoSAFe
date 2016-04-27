package edu.ucsd.livesearch.pepnovo;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Collection;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringEscapeUtils;

import edu.ucsd.livesearch.task.Task;
import edu.ucsd.livesearch.task.TaskManager.TaskStatus;
import edu.ucsd.livesearch.util.FileIOUtils;

public class PepnovoUtils {
	public static String generateBrowseBlock(Task task, HttpServletRequest request){
		StringBuffer buffer = new StringBuffer();
		if(task.getStatus() == TaskStatus.DONE){
			String context = request.getContextPath();
			String format = 
				String.format("<a href='%s/pepnovo/%%s?task=%s'>%%s</a>", context, task.getID());
			buffer.append("[ ");
			buffer.append(String.format(format, "result.jsp", "group by peptide")).append(" ] \n");
		}
		return buffer.toString();
	}
	
	public static String generateDownloadBlock(Task task, HttpServletRequest request, String prefix){
		StringBuffer buffer = new StringBuffer();
		String context = request.getContextPath();
		String format = 
			String.format("<a href='#' onclick=\"" +
				"this.href='%s/pepnovo/Download?task=%s&target=%%s&compress=' + " +
				"document.getElementById('%s_compress').value;return true;\">%%s</a>",
			context, task.getID(), prefix);
		buffer.append("[ ");
//		buffer.append(String.format(format, "profile", "search profile")).append(" | \n");
		buffer.append(String.format(format, "result", "search result")).append(" ] \n");
//		buffer.append(String.format(format, "all", "all")).append(" ] \n");		
		buffer.append(String.format("<select id='%s_compress'/>", prefix));
		buffer.append("<option value='NONE'>Uncompressed</option>");
		buffer.append("<option value='ZIP'>ZIP</option>");
		buffer.append("<option value='GZIP'>GZIP</option>");
		buffer.append("<option value='BZIP2'>BZIP</option>");
		buffer.append("</select>");
		return buffer.toString();
	}
	
	public static void writeHitsJS(Writer writer, String variable, PepnovoResult result){
		try{
			int counter = 0;
			write(writer, "<script language='javascript'>%n");
			write(writer, "var %s = [%n", variable);

			boolean first = true;
			for(PepnovoScan scan: result.getScans()){
				if(first){
					first = false;
					write(writer, "  {", variable);
				}
				else write(writer, " ,{", variable);
				Collection<PepnovoHit> hits = scan.getHits();
				PepnovoHit hit = hits.iterator().next();

				write(writer, "Title:%s,", quote(scan.getTitle()));
				write(writer, "Scan:%d,", scan.getScanNumber());
				write(writer, "Prob:%f,", hit.getProb());
				write(writer, "Score:%f,", hit.getScore());
				write(writer, "NGap:%f,", hit.getNGap());
				write(writer, "CGap:%f,", hit.getCGap());
				write(writer, "MH:%f,", hit.getMH());
				write(writer, "Charge:%f,", hit.getCharge());
				write(writer, "Sequence:%s,", quote(hit.getSequence()));
				write(writer, "Index:%d,", scan.getIndex());

				write(writer, "Counter :'P%d'", counter++);
				write(writer, "  }%n");
			}
			write(writer, "];%n");
			write(writer, "</script>%n");
		}
		catch(IOException e){
			e.printStackTrace();
		}
	}
	
	private static void write(Writer writer, String format, Object ... args)
			throws IOException {
		writer.write(String.format(format, args));
	}

	public static String quote(String value){
		return '\'' + StringEscapeUtils.escapeJson(value) + '\'';
	}

	public static File getResultPath(Task task){
		return FileIOUtils.getSingleFile(task.getPath("result/"));
	}
	
	public static File getLogPath(Task task){
		return FileIOUtils.getSingleFile(task.getPath("log/"));
	}
}
