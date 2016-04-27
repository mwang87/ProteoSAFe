package edu.ucsd.livesearch.servlet;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.HashMap; 
import java.util.Map; 

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


import edu.ucsd.livesearch.util.FileIOUtils;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.JSONArray;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import edu.ucsd.livesearch.result.QueryResult;
import edu.ucsd.livesearch.task.Task;
import edu.ucsd.livesearch.task.TaskManager;

import edu.ucsd.saint.commons.WebAppProps;

public class ResultComparisonServlet extends BaseServlet{

	private static final Logger logger = LoggerFactory.getLogger(ResultComparisonServlet.class);
	private final String result_comparison_root_dir =  WebAppProps.getPath("livesearch.user.path", "") + "/../result_comparisons/";
	public static final String DEFAULT_RESULT_QUERY = "SELECT * FROM Result";
	
	//Posting Rating of Match
	@Override
	protected void doPost(
		HttpServletRequest request, HttpServletResponse response
	) throws ServletException, IOException {
		//No op now
		return;
		/*
		String username = (String)request.getSession().getAttribute("livesearch.user");
		String parameters = request.getParameter("compareparameters");
		JSONObject json = null;
		try {
			json = (JSONObject)JSONValue.parse(parameters);
		} catch (Throwable error) {
			response.getOutputStream().print("{\"status\":\"error\"}");
		}
		
		if(json != null){
			JSONArray left_compare = (JSONArray)JSONValue.parse(json.get("left_compare").toString());
			JSONArray right_compare = (JSONArray)JSONValue.parse(json.get("right_compare").toString());
			
			
			
			logger.info(result_comparison_root_dir);
			
			//Update to be random
			String result_session_id = java.util.UUID.randomUUID().toString();
			
			//Making sure directory exists
			String result_session_directory = result_comparison_root_dir + result_session_id;
			try{
				FileIOUtils.validateDirectory(new File(result_session_directory));
			}
			catch(Exception error){
				String mkdir_cmd = "mkdir " + result_session_directory;
				logger.info(mkdir_cmd);
				Process p = Runtime.getRuntime().exec(mkdir_cmd);
				try {
					p.waitFor();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					return;
				}
			}
			
			String output_script_file = result_session_directory + "/" + "execute.sh";
			//Writer for outputting the execution script
			PrintWriter writer = new PrintWriter(output_script_file, "UTF-8");
			
			
			//Creating remove_columns.sqlite
			String output_sqlite_column_removal_file = result_session_directory + "/" + "remove_columns.sqlite";
			PrintWriter output_sqlite_column_removal_writer = new PrintWriter(output_sqlite_column_removal_file, "UTF-8");
			
			output_sqlite_column_removal_writer.println("BEGIN TRANSACTION;"
		    + "CREATE TEMPORARY TABLE t1_backup(sequence, accession, modifications, nativeID, [#SpecFile], modified_sequence, nativeID_index, nativeID_scan, internalFilename);"
		    + "INSERT INTO t1_backup SELECT sequence, accession, modifications, nativeID, [#SpecFile], modified_sequence, nativeID_index, nativeID_scan, internalFilename FROM Result;"
		    + "DROP TABLE Result;"
		    + "CREATE TABLE Result(sequence, accession, modifications, nativeID, [#SpecFile], modified_sequence, nativeID_index, nativeID_scan, internalFilename);"
		    + "INSERT INTO Result SELECT sequence, accession, modifications, nativeID, [#SpecFile], modified_sequence, nativeID_index, nativeID_scan, internalFilename FROM t1_backup;"
		    + "DROP TABLE t1_backup;"
		    + "COMMIT;");
			
			output_sqlite_column_removal_writer.close();
			
			String merged_database = result_session_directory + "/" + result_session_id + "-merged.db";
			
			//Writing the Left Side
			for(int i=0; i<left_compare.size(); i++){
				JSONObject object = (JSONObject)left_compare.get(i);
				String task_id = (String)object.get("task");
				String tab_name = (String)object.get("tab_name");
				
				//Trying to do some funny business, so fail out
				if(tab_name.length() != 38){
					response.getOutputStream().print("{\"status\":\"error\"}");
					return;
				}
				
				Task task = TaskManager.queryTask(task_id);
				String user = task.getUser();
				
				String remote_sql_file = WebAppProps.getPath("livesearch.user.path", "") + "/" + user + "/" + task.getID() + "/" + "sqlite";
				remote_sql_file += "/" + "group_by_spectrum-main_" + tab_name.substring(0, tab_name.lastIndexOf(".")) + ".db";

				logger.info(remote_sql_file);
				FileIOUtils.validateFile(new File(remote_sql_file));
				
				String target_sql_file = result_session_directory + "/" + "group_by_spectrum-main_" + tab_name.substring(0, tab_name.lastIndexOf(".")) + ".db";
				
				//Copying database files closer
				String cp_cmd = "cp " + remote_sql_file + " " + target_sql_file;
				writer.println(cp_cmd);
				logger.info(cp_cmd);
				
				//Removing the extraneous columns
				String remove_column_command = "cat " + output_sqlite_column_removal_file + " | sqlite3 " + target_sql_file;
				writer.println(remove_column_command);
				
				//Adding ComparePartition
				writer.println("echo \"alter table Result add column ComparePartition int;\" | sqlite3 "  + target_sql_file);
				//Writing number in partition
				writer.println("echo \"update Result SET ComparePartition=1;\" | sqlite3 "  + target_sql_file);
				
				//Adding Task Identifier
				writer.println("echo \"alter table Result add column taskid TEXT;\" | sqlite3 "  + target_sql_file);
				//Writing Task 
				writer.println("echo \"update Result SET taskid=\\\"" + task_id + "\\\";\" | sqlite3 "  + target_sql_file);
				
				//Creating the db dump
				String target_dump_file = target_sql_file + ".dump";
				writer.println("sqlite3 " + target_sql_file + " .dump > " + target_dump_file);
				
				//Merging it 
				writer.println("cat " + target_dump_file + " | sqlite3 " + merged_database );
			}
			
			for(int i=0; i<right_compare.size(); i++){
				JSONObject object = (JSONObject)right_compare.get(i);
				String task_id = (String)object.get("task");
				String tab_name = (String)object.get("tab_name");
				
				//Trying to do some funny business, so fail out
				if(tab_name.length() != 38){
					response.getOutputStream().print("{\"status\":\"error\"}");
					return;
				}
				
				Task task = TaskManager.queryTask(task_id);
				String user = task.getUser();
				
				String remote_sql_file = WebAppProps.getPath("livesearch.user.path", "") + "/" + user + "/" + task.getID() + "/" + "sqlite";
				remote_sql_file += "/" + "group_by_spectrum-main_" + tab_name.substring(0, tab_name.lastIndexOf(".")) + ".db";

				logger.info(remote_sql_file);
				FileIOUtils.validateFile(new File(remote_sql_file));
				
				String target_sql_file = result_session_directory + "/" + "group_by_spectrum-main_" + tab_name.substring(0, tab_name.lastIndexOf(".")) + ".db";
				
				//Copying database files closer
				String cp_cmd = "cp " + remote_sql_file + " " + target_sql_file;
				writer.println(cp_cmd);
				logger.info(cp_cmd);
				
				//Removing the extraneous columns
				String remove_column_command = "cat " + output_sqlite_column_removal_file + " | sqlite3 " + target_sql_file;
				writer.println(remove_column_command);
				
				//Adding ComparePartition
				writer.println("echo \"alter table Result add column ComparePartition int;\" | sqlite3 "  + target_sql_file);
				//Writing number in partition
				writer.println("echo \"update Result SET ComparePartition=2;\" | sqlite3 "  + target_sql_file);
				
				//Adding Task Identifier
				writer.println("echo \"alter table Result add column taskid TEXT;\" | sqlite3 "  + target_sql_file);
				//Writing Task 
				writer.println("echo \"update Result SET taskid=\\\"" + task_id + "\\\";\" | sqlite3 "  + target_sql_file);
				
				//Creating the db dump
				String target_dump_file = target_sql_file + ".dump";
				writer.println("sqlite3 " + target_sql_file + " .dump > " + target_dump_file);
				
				//Merging it 
				writer.println("cat " + target_dump_file + " | sqlite3 " + merged_database );
			}
			
			//Making the merged database more efficient with some preprocessing
			String precompute_mismatch_query = "echo \"CREATE TABLE mismatched_rows AS SELECT * FROM Result as first JOIN Result as second ON (first.nativeID = second.nativeID) AND (first.[#SpecFile] = second.[#SpecFile]) AND (first.ComparePartition <> second.ComparePartition) AND (first.modified_sequence <> second.modified_sequence) WHERE first.ComparePartition=1;\" | sqlite3 " + merged_database;
			String precompute_match_query = "echo \"CREATE TABLE matched_rows AS SELECT * FROM Result as first JOIN Result as second ON (first.nativeID = second.nativeID) AND (first.[#SpecFile] = second.[#SpecFile]) AND (first.ComparePartition <> second.ComparePartition) AND (first.modified_sequence = second.modified_sequence) WHERE first.ComparePartition=1;\" | sqlite3 " + merged_database;
			String precompute_first_unique_query = "echo \"CREATE TABLE unique_first AS SELECT * FROM Result as first LEFT JOIN Result as second ON (first.nativeID = second.nativeID) AND (first.[#SpecFile] = second.[#SpecFile]) AND (first.ComparePartition <> second.ComparePartition) WHERE first.ComparePartition=1 AND second.[#SpecFile] is NULL;\" | sqlite3 " + merged_database;
			String precompute_second_unique_query = "echo \"CREATE TABLE unique_second AS SELECT * FROM Result as first LEFT JOIN Result as second ON (first.nativeID = second.nativeID) AND (first.[#SpecFile] = second.[#SpecFile]) AND (first.ComparePartition <> second.ComparePartition) WHERE first.ComparePartition=2 AND second.[#SpecFile] is NULL;\" | sqlite3 " + merged_database;
			
			writer.println(precompute_mismatch_query);
			writer.println(precompute_match_query);
			writer.println(precompute_first_unique_query);
			writer.println(precompute_second_unique_query);
			
			//Closing writer;
			writer.close();
			
			//Execute the code
			String execute_merge_command = "sh " + output_script_file;
			logger.info(execute_merge_command);
			Process p = Runtime.getRuntime().exec(execute_merge_command);
			try {
				p.waitFor();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return;
			}
			
			response.setContentType("application/json");
			response.getOutputStream().print("{\"status\":\"success\", \"sessionid\":\"" + result_session_id +  "\"}");
			
		}
		else{
			logger.info("NULL PARAMTERS");
		}
		*/
	}
	
	
	//Doing a query on the sqlite database
	@Override
	protected void doGet(
			HttpServletRequest request, HttpServletResponse response
		) throws ServletException, IOException {
		
		//Doing the query in this
		String task_id = request.getParameter("task");
		String compare_type = request.getParameter("compare_type");
		
		//String result_session_directory = result_comparison_root_dir + compare_session;
		//String merged_database = result_session_directory + "/" + compare_session + "-merged.db";
		Task task = TaskManager.queryTask(task_id);
		String user = task.getUser();
		String merged_database = WebAppProps.getPath("livesearch.user.path", "") + "/" + user + "/" + task.getID() + "/" + "sqlite";
		
		String query = request.getParameter("query");
		int pageSize = Integer.parseInt(request.getParameter("pageSize"));
		int offset = Integer.parseInt(request.getParameter("offset"));
		
		//String SQL_query = getSQLFromRequestQuery(query, pageSize, offset);
		String SQL_query = getSQLFromRequestQueryClassic(query, pageSize, offset);
		
				
		//Spectrum Level
		if(compare_type.compareTo("spectrum_match") == 0){
			SQL_query = "SELECT * FROM matched_rows " + SQL_query;
			merged_database += "/spectrum_level.db";
		}
		if(compare_type.compareTo("spectrum_unique_first") == 0){
			SQL_query = "SELECT * FROM unique_first " + SQL_query;
			merged_database += "/spectrum_level.db";
		}
		if(compare_type.compareTo("spectrum_unique_second") == 0){
			SQL_query = "SELECT * FROM unique_second " + SQL_query;
			merged_database += "/spectrum_level.db";
		}
		if(compare_type.compareTo("spectrum_mismatch") == 0){
			SQL_query = "SELECT * FROM mismatched_rows " + SQL_query;
			merged_database += "/spectrum_level.db";
		}
		if(compare_type.compareTo("spectrum_all") == 0){
			SQL_query = "SELECT * FROM Result " + SQL_query;
			merged_database += "/spectrum_level.db";
		}
		
		//Peptide Level
		if(compare_type.compareTo("peptide_match") == 0){
			SQL_query = "SELECT * FROM matched_rows " + SQL_query;
			merged_database += "/peptide_level.db";
		}
		if(compare_type.compareTo("peptide_unique_first") == 0){
			SQL_query = "SELECT * FROM unique_first " + SQL_query;
			merged_database += "/peptide_level.db";
		}
		if(compare_type.compareTo("peptide_unique_second") == 0){
			SQL_query = "SELECT * FROM unique_second " + SQL_query;
			merged_database += "/peptide_level.db";
		}
		if(compare_type.compareTo("peptide_all") == 0){
			SQL_query = "SELECT * FROM Result " + SQL_query;
			merged_database += "/peptide_level.db";
		}
		
		//Protein Level
		if(compare_type.compareTo("protein_match") == 0){
			SQL_query = "SELECT * FROM matched_rows " + SQL_query;
			merged_database += "/protein_level.db";
		}
		if(compare_type.compareTo("protein_unique_first") == 0){
			SQL_query = "SELECT * FROM unique_first " + SQL_query;
			merged_database += "/protein_level.db";
		}
		if(compare_type.compareTo("protein_unique_second") == 0){
			SQL_query = "SELECT * FROM unique_second " + SQL_query;
			merged_database += "/protein_level.db";
		}
		if(compare_type.compareTo("protein_all") == 0){
			SQL_query = "SELECT * FROM Result " + SQL_query;
			merged_database += "/protein_level.db";
		}
		
		logger.info(SQL_query);
		
		//String result_string = queryDatabase(new File(merged_database), SQL_query);
		String result_string = QueryResult.queryDatabase(new File(merged_database), SQL_query);
		response.setContentType("application/json");
		response.getOutputStream().println(result_string);
	}
	
	
	
	/*========================================================================
	 * Convenience methods
	 *========================================================================*/
	private static String getSQLFromRequestQuery(
		String query, int pageSize, int offset
	) {
		String sql = "";
		// determine the pagination suffix
		String suffix = null;
		// if the requested page size is 0 or less, then the user
		// is requesting an un-paginated, i.e. full result set
		if (pageSize < 1)
			suffix = "";
		else suffix = String.format(" LIMIT %d OFFSET %d", pageSize, offset);
		// parse the query and convert it to valid SQL
		if (query != null && query.trim().equals("") == false) {
			// first try to decode query from URL parameter
			try { query = URLDecoder.decode(query, "UTF-8"); }
			catch (Throwable error) {}
			// strip off leading hash mark, if present
			if (query.startsWith("#"))
				query = query.substring(1);
			// parse query as JSON
			JSONObject json = null;
			try {
				json = (JSONObject)JSONValue.parse(query);
				if (json == null)
					throw new NullPointerException(
						String.format("JSONValue.parse(\"%s\") returned null.",
						query));
			} catch (Throwable error) {
				logger.error(String.format(
					"Could not parse request query [%s] as JSON.", query),
					error);
				return String.format("%s%s", sql, suffix);
			}
			// iterate over all the query elements,
			// and write them into the SQL query
			StringBuffer filters = new StringBuffer();
			StringBuffer sorts = new StringBuffer();
			for (Object member : json.keySet()) {
				String key = member.toString();
				String value = json.get(key).toString();
				// handle sorts
				if (key.equals("table_sort_history")) {
					// "table_sort_history" should be a semicolon-delimited
					// list of sort operations, recorded in the order in
					// which they were applied; therefore, they should be
					// written into the SQL string in the reverse of the order
					// found in the list, to apply more recent sorts first
					String[] operations = value.split(";");
					if (operations == null || operations.length < 1)
						continue;
					else for (String operation : operations) {
						// each sort operation string should have the
						// following format: <field>_<direction>
						int lastUnderscore = operation.lastIndexOf('_');
						if (lastUnderscore < 0) {
							// TODO: throw error
							continue;
						}
						String column = operation.substring(0, lastUnderscore);
						String operator =
							operation.substring(lastUnderscore + 1);
						String direction = null;
						if ("asc".equalsIgnoreCase(operator))
							direction = "ASC";
						else if ("dsc".equalsIgnoreCase(operator))
							direction = "DESC";
						else {
							// TODO: throw error
							continue;
						}
						sorts.insert(0,
							String.format("%s %s, ", column, direction));
					}
					// chomp trailing comma and space
					if (sorts.toString().endsWith(", "))
						sorts.setLength(sorts.length() - 2);
					// prepend "ORDER BY" SQL statement, if any sorts were found
					if (sorts.length() > 0)
						sorts.insert(0, "ORDER BY ");
				}
				// handle text filters
				else if (key.endsWith("_input")) {
					filters.append(key.substring(0, key.length() - 6));
					filters.append(" LIKE '%").append(value).append("%' AND ");
				}
				// handle numerical filters
				else if (key.endsWith("_lowerinput")) {
					filters.append(key.substring(0, key.length() - 11));
					filters.append(" >= ").append(value).append(" AND ");
				} else if (key.endsWith("_upperinput")) {
					filters.append(key.substring(0, key.length() - 11));
					filters.append(" <= ").append(value).append(" AND ");
				}
			}
			// chomp trailing " AND " from filters
			if (filters.toString().endsWith(" AND "))
				filters.setLength(filters.length() - 5);
			// prepend "WHERE" SQL statement to filters, if any  were found
			if (filters.length() > 0){
				//filters.insert(0, "WHERE ");
				filters.insert(0, "AND ");
			}
			// write filters into the SQL query, if any were found
			if (filters.length() > 0)
				sql = String.format("%s %s", sql, filters.toString());
			// write sorts into the SQL query, if any were found
			if (sorts.length() > 0)
				sql = String.format("%s %s", sql, sorts.toString());
		}
		return String.format("%s%s", sql, suffix);
	}
	
	private static String getSQLFromRequestQueryClassic(
			String query, int pageSize, int offset
		) {
			String sql = "";
			// determine the pagination suffix
			String suffix = null;
			// if the requested page size is 0 or less, then the user
			// is requesting an un-paginated, i.e. full result set
			if (pageSize < 1)
				suffix = "";
			else suffix = String.format(" LIMIT %d OFFSET %d", pageSize, offset);
			// parse the query and convert it to valid SQL
			if (query != null && query.trim().equals("") == false) {
				// first try to decode query from URL parameter
				try { query = URLDecoder.decode(query, "UTF-8"); }
				catch (Throwable error) {}
				// strip off leading hash mark, if present
				if (query.startsWith("#"))
					query = query.substring(1);
				// parse query as JSON
				JSONObject json = null;
				try {
					json = (JSONObject)JSONValue.parse(query);
					if (json == null)
						throw new NullPointerException(
							String.format("JSONValue.parse(\"%s\") returned null.",
							query));
				} catch (Throwable error) {
					logger.error(String.format(
						"Could not parse request query [%s] as JSON.", query),
						error);
					return String.format("%s%s", sql, suffix);
				}
				// iterate over all the query elements,
				// and write them into the SQL query
				StringBuffer filters = new StringBuffer();
				StringBuffer sorts = new StringBuffer();
				for (Object member : json.keySet()) {
					String key = member.toString();
					String value = json.get(key).toString();
					// handle sorts
					if (key.equals("table_sort_history")) {
						// "table_sort_history" should be a semicolon-delimited
						// list of sort operations, recorded in the order in
						// which they were applied; therefore, they should be
						// written into the SQL string in the reverse of the order
						// found in the list, to apply more recent sorts first
						String[] operations = value.split(";");
						if (operations == null || operations.length < 1)
							continue;
						else for (String operation : operations) {
							// each sort operation string should have the
							// following format: <field>_<direction>
							int lastUnderscore = operation.lastIndexOf('_');
							if (lastUnderscore < 0) {
								// TODO: throw error
								continue;
							}
							String column = operation.substring(0, lastUnderscore);
							column = "\"" + column + "\"";
							String operator =
								operation.substring(lastUnderscore + 1);
							String direction = null;
							if ("asc".equalsIgnoreCase(operator))
								direction = "ASC";
							else if ("dsc".equalsIgnoreCase(operator))
								direction = "DESC";
							else {
								// TODO: throw error
								continue;
							}
							sorts.insert(0,
								String.format("%s %s, ", column, direction));
						}
						// chomp trailing comma and space
						if (sorts.toString().endsWith(", "))
							sorts.setLength(sorts.length() - 2);
						// prepend "ORDER BY" SQL statement, if any sorts were found
						if (sorts.length() > 0)
							sorts.insert(0, "ORDER BY ");
					}
					// handle text filters
					else if (key.endsWith("_input")) {
						filters.append("\"" + key.substring(0, key.length() - 6) + "\"");
						//Do Stuff Here to look for periods at beginning and end
						String modified_value = value;
						
						if(modified_value.charAt(0) == '.'){
							modified_value = modified_value.substring(1);
						}
						else{
							modified_value = "%" + modified_value;
						}
						if(modified_value.charAt(modified_value.length() - 1) == '.'){
							modified_value = modified_value.substring(0, modified_value.length() - 1);
						}
						else{
							modified_value = modified_value + "%";
						}
						filters.append(" LIKE '").append(modified_value).append("' AND ");
					}
					// handle numerical filters
					else if (key.endsWith("_lowerinput")) {
						filters.append("\"" + key.substring(0, key.length() - 11) + "\"");
						filters.append(" >= ").append(value).append(" AND ");
					} else if (key.endsWith("_upperinput")) {
						filters.append("\"" + key.substring(0, key.length() - 11) + "\"");
						filters.append(" <= ").append(value).append(" AND ");
					}
				}
				// chomp trailing " AND " from filters
				if (filters.toString().endsWith(" AND "))
					filters.setLength(filters.length() - 5);
				// prepend "WHERE" SQL statement to filters, if any  were found
				if (filters.length() > 0)
					filters.insert(0, "WHERE ");
				// write filters into the SQL query, if any were found
				if (filters.length() > 0)
					sql = String.format("%s %s", sql, filters.toString());
				// write sorts into the SQL query, if any were found
				if (sorts.length() > 0)
					sql = String.format("%s %s", sql, sorts.toString());
			}
			return String.format("%s%s", sql, suffix);
		}
	
	
	
	public static String queryDatabase(File database, String sql) {
		if (database == null || sql == null)
			return null;
		StringBuffer rows = new StringBuffer();
		StringBuffer metadata = new StringBuffer("{\"table_metadata\":true,");
		metadata.append("\"file\":\"").append(database.getName()).append("\",");
		Connection connection = null;
		Statement statement = null;
		ResultSet result = null;
		// first, establish a connection to the database
		try {
			Class.forName("org.sqlite.JDBC");
			connection = DriverManager.getConnection(
				String.format("jdbc:sqlite:%s", database.getAbsolutePath()));
		} catch (Throwable error) {
			logger.error(String.format(
				"There was an error connecting to database file [%s].",
				database.getAbsolutePath()), error);
			try { connection.close(); } catch (Throwable innerError) {}
			return null;
		}
		// then, get the total row count for the un-paged version of this query
		int count = -1;
		try {
			String countQuery = getCountSQL(sql);
			statement = connection.createStatement();
			result = statement.executeQuery(countQuery);
			count = result.getInt(1);
		} catch (Throwable error) {
			logger.error(String.format(
				"There was an error determining the total un-paged " +
				"row count for query [%s] to database file [%s].",
				sql, database.getAbsolutePath()), error);
			try { connection.close(); } catch (Throwable innerError) {}
			return null;
		} finally {
			try { result.close(); } catch (Throwable error) {}
			try { statement.close(); } catch (Throwable innerError) {}
		}
		// finally, execute the query itself
		int counter = 0;
		try {
//			logger.info(String.format("Querying result file [%s]:\n%s",
//				database.getAbsolutePath(), sql));
			statement = connection.createStatement();
			result = statement.executeQuery(sql);
			ResultSetMetaData columns = result.getMetaData();
			while (result.next()) {
				rows.append("\n{");
				//Keep Track if we've seent his column before
				Map<String, Integer> column_counts = new HashMap<String, Integer>();
				
				// iterate over all columns of this row
				for (int i=1; i<=columns.getColumnCount(); i++) {
					// write column name into JSON object
					String column = columns.getColumnName(i);
					if (column == null)
						rows.append(i);
					else {
						column.replaceAll("\"", "_");
						if(column_counts.containsKey(column)){
							rows.append("\"").append("second." + column).append("\"");
						}
						else{
							rows.append("\"").append("first." + column).append("\"");
							column_counts.put(column, 1);
						}
					}
					rows.append(":");
					// write column value into JSON object
					
					//String value = result.getString(column);
					String value = result.getString(i);
					if (value == null)
						rows.append("\"null\"");
					else {
						value.replaceAll("\"", "_");
						rows.append("\"").append(value).append("\"");
					}
					rows.append(",");
				}
				// add "id" field and close this row
				rows.append("\"id\":\"");
				rows.append(counter++);
				rows.append("\"},");
			}
			// chomp trailing comma
			if (rows.length() > 0 && rows.charAt(rows.length() - 1) == ',')
				rows.setLength(rows.length() - 1);
		} catch (Throwable error) {
			logger.error(String.format("There was an error " +
				"executing query [%s] to database file [%s].",
				sql, database.getAbsolutePath()), error);
			return null;
		} finally {
			try { result.close(); } catch (Throwable error) {}
			try { statement.close(); } catch (Throwable error) {}
			try { connection.close(); } catch (Throwable error) {}
		}
		// ensure that a valid count was returned; if not, then
		// we'll need to just use the size of the returned page
		if (count < 0)
			count = counter;
		metadata.append("\"total_rows\":\"").append(count).append("\"");
		// close metadata object
		metadata.append("}");
		if (rows.length() > 0)
			metadata.append(",");
		// prepend metadata object to rows
		rows.insert(0, metadata.toString()).insert(0, "[\n");
		// close rows array and return it
		rows.append("\n]");
		return rows.toString();
	}
	
	private static String getCountSQL(String sql) {
		if (sql == null)
			return null;
		// change "*" to "COUNT(*)"
		sql = sql.replaceFirst("\\*", "COUNT(*)");
		// remove paging qualifiers at the end of the query
		int index = sql.lastIndexOf("LIMIT");
		if (index >= 0)
			sql = sql.substring(0, index);
		return sql.trim();
	}
}
