package edu.ucsd.livesearch.task;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.LinkedList;
import java.util.UUID;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsd.livesearch.account.AccountManager;
import edu.ucsd.livesearch.dataset.Dataset;
import edu.ucsd.livesearch.dataset.DatasetManager;
import edu.ucsd.livesearch.util.Commons;
import edu.ucsd.livesearch.util.FormatUtils;
import edu.ucsd.livesearch.util.VersionTuple;
import edu.ucsd.livesearch.util.WorkflowParameterUtils;
import edu.ucsd.saint.commons.ConnectionPool;

public class TaskManager {

	private static final Logger logger = LoggerFactory.getLogger(TaskManager.class);

	public static enum TaskStatus{
		UPLOADING, QUEUED, LAUNCHING, FAILED, RUNNING, DONE, CRASHED, SUSPENDED, NONEXIST, DELETED
	}

	public static Task createTask(String user, String site){
		Connection conn = null;
		PreparedStatement stmt = null;
		ConnectionPool pool = null;
		try{
			String taskID = UUID.randomUUID().toString().replaceAll("-", "");
			String version = Commons.getVersion().toString();
			pool = Commons.getConnectionPool();
			conn = pool.aquireConnection();
			stmt = conn.prepareStatement(
				"INSERT INTO tasks(task_id, user_id, create_time, status, site, version) " +
				"VALUES(?, ?, now(), 'LAUNCHING', ?, ?)");
			stmt.setString(1, taskID);
			stmt.setString(2, user);
			stmt.setString(3, site);
			stmt.setString(4, version);
			if(stmt.executeUpdate() == 1)
				return new Task(
					taskID, user, null, TaskStatus.LAUNCHING, site, version);
		}
		catch(SQLException e){
			logger.error(String.format(
			"Failed to create task for [%s:%s][code=%d, state=%s, message=%s]%n",
				user, site, e.getErrorCode(), e.getSQLState(), e.getLocalizedMessage()), e);
		}
		finally{
			pool.close(stmt, conn);
		}
		return null;
	}

	public static boolean deleteTask(Task task){
		// first, if the task is still running, suspendeded it
		boolean successful = WorkflowUtils.abortWorkflow(task);
		if (!successful)
			return false;
		// then delete the folder (but only if it's not a dataset)
		if (isDatasetTask(task) == false) {
			File folder = task.getPath("");
			if (folder.exists() && folder.isDirectory())
				successful = successful && FileUtils.deleteQuietly(folder);
			logger.info("Deleted task directory {}, {}",
				folder.getAbsolutePath(), successful);
			if (!successful) return false;
		}
		// finally mark the task's database record as "DELETED"
		Connection conn = null;
		PreparedStatement stmt = null;
		ConnectionPool pool = null;
		try {
			pool = Commons.getConnectionPool();
			conn = pool.aquireConnection();
			stmt = conn.prepareStatement(
				"UPDATE tasks SET status='DELETED' WHERE task_id = ?");
			stmt.setString(1, task.getID());
			successful = successful && (stmt.executeUpdate() == 1);
		} catch (SQLException error) {
			logger.error(String.format("Failed to delete task for " +
				"[%s][code=%d, state=%s, message=%s]%n", task.getID(),
				error.getErrorCode(), error.getSQLState(),
				error.getLocalizedMessage()), error);
		} finally {
			pool.close(stmt, conn);
		}
		return successful;
	}

	private static boolean setField(String taskID, String field, String value){
		return 0 < Commons.executeUpdate(
			"UPDATE tasks SET " + field + "=? WHERE task_id=?",
			value, taskID);
	}

	static boolean setFlowName(Task task, String type){
		return setField(task.getID(), "tool", type);
	}

	static boolean setFlowVersion(Task task, String version) {
		return setField(task.getID(), "version", version);
	}

	public static boolean setNotification(Task task, String email){
		return setField(task.getID(), "notification", email);
	}

	public static boolean setComment(Task task, String comment){
		return setField(task.getID(), "description", comment);
	}

	public static void setUploading(Task task){
		Commons.executeUpdate(
			"UPDATE tasks SET begin_time=now(), status='UPLOADING' WHERE task_id=?",
			task.getID());
	}

	public static void setRunning(Task task){
		Commons.executeUpdate(
			"UPDATE tasks SET begin_time=now(), status='RUNNING' WHERE task_id=?",
			task.getID());
	}

	static void setFailed(Task task, Collection<String> messages){
		StringBuffer buffer = new StringBuffer();

		if(messages != null)
			for(Object msg: messages)
				buffer.append(msg).append('\n');
		String m = buffer.toString();
		Commons.executeUpdate(
			"UPDATE tasks SET status='FAILED', messages=?, end_time=NOW() WHERE task_id=?",
			m.substring(0, Math.min(m.length(), 2000))
			, task.getID());
	}

	public static void setSuspended(Task task){
		Commons.executeUpdate(
			"UPDATE tasks SET status='SUSPENDED' WHERE task_id=?",
			task.getID());
	}

	public static void setDone(Task task){
		Commons.executeUpdate(
			"UPDATE tasks SET end_time=now(), status='DONE' WHERE task_id=?",
			task.getID());
	}

	public static Task queryTask(String taskID){
		Collection<Task> tasks = queryTasks(
			"WHERE task_id=?", taskID);
		return tasks.isEmpty() ?
			 new NullTask() : tasks.iterator().next();
	}

	private static Collection<Task> queryTasks(
		String whereClause, String ... args
	) {
		Connection connection = null;
		PreparedStatement statement = null;
		ResultSet results = null;
		Collection<Task> tasks = new LinkedList<Task>();
		ConnectionPool pool = null;
		try{
			pool = Commons.getConnectionPool();
			connection = pool.aquireConnection();
			statement = connection.prepareStatement(
				"SELECT task_id, user_id, create_time, begin_time, end_time, " +
				"now() AS now, status, messages, description, tool, " +
				"notification, site, version FROM tasks " + whereClause
			);
			int i = 1;
			for (String arg : args)
				statement.setString(i++, arg);
			results = statement.executeQuery();
			logger.debug("Query task with statement: [{}]", statement);
			// return all dataset tasks whose version is compatible
			// with the current system version
			while (results.next()) {
				VersionTuple version =
					new VersionTuple(results.getString("version"));
				logger.debug(
					"Current version [{}], target version [{}], compatible: " +
					Commons.getVersion().compatibleWith(version),
					Commons.getVersion(), version);
				if (Commons.getVersion().compatibleWith(version))
					tasks.add(populateTask(results));
			}
		}
		catch (Throwable error) {
			logger.error("Failed to query dataset tasks with where clause",
				error);
		} finally {
			pool.close(results, statement, connection);
		}
		return tasks;
	}

	public static Task populateTask(ResultSet rs) throws SQLException{
		String taskID = rs.getString("task_id");
		String user = rs.getString("user_id");
		String tool = rs.getString("tool");
		String messages = rs.getString("messages");
		String notification = rs.getString("notification");
		String desc = rs.getString("description");
		String site = rs.getString("site");
		String version = rs.getString("version");
		TaskStatus status = TaskStatus.valueOf(rs.getString("status"));
		Timestamp createTime = rs.getTimestamp("create_time");
		Timestamp beginTime = rs.getTimestamp("begin_time");
		Timestamp endTime = rs.getTimestamp("end_time");
		Timestamp now = rs.getTimestamp("now");
		long elapsed = 0;
		switch (status) {
			case DONE:
			case CRASHED:
			case FAILED:
			case SUSPENDED:
				elapsed = (endTime == null) ? 0 :
					endTime.getTime() - createTime.getTime();
				break;
			case RUNNING:
				elapsed = now.getTime() - beginTime.getTime();
				break;
			case LAUNCHING:
			case QUEUED:
			case UPLOADING:
				elapsed = now.getTime() - createTime.getTime();
				break;
			case DELETED:
			case NONEXIST:
				elapsed = 0;
				break;
		}

		Task task = new Task(taskID, user, tool, status, site, version);
		task.setMessage(messages);
		task.setDescription(desc);
		task.setNotification(notification);
		task.setTimes(createTime, beginTime, endTime, elapsed);
		return task;
	}

	/**
	 * Warning: don't call this method unless relatively few tasks have been
	 * run on this ProteoSAFe instance.
	 *
	 * @return	a java.util.Collection containing all tasks ever submitted
	 * 			to this ProteoSAFe instance
	 */
	public static Collection<Task> queryAllTasks() {
		return queryTasks("");
	}

	public static Collection<Task> queryRecentTasks() {
		// compute date string for two weeks ago
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(new Date());
		calendar.add(Calendar.DAY_OF_MONTH, -14);
		String twoWeeksAgo =
			new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(
				calendar.getTime());
		return queryTasks(
			"WHERE STATUS = 'RUNNING' OR IFNULL(end_time, create_time) >= '" +
			twoWeeksAgo + "' ORDER BY create_time DESC");
	}

	/**
	 * Return the most recent K tasks
	 * @param number_number
	 * @return
	 */
	public static Collection<Task> queryRecentNumberTasks(int number_number){
		return queryTasks(
				" ORDER BY create_time DESC" + " LIMIT " + number_number);
	}

	public static Collection<Task> queryOwnedTasks(String user){
		return queryTasks(
			"WHERE user_id=? ORDER BY create_time DESC", user);
	}

	public static Collection<Task> queryTasksBySite(TaskStatus status, String site){
		return queryTasks(
			"WHERE status=? and site=? ORDER BY create_time DESC", status.toString(), site);
	}

	// dataset task queries
	public static Collection<Task> queryAllDatasetTasks() {
		return queryTasks(
			"WHERE task_id IN (SELECT task_id FROM datasets) " +
			"ORDER BY create_time DESC");
	}

	public static Collection<Task> queryDatasetTasksByPrivacy(
		boolean isPrivate
	) {
		return queryTasks(
			"WHERE task_id IN (SELECT task_id FROM datasets WHERE private=?)" +
			"ORDER BY create_time DESC", isPrivate ? "1" : "0");
	}

	public static Task queryDatasetTaskByID(String id) {
		Collection<Task> tasks = queryTasks("WHERE task_id IN " +
			"(SELECT task_id FROM datasets WHERE dataset_id=?)" +
			"ORDER BY create_time DESC",
			Integer.toString(Dataset.parseDatasetIDString(id)));
		if (tasks.isEmpty())
			return null;
		// there should be at most one task in this collection
		else if (tasks.size() > 1)
			throw new RuntimeException(String.format("Found %d workflow " +
				"tasks associated with MassIVE dataset \"%s\".",
				tasks.size(), id));
		else return tasks.iterator().next();
	}

	public static Collection<Task> queryOwnedDatasetTasks(String user) {
		return queryTasks(
			"WHERE user_id=? AND task_id IN (SELECT task_id FROM datasets) " +
			"ORDER BY create_time DESC", user);
	}

	public static boolean isDatasetTask(Task task) {
		Dataset dataset = null;
		try {
			dataset = DatasetManager.queryDatasetByTaskID(task.getID());
		} catch (Throwable error) {
			return false;
		}
		if (dataset == null)
			return false;
		else return true;
	}

	private static int queryNumTasks(String site, String status, int days, boolean finished){
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		int result = -1;
		ConnectionPool pool = null;
		String pivot = finished ? "end_time" : "begin_time";

		try{
			pool = Commons.getConnectionPool();
			conn = pool.aquireConnection();
			String query = "SELECT count(*) as cnt FROM tasks WHERE site = ?";

			if(days > 0)
				query += String.format(" AND NOW() < ADD_DAYS(%s, %d)", pivot, days);
			if(status != null)
				query += String.format(" AND status = '%s'", status);

			stmt = conn.prepareStatement(query);
			stmt.setString(1, site);

			rs = stmt.executeQuery();
			if(rs.next())
				result = rs.getInt("cnt");
		}
		catch(Throwable th){
			logger.error("Failed to query the number of tasks", th);
		}
		finally{
			pool.close(rs, stmt, conn);
		}
		return result;
	}

	public static int queryNumSubmittedTasks(String site, int days){
		return queryNumTasks(site, null, days, false);
	}

	public static int queryNumFailedTasks(String site, int days){
		return queryNumTasks(site, TaskStatus.FAILED.toString(), days, true);
	}

	public static int queryNumCompletedTasks(String site, int days){
		return queryNumTasks(site, TaskStatus.DONE.toString(), days, true);
	}

	public static int queryNumRunningTasks(String site){
		return queryNumTasks(site, TaskStatus.RUNNING.toString(), -1, false);
	}

	public static int queryNumSuspendedTasks(String site){
		return queryNumTasks(site, TaskStatus.SUSPENDED.toString(), -1, false);
	}

	public static int queryNumLongRunningTasks(String site, int days){
		return queryNumTasks(site, TaskStatus.RUNNING.toString(), days, false);
	}

	public static List<Map<String, String>> queryActiveUsers(String site){
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		ConnectionPool pool = null;
		List<Map<String, String>> result = new LinkedList<Map<String,String>>();

		try{
			pool = Commons.getConnectionPool();
			conn = pool.aquireConnection();
			String sql =
				"select count(*) as cnt, T.user_id as id, T.notification, realname, organization " +
				" from tasks T left join users U on T.user_id = U.user_id" +
				" where site=? and status ='RUNNING' group by T.user_id, notification";

			stmt = conn.prepareStatement(sql);
			stmt.setString(1, site);

			rs = stmt.executeQuery();
			String fields[] = {"cnt", "id", "notification", "realname", "organization"};
			while(rs.next()){
				Map<String, String> entry = new HashMap<String, String>();
				for(String field: fields)
					entry.put(field, rs.getString(field));
				result.add(entry);
			}
		}
		catch(Throwable th){
			logger.error("Failed to query active users", th);
		}
		finally{
			pool.close(rs, stmt, conn);
		}
		return result;
	}

	/**
	 * Given a list of tasks, return a map of all those strings to the task objects
	 * @param task_list
	 * @return
	 */
	public static Map<String, Task> queryTaskList(List<String> task_list){
		//Construct query string
		String query_string = "WHERE task_id in (";
		int query_counts = 0;
		String[] arguments = new String[task_list.size()];
		for(String task_id : task_list){
			query_counts += 1;
			arguments[query_counts - 1] = task_id;
			if(query_counts == 1){
				query_string += "?" ;
			}
			else{
				query_string += ",?" ;
			}
		}

		query_string += ")";


		Collection<Task> all_returned_tasks = queryTasks(query_string, arguments);
		Map<String, Task> task_map = new HashMap<String, Task>();

		for(Task task : all_returned_tasks){
			task_map.put(task.getID(), task);
		}

	    return task_map;
	}

	public static String getTaskListJSON(Collection<Task> tasks) {
		if (tasks == null || tasks.isEmpty())
			return "[]";
		// build JSON string
		StringBuffer json = new StringBuffer("[");
		int i = 0;
		for (Task task : tasks) {
			TaskStatus status = task.getStatus();
			if (TaskStatus.DELETED.equals(status))
				continue;
			String desc = task.getDescription();
			if (desc == null || desc.trim().equals(""))
				desc = "";
			String workflow = task.getFlowName();
			if (workflow == null || workflow.trim().equals(""))
				workflow = "N/A";
			json.append("\n\t{task:\"");
			json.append(task.getID());
			json.append("\",desc:\"");
			json.append(StringEscapeUtils.escapeJson(desc));
			json.append("\",user:\"");
			json.append(task.getUser());
			json.append("\",workflow:\"");
			json.append(StringEscapeUtils.escapeJson(task.getFlowName()));
			json.append("\",site:\"");
			json.append(task.getSite());
			json.append("\",status:\"");
			json.append(task.getStatus().toString());
			json.append("\",createdMillis:\"");
			json.append(task.getCreateTime().getTime());
			json.append("\",created:\"");
			json.append(new SimpleDateFormat("MMM. d, yyyy, h:mm a").format(
				task.getCreateTime()));
			json.append("\",elapsedMillis:\"");
			json.append(task.getElapsedTime());
			json.append("\",elapsed:\"");
			json.append(FormatUtils.formatTimePeriod(task.getElapsedTime()));
			json.append("\",id:\"");
			json.append(i);
			json.append("\"},");
			i++;
		}
		// chomp trailing comma and close JSON array
		json.setLength(json.length() - 1);
		json.append("\n]");
		return json.toString();
	}

	public static Task createSystemTask(
		String user, String workflow, String title
	) {
		return createSystemTask(user, workflow, title, null);
	}

	public static Task createSystemTask(
		String user, String workflow, String title,
		Map<String, Collection<String>> parameters
	) {
		// validate user and workflow string values
		if (user == null || workflow == null) {
			logger.error(String.format("Could not create system task " +
				"of workflow type [%s], as submitted by user [%s]: " +
				"both of these argument values must be non-null.",
				workflow != null ? workflow : "null",
				user != null ? user : "null"));
			return null;
		} else if (AccountManager.isRegistered(user) == false) {
			logger.error(String.format("Could not create system task " +
				"of workflow type [%s], as submitted by user [%s]: " +
				"this user could not be found in the database.",
				workflow, user));
			return null;
		}
		// set up task parameters
		if (parameters == null)
			parameters = new LinkedHashMap<String, Collection<String>>(2);
		WorkflowParameterUtils.setParameterValue(
			parameters, "workflow", workflow);
		if (title != null)
			WorkflowParameterUtils.setParameterValue(
				parameters, "desc", title);
		Task task = TaskFactory.createTask(user, parameters);
		// if task creation failed for any reason, report errors
		if (task == null)
			logger.error("System task creation failed for unknown reasons.");
		else if (task instanceof NullTask ||
			TaskStatus.FAILED.equals(task.getStatus())) {
			String error = task.getMessage();
			logger.error(String.format(
				"System task creation failed with these error messages:\n%s",
				error != null ? error : ""));
			return null;
		}
		return task;
	}

	public static boolean isValidTaskIDString(String taskID) {
		if (taskID == null)
			return false;
		// task ID strings are 32-digit hexadecimal numbers
		return Pattern.matches("^[0-9a-f]{32}$", taskID);
	}
}
