package edu.ucsd.livesearch.subscription;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import edu.ucsd.livesearch.dataset.Dataset;
import edu.ucsd.livesearch.dataset.DatasetManager;
import edu.ucsd.livesearch.subscription.ContinuousIDJob;
import edu.ucsd.livesearch.task.Task;
import edu.ucsd.livesearch.task.TaskManager;
import edu.ucsd.livesearch.util.Commons;

import edu.ucsd.saint.commons.ConnectionPool;

public class ContinuousIDManager {
	
	private static final Logger logger =
		LoggerFactory.getLogger(ContinuousIDManager.class);
	
	public static List<ContinuousIDJob> get_CI_Jobs(int dataset_id){
		ConnectionPool pool = null;
		Connection connection = null;
		
		// retrieve dataset row from the database
		PreparedStatement statement = null;
		ResultSet result = null;
		
		try {
			pool = Commons.getConnectionPool();
			connection = pool.aquireConnection();
			
			String sql_query = "SELECT * FROM massive.continuous_id_operations, ";
			sql_query += "tasks ";
			sql_query += "WHERE dataset_id=? AND ";
			sql_query += "tasks.task_id=massive.continuous_id_operations.task_id";

			statement = connection.prepareStatement(sql_query);
		    statement.setInt(1, dataset_id);
			result = statement.executeQuery();
			
			logger.info(sql_query);
			
			List<ContinuousIDJob> jobs = new ArrayList<ContinuousIDJob>();
			
			int job_count = 0;
			while(result.next() == true){
				job_count++;
				Timestamp create_time =  result.getTimestamp("create_time");
				int reported = result.getInt(("reported"));
				String status = result.getString("status");
				String task_id = result.getString("task_id");
				
				Task comment_task = TaskManager.queryTask(task_id);
				
				
				logger.info(status);
				
				ContinuousIDJob job = new ContinuousIDJob(task_id, dataset_id, create_time, reported, status);
				job.setExecution_site(comment_task.getSite());
				job.setWorkflow_name(comment_task.getFlowName());
				jobs.add(job);
			}

			
			return jobs;
			
		} catch (Exception error) {
			throw new RuntimeException("Error Checking Continuous Identification Jobs.", error);
		} finally {
			pool.close(statement, connection);
		}
	}
	
	//return 0 for no, 1 yes, -1 for not existing
	public static int get_CI_report(String task_id, int dataset_id){
		ConnectionPool pool = null;
		Connection connection = null;
		// retrieve dataset row from the database
		PreparedStatement statement = null;
		ResultSet result = null;
		
		try {
			pool = Commons.getConnectionPool();
			connection = pool.aquireConnection();
			// first get the dataset
			statement = connection.prepareStatement(
			        "SELECT * FROM massive.continuous_id_operations WHERE dataset_id=? and task_id=?" );
			    statement.setInt(1, dataset_id);
			    statement.setString(2, task_id);
			result = statement.executeQuery();
			
			if(result.next() == false){
				logger.info("not a real job");
				return -1;
			}
			else{
				int report_status = result.getInt("reported");
				logger.info("report status: " + report_status);
				return report_status;
			}
			
		} catch (Exception error) {
			throw new RuntimeException("Error Checking subscription.", error);
		} finally {
			pool.close(statement, connection);
		}
	}
	
	
	
	public static int create_CI_Job(String task_id, int dataset_id, int reported){
		if(ContinuousIDManager.get_CI_report(task_id, dataset_id) != -1){
			logger.info("Already in the db");
			return 0;
		}
		
		ConnectionPool pool = null;
		Connection connection = null;
		
		
		// retrieve dataset row from the database
		PreparedStatement statement = null;
		
		try {
			pool = Commons.getConnectionPool();
			connection = pool.aquireConnection();
			// first get the dataset
			statement = connection.prepareStatement(
			        "INSERT INTO massive.continuous_id_operations (task_id, dataset_id, reported) " +
			        "VALUES(?, ?, ?)");
			    statement.setString(1, task_id);
			    statement.setInt(2, dataset_id);
			    statement.setInt(3, reported);
			int insertion = statement.executeUpdate();
			logger.info("Insertion " + insertion);
			
		} catch (Exception error) {
			throw new RuntimeException("Error adding Continuous ID job: There was " +
			        "an error inserting the spectrum row into the database.", error);
		} finally {
			pool.close(statement, connection);
		}
		
		return 0;
	}
	
	/**
	 * Removes continuous identification thing
	 * @param connection
	 * @return
	 */
	public static int remove_CI_Job(String task_id, int dataset_id){
		if(ContinuousIDManager.get_CI_report(task_id, dataset_id) == -1){
			logger.info("Not in DB");
			return -1;
		}
		
		ConnectionPool pool = null;
		Connection connection = null;
		
		
		// retrieve dataset row from the database
		PreparedStatement statement = null;
		
		Dataset dataset = DatasetManager.queryDatasetByID(Dataset.generateDatasetIDString(dataset_id));
		Task task = TaskManager.queryTask(task_id);
		if(dataset == null || task == null){
			logger.error("Invalid entries, no delete is happenign today");
			return -1;
		}
		
		try {
			pool = Commons.getConnectionPool();
			connection = pool.aquireConnection();
			// first get the dataset
			statement = connection.prepareStatement(
					"DELETE FROM massive.continuous_id_operations WHERE task_id=? AND dataset_id=?");
			    statement.setString(1, task_id);
			    statement.setInt(2, dataset_id);
			int deletion = statement.executeUpdate();
			logger.info("Deletion " + deletion);
			
		} catch (Exception error) {
			throw new RuntimeException("Error removing Continuous ID job: There was " +
			        "an error inserting the spectrum row into the database.", error);
		} finally {
			pool.close(statement, connection);
		}
		
		return 0;
	}
}
