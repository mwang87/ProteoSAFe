package edu.ucsd.livesearch.inspect;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.util.Collection;
import java.util.TreeSet;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsd.livesearch.task.OnDemandOperation;
import edu.ucsd.livesearch.task.Task;
import edu.ucsd.livesearch.util.FormatUtils;

public class GenerateSortedView implements OnDemandOperation{
//	private static final String FILTERED_OUTPUT = "inspect.out";
//	private static final String PROTEIN_SORTED = "inspect.sorted";

	private final Logger logger = LoggerFactory.getLogger(GenerateSortedView.class);
	
	private Task task;
	private File source;
	private File sorted;

	public GenerateSortedView(Task task){
		this.task = task;
		source = InspectUtils.getResultPath(task);
		sorted = task.getPath("inspect.sorted");
	}
	
	public GenerateSortedView(Task task, File source, File sorted) {
		this.task = task;
		this.source = source;
		this.sorted = sorted;
	}

	public String getResourceName() {
		return task.getID() + ".sorted";
	}
	
	public boolean resourceExists() {
		if (sorted == null)
			return false;
		else return sorted.exists();
	}
	
	public boolean resourceDated() {
		if (sorted == null || sorted.exists() == false ||
			source == null || source.exists() == false)
			return false;
		else return sorted.lastModified() < source.lastModified(); 
	}

	public File getFile(){
		return sorted;
	}

	@SuppressWarnings("unchecked")
	public boolean execute() {
		RandomAccessFile	file = null;
		PrintWriter			writer = null;
		MultiMap			pointers = new MultiValueMap();
		try{
			try{
				if(source == null) return false;
				file = new RandomAccessFile(source, "r");
				String header = file.readLine();
				if(header == null) return false;
				while(true){
					long pointer = file.getFilePointer();
					String line = file.readLine();
					if(line == null) break;
					String tokens[] = line.split("\\t");
					if(tokens.length < 4) continue;
					//String protein = getProteinCode(tokens[3]);
					pointers.put(tokens[3], pointer);
				}
				writer = new PrintWriter(sorted);
				writer.println(header);
				for(String protein: new TreeSet<String>(pointers.keySet())){
					Collection<Long> positions = (Collection)pointers.get(protein);
					if(positions == null) continue;
					for(long pos: positions){
						file.seek(pos);
						writer.println(file.readLine());
					}
				}
				return true;
			}
			finally{
				if(file != null) file.close();
				if(writer != null) writer.close();
			}
		} catch(IOException e){
			logger.warn(FormatUtils.formatExceptoin(e, 
					"Failed to load [%s]", sorted.getAbsolutePath()));			
			return false;
		}
	}

	@SuppressWarnings("unused")
	private static String getProteinCode(String str){			
		if(str != null){
			String parts[] = str.split(" ", 2);
			parts = parts[0].split("\\|");
			if(parts.length > 0){
				String code = parts[parts.length - 1];
				int underscore = code.lastIndexOf('_');
				if(underscore != -1)
					return code.substring(underscore + 1) + '_' + code.substring(0, underscore);
				return code;
			}
		}
		return "";
	}
}
