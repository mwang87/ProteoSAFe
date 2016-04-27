package edu.ucsd.livesearch.libraries;

import java.sql.Timestamp;
import java.util.Date;

import org.json.simple.JSONObject;

import edu.ucsd.livesearch.dataset.Dataset;

public class SpectrumRating {
	private int spectrum_id;
	private int dataset_id;
	private String user_id;
	private String task_id;

	private int scan;
	private int rating;
	
	private Timestamp rating_date;
	
	private String rating_comment;

	

	public SpectrumRating(int spectrum_id, int dataset_id, String user_id,
			String task_id, int scan, int rating, Timestamp rating_date, String rating_comment) {
		super();
		this.spectrum_id = spectrum_id;
		this.dataset_id = dataset_id;
		this.user_id = user_id;
		this.task_id = task_id;
		this.scan = scan;
		this.rating = rating;
		this.rating_date = rating_date;
		this.rating_comment = rating_comment;
	}
	
	public SpectrumRating(int spectrum_id, int dataset_id, String user_id,
			String task_id, int scan, int rating, Timestamp rating_date) {
		super();
		this.spectrum_id = spectrum_id;
		this.dataset_id = dataset_id;
		this.user_id = user_id;
		this.task_id = task_id;
		this.scan = scan;
		this.rating = rating;
		this.rating_date = rating_date;
	}
	
	public String toJSON(){
		String output_string = "";
		output_string += "{";
		output_string += "\"spectrum_id\" : \""  +  spectrum_id + "\",";
		output_string += "\"dataset_id\" : \""  +  Dataset.generateDatasetIDString(dataset_id) + "\",";
		output_string += "\"user_id\" : \""  +  JSONObject.escape(user_id) + "\",";
		output_string += "\"task_id\" : \""  +  task_id + "\",";
		output_string += "\"scan\" : \""  +  scan + "\",";
		output_string += "\"rating\" : \""  +  rating + "\",";
		if(rating_comment == null){
			output_string += "\"rating_comment\" : \"" + "\",";
		}
		else{
			output_string += "\"rating_comment\" : \""  +  JSONObject.escape(rating_comment) + "\",";
		}
		output_string += "\"rating_date\" : \""  +  rating_date.toString() + "\"";
		output_string += "}";
		
		return output_string;
	}
	
	

	public int getSpectrum_id() {
		return spectrum_id;
	}

	public void setSpectrum_id(int spectrum_id) {
		this.spectrum_id = spectrum_id;
	}

	public int getDataset_id() {
		return dataset_id;
	}

	public void setDataset_id(int dataset_id) {
		this.dataset_id = dataset_id;
	}

	public String getUser_id() {
		return user_id;
	}

	public void setUser_id(String user_id) {
		this.user_id = user_id;
	}

	public String getTask_id() {
		return task_id;
	}

	public void setTask_id(String task_id) {
		this.task_id = task_id;
	}

	public int getScan() {
		return scan;
	}

	public void setScan(int scan) {
		this.scan = scan;
	}

	public int getRating() {
		return rating;
	}

	public void setRating(int rating) {
		this.rating = rating;
	}

	public Timestamp getRating_date() {
		return rating_date;
	}

	public void setRating_date(Timestamp rating_date) {
		this.rating_date = rating_date;
	}
	
	public String getRating_comment() {
		return rating_comment;
	}

	public void setRating_comment(String rating_comment) {
		this.rating_comment = rating_comment;
	}
	
}
