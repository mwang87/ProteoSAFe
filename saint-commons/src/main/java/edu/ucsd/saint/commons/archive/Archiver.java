package edu.ucsd.saint.commons.archive;

import java.io.File;
import java.io.IOException;

public interface Archiver {
	public void putNextEntry(ArchiveEntry entry) throws IOException;
	public void closeEntry() throws IOException;
	public void write(File file) throws IOException;
	public void print(String str) throws IOException;
	public void println() throws IOException;
	public void close() throws IOException;
}
