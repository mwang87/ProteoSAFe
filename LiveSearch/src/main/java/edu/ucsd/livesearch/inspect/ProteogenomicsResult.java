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

public class ProteogenomicsResult
implements Iterator<ProteogenomicsResult.Hit>, Iterable<ProteogenomicsResult.Hit>
{
	/*========================================================================
	 * Constants
	 *========================================================================*/
	private static final Logger logger =
		LoggerFactory.getLogger(ProteogenomicsResult.class);
	
	/*========================================================================
	 * Properties
	 *========================================================================*/
	private Task task;
	private Map<String, Integer> indexMap = new HashMap<String, Integer>();
	private String fields[];
	private File path;
	private RandomAccessFile ra = null;
	private boolean loaded;
	
	/*========================================================================
	 * Constructors
	 *========================================================================*/
	public ProteogenomicsResult(Task task){
		if (task instanceof NullTask)
			return;
		this.task = task;
		GenerateSortedView loader =
			new GenerateSortedView(task,
				ProteogenomicsUtils.getResultPath(task),
				task.getPath("proteogenomics.sorted"));
		OnDemandLoader.load(loader);
		path = loader.getFile();
		loaded = false;
	}
	
	public ProteogenomicsResult(Task task, File source, File sorted) {
		if (task instanceof NullTask)
			return;
		this.task = task;
		GenerateSortedView loader =
			new GenerateSortedView(task, source, sorted);
		OnDemandLoader.load(loader);
		path = loader.getFile();
		loaded = false;
	}
	
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	public boolean ready() {
		return path.exists();
	}
	
	public long length() {
		return path.length();
	}
	
	public String[] getFieldNames() {
		if (loaded == false)
			load();
		return fields;
	}
	
	public long size() {
		if (path != null && path.isFile())
			return path.length();
		else return -1;
	}
	
	public void seek(long position) {
		if (loaded == false)
			load();
		try {
			if (ra != null)			
				ra.seek(position);
		} catch (IOException e) {}
	}

	public void close() {
		try {
			if (ra != null)
				ra.close();
			ra = null;			
		} catch (IOException e) {}		
	}
	
	public class Hit {
		String values[];
		String internalName = null;
		long position = -1;
		
		public String getFieldValue(String fieldName) {
			Integer index = indexMap.get(fieldName);
			if (index != null && index < values.length)
				return values[index];
			else return null;
		}
		
		public String[] getFieldValues(){
			return values;
		}
		
		public String getInternalName(){
			return internalName;
		}
		
		public long getPosition(){
			return position;
		}
	}

	public Hit next() {
		if (loaded == false)
			load();
		return ra == null ? null : readHit();
	}

	public boolean hasNext() {
		if (loaded == false)
			load();
		try {
			return ra != null && ra.getFilePointer() < ra.length();
		} catch(IOException e){
			return false;
		}
	}
 
	public void remove() {
	}
	
	public Iterator<Hit> iterator() {
		return this;
	}
	
	/*========================================================================
	 * Convenience methods
	 *========================================================================*/
	private void load() {
		try {
			ra = new RandomAccessFile(path, "r");
			readHeader();
			loaded = true;
		} catch (IOException e) {
			logger.warn(FormatUtils.formatExceptoin(e, 
				"Failed to load [%s]", path.getAbsolutePath()));
		}
	}

	private void readHeader()
	throws IOException {
		String header = ra.readLine();
		if (header!= null) {	
			fields = header.split("\\t+");
			for (int i=0; i<fields.length; i++)
				indexMap.put(fields[i], i);
		}
	}
	
	private Hit readHit() {
		String line = null;
		long position = -1;
		try {
			position = ra.getFilePointer();
			if ((line = ra.readLine()) == null)			
				return null;
			String values[] = line.split("\\t");
			int index = indexMap.get("#SpectrumFile");
			if (values.length > index) {
				String value = values[index];
				String filename = FilenameUtils.getName(value);
				String uploadName = task.queryOriginalName(filename);
				values[index] = (uploadName == null)? filename : uploadName;
				Hit hit = new Hit();
				hit.values = values;
				hit.internalName = filename;
				hit.position = position;
				return hit;
			}
		} catch (Exception e) {
			logger.warn(FormatUtils.formatExceptoin(e, 
				"Failed to read Proteogenomics hit at postion %d of %s]",
					position, path.getAbsolutePath()));
		}
		return null;
	}
}
