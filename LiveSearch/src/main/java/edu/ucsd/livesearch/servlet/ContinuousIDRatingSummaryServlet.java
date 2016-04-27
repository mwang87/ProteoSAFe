package edu.ucsd.livesearch.servlet;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsd.livesearch.libraries.SpectrumRating;
import edu.ucsd.livesearch.libraries.SpectrumRatingManager;

public class ContinuousIDRatingSummaryServlet extends BaseServlet{

	private Logger logger = LoggerFactory.getLogger(ContinuousIDRatingSummaryServlet.class);


	/**
	 * Returning Various Summarizing Queries about ratings
	 * summary_type = per_spectrum, per_dataset
	 * 
	 */
	@Override
	protected void doGet(
			HttpServletRequest request, HttpServletResponse response
		) throws ServletException, IOException {
		
		String summary_type = request.getParameter("summary_type");
		
		if(summary_type == null){
			response.getOutputStream().print("{\"status\": \"rating error\"}");
			return;
		}
		
		if(summary_type.compareTo("per_dataset_spectrum_scan") == 0){
			String spectrum_id = request.getParameter("spectrum_id");
			String dataset_id = request.getParameter("dataset_id");
			int scan = Integer.parseInt(request.getParameter("scan"));
			if(spectrum_id == null){
				response.getOutputStream().print("{\"status\": \"request error\"}");
				return;
			}
			
			List<SpectrumRating> ratings = SpectrumRatingManager. Get_All_Ratings_Per_Anything(spectrum_id, dataset_id, scan, null);
			
			//Creating JSON
			String output_json = "{ \"ratings\":[";
			for(int i = 0; i < ratings.size(); i++){
				output_json += ratings.get(i).toJSON();
				if(i < ratings.size() -1){
					output_json += ",";
				}
			}
			output_json += "], \"status\": \"success\"}";
			
			response.getOutputStream().print(output_json);
			return;
		}
		
		if(summary_type.compareTo("per_spectrum") == 0){
			String spectrum_id = request.getParameter("spectrum_id");
			if(spectrum_id == null){
				response.getOutputStream().print("{\"status\": \"request error\"}");
				return;
			}
			
			List<SpectrumRating> ratings = SpectrumRatingManager.Get_All_Ratings_Per_Spectrum(spectrum_id);
			
			//Creating JSON
			String output_json = "{ \"ratings\":[";
			for(int i = 0; i < ratings.size(); i++){
				output_json += ratings.get(i).toJSON();
				if(i < ratings.size() -1){
					output_json += ",";
				}
			}
			output_json += "], \"status\": \"success\"}";
			
			response.getOutputStream().print(output_json);
			return;
		}
		
		if(summary_type.compareTo("per_dataset") == 0){
			String dataset_id = request.getParameter("dataset_id");
			if(dataset_id == null){
				response.getOutputStream().print("{\"status\": \"request error\"}");
				return;
			}
			
			List<SpectrumRating> ratings = SpectrumRatingManager.Get_All_Ratings_Per_Dataset(dataset_id);
			
			//Creating JSON
			String output_json = "{ \"ratings\":[";
			for(int i = 0; i < ratings.size(); i++){
				output_json += ratings.get(i).toJSON();
				if(i < ratings.size() -1){
					output_json += ",";
				}
			}
			output_json += "], \"status\": \"success\"}";
			
			response.getOutputStream().print(output_json);
			return;
		}
		
		//Dangerous API call, could be huge
		if(summary_type.compareTo("per_all") == 0){
			List<SpectrumRating> ratings = SpectrumRatingManager.Get_All_Ratings();
			
			//Creating JSON
			String output_json = "{ \"ratings\":[";
			for(int i = 0; i < ratings.size(); i++){
				output_json += ratings.get(i).toJSON();
				if(i < ratings.size() -1){
					output_json += ",";
				}
			}
			output_json += "], \"status\": \"success\"}";
			
			response.getOutputStream().print(output_json);
			return;
		}
		
		response.getOutputStream().print("{\"status\": \"rating error\"}");	
	}

}
