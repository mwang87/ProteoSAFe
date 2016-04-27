package edu.ucsd.saint.commons.archive;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

import edu.ucsd.saint.commons.IOUtils;

public class TarArchive implements Archive{	
	private TarArchiveInputStream tin = null;
	private ArchiveEntry entry = null;

	public TarArchive(InputStream input){
		tin = new TarArchiveInputStream(input);		
	}
	
	public void closeEntry() {
		entry = null;
	}

	public ArchiveEntry getNextEntry() throws IOException {
		try{
			TarArchiveEntry tar = tin.getNextTarEntry();
			while(tar!= null && tar.isDirectory())
				tar = tin.getNextTarEntry();
			if(tar != null)
				return (entry = new ArchiveEntry(tar.getName()));
		}
		catch(EOFException e){
		}
		return null;
	}

	public void read(File savedAs) throws IOException{
		if(entry == null)
			throw new IOException("Null TAR entry");
		if(!IOUtils.dumpToFile(tin, savedAs))
			throw new IOException("Failed to extract to file: " + savedAs.getAbsolutePath());
	}	
	
	public void close() throws IOException{
		if(tin != null)
			tin.close();
	}	
}
