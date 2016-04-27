package edu.ucsd.livesearch.servlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsd.livesearch.account.AccountManager;
import edu.ucsd.livesearch.dataset.Dataset;
import edu.ucsd.livesearch.dataset.DatasetManager;
import edu.ucsd.livesearch.dataset.DatasetPublisher;
import edu.ucsd.livesearch.dataset.MassiveComment;
import edu.ucsd.livesearch.dataset.MassiveCommentManager;
import edu.ucsd.livesearch.dataset.UserDatasetCountPair;
import edu.ucsd.livesearch.parameter.ResourceManager;
import edu.ucsd.livesearch.subscription.SubscriptionManager;
import edu.ucsd.livesearch.task.Task;
import edu.ucsd.livesearch.task.TaskManager;
import edu.ucsd.livesearch.util.WorkflowParameterUtils;



public class MassiveServlet extends BaseServlet{
	private static final long serialVersionUID = 1L;
	private Logger logger = LoggerFactory.getLogger(MassiveServlet.class);

	//Signing up for Subscription
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String username = (String)request.getSession().getAttribute("livesearch.user");
		String task_id = request.getParameter("task");

		//String massive_id = request.getParameter("massiveID");
		//Checking if user is logged in;
		if(username == null){
			response.getOutputStream().print("LOGIN");
			return;
		}

		if(task_id != null){
			Dataset massive_dataset = DatasetManager.queryDatasetByTaskID(task_id);
			response.getOutputStream().print("{\"dataset_id\" : " + massive_dataset.getDatasetID() + ",");
			int sub_status = SubscriptionManager.add_subscription(username, massive_dataset.getDatasetID());
			//Status 0 means success
			if(sub_status == 0){
				//Launch Conversion Task
				//HttpParameters task_parameters = new HttpParameters();
				//task_parameters.setParameter("workflow", "CONVERT-RAW");
				//String massive_input = "d." + String.format("MSV%09d", massive_dataset.getDatasetID()) + "/spectrum%3B";
				//task_parameters.setParameter("spec_on_server", massive_input);
				//TaskFactory.createTask("continuous", task_parameters);
			}

			response.getOutputStream().print("\"status\" : " + sub_status + "}");
			return;
		}

		response.getOutputStream().print("ERROR");

	}

	//Getting Sub Status
	@Override
	protected void doGet(
		HttpServletRequest request, HttpServletResponse response
	) throws ServletException, IOException {
		String function = request.getParameter("function");

		if(function == null){
			response.getOutputStream().print("ERROR");
			return;
		}

		if(function.compareTo("subscription") == 0){
			String username = (String)request.getSession().getAttribute("livesearch.user");
			String task_id = request.getParameter("task");
			//String massive_id = request.getParameter("massiveID");
			//Checking if user is logged in;
			if(username == null){
				response.getOutputStream().print("LOGIN");
				return;
			}

			if(task_id != null){
				Dataset massive_dataset = DatasetManager.queryDatasetByTaskID(task_id);
				response.getOutputStream().print("{\"dataset_id\" : " + massive_dataset.getDatasetID() + ",");
				int sub_status = SubscriptionManager.check_subscription(username, massive_dataset.getDatasetID());
				response.getOutputStream().print("\"status\" : " + sub_status + "}");
				return;
			}

			response.getOutputStream().print("ERROR");
			return;
		}
		if(function.compareTo("comment") == 0){
			String task_id = request.getParameter("task");
			if(task_id != null){
				Dataset massive_dataset = DatasetManager.queryDatasetByTaskID(task_id);

				if(massive_dataset == null){
					logger.info(task_id + " is not associated with a dataset");
					response.getOutputStream().print("ERROR: Not a Dataset");
					return;
				}

				int massive_dataset_id = massive_dataset.getDatasetID();
				List<MassiveComment> comments = MassiveCommentManager.get_All_Dataset_Comments(massive_dataset_id);

				//Filling In username
				for(int i = 0; i < comments.size(); i++){
						MassiveComment comment = comments.get(i);
						String comment_task_id = comment.getTask_id();
						Task task = TaskManager.queryTask(comment_task_id);
						if(task == null){
							logger.error("Could Not Find Task Element " + comment_task_id);
							continue;
						}
						comment.setUser(task.getUser());
						comment.setCreate_time(task.getCreateTime());
					}

				String response_string = "{ ";

				response_string += "\"massivecomments\" : [";
				for(int i = 0; i < comments.size(); i++){
					MassiveComment comment = comments.get(i);
					String annotation_json = comment.toJSON();
					response_string += annotation_json;
					if(i < comments.size() -1 ) response_string += ",";
				}
				response_string += " ]  \n";

				response_string += " } ";

				response.getOutputStream().println(response_string);

				return;
			}
			response.getOutputStream().print("ERROR");
			return;
		}

		if(function.compareTo("reanalysis") == 0){
			String task_id = request.getParameter("task");
			if(task_id != null){
				Dataset massive_dataset = DatasetManager.queryDatasetByTaskID(task_id);

				if(massive_dataset == null){
					logger.info(task_id + " is not associated with a dataset");
					response.getOutputStream().print("ERROR: Not a Dataset");
					return;
				}

				int massive_dataset_id = massive_dataset.getDatasetID();
				List<MassiveComment> reanalyses = MassiveCommentManager.get_All_Dataset_Reanalyses(massive_dataset_id);

				//Filling In username
				for(int i = 0; i < reanalyses.size(); i++){
						MassiveComment reanalysis = reanalyses.get(i);
						String comment_task_id = reanalysis.getTask_id();
						Task task = TaskManager.queryTask(comment_task_id);
						if(task == null){
							logger.error("Could Not Find Task Element " + comment_task_id);
							continue;
						}
						reanalysis.setUser(task.getUser());
						reanalysis.setCreate_time(task.getCreateTime());
					}

				String response_string = "{ ";

				response_string += "\"massivereanalyses\" : [";
				for(int i = 0; i < reanalyses.size(); i++){
					MassiveComment reanalysis = reanalyses.get(i);
					String annotation_json = reanalysis.toJSON();
					response_string += annotation_json;
					if(i < reanalyses.size() -1 ) response_string += ",";
				}
				response_string += " ]  \n";

				response_string += " } ";

				response.getOutputStream().println(response_string);

				return;
			}
			response.getOutputStream().print("ERROR");
			return;
		}

		if(function.compareTo("tasktomassiveid") == 0){
			String task_id = request.getParameter("task");
			if(task_id != null){
				Dataset massive_dataset = DatasetManager.queryDatasetByTaskID(task_id);

				if(massive_dataset == null){
					logger.info(task_id + " is not associated with a dataset");
					response.getOutputStream().print("ERROR: Not a Dataset");
					return;
				}

				int massive_dataset_id = massive_dataset.getDatasetID();

				response.getOutputStream().print(massive_dataset_id);
				return;
			}
			response.getOutputStream().print("ERROR: Task is null");
			return;
		}

		if(function.compareTo("massiveinformation") == 0){
			String task_id = request.getParameter("task");
			String massive_id = request.getParameter("massiveid");
			String callback_function = request.getParameter("callback");
			Dataset massive_dataset = null;
			String username = (String)request.getSession().getAttribute("livesearch.user");
			if(task_id != null){
				massive_dataset = DatasetManager.queryDatasetByTaskID(task_id);
			}
			// if querying by MassIVE ID, don't show private datasets for
			// security reasons - it's ok if querying by obfuscated task ID
			if(massive_id != null){
				massive_dataset = DatasetManager.queryDatasetByID(massive_id);
				if(massive_dataset.isPrivate()){
					massive_dataset = null;
				}
			}

			if(massive_dataset == null){
				logger.info(task_id + " is not associated with a dataset");
				response.getOutputStream().print("ERROR: Not a Dataset");
				return;
			}

			int massive_dataset_id = massive_dataset.getDatasetID();
			StringBuilder result = new StringBuilder();
			task_id = massive_dataset.getTaskID();

			Task dataset_task = TaskManager.queryTask(task_id);

			String desc = dataset_task.getDescription();
			String upload_user = dataset_task.getUser();
			// determine if the current user has
			// authorization to modify this dataset
			HttpSession session = request.getSession();
			boolean hasAccess = ServletUtils.isAdministrator(session) ||
				ServletUtils.sameIdentity(session, upload_user);

			List<String> subscribed_users = SubscriptionManager.get_all_dataset_subscription(massive_dataset_id);
			// verify "keywords" string value, since it might be null for
			// legacy datasets, and we don't want to show "null" to the user
			String keywords = null;
			try {
				keywords = WorkflowParameterUtils.getParameter(
					dataset_task, "dataset.keywords");
			} catch (Throwable error) {
//				logger.error(String.format("Could not retrieve the " +
//					"keywords for legacy dataset %s (task [%s]).",
//					massive_dataset.getDatasetIDString(),
//					dataset_task.getID()), error);
			}
			if (keywords == null)
				keywords = "";
			else keywords = resolveSemicolonDelimitedList(keywords);

			result.append("{ ");
			result.append("\"task\" : \"" + task_id + "\" ,");
			result.append("\"title\" : \"" + JSONObject.escape(desc) + "\" ,");
			result.append("\"dataset_id\" : \"" + Dataset.generateDatasetIDString(massive_dataset_id) + "\" ,");
			result.append("\"modifications\" : \"" + JSONObject.escape( resolveCVLabels(massive_dataset.getModification(), "modification" )) + "\" ,");
			result.append("\"instrument\" : \"" + JSONObject.escape( resolveCVLabels(massive_dataset.getInstrument(), "instrument" )) + "\" ,");
			result.append("\"species\" : \"" + JSONObject.escape( resolveCVLabels(massive_dataset.getSpecies(), "species")) + "\" ,");
			result.append("\"pi\" : \"" + JSONObject.escape(massive_dataset.getPI()) + "\" ,");
			result.append("\"description\" : \"" + JSONObject.escape(massive_dataset.getDescription()) + "\" ,");
			result.append("\"publications\" : " + massive_dataset.getPublicationsJSON() + " ,");
			result.append("\"pxaccession\" : \"" + JSONObject.escape(massive_dataset.getAnnotation("px_accession")) + "\" ,");
			result.append("\"convertedandcomputable\" : \"" + massive_dataset.isConvertedAndComputable() + "\" ,");
			result.append("\"subscriptions\" : \"" + subscribed_users.size() + "\" ,");
			if(ServletUtils.isAdministrator(session)){
				result.append("\"subscribers\" : " + JSONValue.toJSONString(subscribed_users) + " ,");
			}
			result.append("\"filesize\" : \"" + massive_dataset.getFileSizeString() + "\" ,");
			result.append("\"private\" : \"" + massive_dataset.isPrivate() + "\" ,");
			result.append("\"complete\" : \"" + massive_dataset.isComplete() + "\" ,");
			result.append("\"user\" : \"" + JSONObject.escape(upload_user) + "\" ,");
			if(username != null){
				Map<String, String> profile = AccountManager.getInstance().getProfile(upload_user);
				result.append("\"email\" : \"" + JSONObject.escape(profile.get("email")) + "\" ,");
			}
			result.append("\"has_access\" : \"" + hasAccess + "\" ,");
			result.append("\"filecount\" : \"" + massive_dataset.getFileCountString() + "\" , ");
			result.append("\"keywords\" : \"" + JSONObject.escape(keywords) + "\" , ");
			result.append("\"ftp\" : \"" + JSONObject.escape(DatasetPublisher.getDatasetFTPURL(massive_dataset)) + "\" ");

			result.append(" }");

			if(callback_function != null){
				//Do JSONP Stuff
				String jsonp_string = callback_function + "(" + result.toString() + ");";
				response.getOutputStream().write(jsonp_string.getBytes("UTF-8"));
			}
			else{
				response.getOutputStream().write(result.toString().getBytes("UTF-8"));
			}


			//response.getOutputStream().print(result.toString());
			return;

		}

		if(function.compareTo("massivestatistics") == 0){

			Map<Task, Dataset> datasets = DatasetManager.queryDatasetsByPrivacy(false);
			Map<String, Integer> user_datasets_count = new HashMap<String, Integer>();

			for (Task key : datasets.keySet()) {
				String username = key.getUser();
				if(user_datasets_count.containsKey(username) == false){
					user_datasets_count.put(username, 0);
				}
				user_datasets_count.put(username, user_datasets_count.get(username) + 1);
			}

			List<UserDatasetCountPair> user_datasets_count_list = new ArrayList<UserDatasetCountPair>();

			for (String username : user_datasets_count.keySet()){
				if(!username.contains("tranche"))
					user_datasets_count_list.add(new UserDatasetCountPair(username, user_datasets_count.get(username)));
			}

			//Sorting list
			Collections.sort(user_datasets_count_list);

			String user_counts_list_json = "[";

			for(int i = 0; i < user_datasets_count_list.size(); i++){
				user_counts_list_json += "{";

				user_counts_list_json += "\"username\":\"";
				user_counts_list_json += user_datasets_count_list.get(i).username;
				user_counts_list_json += "\"";

				user_counts_list_json += ",";

				user_counts_list_json += "\"count\":\"";
				user_counts_list_json += user_datasets_count_list.get(i).count;
				user_counts_list_json += "\"";

				user_counts_list_json += "}";

				if(i != user_datasets_count_list.size() -1){
					user_counts_list_json += ",";
				}
			}

			user_counts_list_json += "]";

			StringBuilder result = new StringBuilder();
			result.append("{ ");
			result.append("\"user_dataset_count\" : " + user_counts_list_json + "");
			result.append(" }");


			response.getOutputStream().print(result.toString());
			return;
		}

		if(function.compareTo("massiveidtotask") == 0){
			String massive_id = request.getParameter("massiveid");
			if(massive_id != null){
				Dataset massive_dataset = DatasetManager.queryDatasetByID(massive_id);

				if(massive_dataset == null){
					logger.info(massive_id + " is not associated with a dataset");
					response.getOutputStream().print("ERROR: Not a Dataset");
					return;
				}

				//Checking if dataset is private
				if(massive_dataset.isPrivate()){
					logger.info(massive_id + " is not associated with a dataset");
					response.getOutputStream().print("ERROR: Not a Dataset");
					return;
				}

				String massive_task_id = massive_dataset.getTaskID();

				StringBuilder result = new StringBuilder();
				result.append("{ ");
				result.append("\"massive_task_id\" : \"" + massive_task_id + "\"");
				result.append(" }");

				response.getOutputStream().print(result.toString());
				return;
			}
			response.getOutputStream().print("ERROR: Task is null");
			return;
		}

	}

	//Deleting Sub
	@Override
	protected void doDelete(
		HttpServletRequest request, HttpServletResponse response
	) throws ServletException, IOException {
		String username = (String)request.getSession().getAttribute("livesearch.user");
		String task_id = request.getParameter("task");
		//String massive_id = request.getParameter("massiveID");
		//Checking if user is logged in;
		if(username == null){
			response.getOutputStream().print("LOGIN");
			return;
		}

		if(task_id != null){
			Dataset massive_dataset = DatasetManager.queryDatasetByTaskID(task_id);
			response.getOutputStream().print("{\"dataset_id\" : " + massive_dataset.getDatasetID() + ",");
			int sub_status = SubscriptionManager.del_subscription(username, massive_dataset.getDatasetID());
			response.getOutputStream().print("\"status\" : " + sub_status + "}");
			return;
		}

		response.getOutputStream().print("ERROR");

	}

	/*
	 * Resolving CV terms.
	 */
	private static String resolveCVLabels(String list, String resource) {
		if (list == null)
			return "";
		StringBuffer resolved = new StringBuffer();
		Map<String, String> cache = new LinkedHashMap<String, String>();
		Collection<String> labels = ResourceManager.getCVResourceLabels(list, resource, cache);
		for (String label : labels)
			resolved.append(label.trim()).append(" | ");
		// chomp trailing pipe
		if (resolved.toString().endsWith(" | "))
			resolved.setLength(resolved.length() - 3);
		return resolved.toString();
	}

	private static String resolveSemicolonDelimitedList(String list) {
		if (list == null)
			return "";
		String[] elements = list.split(";");
		if (elements == null || elements.length < 1)
			return list;
		StringBuffer resolved = new StringBuffer();
		for (String element : elements)
			resolved.append(element.trim()).append(" | ");
		// chomp trailing pipe
		if (resolved.toString().endsWith(" | "))
			resolved.setLength(resolved.length() - 3);
		return resolved.toString();
	}

}
