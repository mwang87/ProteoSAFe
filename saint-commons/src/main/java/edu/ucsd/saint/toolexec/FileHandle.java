package edu.ucsd.saint.toolexec;

public class FileHandle {
	private String name;
	private int index;

	FileHandle(String name, int index){
		this.name = name;
		this.index = index;
	}
	
	public String getName(){
		return name;
	}
	
	public int getIndex(){
		return index;
	}
}
