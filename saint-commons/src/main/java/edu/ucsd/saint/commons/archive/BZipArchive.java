package edu.ucsd.saint.commons.archive;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.io.FilenameUtils;

import edu.ucsd.saint.commons.IOUtils;

public class BZipArchive implements Archive {
	private BZip2CompressorInputStream bin = null;
	private ArchiveEntry entry = null;
	private TarArchive tar = null;
	private boolean done = false;	
		
	public BZipArchive(String filename, InputStream input)
		throws IOException{
		bin = new BZip2CompressorInputStream(input);
		if(filename.endsWith(".tb2") || filename.endsWith(".tar.bz2") || filename.endsWith(".tbz2"))
			tar = new TarArchive(bin);
		else{
			// Ant's bzip2 library will assume the first two bytes are consumed already 
			entry = new ArchiveEntry(FilenameUtils.getBaseName(filename));
		}
	}

	public BZipArchive(InputStream input) throws IOException{
		bin = new BZip2CompressorInputStream(input);
		tar = new TarArchive(bin);
	}
	
	public void closeEntry() throws IOException{
		if(tar != null) tar.closeEntry();
		else{
			if(entry != null) bin.close();
			entry = null;
		}
	}

	public ArchiveEntry getNextEntry() throws IOException {
		if(tar != null) return tar.getNextEntry();
		if(done) return null;		
		ArchiveEntry result = entry;
		done = true;
		return result;
	}

	public void read(File savedAs) throws IOException{
		if(tar != null)
			tar.read(savedAs);
		else{
			if(!IOUtils.dumpToFile(bin, savedAs))
				throw new IOException("Failed to extract to file: " + savedAs.getAbsolutePath());
		}
	}

	public void close() throws IOException{
		if(tar != null)
			tar.close();
		else if(bin != null)
			bin.close();
	}
}
