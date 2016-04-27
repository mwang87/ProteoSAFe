package edu.ucsd.saint.commons.archive;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.io.FilenameUtils;

import edu.ucsd.saint.commons.IOUtils;

public class PlainArchive implements Archive {
	
	private ArchiveEntry entry = null;
	private FileItem item = null;
	private InputStream input = null;
	private boolean done = false;

	
	public PlainArchive(FileItem item){
		this.item = item;
		entry = new ArchiveEntry(FilenameUtils.getName(item.getName()));
	}
 
	public PlainArchive(InputStream input){
		this.item = null;
		this.input = input;
		entry = new ArchiveEntry("file.txt");
	}
	
	public void closeEntry() {
	}

	public ArchiveEntry getNextEntry() {
		ArchiveEntry result = entry;
		if(done) return null;
		done = true;
		return result;
	}
	
	public void read(File savedAs) throws IOException {
		if(item != null){
			if(!IOUtils.dumpToFile(item.getInputStream(), savedAs))
				throw new IOException("Failed to extract to file: " + savedAs.getAbsolutePath());
		}
		else if(!IOUtils.dumpToFile(input, savedAs))
			throw new IOException("Failed to extract to file: " + savedAs.getAbsolutePath());
		input = null;
	}
	
	public void close() throws IOException{
		if(input != null)
			input.close();
	}	
}
