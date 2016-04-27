package edu.ucsd.livesearch.storage;

public class SequenceFile{	
	private String code;
	private String display;

	SequenceFile(String code, String display) {
		this.code = code;
		this.display = display;
	}
	public String getCode() {
		return code;
	}
	public String getDisplay(){
		return display;
	}
}
