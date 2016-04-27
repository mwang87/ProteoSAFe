package edu.ucsd.saint.commons.archive;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;

import edu.ucsd.saint.commons.IOUtils;

public class GZipArchiver implements Archiver {

	ArchiveEntry entry = null;
	private TarArchiveOutputStream tout = null;
	
	public GZipArchiver(OutputStream output) throws IOException{
		tout = new TarArchiveOutputStream(new GZIPOutputStream(output));
	}
	
	public void closeEntry() throws IOException{
		tout.closeArchiveEntry();
	}

	public void putNextEntry(ArchiveEntry entry) throws IOException {
		this.entry = entry;
		TarArchiveEntry te = new TarArchiveEntry(entry.getFilename());
		te.setSize(entry.getSize());
		tout.putArchiveEntry(te);
	}
	
	public void write(File file) throws IOException{
		if(entry == null)
			throw new IOException("Null GZIP entry");
		if(!IOUtils.appendFileTo(file, tout))
			throw new IOException("Failed to archive file: " + file.getAbsolutePath());
	}

	public void print(String str) throws IOException{
		tout.write(str.getBytes());
	}

	public void println() throws IOException{
		print("\n");
	}
	
	public void close() throws IOException{
		tout.close();
	}
}
