package edu.ucsd.saint.commons.archive;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import edu.ucsd.saint.commons.IOUtils;

public class ZipArchive implements Archive {

	ArchiveEntry entry = null;
	private ZipInputStream zin = null;
	
	public ZipArchive(InputStream input){
		zin = new ZipInputStream(input);		
	}
	
	public void closeEntry() throws IOException {
		zin.closeEntry();
		entry = null;
	}

	public ArchiveEntry getNextEntry() throws IOException {
		ZipEntry zip = zin.getNextEntry();
		while(zip != null && zip.isDirectory())
			zip = zin.getNextEntry();
		if(zip != null)
			return (entry = new ArchiveEntry(zip.getName()));
		return null;
	}
	public void read(File savedAs) throws IOException{
		if(entry == null)
			throw new IOException("Null ZIP entry");
		if(!IOUtils.dumpToFile(zin, savedAs))
			throw new IOException("Failed to extract to file: " + savedAs.getAbsolutePath());
	}
	
	public void close() throws IOException{
		if(zin != null)
			zin .close();
	}
	
}
