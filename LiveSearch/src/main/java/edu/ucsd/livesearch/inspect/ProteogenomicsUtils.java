package edu.ucsd.livesearch.inspect;

import java.io.File;
import java.io.IOException;
import java.io.Writer;

import javax.servlet.http.HttpServletRequest;

import edu.ucsd.livesearch.inspect.ProteogenomicsResult.Hit;
import edu.ucsd.livesearch.task.Task;
import edu.ucsd.livesearch.task.TaskManager.TaskStatus;
import edu.ucsd.livesearch.util.FileIOUtils;

public class ProteogenomicsUtils
extends InspectUtils
{
	public static String generateBrowseBlock(Task task,
		HttpServletRequest request) {
		StringBuffer buffer = new StringBuffer();
		if (task.getStatus() == TaskStatus.DONE) {
			String context = request.getContextPath();
			String format = String.format(
				"<a href='%s/inspect/%%s?task=%s'>%%s</a>", context,
				task.getID());
			buffer.append("Known Peptides [ ");
			buffer.append(
				String.format(format, "result.jsp", "group by peptide"))
					.append(" |\n");
			buffer.append(
				String.format(format, "protein_sorted.jsp", "group by protein"))
					.append(" ]\n<br/>\n");
			format = String.format(
				"<a href='%s/inspect/%%s?task=%s&options=novel'>%%s</a>",
				context, task.getID());
			buffer.append("Novel Peptides [ ");
			buffer.append(
				String.format(format, "result.jsp", "group by peptide"))
					.append(" ]\n");
		}
		return buffer.toString();
	}
	
	public static void writeHitsJS(Writer writer, String variable, Task task) {
		ProteogenomicsResult result = null;		
		try {
			int counter = 0;
			result = new ProteogenomicsResult(task);
			write(writer,
				"<script language='javascript' type='text/javascript'>%n");
			write(writer, "/* <![CDATA[ */%n");
			write(writer, "var %s = [%n", variable);
			boolean first = true;
			for (Hit hit: result) { 
				if (first) {
					first = false;
					write(writer, "  {", variable);
				}
				else write(writer, " ,{", variable);
				for(String field: result.getFieldNames())
					write(writer, "'%s':%s,", field,
						quote(hit.getFieldValue(field)));
				write(writer, "Internal :%s,", quote(hit.getInternalName()));
				write(writer, "Position :%d,",   hit.getPosition());
				write(writer, "Counter  :'H%d'", counter++);
				write(writer, "  }%n");
			}
			write(writer, "];%n");
			write(writer, "/* ]]> */ %n");
			write(writer, "</script>%n");
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			result.close();
		}
	}

	public static File getResultPath(Task task){
		return FileIOUtils.getSingleFile(task.getPath("novel/"));
	}
}
