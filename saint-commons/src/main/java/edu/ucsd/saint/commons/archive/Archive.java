package edu.ucsd.saint.commons.archive;

import java.io.File;
import java.io.IOException;

public interface Archive {
	public abstract ArchiveEntry getNextEntry() throws IOException;
	public abstract void closeEntry() throws IOException;
	public abstract void read(File path) throws IOException;
	public abstract void close() throws IOException;
}
