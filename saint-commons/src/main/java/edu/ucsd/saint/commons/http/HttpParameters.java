package edu.ucsd.saint.commons.http;

import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections.map.LRUMap;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.ProgressListener;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

@SuppressWarnings("unchecked")
public class HttpParameters {
	private Map<String, LinkedList<String>> parameters;
	private Map<String, FileItem> files;
	private String uuid;
	
	/**
	 * This constructor is for testing purposes only, and should only be used
	 * in contrived situations in which no real HttpServletRequest instance
	 * is available.
	 */
	public HttpParameters(){
		uuid = null;
		parameters = new HashMap<String, LinkedList<String>>();
		files = new HashMap<String, FileItem>();
	}
	
	public HttpParameters(HttpServletRequest request, String uuid)
			throws FileUploadException{
		this.uuid = uuid;
		parameters = new HashMap<String, LinkedList<String>>();
		files = new HashMap<String, FileItem>();
		fetchPlainParameters(request); 
		if(ServletFileUpload.isMultipartContent(request))
			fetchMultipartParameters(request);
	}
	
	private void fetchPlainParameters(HttpServletRequest request){
		Enumeration<?> names = request.getParameterNames();
		while(names.hasMoreElements()){
			String name = (String)names.nextElement();
			String[] params = request.getParameterValues(name);
			for (String param : params)
				setParameter(name, param);
		}
	}

	private void fetchMultipartParameters(HttpServletRequest request)
			throws FileUploadException{
		FileItemFactory factory = new DiskFileItemFactory();
		ServletFileUpload Fileupload = new ServletFileUpload(factory);
		if(uuid != null) Fileupload.setProgressListener(new UploadListener(request, uuid));
		for(Object o: Fileupload.parseRequest(request)){
			FileItem item = (FileItem)o;
			String name = item.getFieldName();
			if(item.isFormField())
				setParameter(name, item.getString());
			else if(item.getName().length() > 0)
				files.put(name, item);
		}
	}
	
	public Collection<String> getParameterNames(){
		return parameters.keySet();
	}
	
	public Collection<String> getFileParamNames(){
		return files.keySet();
	}

	public LinkedList<String> removeParameter(String key){
		return parameters.remove(key);
	}

	public FileItem removeFileParam(String key){
		return files.remove(key);
	}

	public void renameParameter(String key, String newKey){		
		LinkedList<String> values = removeParameter(key);
		if(values != null)
			parameters.put(newKey, values);
	}

	public void renameFileParam(String key, String newKey){
		FileItem file = removeFileParam(key);
		if(file != null)
			files.put(newKey, file);
	}
	
	public String getParameter(String key){
		LinkedList<String> values = parameters.get(key);
		return (values == null || values.isEmpty()) ? null : values.getFirst();
	}

	public Collection<String> getMultiParams(String key){ 
		return parameters.get(key);
	}
	
	public void setParameter(String name, String value){
		if(!parameters.containsKey(name))
			parameters.put(name, new LinkedList<String>());
		parameters.get(name).add(value);
	}
	
	public FileItem getFile(String name){
		return files.get(name);
	}
	
	public void setFile(String name, FileItem value){
		files.put(name, value);
	}
	
	private static final int MAX_UPLOAD_RECORD = 100;	
	private static Map<String, UploadListener> uploads;
	private static Lock read, write;
	
	static{
		uploads = new LRUMap(MAX_UPLOAD_RECORD);
		ReentrantReadWriteLock rw = new ReentrantReadWriteLock();
		read = rw.readLock();
		write = rw.writeLock();
	}
	
	public static UploadListener getUploaListener(String uuid){
		read.lock();
		try{
			UploadListener result = uploads.get(uuid);
			if(result != null)
				return result.clone();
			return null;
		}finally{read.unlock();}
	}

	public static class UploadListener implements ProgressListener{
		long read, bytes, beginTime, endTime;
		boolean tracking;
		String uuid;

		private UploadListener(){
		}
		
		public UploadListener clone(){
			UploadListener result = new UploadListener();
			result.uuid = uuid;
			result.read = read;
			result.bytes = bytes;
			result.beginTime = beginTime;
			result.endTime = endTime;
			return result;
		}
		
		public UploadListener(HttpServletRequest request, String uuid){
			this.uuid = uuid;
			write.lock();
			try{
				tracking = !uploads.containsKey(uuid);
				if(tracking)
					uploads.put(uuid, this);
			}finally{write.unlock();}
			if(tracking){
				read = 0;
				bytes = -1;
				beginTime = System.currentTimeMillis();
				endTime = -1;
			}
		}

		public void update(long pBytesRead, long pContentLength, int pItems) {
			if(!tracking) return;
			if(pContentLength == -1 || pContentLength == 0){
				tracking = false;
			}else{
				read = pBytesRead;
				bytes = pContentLength;
				if(read == bytes)
					endTime = System.currentTimeMillis();
			}
		}

		public long getTotalBytes(){
			return bytes;
		}

		public long getBytesRead(){
			return read;
		}

		public long getElapsed(){
			return (endTime != -1 ? endTime : System.currentTimeMillis()) - beginTime;
		}
	}
}
