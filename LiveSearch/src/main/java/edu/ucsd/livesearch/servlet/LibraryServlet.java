package edu.ucsd.livesearch.servlet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Collection;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsd.livesearch.account.AccountManager;
import edu.ucsd.livesearch.libraries.AnnotationManager;
import edu.ucsd.livesearch.libraries.SpectrumAnnotation;
import edu.ucsd.livesearch.libraries.SpectrumInfo;
import edu.ucsd.livesearch.result.parsers.TabularResult;
import edu.ucsd.livesearch.task.Task;
import edu.ucsd.livesearch.task.TaskManager;
import edu.ucsd.saint.commons.WebAppProps;
import edu.ucsd.livesearch.util.WorkflowParameterUtils;


public class LibraryServlet extends BaseServlet{

	private Logger logger = LoggerFactory.getLogger(LibraryServlet.class);
	//Getting the Annotations and Comments
	@Override
	protected void doGet(
		HttpServletRequest request, HttpServletResponse response
	) throws ServletException, IOException {
		String library_name = request.getParameter("library");
		String showpeaks = request.getParameter("showpeaks");

		String identity = getUser();
	    boolean isAdmin = AccountManager.getInstance().checkRole(identity, "administrator");

		List<String> all_names = new ArrayList<String>();
		if(library_name.compareTo("all") == 0){
	        all_names.add("GNPS-LIBRARY");
	        all_names.add("GNPS-PRESTWICKPHYTOCHEM");
	        all_names.add("GNPS-SELLECKCHEM-FDA-PART1");
	        all_names.add("GNPS-SELLECKCHEM-FDA-PART2");
	        all_names.add("GNPS-NIH-CLINICALCOLLECTION1");
	        all_names.add("GNPS-NIH-CLINICALCOLLECTION2");
	        all_names.add("GNPS-NIH-NATURALPRODUCTSLIBRARY");
	        all_names.add("GNPS-NIH-SMALLMOLECULEPHARMACOLOGICALLYACTIVE");
	        all_names.add("GNPS-FAULKNERLEGACY");
			all_names.add("GNPS-EMBL-MCF");
		}
		else if(library_name.compareTo("PRIVATE-USER") != 0){
			all_names.add(library_name);
		}
		else if(library_name.compareTo("PRIVATE-USER") == 0 && isAdmin){
			all_names.add(library_name);
        }

		if(showpeaks == null){
			response.getOutputStream().print(queryLibrarySpectra(all_names, false));
		}
		else if(showpeaks.compareTo("true") == 0){
			response.getOutputStream().print(queryLibrarySpectra(all_names, true));
		}
		else{
			response.getOutputStream().print(queryLibrarySpectra(all_names, false));
		}

		/*
		String task_id = request.getParameter("task");


		if(task_id != null){
			String output_string = get_current_library_workflow(task_id);
			response.getOutputStream().print(output_string);
			return;
		}

		response.getOutputStream().print("ERROR");*/
	}


	/**
	 * Post to update library status.
	 */
	@Override
	protected void doPost(
		HttpServletRequest request, HttpServletResponse response
	) throws ServletException, IOException {
		String identity = (String)request.getSession().getAttribute("livesearch.user");

		if(identity == null){
			logger.info("Invalid User");
	    	response.getOutputStream().print("ERROR");
			return;
		}

	    boolean isAdmin = AccountManager.getInstance().checkRole(identity, "administrator");

	    String spectrum_id = request.getParameter("SpectrumID");
	    String action = request.getParameter("action");

	    if(action == null){
	    	logger.info("Invalid Action");
	    	response.getOutputStream().print("ERROR");
	    	return;
	    }

	    if(action.equals("enabledisable")){
	    	String newstatus_string = request.getParameter("newstatus");
	    	logger.info("User requesting spectrum status change: " + identity + " to status " + newstatus_string + " on spectrum " + spectrum_id + " is admin " + isAdmin);
		    //checking if user owns this spectrum, that is, it is the first annotator. If not, then reject post
		    if(isAdmin){
		    	int newstatus_int = Integer.parseInt(newstatus_string);
		    	if(newstatus_int == 1 || newstatus_int == 2){
		    		if(spectrum_id.length() == 18 && spectrum_id.indexOf("CCMSLIB") == 0){
		    			//Do Stuff
		    			AnnotationManager.UpdateSpectrumStatus(spectrum_id, newstatus_int);
		    		}
		    		else{
		    			logger.info("Invalid Spectrum ID");
		    		}
		    	}
		    	else{
		    		logger.info("Invalid Status");
		    	}
		    }

	    }

	    //Promoting Private spectrum to public GNPS
	    if(action.equals("promotepublic")){
	    	logger.info("Promoting Public " + spectrum_id);
	    	if(isAdmin){
	    		if(spectrum_id.length() == 18 && spectrum_id.indexOf("CCMSLIB") == 0){
	    			if(AnnotationManager.IsUserOwnerOfLibrarySpectrum(identity, spectrum_id)){
	    				int library_quality = AnnotationManager.Get_Annotation_Recent(spectrum_id).getLibrary_Class();
	    				AnnotationManager.UpdateSpectrumLibraryName(spectrum_id, "GNPS-LIBRARY");
	    				/*if(AnnotationManager.canUserAddToLibrary(identity, library_quality)){
	    				}
	    				else{
	    					logger.info("Invalid User Quality Permission");
	    				}*/
	    			}
	    		}
	    		else{
	    			logger.info("Invalid Spectrum ID");
		    	}
	    	}
	    	else{
	    		logger.info("Invalid Permission");
	    	}
	    }
	    //Making it private again
	    if(action.equals("demoteprivate")){
	    	if(isAdmin){
	    		if(spectrum_id.length() == 18 && spectrum_id.indexOf("CCMSLIB") == 0){
	    			AnnotationManager.UpdateSpectrumLibraryName(spectrum_id, "PRIVATE-USER");
	    		}
	    		else{
	    			logger.info("Invalid Spectrum ID");
		    	}
	    	}
	    	else{
	    		logger.info("Invalid Permission");
	    	}
	    }

		//Updating Library peaks
		if(action.equals("updatepeaks")){
	    	if(isAdmin){
	    		if(spectrum_id.length() == 18 && spectrum_id.indexOf("CCMSLIB") == 0){
					String peaks = request.getParameter("peaks");
	    			AnnotationManager.UpdateSpectrumPeaks(spectrum_id, peaks);
	    		}
	    		else{
	    			logger.info("Invalid Spectrum ID");
		    	}
	    	}
	    	else{
	    		logger.info("Invalid Permission");
	    	}
	    }

		//Updating Library peaks
		if(action.equals("updatesplash")){
	    	if(isAdmin){
	    		if(spectrum_id.length() == 18 && spectrum_id.indexOf("CCMSLIB") == 0){
					String block1 = request.getParameter("block1");
					String block2 = request.getParameter("block2");
					String block3 = request.getParameter("block3");
	    			AnnotationManager.UpdateSpectrumSplash(spectrum_id, block1, block2, block3);
	    		}
	    		else{
	    			logger.info("Invalid Spectrum ID");
		    	}
	    	}
	    	else{
	    		logger.info("Invalid Permission");
	    	}
	    }

		if(action.equals("addspectrumtag")){
			//Check if spectrum id is valid
			SpectrumInfo queried_spectrum = AnnotationManager.Get_Spectrum_Info(spectrum_id);

			if(queried_spectrum == null){
				logger.info("Invalid Spectrum");
				return;
			}

			String tag_type = request.getParameter("tag_type");
			String tag_desc = request.getParameter("tag_desc");
			String tag_database = request.getParameter("tag_database");
			String tag_url = request.getParameter("tag_database_url");

			logger.info(tag_type);
			logger.info(tag_desc);
			logger.info(tag_database);
			logger.info(tag_url);

			if(tag_type == null || tag_desc == null || tag_database == null || tag_url == null){
				response.getOutputStream().print("ERROR");
				return;
			}

			if(tag_type.length() == 0 || tag_desc.length() == 0){
				response.getOutputStream().print("Set type and description ERROR");
				return;
			}

			//Creating a system task
			String systemTaskDescription = String.format("Adding Spectrum Tag for spectrum [%s]", spectrum_id);
			Map<String, Collection<String>> systemTaskParameters = new LinkedHashMap<String, Collection<String>>(15);
			WorkflowParameterUtils.setParameterValue(systemTaskParameters, "spectrumid", spectrum_id);
			WorkflowParameterUtils.setParameterValue(systemTaskParameters, "tag_type", tag_type);
			WorkflowParameterUtils.setParameterValue(systemTaskParameters, "tag_desc", tag_desc);
			WorkflowParameterUtils.setParameterValue(systemTaskParameters, "tag_database", tag_database);
			WorkflowParameterUtils.setParameterValue(systemTaskParameters, "tag_url", tag_url);

			Task systemTask = TaskManager.createSystemTask(identity, "ADD-SPECTRUM-TAG", systemTaskDescription, systemTaskParameters);
            TaskManager.setDone(systemTask);

			AnnotationManager.AddSpectrumTag(spectrum_id, tag_type, tag_desc, tag_database, tag_url, systemTask.getID());
		}

		if(action.equals("removespectrumtag")){
			String task_id = request.getParameter("task_id");

            if(isAdmin){
                String systemTaskDescription = String.format("Disabling Spectrum Tag for spectrum [%s]", spectrum_id);
    			Map<String, Collection<String>> systemTaskParameters = new LinkedHashMap<String, Collection<String>>(15);
    			WorkflowParameterUtils.setParameterValue(systemTaskParameters, "spectrumid", spectrum_id);
                WorkflowParameterUtils.setParameterValue(systemTaskParameters, "tag_task_id", task_id);

    			Task systemTask = TaskManager.createSystemTask(identity, "REMOVE-SPECTRUM-TAG", systemTaskDescription, systemTaskParameters);
                TaskManager.setDone(systemTask);

                AnnotationManager.RemoveSpectrumTag(task_id);

            }

		}
	}


	public static String queryLibrarySpectra(List<String> library_names, boolean populate_peaks){
		List<SpectrumInfo> spectra = new ArrayList<SpectrumInfo>();

		for(String library_name : library_names){
            List<SpectrumInfo> additional_lib_spectra = AnnotationManager.Get_Library_Spectra(library_name, populate_peaks);
            spectra.addAll(additional_lib_spectra);
        }

		List<String> all_spectra_id = new ArrayList<String>();
	    for(SpectrumInfo spec : spectra){
	        all_spectra_id.add(spec.getSpectrum_id());
	    }
	    Map<String, SpectrumAnnotation> library_annotations = AnnotationManager.Get_Most_Recent_Spectrum_Annotations_Batch(all_spectra_id);

	    //Creating a list of tasks to query the owner
	    List<String> submitted_spectra_tasks_list = new ArrayList<String>();
	    for(SpectrumInfo spec : spectra){
	        submitted_spectra_tasks_list.add(spec.getTask_id());
	    }
	    Map<String, Task> submitted_spectra_tasks_map = TaskManager.queryTaskList(submitted_spectra_tasks_list);

	  //Building the String
	    StringBuilder render_string_builder = new StringBuilder();
	    render_string_builder.append("{ \"spectra\": [");
	    int spectrum_count = 0;
	    for(SpectrumInfo spec : spectra){
	    	spectrum_count++;
	        spec.setSubmit_user(submitted_spectra_tasks_map.get(spec.getTask_id()).getUser());
	        if(spectrum_count < spectra.size()){
	        	render_string_builder.append(spec.toJSON_withAnnotation(library_annotations.get(spec.getSpectrum_id())) + ",");
	        }
	        else{
	        	render_string_builder.append(spec.toJSON_withAnnotation(library_annotations.get(spec.getSpectrum_id())));
	        }
	    }
	    render_string_builder.append("]}");
	    String render_string = render_string_builder.toString();

	    return render_string;
	}

	private String get_current_library_workflow(String task_id) throws NullPointerException, IllegalArgumentException, IOException{
		Task task = TaskManager.queryTask(task_id);
		if(task != null){
			String task_temp_path = task.getPath("temp").getAbsolutePath();


			String new_parameter_file_path = task_temp_path + "/" + "extractlibrary.params";
			String viewlibraray_execmodule_output = task_temp_path + "/" + "specnets_output.result";
			String viewlibraray_DB_output = task_temp_path + "/" + "DB_output.result";

			String main_execmodule_path = WebAppProps.getPath("livesearch.tools.path") + "/main_execmodule";
			File main_execmodule_tool = new File(WebAppProps.getPath("livesearch.tools.path"), "main_execmodule");
			String DBTools_path = WebAppProps.getPath("livesearch.tools.path") + "/DBTools.jar";

			String user_data_path = WebAppProps.getPath("livesearch.ftp.path");;

			String library_file = ManageParameters.getFirstTaskParameter(task_id, "upload_file_mapping");
			String[] parts = library_file.split("\\|");
			library_file = parts[1];
			library_file = user_data_path + "/" + library_file;

			String execmodule_parameters = "EXISTING_LIBRARY_MGF=" + library_file + "\n";
			execmodule_parameters += "RESULTS_DIR=" + viewlibraray_execmodule_output + "\n";


			//Writing Params Files
			File f = new File(new_parameter_file_path);
			PrintStream param_file_stream = null;
			try {
				param_file_stream = new PrintStream(f);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			param_file_stream.print(execmodule_parameters);
			param_file_stream.close();

			//Writing execmodule exe string
			String execmodule_execution = main_execmodule_path  + " ExecLibraryView " + new_parameter_file_path;


			logger.error(execmodule_execution);


			//Executing it
			Process p = Runtime.getRuntime().exec(execmodule_execution);

			InputStream err = p.getErrorStream();
		    InputStream std = p.getInputStream();

		    BufferedReader d
	          = new BufferedReader(new InputStreamReader(err));

		    String std_err_line = d.readLine();

		    while(std_err_line != null){
		    	logger.info(std_err_line);
		    	std_err_line = d.readLine();
		    }

			try {
				p.waitFor();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}




			//Assembling DB Tool Exe String
			String dbtools_execution = "java -Xmx128M -cp " + DBTools_path + " com.ucsd.dbtools.GetSpectrumAnnotations " +
				viewlibraray_execmodule_output + " " + viewlibraray_DB_output;

			logger.info(dbtools_execution);

			p = Runtime.getRuntime().exec(dbtools_execution);



			err = p.getErrorStream();
		    std = p.getInputStream();

		    d = new BufferedReader(new InputStreamReader(std));

		    std_err_line = d.readLine();

		    while(std_err_line != null){
		    	logger.info(std_err_line);
		    	std_err_line = d.readLine();
		    }

			try {
				p.waitFor();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			try {
				p.waitFor();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			File db_output_file = new File(viewlibraray_DB_output);

			TabularResult tab_result =
				new TabularResult(db_output_file, task, "");
			tab_result.load();
			String result_json = tab_result.getData();
			//result_json = result_json.replaceAll("'", "\"");

			return result_json;
			//response.getOutputStream().println(dbtools_execution);
			//response.getOutputStream().println(viewlibraray_execmodule_output);
			//response.getOutputStream().println(main_execmodule_path);
			//response.getOutputStream().println(library_file);
		}
		return "";
	}

	private String db_file_to_JSON(String filename){
		BufferedReader in_buf  = null;
		try {
			in_buf = new BufferedReader(new FileReader(filename));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String currentLine = null;
		int line_count = 0;
		Map<String, Integer> column_mapping = new HashMap<String, Integer>();

		try {
			while( (currentLine = in_buf.readLine()) != null ){
				line_count++;
				String[] splits = currentLine.split("\t");
				if(line_count == 1){
					for(int i = 0; i < splits.length; i++){
						//System.out.println(splits[i]+ " => " + i);
						column_mapping.put(splits[i], i);
					}
					continue;
				}



			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "";
	}
}
