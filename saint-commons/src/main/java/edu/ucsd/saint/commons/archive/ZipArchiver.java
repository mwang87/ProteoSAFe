package edu.ucsd.saint.commons.archive;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import edu.ucsd.saint.commons.IOUtils;

public class ZipArchiver implements Archiver {

	ArchiveEntry entry = null;
	private ZipOutputStream zout = null;
	
	public ZipArchiver(OutputStream output){
		zout = new ZipOutputStream(output);		
	}
	
	public void closeEntry() throws IOException {
		zout.closeEntry();
	}

	public void putNextEntry(ArchiveEntry entry) throws IOException {
		this.entry = entry;
		zout.putNextEntry(new ZipEntry(entry.getFilename()));
	}
	
	public void write(File file) throws IOException {
		if(entry == null)
			throw new IOException("Null ZIP entry");
		if(!IOUtils.appendFileTo(file, zout))
			throw new IOException("Failed to archive file: " + file.getAbsolutePath());
	}

	public void print(String str) throws IOException{
		zout.write(str.getBytes());
	}

	public void println() throws IOException{
		print("\n");
	}
	
	public void close() throws IOException{
		zout.close();
	}

}
