package edu.ucsd.livesearch.servlet;

import java.io.IOException;
import java.util.HashMap;
import java.util.Set;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsd.livesearch.account.AccountManager;
import edu.ucsd.livesearch.dataset.Dataset;
import edu.ucsd.livesearch.dataset.DatasetManager;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.JSONArray;


public class DatasetAnnotationServlet extends BaseServlet{
	
	private static final Logger logger =
		LoggerFactory.getLogger(DatasetAnnotationServlet.class);
	
	//Gets the Annotations per dataset
	@Override
	protected void doGet(
		HttpServletRequest request, HttpServletResponse response
	) throws ServletException, IOException {
		String dataset_name = request.getParameter("dataset");
		Dataset queried_dataset = DatasetManager.queryDatasetByID(dataset_name);
		if(queried_dataset.isPrivate() == false){
			Set<String> annotation_keys = queried_dataset.getAnnotationNames();
			Map<String, String> annotations = queried_dataset.getAllAnnotations();

			String jsonText = JSONValue.toJSONString(annotations);
						
			
			response.getWriter().println(jsonText);
			return;
		}
		
		response.getWriter().println("{\"status\":\"nopermission\"}");
		return;
	}
	
	
	//Adds or removes datataset annotation
	@Override
	protected void doPost(
		HttpServletRequest request, HttpServletResponse response
	) throws ServletException, IOException{
		String username = (String)request.getSession().getAttribute("livesearch.user");
	    boolean isAdmin = AccountManager.getInstance().checkRole(username, "administrator");
	    String dataset_name = request.getParameter("dataset");
	    String function = request.getParameter("function");
	    
	    response.getWriter().println(username);
	    
	    if(isAdmin){
	    	if(function.compareTo("addannotation") == 0){
	    		String annotation_key = request.getParameter("annotation_name");
	    		String annotation_value = request.getParameter("annotation_value");
	    		
	    		Dataset queried_dataset = DatasetManager.queryDatasetByID(dataset_name);
	    		queried_dataset.setAnnotation(annotation_key, annotation_value);
	    		DatasetManager.updateDataset(queried_dataset);
	    	}
	    	if(function.compareTo("removeannotation") == 0){
	    		String annotation_key = request.getParameter("annotation_name");
	    		
	    		Dataset queried_dataset = DatasetManager.queryDatasetByID(dataset_name);
	    		queried_dataset.clearAnnotation(annotation_key);
	    		DatasetManager.updateDataset(queried_dataset);
	    	}
	    	
	    	response.getWriter().println("{\"status\":\"success\"}");
	    	return;
	    }
	    
	    response.getWriter().println("{\"status\":\"nopermission\"}");
		return;
	}

}
