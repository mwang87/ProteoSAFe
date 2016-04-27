package edu.ucsd.livesearch.servlet;

import java.io.IOException;
import java.sql.Timestamp;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsd.livesearch.dataset.Dataset;
import edu.ucsd.livesearch.libraries.AnnotationManager;
import edu.ucsd.livesearch.libraries.SpectrumRating;
import edu.ucsd.livesearch.libraries.SpectrumRatingManager;


public class ContinuousIDRatingServlet extends BaseServlet{

	private Logger logger = LoggerFactory.getLogger(ContinuousIDRatingServlet.class);
	
	//Posting Rating of Match
	@Override
	protected void doPost(
		HttpServletRequest request, HttpServletResponse response
	) throws ServletException, IOException {
		String username = (String)request.getSession().getAttribute("livesearch.user");
		String task_id = request.getParameter("task");
		String Spectrum_ID = request.getParameter("spectrumID");
		String scan = request.getParameter("scan");
		String rating = request.getParameter("rating");
		String dataset_string = request.getParameter("massiveID");
		String function = request.getParameter("function");
		//Checking if user is logged in;
		if(username == null){
			response.getOutputStream().print("LOGIN");
			return;
		}
		
		if(function == null){
			response.getOutputStream().print("{\"status\": \"error function\"}");
			return;
		}
		
		if(function.compareTo("add_rating") == 0 || function.compareTo("delete_rating") == 0){
			if(Spectrum_ID == null || scan == null || rating == null || dataset_string == null){
				response.getOutputStream().print("{\"status\": \"error\"}");
				return;
			}
			
			int rating_int = Integer.parseInt(rating);
			
			if(rating_int < 0 || rating_int > 4){
				response.getOutputStream().print("{\"status\": \"rating error\"}");
				return;
			}
			
			java.util.Date date= new java.util.Date();
			SpectrumRating newRating = new SpectrumRating(AnnotationManager.SpectrumID_fromString(Spectrum_ID), 
					Dataset.parseDatasetIDString(dataset_string), username, task_id, Integer.parseInt(scan), rating_int, new Timestamp(date.getTime()));
			
			
			//Deleting Entry
			if(rating_int == 0){
				SpectrumRatingManager.Delete_Spectrum_Rating_For_User_Dataset_Scan(newRating);
				logger.info("Deleting Rating Servlet\t" + 0 + "\t" + task_id + "\t" + Spectrum_ID + "\t" + scan  + "\t" +  dataset_string);
			}
			else{
				SpectrumRatingManager.Set_Spectrum_Rating_For_User_Dataset_Scan(newRating);
				logger.info("Rating Servlet\t" + rating_int + "\t" + task_id + "\t" + Spectrum_ID + "\t" + scan  + "\t" +  dataset_string);
			}
			
			
			response.getOutputStream().print("{\"status\": \"updated\"}");
			
			return;
		}
		
		if(function.compareTo("add_comment") == 0){
			if(Spectrum_ID == null || scan == null || dataset_string == null){
				response.getOutputStream().print("{\"status\": \"error\"}");
				return;
			}
			
			String comment = request.getParameter("rating_comment");
			
			if(comment == null){
				response.getOutputStream().print("{\"status\": \"comment error empty\"}");
				return;
			}
			
			logger.info("Creating Comment: " + comment);
			
			java.util.Date date= new java.util.Date();
			SpectrumRating newRatingComment = new SpectrumRating(AnnotationManager.SpectrumID_fromString(Spectrum_ID), 
					Dataset.parseDatasetIDString(dataset_string), username, task_id, Integer.parseInt(scan), 0, new Timestamp(date.getTime()), comment);
			
			int ret_val = SpectrumRatingManager.Set_Spectrum_Rating_Comment(newRatingComment);
			logger.info("Rating Servlet Adding Comment Ret: " + ret_val);
			
			if(ret_val != 0){
				response.getOutputStream().print("{\"status\": \"comment error\"}");
				return;
			}
			
			response.getOutputStream().print("{\"status\": \"updated\"}");
			
			return;
		}
		
		
		
		
		
	}
	
	
	//Getting stored ratings that already exist on the system
	@Override
	protected void doGet(
			HttpServletRequest request, HttpServletResponse response
		) throws ServletException, IOException {
		String username = (String)request.getSession().getAttribute("livesearch.user");
		String scan = request.getParameter("scan");
		String Spectrum_ID = request.getParameter("spectrumID");
		String dataset_string = request.getParameter("massiveID");
		
		if(username == null){
			response.getOutputStream().print("LOGIN");
			return;
		}
		
		if(Spectrum_ID == null || scan == null || dataset_string == null){
			response.getOutputStream().print("{\"status\": \"error\"}");
			return;
		}
		
		SpectrumRating queried_rating = SpectrumRatingManager.Get_Spectrum_Rating_For_User_Dataset_Scan(Spectrum_ID, username, dataset_string, Integer.parseInt(scan));
		
		if(queried_rating == null){
			response.getOutputStream().print("{\"rating\": \"0\"}");
		}
		else{
			//response.getOutputStream().print("{\"rating\": \"" + queried_rating.getRating() +  "\"}");
			response.getOutputStream().print(queried_rating.toJSON());
		}
	}
}
