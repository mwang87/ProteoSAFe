package edu.ucsd.livesearch.inspect;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsd.livesearch.task.OnDemandOperation;
import edu.ucsd.livesearch.task.Task;
import edu.ucsd.livesearch.util.Commons;
import edu.ucsd.saint.commons.WebAppProps;

public class GenerateForParis implements OnDemandOperation{
	private final Logger logger = LoggerFactory.getLogger(GenerateForParis.class);
	
	private Task task;
	private File paris;
	private File folder;
	private final String INSPECT_PATH;
	
	public GenerateForParis(Task task){
		this.task = task;
		paris = task.getPath("inspect.paris");
		folder = task.getPath("paris/");
		INSPECT_PATH = WebAppProps.getPath("livesearch.util.path") + File.separatorChar + "inspect";
	}
	
	
	public boolean execute(){
		try{
			File script = new File(INSPECT_PATH, "inspect/GatherSpectraParis.py");
			boolean msc = task.getFlowName().matches("MSC-INSPECT|MSC-MSALIGN");
			ProcessBuilder pb = new ProcessBuilder(
					"python", script.getAbsolutePath(),
					"-r", InspectUtils.getResultPath(task).getAbsolutePath(),
					"-w", paris.getAbsolutePath(),
					"-D", folder.getAbsolutePath(),
					"-m", task.getPath(msc ? "mgf/" : "spec/" ).getAbsolutePath());
			Commons.executeProcess(
					new File(INSPECT_PATH), task.getPath("paris/paris.log"), pb);
			StringBuffer buf = new StringBuffer();
			for(String cmd: pb.command())
				buf.append(cmd).append(' ');
			logger.info(buf.toString());
			return true;
		}
		catch(IOException e){e.printStackTrace(); }
		return false;
	}
	public boolean resourceExists(){
		return paris.exists() && folder.exists();
	}

	public boolean resourceDated(){
		File source = InspectUtils.getResultPath(task);
		if(!paris.exists() || !folder.exists() || ! source.exists())
			return false;
		return paris.lastModified() < source.lastModified() ||
			folder.lastModified() < source.lastModified();
	}
	
	public String getResourceName(){
		return task + ".paris"; 
	}
	public File getFolder(){
		return folder;
	}
}
