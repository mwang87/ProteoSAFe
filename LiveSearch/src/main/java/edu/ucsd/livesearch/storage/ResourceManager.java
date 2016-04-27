package edu.ucsd.livesearch.storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsd.livesearch.task.Task;
import edu.ucsd.livesearch.util.Commons;
import edu.ucsd.saint.commons.ConnectionPool;

public class ResourceManager {
	
	private static final Logger logger = LoggerFactory.getLogger(ResourceManager.class);
	
	public static ResourceAgent assignAgent(Task task){
		return new ResourceAgent(task);
	}
	
	public static List<ResourceRecord> queryAssociatedResource(Task task){
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		ConnectionPool pool = null;
		List<ResourceRecord> result = new LinkedList<ResourceRecord>();
		try{
			pool = Commons.getConnectionPool();
			conn = pool.aquireConnection();
			stmt = conn.prepareStatement(
					"SELECT * FROM uploads WHERE task_id=? ORDER BY saved_as ASC");
			stmt.setString(1, task.getID());
			rs = stmt.executeQuery();
			while(rs.next()){
				result.add(new ResourceRecord(
					rs.getString("task_id"),
					rs.getString("user_id"),
					rs.getString("original_name"),
					rs.getString("saved_as"),
					rs.getString("purpose"),
					rs.getTimestamp("upload_time")));
			}
			return result;
		}
		catch(Throwable th){
			logger.error("Failed to query resources associated with Task " + task, th);
		}
		finally{
			pool.close(rs, stmt, conn);
		}
		return null;		
	}
}
