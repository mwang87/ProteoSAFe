package edu.ucsd.saint.commons.archive;

import java.io.File;

import org.apache.commons.io.FilenameUtils;

public class ArchiveEntry{
	protected String filename;
	protected String extension;
	protected File file;

	public ArchiveEntry(String path, File file){
		this(path);
		this.file = file;
	}
	
	public ArchiveEntry(String path){
		filename = path;
		extension = FilenameUtils.getExtension(path);
		file = null;
	}
	
	public String getExtension() {
		return extension;
	}
	
	public String getFilename() {
		return filename;
	}

	public File getFile(){
		return file;
	}

	public long getSize(){
		return file != null ? file.length() : 0;
	}
}
