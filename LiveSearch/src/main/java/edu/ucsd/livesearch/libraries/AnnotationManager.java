package edu.ucsd.livesearch.libraries;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import edu.ucsd.livesearch.account.AccountManager;
import edu.ucsd.livesearch.servlet.DownloadWorkflowInterface;
import edu.ucsd.livesearch.task.Task;
import edu.ucsd.livesearch.task.TaskManager;
import edu.ucsd.livesearch.util.Commons;
import edu.ucsd.saint.commons.ConnectionPool;

public class AnnotationManager {

	private static final Logger logger =
		LoggerFactory.getLogger(AnnotationManager.class);

	public static SpectrumInfo Get_Spectrum_Info(String SpectrumID){
		ConnectionPool pool = null;
		Connection connection = null;

		// retrieve dataset row from the database
		PreparedStatement statement = null;
		ResultSet result = null;

		SpectrumInfo spec_info_output = null;

		try {
			pool = Commons.getConnectionPool();
			connection = pool.aquireConnection();

			// first get the dataset
			statement = connection.prepareStatement(
	        	"SELECT * FROM massive.spectra WHERE spectrum_id=?");

			System.out.println(SpectrumID);
			int SPECTRUM_ID = SpectrumID_fromString(SpectrumID);
			statement.setInt(1, SPECTRUM_ID);

			result = statement.executeQuery();

			if (result.next() == false)
		        throw new RuntimeException(String.format("No spectrum row could " +
		            "be found with spectrum ID %d.", SPECTRUM_ID));
		    else {
		        String SOURCE_FILE_PATH = result.getString("source_file");
		        int SCAN_NUMBER = Integer.parseInt(result.getString("scan"));
		        String TASK_ID = result.getString("task_id");
		        int MSLEVEL = Integer.parseInt(result.getString("ms_level"));
		        String library_name = result.getString("library_name");
		        int spectrum_status = 0;
		        if(result.getString("status") != null)
		        	spectrum_status = Integer.parseInt(result.getString("status"));
		        String peaks = result.getString("peaks");
				String block1 = result.getString("splash_block1");
				String block2 = result.getString("splash_block2");
				String block3 = result.getString("splash_block3");

		        spec_info_output = new SpectrumInfo(SpectrumID, SOURCE_FILE_PATH, SCAN_NUMBER, TASK_ID, MSLEVEL);
		        spec_info_output.setLibrary_membership(library_name);
		        spec_info_output.setPeaks_json(peaks);
		        spec_info_output.setSpectrum_status(spectrum_status);
				spec_info_output.setSplash(block1, block2, block3);
		    }

		} catch (Exception error) {
			throw new RuntimeException("Error querying spectrum", error);
		} finally {
			pool.close(statement, connection);
		}

		return spec_info_output;
	}

	/**
	 * Querying for all spectra given a library name, all active spectra
	 * @param library_name
	 * @return
	 */
	public static List<SpectrumInfo> Get_Library_Spectra(String library_name, boolean populate_peaks){
		return Get_Library_Spectra(library_name, populate_peaks, 1);
	}

	/**
	 * Querying for all spectra given a library name, all inactive spectra
	 * @param library_name
	 * @return
	 */
	public static List<SpectrumInfo> Get_Library_Spectra_disabled(String library_name, boolean populate_peaks){
		return Get_Library_Spectra(library_name, populate_peaks, 2);
	}


	/**
	 * Querying for all spectra given a library name, regardless of activeness
	 * @param library_name
	 * @param populate_peaks
	 * @return
	 */
	public static List<SpectrumInfo> Get_Library_Spectra_all(String library_name, boolean populate_peaks){
		return Get_Library_Spectra(library_name, populate_peaks, -1);
	}

	/**
	 * Returning the library spectra given the library name and status of the spectra
	 * @param library_name
	 * @param populate_peaks
	 * @param status
	 * @return
	 */
	public static List<SpectrumInfo> Get_Library_Spectra(String library_name, boolean populate_peaks, int status){
		logger.info("QUERYING LIBRARY NAME: " + library_name);

		List<SpectrumInfo> specs = new ArrayList<SpectrumInfo>();
		ConnectionPool pool = null;
		Connection connection = null;

		// retrieve dataset row from the database
		PreparedStatement statement = null;
		ResultSet result = null;

		SpectrumInfo spec_info_output = null;

		try {
			pool = Commons.getConnectionPool();
			connection = pool.aquireConnection();

			if(status > 0){
				// first get the dataset
				statement = connection.prepareStatement(
		        	"SELECT * FROM massive.spectra WHERE library_name=? AND status=?");

				statement.setString(1, library_name);
				statement.setInt(2, status);
			}
			else{
				// first get the dataset
				statement = connection.prepareStatement(
		        	"SELECT * FROM massive.spectra WHERE library_name=?");

				statement.setString(1, library_name);
			}

			result = statement.executeQuery();

			while(result.next()){
				String SOURCE_FILE_PATH = result.getString("source_file");
		        int SCAN_NUMBER = Integer.parseInt(result.getString("scan"));
		        int spectrum_id = Integer.parseInt(result.getString("spectrum_id"));
		        String TASK_ID = result.getString("task_id");
		        int MSLEVEL = Integer.parseInt(result.getString("ms_level"));
		        String library_name_out = result.getString("library_name");
		        int spectrum_status = 0;
		        if(result.getString("status") != null)
		        	spectrum_status = Integer.parseInt(result.getString("status"));


		        spec_info_output = new SpectrumInfo(SpectrumID_toString(spectrum_id), SOURCE_FILE_PATH, SCAN_NUMBER, TASK_ID, MSLEVEL);
		        spec_info_output.setLibrary_membership(library_name_out);
		        if(populate_peaks){
		        	String peaks = result.getString("peaks");
		        	spec_info_output.setPeaks_json(peaks);
		        }
				String block1 = result.getString("splash_block1");
				String block2 = result.getString("splash_block2");
				String block3 = result.getString("splash_block3");
				spec_info_output.setSplash(block1, block2, block3);

		        spec_info_output.setSpectrum_status(spectrum_status);

		        specs.add(spec_info_output);
			}
		} catch (Exception error) {
			throw new RuntimeException("Error querying for spectra", error);
		} finally {
			pool.close(statement, connection);
		}

		return specs;
	}

	/**
	 *
	 * @param user
	 * @return
	 */
	public static Map<SpectrumInfo, SpectrumAnnotation> Get_Private_Spectrum_Recent_Annotation(String user){
		List<SpectrumInfo> library_spectra = Get_Library_Spectra_all("PRIVATE-USER", false);

		Map<SpectrumInfo, SpectrumAnnotation> users_annotations = new HashMap<SpectrumInfo, SpectrumAnnotation>();
		Map<SpectrumInfo, SpectrumAnnotation> recent_annotations = Get_Most_Recent_Spectrum_Annotations_Batch_SpectrumInfo(library_spectra);

		//Getting Task LIst
		Map<String, SpectrumInfo> task_id_to_info = new HashMap<String, SpectrumInfo>();
		List<String> task_list = new ArrayList<String>();
		for(SpectrumInfo info : library_spectra){
			task_id_to_info.put(info.getTask_id(), info);
			task_list.add(info.getTask_id());
		}

		Map<String, Task> submitted_spectra_tasks_map = TaskManager.queryTaskList(task_list);

		for(SpectrumInfo info : library_spectra){
			if(submitted_spectra_tasks_map.get(info.getTask_id()).getUser().equals(user)){
				users_annotations.put(info, recent_annotations.get(info));
			}
		}


		return users_annotations;
	}

	/**
	 * Getting most recent annotation for library id
	 * @param SpectrumID
	 * @return
	 */
	public static SpectrumAnnotation Get_Annotation_Recent(String SpectrumID){

		SpectrumAnnotationSet output_annotations = Get_All_Spectrum_Annotations(SpectrumID);

		if(output_annotations.Annotation_List.size() <= 0)
			return null;
		//Figure out how to sort by TimeStamp
		Collections.sort(output_annotations.Annotation_List, new TimeStampComparater());

		return output_annotations.Get_Annotation_By_Index(0);
	}


	public static SpectrumAnnotationSet Get_All_Spectrum_Annotations(String SpectrumID){
		ConnectionPool pool = null;
		Connection connection = null;


		// retrieve dataset row from the database
		PreparedStatement statement = null;
		ResultSet result = null;

		SpectrumAnnotationSet output_annotation = new SpectrumAnnotationSet();

		try {
			pool = Commons.getConnectionPool();
			connection = pool.aquireConnection();


			String sql_query = "SELECT * FROM massive.spectrum_annotations, ";
			sql_query +=  "tasks ";
			sql_query += "WHERE spectrum_id=? AND ";
			sql_query += "tasks.task_id=massive.spectrum_annotations.task_id";

			System.out.println(sql_query);

			statement = connection.prepareStatement(sql_query);
			statement.setInt(1, SpectrumID_fromString(SpectrumID));
			logger.info(statement.toString());
			result = statement.executeQuery();

			while (result.next()){
				System.out.println("Result Something");
				Timestamp create_time =  result.getTimestamp("create_time");
				System.out.println(create_time.toString());
				output_annotation.Add_Annotation(Result_To_Annotation(result));
			}

		} catch (Exception error) {
			throw new RuntimeException("There was an error annotation", error);
		} finally {
			pool.close(statement, connection);
		}

		return output_annotation;
	}

	public static List<SpectrumComment> get_Comments_of_Annotation(String annotation_task_id, String spectrum_of_annotation_id){
		ConnectionPool pool = null;
		Connection connection = null;

		PreparedStatement statement = null;

		try {
			pool = Commons.getConnectionPool();
			connection = pool.aquireConnection();

					//First Create Comment
			statement = connection.prepareStatement(
					        "SELECT DISTINCT massive.spectrum_comments.spectrum_id, massive.spectrum_comments.task_id, massive.spectrum_comments.comment " +
				        	"FROM massive.spectrum_comments, massive.spectrum_annotations, massive.spectrum_annotation_comments " +
					        "WHERE massive.spectrum_annotation_comments.comment_id = massive.spectrum_comments.task_id " +
							"AND massive.spectrum_annotation_comments.annotation_id = ? AND massive.spectrum_comments.spectrum_id = ?");
			statement.setString(1, annotation_task_id);
			statement.setInt(2, SpectrumID_fromString(spectrum_of_annotation_id));

			ResultSet result = statement.executeQuery();

			List<SpectrumComment> annotation_comments = new ArrayList<SpectrumComment>();
			while(result.next() == true){
				int spectrum_id = result.getInt("spectrum_id");
				String comment = result.getString("comment");
				String task_id = result.getString("task_id");
				Task comment_task = TaskManager.queryTask(task_id);
				System.out.println(spectrum_id + " " +  comment + " " + task_id);
				SpectrumComment spec_comment = new SpectrumComment(spectrum_id, comment, task_id);
				spec_comment.setAnnotation_id(annotation_task_id);
				spec_comment.setExecution_site(comment_task.getSite());
				spec_comment.setCreate_time(comment_task.getCreateTime());
				annotation_comments.add(spec_comment);
			}

			return annotation_comments;

		} catch (Exception error) {
			throw new RuntimeException("Error adding Comment: There was " +
			        "an error inserting the spectrum row into the database.", error);
		} finally {
			pool.close(statement, connection);
		}
	}

	public static List<SpectrumComment> get_All_Annotation_Comments_per_Spectrum(String spectrum_id){

		SpectrumAnnotationSet annots = Get_All_Spectrum_Annotations(spectrum_id);

		List<SpectrumComment> all_comments = new ArrayList<SpectrumComment>();

		for(SpectrumAnnotation annotation : annots.Annotation_List){
			List<SpectrumComment> comments = get_Comments_of_Annotation(annotation.getTask_id(), spectrum_id);
			all_comments.addAll(comments);
		}

		return all_comments;
	}


	public static Map<SpectrumInfo, SpectrumAnnotation> Get_Most_Recent_Spectrum_Annotations_Batch_SpectrumInfo(List<SpectrumInfo> SpectrumIDs){
		List<String> spectrumIds = new ArrayList<String>();
		for(SpectrumInfo info : SpectrumIDs){
			spectrumIds.add(info.getSpectrum_id());
		}
		Map<String, SpectrumAnnotation> annotations = Get_Most_Recent_Spectrum_Annotations_Batch(spectrumIds);

		Map<SpectrumInfo, SpectrumAnnotation> annotations_map = new HashMap<SpectrumInfo, SpectrumAnnotation>();
		for(SpectrumInfo info : SpectrumIDs){
			annotations_map.put(info, annotations.get(info.getSpectrum_id()));
		}

		return annotations_map;
	}

	/**
	 * Returns the most recent annotation in a batch
	 * @param SpectrumIDs
	 * @return
	 */
	public static Map<String, SpectrumAnnotation> Get_Most_Recent_Spectrum_Annotations_Batch(List<String> SpectrumIDs){
		Map<String, SpectrumAnnotationSet> intermediate_spectrum_map = new HashMap<String, SpectrumAnnotationSet>();

		for(int i = 0; i < SpectrumIDs.size(); i++){
			intermediate_spectrum_map.put(SpectrumIDs.get(i), new SpectrumAnnotationSet());
		}

		SpectrumAnnotationSet all_annotations = Get_All_Spectrum_Annotations_Batch(SpectrumIDs);

		//Putting all annotations in the map
		for(int i = 0; i < all_annotations.Annotation_List.size(); i++){
			String cur_specid = all_annotations.Annotation_List.get(i).getSpectrumID();
			intermediate_spectrum_map.get(cur_specid).Annotation_List.add(all_annotations.Annotation_List.get(i));
		}

		//Sorting all annotations per spectrum
		Map<String, SpectrumAnnotation> recent_annotation_map = new HashMap<String, SpectrumAnnotation>();
		for(int i = 0; i < SpectrumIDs.size(); i++){
			Collections.sort(intermediate_spectrum_map.get(SpectrumIDs.get(i)).Annotation_List, new TimeStampComparater());

			if(intermediate_spectrum_map.get(SpectrumIDs.get(i)).Annotation_List.size() > 0){
				recent_annotation_map.put(SpectrumIDs.get(i), intermediate_spectrum_map.get(SpectrumIDs.get(i)).Get_Annotation_By_Index(0));
			}
		}

		return recent_annotation_map;
	}

	public static Map<SpectrumInfo, SpectrumAnnotation> Get_Oldest_Spectrum_Annotations_Batch_SpectrumInfo(List<SpectrumInfo> SpectrumIDs){
		List<String> spectrumIds = new ArrayList<String>();
		for(SpectrumInfo info : SpectrumIDs){
			spectrumIds.add(info.getSpectrum_id());
		}
		Map<String, SpectrumAnnotation> annotations = Get_Oldest_Spectrum_Annotations_Batch(spectrumIds);

		Map<SpectrumInfo, SpectrumAnnotation> annotations_map = new HashMap<SpectrumInfo, SpectrumAnnotation>();
		for(SpectrumInfo info : SpectrumIDs){
			annotations_map.put(info, annotations.get(info.getSpectrum_id()));
		}

		return annotations_map;
	}

	/**
	 * Returns the most old annotation in a batch
	 * @param SpectrumIDs
	 * @return
	 */
	public static Map<String, SpectrumAnnotation> Get_Oldest_Spectrum_Annotations_Batch(List<String> SpectrumIDs){
		Map<String, SpectrumAnnotationSet> intermediate_spectrum_map = new HashMap<String, SpectrumAnnotationSet>();

		for(int i = 0; i < SpectrumIDs.size(); i++){
			intermediate_spectrum_map.put(SpectrumIDs.get(i), new SpectrumAnnotationSet());
		}

		SpectrumAnnotationSet all_annotations = Get_All_Spectrum_Annotations_Batch(SpectrumIDs);

		//Putting all annotations in the map
		for(int i = 0; i < all_annotations.Annotation_List.size(); i++){
			String cur_specid = all_annotations.Annotation_List.get(i).getSpectrumID();
			intermediate_spectrum_map.get(cur_specid).Annotation_List.add(all_annotations.Annotation_List.get(i));
		}

		//Sorting all annotations per spectrum
		Map<String, SpectrumAnnotation> recent_annotation_map = new HashMap<String, SpectrumAnnotation>();
		for(int i = 0; i < SpectrumIDs.size(); i++){
			Collections.sort(intermediate_spectrum_map.get(SpectrumIDs.get(i)).Annotation_List, new TimeStampComparater());

			if(intermediate_spectrum_map.get(SpectrumIDs.get(i)).Annotation_List.size() > 0){
				recent_annotation_map.put(SpectrumIDs.get(i), intermediate_spectrum_map.get(SpectrumIDs.get(i)).Get_Annotation_By_Index(intermediate_spectrum_map.get(SpectrumIDs.get(i)).Annotation_List.size() - 1));
			}
		}

		return recent_annotation_map;
	}

	/**
	 * Returns all annotations per spectrum ID
	 * @param SpectrumIDs
	 * @return
	 */
	public static Map<String, SpectrumAnnotationSet > Get_All_Spectrum_Annotations_Batch_Map(List<String> SpectrumIDs){
		Map<String, SpectrumAnnotationSet> intermediate_spectrum_map = new HashMap<String, SpectrumAnnotationSet>();

		for(int i = 0; i < SpectrumIDs.size(); i++){
			intermediate_spectrum_map.put(SpectrumIDs.get(i), new SpectrumAnnotationSet());
		}

		SpectrumAnnotationSet all_annotations = Get_All_Spectrum_Annotations_Batch(SpectrumIDs);

		//Putting all annotations in the map
		for(int i = 0; i < all_annotations.Annotation_List.size(); i++){
			String cur_specid = all_annotations.Annotation_List.get(i).getSpectrumID();
			intermediate_spectrum_map.get(cur_specid).Annotation_List.add(all_annotations.Annotation_List.get(i));
		}

		return intermediate_spectrum_map;
	}

	/**
	 * Returns all the annotations giving a set of spectrumIDs
	 * @param SpectrumIDs
	 * @return
	 */
	public static SpectrumAnnotationSet Get_All_Spectrum_Annotations_Batch(List<String> SpectrumIDs){
		ConnectionPool pool = null;
		Connection connection = null;

		// retrieve dataset row from the database
		PreparedStatement statement = null;
		ResultSet result = null;

		SpectrumAnnotationSet output_annotation = new SpectrumAnnotationSet();

		List<Integer> SpectrumID_ints = new ArrayList<Integer>();
		int non_null_spectrumIDs = 0;
		for(String id : SpectrumIDs){
			if(id.compareTo("N/A") == 0)
				continue;
			SpectrumID_ints.add(SpectrumID_fromString(id));
			non_null_spectrumIDs++;
		}

		try {
			pool = Commons.getConnectionPool();
			connection = pool.aquireConnection();

			// first get the dataset
			String sql_query = "SELECT * FROM massive.spectrum_annotations, ";
			sql_query +=  "tasks ";
			sql_query += "WHERE spectrum_id in (%s) AND ";
			sql_query += "tasks.task_id=massive.spectrum_annotations.task_id";

			System.out.println(sql_query);

			if(non_null_spectrumIDs == 0)
				return output_annotation;

			String query_placeholder = preparePlaceHolders(non_null_spectrumIDs);


			String sql_formatted = String.format(sql_query, query_placeholder);

			statement = connection.prepareStatement(sql_formatted);

			setValues(statement, SpectrumID_ints);

			System.out.println(statement.toString());
			result = statement.executeQuery();

			while (result.next()){
				System.out.println("Result Something");
				Timestamp create_time =  result.getTimestamp("create_time");
				System.out.println(create_time.toString());
				output_annotation.Add_Annotation(Result_To_Annotation(result));
			}

		} catch (Exception error) {
			throw new RuntimeException("Error adding Comment: There was " +
			        "an error inserting the spectrum row into the database.", error);
		} finally {
			pool.close(statement, connection);
		}

		return output_annotation;
	}

	/**
	 * Updating new status for spectrum id
	 * @param spectrum_id
	 * @param new_status
	 * @return
	 */
	public static int UpdateSpectrumStatus(String SpectrumID, int new_status){
		ConnectionPool pool = null;
		Connection connection = null;

		// retrieve dataset row from the database
		PreparedStatement statement = null;

		try {
			pool = Commons.getConnectionPool();
			connection = pool.aquireConnection();

			statement = connection.prepareStatement(
	        "UPDATE massive.spectra SET status=? WHERE spectrum_id=? ");
		    statement.setInt(1, new_status);
		    statement.setInt(2, SpectrumID_fromString(SpectrumID));

		    logger.info(statement.toString());
			int insertion = statement.executeUpdate();
			logger.info("Update SpectrumInfo " + insertion);

		} catch (Exception error) {
			throw new RuntimeException("Error querying spectrum", error);
		} finally {
			pool.close(statement, connection);
		}

		return 0;
	}

	/**
	 * Updating the Spectrum Library Name
	 * @param SpectrumID
	 * @param new_library
	 * @return
	 */
	public static int UpdateSpectrumLibraryName(String SpectrumID, String new_library){
		ConnectionPool pool = null;
		Connection connection = null;

		// retrieve dataset row from the database
		PreparedStatement statement = null;

		try {
			pool = Commons.getConnectionPool();
			connection = pool.aquireConnection();

			statement = connection.prepareStatement(
	        "UPDATE massive.spectra SET library_name=? WHERE spectrum_id=? ");
		    statement.setString(1, new_library);
		    statement.setInt(2, SpectrumID_fromString(SpectrumID));

		    logger.info(statement.toString());
			int insertion = statement.executeUpdate();
			logger.info("Update SpectrumInfo " + insertion);

		} catch (Exception error) {
			throw new RuntimeException("Error querying spectrum", error);
		} finally {
			pool.close(statement, connection);
		}

		return 0;
	}


	/**
	 * Updating the peaks in a spectrum
	 * @param SpectrumID
	 * @param new_library
	 * @return
	 */
	public static int UpdateSpectrumPeaks(String SpectrumID, String peaks_string){
		ConnectionPool pool = null;
		Connection connection = null;

		// retrieve dataset row from the database
		PreparedStatement statement = null;

		try {
			pool = Commons.getConnectionPool();
			connection = pool.aquireConnection();

			statement = connection.prepareStatement(
	        "UPDATE massive.spectra SET peaks=? WHERE spectrum_id=? ");
		    statement.setString(1, peaks_string);
		    statement.setInt(2, SpectrumID_fromString(SpectrumID));

		    logger.info(statement.toString());
			int insertion = statement.executeUpdate();
			logger.info("Update Spectrum Peaks " + insertion);

		} catch (Exception error) {
			throw new RuntimeException("Error querying spectrum", error);
		} finally {
			pool.close(statement, connection);
		}

		return 0;
	}

	/**
	 * Adds a spectrum tag
	 * @param spectrumID
	 * @param tag_type
	 * @param tag_desc
	 * @param tag_database
	 * @param tag_url
	 * @param task_id
	 * @return
	 */
	public static int AddSpectrumTag(String spectrumID, String tag_type, String tag_desc, String tag_database, String tag_url, String task_id){
		ConnectionPool pool = null;
		Connection connection = null;

		// retrieve dataset row from the database
		PreparedStatement statement = null;

		try {
			pool = Commons.getConnectionPool();
			connection = pool.aquireConnection();

			statement = connection.prepareStatement("INSERT INTO `massive`.`spectrum_tags` (`type`, `task_id`, `desc`, `database`, `database_url`, `spectrum_id`, `status`) VALUES(?, ?, ?, ?, ?, ?, ?)");
			statement.setString(1, tag_type);
			statement.setString(2, task_id);
			statement.setString(3, tag_desc);
			statement.setString(4, tag_database);
			statement.setString(5, tag_url);
			statement.setInt(6, SpectrumID_fromString(spectrumID));
			statement.setString(7, "ENABLED");

			int insertion = statement.executeUpdate();
			logger.info("Insertion " + insertion);

		} catch (Exception error) {
			throw new RuntimeException("Error adding tag to spectrum", error);
		} finally {
			pool.close(statement, connection);
		}
		return 0;
	}

	/**
	 * Disables a Spectrum Tag
	 * @param tag_id
	 * @return
	 */
	public static int RemoveSpectrumTag(String task_id){
		ConnectionPool pool = null;
		Connection connection = null;

		// retrieve dataset row from the database
		PreparedStatement statement = null;

		try {
			pool = Commons.getConnectionPool();
			connection = pool.aquireConnection();

			statement = connection.prepareStatement("UPDATE `massive`.`spectrum_tags` SET `status`=? WHERE `task_id`=? ");
		    statement.setString(1, "DISABLED");
			statement.setString(2, task_id);

		    logger.info(statement.toString());
			int insertion = statement.executeUpdate();
			logger.info("Disabling Tag " + insertion);

		} catch (Exception error) {
			throw new RuntimeException("Error disabling tag", error);
		} finally {
			pool.close(statement, connection);
		}

		return 0;
	}

	/**
	 * Get all tags for a spectrum, if get_all_tags is false, only get enabled
	 * @param spectrumID
	 * @return
	 */
	public static List<Map<String, String> > GetAllSpectrumTags(String spectrumID, boolean get_all_tags){
		ConnectionPool pool = null;
		Connection connection = null;

		// retrieve dataset row from the database
		PreparedStatement statement = null;

		List<Map<String, String> > all_tags = new ArrayList<Map<String, String> >();

		try {
			pool = Commons.getConnectionPool();
			connection = pool.aquireConnection();

			statement = connection.prepareStatement("SELECT * FROM massive.spectrum_tags WHERE spectrum_id=? AND status=?");
			statement.setInt(1, SpectrumID_fromString(spectrumID));
			statement.setString(2, "ENABLED");

			logger.info(statement.toString());

			ResultSet result = null;
			result = statement.executeQuery();

			while(result.next()){
				String tag_type = result.getString("type");
				String tag_task_id = result.getString("task_id");
				String tag_desc = result.getString("desc");
				String tag_database = result.getString("database");
				String tag_database_url = result.getString("database_url");

				Map<String, String> tag_map = new HashMap<String, String>();
				tag_map.put("tag_type", tag_type);
				tag_map.put("tag_task_id", tag_task_id);
				tag_map.put("tag_desc", tag_desc);
				tag_map.put("tag_database", tag_database);
				tag_map.put("tag_database_url", tag_database_url);

				all_tags.add(tag_map);
			}
		} catch (Exception error) {
			throw new RuntimeException("Error disabling tag", error);
		} finally {
			pool.close(statement, connection);
		}

		return all_tags;
	}


	/**
	 * Updating the peaks in a spectrum
	 * @param SpectrumID
	 * @param new_library
	 * @return
	 */
	public static int UpdateSpectrumSplash(String SpectrumID, String block1, String block2, String block3){
		ConnectionPool pool = null;
		Connection connection = null;

		// retrieve dataset row from the database
		PreparedStatement statement = null;

		try {
			pool = Commons.getConnectionPool();
			connection = pool.aquireConnection();

			statement = connection.prepareStatement(
	        "UPDATE massive.spectra SET splash_block1=?, splash_block2=?, splash_block3=? WHERE spectrum_id=? ");
		    statement.setString(1, block1);
			statement.setString(2, block2);
			statement.setString(3, block3);
		    statement.setInt(4, SpectrumID_fromString(SpectrumID));

		    logger.info(statement.toString());
			int insertion = statement.executeUpdate();
			logger.info("Update Spectrum Peaks " + insertion);

		} catch (Exception error) {
			throw new RuntimeException("Error querying spectrum", error);
		} finally {
			pool.close(statement, connection);
		}

		return 0;
	}



	public static List<SpectrumInfo> QuerySpectrumSplash(String block1, String block2, String block3){
		logger.info("QUERYING SPLASH: " + block1 + "-" + block2 + "-" + block3 );

		List<SpectrumInfo> specs = new ArrayList<SpectrumInfo>();
		ConnectionPool pool = null;
		Connection connection = null;

		// retrieve dataset row from the database
		PreparedStatement statement = null;
		ResultSet result = null;

		SpectrumInfo spec_info_output = null;

		try {
			pool = Commons.getConnectionPool();
			connection = pool.aquireConnection();

			statement = connection.prepareStatement(
	        	"SELECT * FROM massive.spectra WHERE splash_block1=? AND splash_block2=? AND splash_block3=? AND status=?");

			statement.setString(1, block1);
			statement.setString(2, block2);
			statement.setString(3, block3);
			statement.setInt(4, 1);

			result = statement.executeQuery();

			while(result.next()){
				String SOURCE_FILE_PATH = result.getString("source_file");
		        int SCAN_NUMBER = Integer.parseInt(result.getString("scan"));
		        int spectrum_id = Integer.parseInt(result.getString("spectrum_id"));
		        String TASK_ID = result.getString("task_id");
		        int MSLEVEL = Integer.parseInt(result.getString("ms_level"));
		        String library_name_out = result.getString("library_name");
		        int spectrum_status = 0;
		        if(result.getString("status") != null)
		        	spectrum_status = Integer.parseInt(result.getString("status"));


		        spec_info_output = new SpectrumInfo(SpectrumID_toString(spectrum_id), SOURCE_FILE_PATH, SCAN_NUMBER, TASK_ID, MSLEVEL);
		        spec_info_output.setLibrary_membership(library_name_out);
				spec_info_output.setSplash(block1, block2, block3);

		        spec_info_output.setSpectrum_status(spectrum_status);

		        specs.add(spec_info_output);
			}
		} catch (Exception error) {
			throw new RuntimeException("Error querying for spectra", error);
		} finally {
			pool.close(statement, connection);
		}

		return specs;
	}

	/**
	 * Returns whether the specific user is able to administer this library spectrum
	 * @param user
	 * @param spectrumID
	 * @return
	 */
	public static boolean IsUserOwnerOfLibrarySpectrum(String user, String spectrumID){
		boolean isAdmin = AccountManager.getInstance().checkRole(user, "administrator");

		if(isAdmin){
			return true;
		}



		List<String> temp_specid_list = new ArrayList<String>();
		temp_specid_list.add(spectrumID);

		Map<String, SpectrumAnnotation> annotations = Get_Oldest_Spectrum_Annotations_Batch(temp_specid_list);
		SpectrumAnnotation oldest_annotation = annotations.get(spectrumID);
		if(TaskManager.queryTask(oldest_annotation.getTask_id()).getUser().equals(user)){
			return false;
		}

		return false;
	}


	/**
	 *
	 * @param user
	 * @param library_quality
	 * @return
	 */
	public static boolean canUserAddToLibrary(String user, int library_quality){
		if(library_quality == 3){
			return true;
		}

		if(library_quality < 1 || (library_quality > 3 && library_quality != 10)){
			return false;
		}

		Map<String, String> user_workflows = DownloadWorkflowInterface.getInstalledWorkflows(user);

		String add_workflow_name = "ADD-SINGLE-ANNOTATED-";

		if(library_quality == 2){
			add_workflow_name += "SILVER";
		}
		if(library_quality == 1){
			add_workflow_name += "BRONZE";
		}

		if(user_workflows.containsKey(add_workflow_name)){
			return true;
		}

		return false;
	}

	public static String SpectrumID_toString(int spectrum_id){
		return String.format("CCMSLIB%011d", spectrum_id);
	}

	public static int SpectrumID_fromString(String spectrum_id){
		//System.out.println(spectrum_id.replace("CCMSLIB", ""));
		return Integer.parseInt(spectrum_id.replace("CCMSLIB", ""));
	}

	public static String massive_toString(int spectrum_id){
		return String.format("MSV%09d", spectrum_id);
	}

	public static int massive_fromString(String spectrum_id){
		//System.out.println(spectrum_id.replace("CCMSLIB", ""));
		return Integer.parseInt(spectrum_id.replace("MSV", ""));
	}

	public static SpectrumAnnotation Result_To_Annotation(ResultSet result) throws SQLException{
		SpectrumAnnotation annot = new SpectrumAnnotation();


		annot.setSpectrumID(SpectrumID_toString(result.getInt("spectrum_id")));
		annot.setCompound_Name(result.getString("compound_name"));
		annot.setIon_Mode(result.getString("ion_mode"));
		annot.setCompound_Source(result.getString("compound_source"));
		annot.setInstrument(result.getString("instrument"));
		annot.setIon_Source(result.getString("ion_source"));
		annot.setAdduct(result.getString("adduct"));
		annot.setPrecursor_MZ(Float.parseFloat(result.getString("precursor_mz")));
		annot.setCharge(Integer.parseInt(result.getString("charge")));
		annot.setExactMass(Float.parseFloat(result.getString("exact_mass")));
		annot.setPI(result.getString("PI"));
		annot.setData_Collector(result.getString("data_collector"));
		annot.setLibrary_Class(Integer.parseInt(result.getString("library_quality")));
		annot.setCreate_time(result.getTimestamp("create_time"));
		annot.setTask_id((result.getString("task_id")));


		if(result.getString("CAS_number") == null)
			annot.setCAS_Number(" ");
		else
			annot.setCAS_Number(result.getString("CAS_number"));

		if(result.getString("PubMed_id") == null)
			annot.setPubmed_ID(" ");
		else
			annot.setPubmed_ID(result.getString("PubMed_id"));

		if(result.getString("smiles") == null)
			annot.setSmiles(" ");
		else
			annot.setSmiles(result.getString("smiles"));

		if(result.getString("inchi") == null)
			annot.setINCHI(" ");
		else
			annot.setINCHI(result.getString("inchi"));

		if(result.getString("inchi_aux") == null)
			annot.setINCHI_AUX(" ");
		else
			annot.setINCHI_AUX(result.getString("inchi_aux"));



		return annot;

	}

	public static String preparePlaceHolders(int length) {
		if(length == 0)
			return "";

	    StringBuilder builder = new StringBuilder(length * 2 - 1);
	    for (int i = 0; i < length; i++) {
	        if (i > 0) builder.append(',');
	        builder.append('?');
	    }
	    return builder.toString();
	}

	public static void setValues(PreparedStatement preparedStatement, List<Integer> ids) throws SQLException {
		if(ids == null){
			System.out.println("Cannot Set because input is null");
			return;
		}

	    for (int i = 0; i < ids.size(); i++) {
	        preparedStatement.setObject(i + 1, ids.get(i));
	    }
	}
}
