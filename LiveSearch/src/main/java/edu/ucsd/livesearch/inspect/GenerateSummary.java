package edu.ucsd.livesearch.inspect;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import edu.ucsd.livesearch.task.OnDemandOperation;
import edu.ucsd.livesearch.task.Task;
import edu.ucsd.livesearch.task.TaskConfig;
import edu.ucsd.livesearch.util.Commons;
import edu.ucsd.saint.commons.WebAppProps;

public class GenerateSummary implements OnDemandOperation{
//	private static final String FILTERED_OUTPUT = "inspect.out";
	private static final String HTML_OUTPUT = "inspect.html";	

	private Task task;
	TaskConfig config;
	private File summary;
	private final String INSPECT_PATH;
	
	public GenerateSummary(Task task){
		this.task = task; 
		config = task.getConfig();
		summary = task.getPath(HTML_OUTPUT);
		INSPECT_PATH = WebAppProps.getPath("livesearch.util.path") + File.separatorChar + "inspect";
	}

	public String getResourceName() {
		return task.getID() + ".summary";
	}

	public boolean execute() {
		File binary = new File(INSPECT_PATH, "Summary.py");
		File summary = task.getPath(HTML_OUTPUT);
		File source = InspectUtils.getResultPath(task);
		try{
			PrintWriter writer = null;
			try{
				writer = new PrintWriter(task.getPath("log/summary.log"));
				ProcessBuilder pb = new ProcessBuilder(
					"python", binary.getAbsolutePath(),
					"-r", source.getAbsolutePath(),
					"-w", summary.getAbsolutePath());
				List<String> command = pb.command();
				for(File database: config.getSeqFiles()){
					command.add("-d");
					command.add(database.getAbsolutePath());
				}
				for(String cmd: pb.command())
					writer.print(cmd + ' ');
				writer.println();
				Commons.executeProcess(new File(INSPECT_PATH), writer , pb);
				return true;
			}
			finally{
				if(writer != null)
					writer.close();
			}
		}			
		catch(IOException e){
			e.printStackTrace();
		}
		return false;
	}

	public boolean resourceExists() {
		return summary.exists();
	}
	
	public boolean resourceDated(){
		return false;
	}
	
	public File getFile(){
		return summary;
	}
}
