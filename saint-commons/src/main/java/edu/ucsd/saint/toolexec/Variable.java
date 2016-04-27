package edu.ucsd.saint.toolexec;

import java.io.File;
import java.util.Collection;

import edu.ucsd.saint.toolexec.FileHandle;

public interface Variable{

	public enum Type{ FOLDER, FILE, ARGUMENT }

	public String	getName();
	public Type		getType();
	public String	getDatatype();

	public void matchContext(File context);
	public String evaluate(String token);
	public Collection<FileHandle> getIdentifiers(String token);
};
