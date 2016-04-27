package edu.ucsd.livesearch.dataset;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsd.livesearch.task.Task;
import edu.ucsd.livesearch.task.TaskManager;
import edu.ucsd.livesearch.util.Commons;
import edu.ucsd.saint.commons.ConnectionPool;


public class MassiveCommentManager {
	@SuppressWarnings("unused")
	private static final Logger logger =
		LoggerFactory.getLogger(MassiveCommentManager.class);
	
	public static List<MassiveComment> get_All_Dataset_Comments(int dataset_id){
		List<MassiveComment> comments = new ArrayList<MassiveComment>();
		
		ConnectionPool pool = null;
		Connection connection = null;
		
		// retrieve dataset row from the database
		PreparedStatement statement = null;
		ResultSet result = null;
		
		
		try {
			pool = Commons.getConnectionPool();
			connection = pool.aquireConnection();
			
			String sqlquery = "SELECT * FROM massive.dataset_comments " +
				"WHERE workflow='ADD-MASSIVE-COMMENT' AND dataset_id=?";
			
			
			// first get the dataset
			statement = connection.prepareStatement(sqlquery);
			statement.setInt(1, dataset_id);
			
			result = statement.executeQuery();
			
			//result = statement.executeQuery();
			while(result.next() == true){
				String comment = result.getString("comment");
				String task_id = result.getString("task_id");
				int result_dataset_id = result.getInt("dataset_id");
				
				Task comment_task = TaskManager.queryTask(task_id);
				
				MassiveComment massive_comment = new MassiveComment();
				massive_comment.setComment(comment);
				massive_comment.setTask_id(task_id);
				massive_comment.setMassive_id(result_dataset_id);
				massive_comment.setExecution_site(comment_task.getSite());
				comments.add(massive_comment);
			}
		
		} catch (Exception error) {
			throw new RuntimeException("Error querying massive comments", error);
		} finally {
			pool.close(statement, connection);
		}
		
		
		return comments;
	}
	
	public static List<MassiveComment> get_All_Dataset_Reanalyses(
		int dataset_id
	) {
		List<MassiveComment> reanalyses = new ArrayList<MassiveComment>();
		ConnectionPool pool = null;
		Connection connection = null;
		// retrieve dataset row from the database
		PreparedStatement statement = null;
		ResultSet result = null;
		try {
			pool = Commons.getConnectionPool();
			connection = pool.aquireConnection();
			String sqlquery = "SELECT * FROM massive.dataset_comments " +
				"WHERE workflow='ADD-MASSIVE-REANALYSIS' AND dataset_id=?";
			// first get the dataset
			statement = connection.prepareStatement(sqlquery);
			statement.setInt(1, dataset_id);
			result = statement.executeQuery();
			while (result.next()) {
				String comment = result.getString("comment");
				String task_id = result.getString("task_id");
				int result_dataset_id = result.getInt("dataset_id");
				Task comment_task = TaskManager.queryTask(task_id);
				MassiveComment massive_reanalysis = new MassiveComment();
				massive_reanalysis.setComment(comment);
				massive_reanalysis.setTask_id(task_id);
				massive_reanalysis.setMassive_id(result_dataset_id);
				massive_reanalysis.setExecution_site(comment_task.getSite());
				reanalyses.add(massive_reanalysis);
			}
		} catch (Exception error) {
			throw new RuntimeException(
				"Error querying massive comments", error);
		} finally {
			pool.close(statement, connection);
		}
		return reanalyses;
	}
}
