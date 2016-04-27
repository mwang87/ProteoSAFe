package edu.ucsd.saint.commons.archive;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;

import edu.ucsd.saint.commons.IOUtils;

public class BZipArchiver implements Archiver {

	ArchiveEntry entry = null;
	private TarArchiveOutputStream tout = null;
	
	public BZipArchiver(OutputStream output) throws IOException{
		tout = new TarArchiveOutputStream(new BZip2CompressorOutputStream(output));
	}
	
	public void closeEntry() throws IOException {
		tout.closeArchiveEntry();
		entry = null;
	}

	public void putNextEntry(ArchiveEntry entry) throws IOException {
		this.entry = entry;
		TarArchiveEntry te = new TarArchiveEntry(entry.getFilename());
		te.setSize(entry.getSize());
		tout.putArchiveEntry(te);
	}
	
	public void write(File file) throws IOException{
		if(entry == null)
			throw new IOException("Null BZIP entry");
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
