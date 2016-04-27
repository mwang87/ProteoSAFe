package edu.ucsd.livesearch.libraries;

import org.json.simple.JSONObject;

public class SpectrumInfo {

	private String spectrum_id;
	private String source_file;
	private int scan;
	private String task_id;
	private int ms_level;

	//Library Organization Information
	private String library_membership;
	private int spectrum_status;
	private String peaks_json;

	//Splash Information
	private String block1;
	private String block2;
	private String block3;

	//Extra information not in the database, but good to display
	private String submit_user;



	public SpectrumInfo(String spectrum_id, String source_file, int scan,
			String task_id, int ms_level) {
		super();
		this.spectrum_id = spectrum_id;
		this.source_file = source_file;
		this.scan = scan;
		this.task_id = task_id;
		this.ms_level = ms_level;
	}


	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append(spectrum_id + "\t");
		result.append(source_file + "\t");
		result.append(scan + "\t");
		result.append(task_id + "\t");
		result.append(ms_level);
		return result.toString();
	}

	public String toJSON(){
		StringBuilder result = new StringBuilder();

		result.append("{ ");
		result.append("\"spectrum_id\" : \"" + spectrum_id + "\" ,");
		result.append("\"source_file\" : \"" + JSONObject.escape(source_file) + "\" ,");
		result.append("\"task\" : \"" + task_id + "\" ,");
		result.append("\"scan\" : \"" + scan + "\" ,");
		result.append("\"ms_level\" : \"" + ms_level + "\" ,");
		result.append("\"library_membership\" : \"" + library_membership + "\" ,");
		result.append("\"spectrum_status\" : \"" + spectrum_status + "\" ,");
		result.append("\"peaks_json\" : \"" + JSONObject.escape(peaks_json) + "\" ,");
		result.append("\"splash\" : \"" + JSONObject.escape(block1) + "-" + JSONObject.escape(block2) + "-" + JSONObject.escape(block3) + "\" ");

		result.append(" }");

		return result.toString();
	}

	public String toJSON_withAnnotation(SpectrumAnnotation annot){
		StringBuilder result = new StringBuilder();

		result.append("{ ");
		result.append("\"spectrum_id\" : \"" + spectrum_id + "\" ,");
		result.append("\"source_file\" : \"" + JSONObject.escape(source_file) + "\" ,");
		result.append("\"task\" : \"" + task_id + "\" ,");
		result.append("\"scan\" : \"" + scan + "\" ,");
		result.append("\"ms_level\" : \"" + ms_level + "\" ,");
		result.append("\"library_membership\" : \"" + library_membership + "\" ,");
		result.append("\"spectrum_status\" : \"" + spectrum_status + "\" ,");
		result.append("\"peaks_json\" : \"" + JSONObject.escape(peaks_json) + "\" ,");
		result.append("\"splash\" : \"" + JSONObject.escape(block1) + "-" + JSONObject.escape(block2) + "-" + JSONObject.escape(block3) + "\", ");

		if(submit_user != null){
			result.append("\"submit_user\" : \"" + JSONObject.escape(submit_user) + "\" ,");
		}

		result.append(annot.toJSON_intermediate());

		result.append(" }");

		return result.toString();
	}


	public String getSpectrum_id() {
		return spectrum_id;
	}


	public void setSpectrum_id(String spectrum_id) {
		this.spectrum_id = spectrum_id;
	}


	public String getSource_file() {
		return source_file;
	}


	public void setSource_file(String source_file) {
		this.source_file = source_file;
	}


	public int getScan() {
		return scan;
	}


	public void setScan(int scan) {
		this.scan = scan;
	}


	public String getTask_id() {
		return task_id;
	}


	public void setTask_id(String task_id) {
		this.task_id = task_id;
	}


	public int getMs_level() {
		return ms_level;
	}


	public void setMs_level(int ms_level) {
		this.ms_level = ms_level;
	}


	public String getLibrary_membership() {
		return library_membership;
	}


	public void setLibrary_membership(String library_membership) {
		this.library_membership = library_membership;
	}


	public int getSpectrum_status() {
		return spectrum_status;
	}


	public void setSpectrum_status(int spectrum_status) {
		this.spectrum_status = spectrum_status;
	}


	public String getPeaks_json() {
		return peaks_json;
	}


	public void setPeaks_json(String peaks_json) {
		this.peaks_json = peaks_json;
	}

	public String getSubmit_user() {
		return submit_user;
	}

	public void setSubmit_user(String submit_user) {
		this.submit_user = submit_user;
	}

	public void setSplash(String block1, String block2, String block3) {
		this.block1 = block1;
		this.block2 = block2;
		this.block3 = block3;
	}



}
