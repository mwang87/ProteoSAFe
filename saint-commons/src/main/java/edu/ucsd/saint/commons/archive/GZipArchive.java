package edu.ucsd.saint.commons.archive;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.FilenameUtils;

import edu.ucsd.saint.commons.IOUtils;

public class GZipArchive implements Archive {
	private GZipInputStream gin = null;
	private ArchiveEntry entry = null;
	private TarArchive tar = null;
	private boolean done = false;
	
	public GZipArchive(String filename, InputStream input)
		throws IOException{
		gin = new GZipInputStream(input);
		if(filename.endsWith(".tgz") || filename.endsWith(".tar.gz"))
			tar = new TarArchive(gin);
		else{
			String original = gin.getFilename();
			filename = original.equals("") ? FilenameUtils.getBaseName(filename): original;
			entry = new ArchiveEntry(filename);
		}
	}

	public GZipArchive(InputStream input) throws IOException{
		gin = new GZipInputStream(input);
		tar = new TarArchive(gin);
	}
	
	public void closeEntry() throws IOException{
		if(tar != null) tar.closeEntry();
		else{
			if(entry != null) gin.close();
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
			if(!IOUtils.dumpToFile(gin, savedAs))
				throw new IOException("Failed to extract to file: " + savedAs.getAbsolutePath());
		}
	}

	public void close() throws IOException{
		if(tar != null)
			tar.close();
		else if(gin != null)
			gin.close();
	}	
}
