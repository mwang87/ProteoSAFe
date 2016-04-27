package edu.ucsd.livesearch.servlet;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.json.simple.JSONObject;
import org.json.simple.JSONArray;

import edu.ucsd.livesearch.dataset.Dataset;
import edu.ucsd.livesearch.dataset.DatasetManager;
import edu.ucsd.livesearch.libraries.AnnotationManager;
import edu.ucsd.livesearch.libraries.SpectrumAnnotation;
import edu.ucsd.livesearch.libraries.SpectrumAnnotationSet;
import edu.ucsd.livesearch.libraries.SpectrumComment;
import edu.ucsd.livesearch.libraries.SpectrumInfo;
import edu.ucsd.livesearch.subscription.CIJobTimeStampComparater;
import edu.ucsd.livesearch.subscription.ContinuousIDJob;
import edu.ucsd.livesearch.subscription.ContinuousIDManager;
import edu.ucsd.livesearch.task.Task;
import edu.ucsd.livesearch.task.TaskManager;
import edu.ucsd.livesearch.util.FormatUtils;
import edu.ucsd.livesearch.account.AccountManager;

public class SpectrumCommentServlet extends BaseServlet{
	private static final Logger logger =
		LoggerFactory.getLogger(SpectrumCommentServlet.class);

	//Getting the Annotations and Comments
	@Override
	protected void doGet(
		HttpServletRequest request, HttpServletResponse response
	) throws ServletException, IOException {
		String username = (String)request.getSession().getAttribute("livesearch.user");
		String SpectrumID = request.getParameter("SpectrumID");
		//String massive_id = request.getParameter("massiveID");
		//Checking if user is logged in;


		if(SpectrumID != null){
			SpectrumAnnotationSet annotations = AnnotationManager.Get_All_Spectrum_Annotations(SpectrumID);
			List<SpectrumComment> comments = AnnotationManager.get_All_Annotation_Comments_per_Spectrum(SpectrumID);
			SpectrumInfo specinfo = AnnotationManager.Get_Spectrum_Info(SpectrumID);
			List<Map<String, String> > all_tags = AnnotationManager.GetAllSpectrumTags(SpectrumID, false);

			//Getting user_ids for all comments and annotations
			for(int i = 0; i < annotations.Annotation_List.size(); i++){
				SpectrumAnnotation annotation = annotations.Annotation_List.get(i);
				String task_id = annotation.getTask_id();
				Task task = TaskManager.queryTask(task_id);
				if(task == null){
					logger.error("Could Not Find Task Element " + task_id);
					continue;
				}
				annotation.setUser_id(task.getUser());
			}
			for(int i = 0; i < comments.size(); i++){
				SpectrumComment comment = comments.get(i);
				String task_id = comment.getTask_id();
				Task task = TaskManager.queryTask(task_id);
				if(task == null){
					logger.error("Could Not Find Task Element " + task_id);
					continue;
				}
				comment.setUser_id(task.getUser());
			}

			String response_string = "{ ";

			response_string += "\"annotations\" : [";
			for(int i = 0; i < annotations.Annotation_List.size(); i++){
			//for(SpectrumAnnotation annotation : annotations.Annotation_List){
				SpectrumAnnotation annotation = annotations.Annotation_List.get(i);
				String annotation_json = "{" + annotation.toJSON_intermediate();
				if(username != null){
					Map<String, String> profile = AccountManager.getInstance().getProfile(annotation.getUser_id());
					annotation_json += "," + "\"user_email\" : \"" + JSONObject.escape(profile.get("email"))  + "\"";
				}
				annotation_json += "}";
				response_string += annotation_json;
				if(i < annotations.Annotation_List.size() -1 ) response_string += ",";
			}
			response_string += " ] , \n";


			response_string += "\"comments\" : [";
			//for(SpectrumComment comment : comments){
			for(int i = 0; i < comments.size(); i++){
				SpectrumComment comment = comments.get(i);
				String comment_json = comment.toJSON();
				response_string += comment_json;
				if(i < comments.size() -1 ) response_string += ",";
			}
			response_string += " ] , \n";

			//Adding Spectrum Info
			response_string += "\"spectruminfo\" : ";
			response_string += specinfo.toJSON();
			response_string += ",\n";

			response_string += "\"canAdmin\" : ";
			if(username != null){
				if(AnnotationManager.IsUserOwnerOfLibrarySpectrum(username, SpectrumID)){
					response_string += "1";
				}
				else{
					response_string += "0";
				}
			}
			else{
				response_string += "0";
			}

			response_string += ",\n";


			//Converting tags to json
			JSONArray json_array = new JSONArray();
			for(int i = 0; i < all_tags.size(); i++){
				JSONObject json_object = new JSONObject();
				for (String key : all_tags.get(i).keySet()) {
					json_object.put(key, all_tags.get(i).get(key));
				}
				json_array.add(json_object);
			}
			response_string += "\"spectrum_tags\" : ";
			response_string += json_array.toJSONString();

			response_string += " } ";

			response.getOutputStream().println(response_string);


			return;
		}

		response.getOutputStream().print("ERROR");
	}
}
