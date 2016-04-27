package edu.ucsd.livesearch.inspect;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsd.livesearch.task.NullTask;
import edu.ucsd.livesearch.task.OnDemandLoader;
import edu.ucsd.livesearch.task.Task;
import edu.ucsd.livesearch.util.FormatUtils;

public class InspectResult implements Iterator<InspectResult.Hit>, Iterable<InspectResult.Hit>{

	private final Logger logger = LoggerFactory.getLogger(InspectResult.class);
	
	private Map<String, Integer> indexMap = new HashMap<String, Integer>(); 
	private String fields[];
	private RandomAccessFile ra = null;
	private File path; 
	private boolean loaded;
	private Task task;	

	public InspectResult(Task task){
		if(task instanceof NullTask) return;
		this.task = task;
		GenerateSortedView loader = new GenerateSortedView(task);
		OnDemandLoader.load(loader);
		path = loader.getFile();
		loaded = false;
	}
	
	public InspectResult(Task task, File source, File sorted) {
		if(task instanceof NullTask) return;
		this.task = task;
		GenerateSortedView loader = new GenerateSortedView(task, source, sorted);
		OnDemandLoader.load(loader);
		path = loader.getFile();
		loaded = false;
	}

	public boolean ready(){
		return path.exists();
	}

	public long length(){
		return path.length();
	}

	public String[] getFieldNames(){
		if(!loaded) load();
		return fields;
	}

	public long size(){
		if(path != null && path.isFile())
			return path.length();
		return -1;
	}

	private void load(){
		try{
		//System.out.printf("[random file path] %s%n", path);
			ra = new RandomAccessFile(path, "r");
			readHeader();
			loaded = true;
		}catch(IOException e){
			logger.warn(FormatUtils.formatExceptoin(e, 
				"Failed to load [%s]", path.getAbsolutePath()));
		}
	}

	public void seek(long position){
		if(!loaded) load();
		try{
			if(ra != null)			
				ra.seek(position);
		} catch(IOException e){}
	}

	public void close(){
		try{
			if(ra != null)
				ra.close();
			ra = null;			
		} catch(IOException e){}		
	}

	private void readHeader() throws IOException{
		String header = ra.readLine();
		if(header!= null){	
			fields = header.split("\\t+");
			for(int i = 0; i < fields.length; i++)
				indexMap.put(fields[i], i);
		}
	}
	
	private Hit readHit(){
		String line = null;
		long position = -1;
		try{
			position = ra.getFilePointer();
			if((line = ra.readLine()) == null)			
				return null;
			String values[] = line.split("\\t");
			int index = indexMap.get("#SpectrumFile");
						
			if(values.length > index){
				String value = values[index];
				String filename = FilenameUtils.getName(value);
				String uploadName = task.queryOriginalName(filename);
				values[index] = (uploadName  == null)? filename : uploadName;
				Hit hit = new Hit();
				hit.values = values;
				hit.internalName = filename;
				hit.position = position;
				hit.setProtein();
				return hit;
			}
		}
		catch(Exception e){
			logger.warn(FormatUtils.formatExceptoin(e, 
				"Failed to read inspect hit at postion %d of %s]",
					position, path.getAbsolutePath()));
		};
		return null;
	}

	public Hit next(){
		if(!loaded) load();
		return ra == null ? null : readHit();
	}

	public boolean hasNext(){
		if(!loaded) load();
		try{
			return ra != null && ra.getFilePointer() < ra.length();
		}catch(IOException e){
			return false;
		}
	}
 
	public void remove(){
	}
	
	public class Hit implements Comparable<Hit> {
		String values[];
		String internalName = null, id = "", desc = "";
		long position = -1;
		
		public String getFieldValue(String fieldName){
			Integer index = indexMap.get(fieldName);
			if(index != null && index < values.length)
				return values[index];

//			if(fieldName.equals("Protein"))
//				return id;
//			if(fieldName.equals("Description"))
//				return desc;
//			if(fieldName.equals("InternalName"))
//				return desc;
			return null;
		}
		
		void setProtein() {
			String protein = getFieldValue("Protein");
			id = InspectUtils.getProteinID(protein);
			desc = InspectUtils.getProteinDescription(protein);
		}
		
		public String[] getFieldValues(){
			return values;
		}
		
		public String getProteinID(){
			return id;
		}
		
		public String getDescription(){
			return desc;
		}
		
		public String getInternalName(){
			return internalName;
		}
		
		public long getPosition(){
			return position;
		}
		
		public int compareTo(Hit hit) {
			if (hit == null)
				return 1;
			else return (int)(this.position - hit.position);
		}
	}
	
	public Iterator<Hit> iterator() {
		return this;
	}
}
