package edu.ucsd.saint.commons.archive;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import edu.ucsd.saint.commons.IOUtils;

public class PlainArchiver implements Archiver {

	ArchiveEntry entry = null;
	OutputStream output = null;

	public PlainArchiver(OutputStream output){
		this.output = output;
	}
	
	public void closeEntry() {
		entry = null;
	}

	public void putNextEntry(ArchiveEntry entry) throws IOException {
		if(entry == null) return;
		this.entry = entry;
	}
	
	public void write(File file) throws IOException{
		if(entry == null)
			throw new IOException("Null PLAIN entry");
		if(!IOUtils.appendFileTo(file, output))
			throw new IOException("Failed to archive file: " + file.getAbsolutePath());
	}

	public void print(String str) throws IOException{
		output.write(str.getBytes());
	}

	public void println() throws IOException{
		print("\n");
	}
	
	public void close() throws IOException{
		output.close();
	}
}
