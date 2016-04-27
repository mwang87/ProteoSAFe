package edu.ucsd.liveadmin;

public class SequenceFile{
	private String code;
	private String display;

	public SequenceFile(String code, String display) {
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
