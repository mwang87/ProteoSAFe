package edu.ucsd.saint.toolexec;

import java.io.File;
import java.util.Collection;
import java.util.LinkedList;

import edu.ucsd.saint.toolexec.FileHandle;
import edu.ucsd.saint.toolexec.Variable;

public class PlaceHolder implements Variable{

	String name;
	String object;

	PlaceHolder(String name, String object){
		this.name = name;
		this.object = object;
	}

	public String getName() {
		// TODO Auto-generated method stub
		return name;
	}

	public Type getType() {
		return Type.FOLDER;
	}

	public String getDatatype() {
		return null;
	}

	public String getObject() {
		return object;
	}

	public void matchContext(File context) {
	}

	public String evaluate(String token) {
		return object;
	}


	public Collection<FileHandle> getIdentifiers(String token) {
		return new LinkedList<FileHandle>();
	}
}
