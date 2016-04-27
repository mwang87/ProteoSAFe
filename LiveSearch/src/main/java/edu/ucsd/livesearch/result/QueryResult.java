package edu.ucsd.livesearch.result;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FilenameUtils;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsd.livesearch.servlet.BaseServlet;
import edu.ucsd.livesearch.task.NullTask;
import edu.ucsd.livesearch.task.Task;
import edu.ucsd.livesearch.task.TaskManager;
import edu.ucsd.livesearch.task.TaskManager.TaskStatus;
import edu.ucsd.saint.commons.http.HttpParameters;

@SuppressWarnings("serial")
public class QueryResult
extends BaseServlet
{
	/*========================================================================
	 * Constants
	 *========================================================================*/
	private static final Logger logger =
		LoggerFactory.getLogger(QueryResult.class);
	public static final String RESULT_DATABASE_TASK_SUBDIRECTORY = "sqlite";
	public static final String DEFAULT_RESULT_QUERY = "SELECT * FROM Result";
	public static final int MAXIMUM_RESULT_QUERY_PAGE_SIZE = 5000;
	
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	/**
	 * <p>Accepts and processes requests to query the contents of a parsed
	 * task result SQLite database file.
	 * 
	 * <p>By convention, a GET request to this servlet is assumed to be a
	 * request to read data only.  No creation, update, or deletion of
	 * server resources is handled by this method.
	 * 
	 * <p>This method implements the <code>dojox.data.FileStore</code>
	 * protocol defined at
	 * <code>http://docs.dojocampus.org/dojox/data/FileStore/protocol</code>.
	 * 
	 * @param request	an {@link HttpServletRequest} object that contains
	 * 					the request the client has made of the servlet
	 * 
	 * @param response	an {@link HttpServletResponse} object that contains
	 * 					the response the servlet sends to the client
	 * 
	 * @throws IOException		if an input or output error is detected
	 * 							when the servlet handles the GET request
	 * 
	 * @throws ServletException	if the request for the GET could not be
	 * 							handled
	 */
	@Override
	protected void doGet(
		HttpServletRequest request, HttpServletResponse response
	) throws ServletException, IOException {
		// initialize properties
		try {
			initialize(request, false);
		} catch (ServletException error) {
			reportError(HttpServletResponse.SC_BAD_REQUEST,
				"Error initializing servlet properties from request",
				response, error);
			return;
		} catch (Throwable error) {
			reportError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
				"Error initializing servlet properties from request",
				response, error);
			return;
		}
		HttpParameters parameters = getParameters();
		PrintWriter out = response.getWriter();
		
		// retrieve and verify task
		String taskID = parameters.getParameter("task");
		if (taskID == null || taskID.trim().equals("")) {
			reportError(HttpServletResponse.SC_BAD_REQUEST,
				"You must provide the ID of a valid task to query its results.",
				response);
			return;
		}
		Task task = TaskManager.queryTask(taskID);
		if (task == null || task instanceof NullTask ||
			task.getStatus().equals(TaskStatus.NONEXIST)) {
			reportError(HttpServletResponse.SC_BAD_REQUEST,
				String.format("Could not find task with ID [%s].", taskID),
				response);
			return;
		}
		
		// retrieve and verify task filename
		String filename = parameters.getParameter("file");
		if (filename == null || filename.trim().equals("")) {
			reportError(HttpServletResponse.SC_BAD_REQUEST,
				"You must provide the name of a valid task " +
				"result file to query.", response);
			return;
		}
		
		// retrieve and verify database file corresponding to request filename
		File database = getDatabaseFile(task, filename);
		if (database == null) {
			reportError(HttpServletResponse.SC_BAD_REQUEST,
				String.format("Could not find database file corresponding " +
					"to result file [%s].", filename), response);
			return;
		}
		
		// retrieve and verify requested result page size
		int pageSize = 30;
		String newPageSize = parameters.getParameter("pageSize");
		if (newPageSize != null) try {
			pageSize = Integer.parseInt(newPageSize);
		} catch (NumberFormatException error) {}
		
		// enforce page size limits
		if (pageSize < 0)
			pageSize = 0;
		else if (pageSize > MAXIMUM_RESULT_QUERY_PAGE_SIZE) {
			reportError(HttpServletResponse.SC_BAD_REQUEST,
					String.format("Requested page size (%d) exceeds the " +
						"maximum allowed page size (%d).", pageSize,
						MAXIMUM_RESULT_QUERY_PAGE_SIZE), response);
			return;
		}
		
		// retrieve and verify requested page offset
		int offset = 0;
		String newOffset = parameters.getParameter("offset");
		if (newOffset != null) try {
			offset = Integer.parseInt(newOffset);
		} catch (NumberFormatException error) {}
		
		// retrieve and process request query into actual SQL to be executed
		String query = parameters.getParameter("query");
		String sql = getSQLFromRequestQuery(query, pageSize, offset);
		if (sql == null) {
			reportError(HttpServletResponse.SC_BAD_REQUEST,
				String.format("Could not formulate valid SQL result " +
					"query from request query string [%s].",
					query != null ? query : "null"), response);
			return;
		}
		
		// execute generated SQL query on the specified result database file
		String json = queryDatabase(database, sql);
		
		// write JSON result to servlet output stream
		response.setContentType("application/json");
		response.setContentLength(json.length());
		out.print(json);
	}
	
	public static String queryDatabase(File database, String sql) {
		if (database == null || sql == null)
			return null;
		StringBuffer rows = new StringBuffer();
		StringBuffer metadata = new StringBuffer("{\"file\":\"");
		metadata.append(database.getName()).append("\",");
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
				rows.append("{");
				// iterate over all columns of this row
				for (int i=1; i<=columns.getColumnCount(); i++) {
					// write column name into JSON object
					String column = columns.getColumnName(i);
					if (column == null)
						rows.append(i);
					else rows.append("\"").append(
						JSONObject.escape(column)).append("\"");
					rows.append(":");
					// write column value into JSON object
					String value = result.getString(column);
					if (value == null)
						rows.append("\"null\"");
					else rows.append("\"").append(
						JSONObject.escape(value)).append("\"");
					rows.append(",");
				}
				// add "id" field and close this row
				rows.append("\"id\":\"");
				rows.append(counter++);
				rows.append("\"},\n");
			}
			// chomp trailing comma and newline
			if (rows.length() > 0 &&
				rows.substring(rows.length() - 2).equals(",\n"));
				rows.setLength(rows.length() - 2);
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
		// enclose rows into array
		rows.insert(0, "[").append("\n]");
		// ensure that a valid count was returned; if not, then
		// we'll need to just use the size of the returned page
		if (count < 0)
			count = counter;
		metadata.append("\"total_rows\":\"").append(count).append("\"");
		// return metadata object and rows array
		// packaged together into a greater hash
		return String.format("%s,\n\"row_data\":%s}",
			metadata.toString(), rows.toString());
	}
	
	/*========================================================================
	 * Convenience methods
	 *========================================================================*/
	private static File getDatabaseFile(Task task, String resultFilename) {
		if (task == null || resultFilename == null)
			return null;
		// determine database filename
		String dbFilename =
			String.format("%s.db", FilenameUtils.getBaseName(resultFilename));
		// retrieve and verify task database directory
		File dbDirectory = task.getPath(RESULT_DATABASE_TASK_SUBDIRECTORY);
		if (dbDirectory.canRead() == false ||
			dbDirectory.isDirectory() == false)
			return null;
		// retrieve and verify database file
		File dbFile = new File(dbDirectory, dbFilename);
		if (dbFile.canRead() == false)
			return null;
		else return dbFile;
	}
	
	private static String getSQLFromRequestQuery(
		String query, int pageSize, int offset
	) {
		String sql = DEFAULT_RESULT_QUERY;
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
						String column = escapeColumnName(
							operation.substring(0, lastUnderscore));
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
					filters.append(
						escapeColumnName(key.substring(0, key.length() - 6)));
					filters.append(" LIKE '%").append(value).append("%' AND ");
				}
				// handle numerical filters
				else if (key.endsWith("_lowerinput")) {
					filters.append(
						escapeColumnName(key.substring(0, key.length() - 11)));
					filters.append(" >= ").append(value).append(" AND ");
				} else if (key.endsWith("_upperinput")) {
					filters.append(
						escapeColumnName(key.substring(0, key.length() - 11)));
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
	
	private static String escapeColumnName(String name) {
		if (name == null)
			return null;
		else return String.format("\"%s\"", name);
	}
}
