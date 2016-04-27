package edu.ucsd.livesearch.subscription;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsd.livesearch.util.Commons;
import edu.ucsd.saint.commons.ConnectionPool;

/**
 * @author mingxun
 *
 */
public class SubscriptionManager {

	private static final Logger logger =
		LoggerFactory.getLogger(SubscriptionManager.class);
	
	/**
	 * @param massivedataset
	 * @return list of subscribers
	 */
	public static List<String> get_all_dataset_subscription(int massivedataset) {
		
		ConnectionPool pool = null;
		Connection connection = null;
		
		List<String> user_names = new ArrayList<String> ();

		// retrieve dataset row from the database
		PreparedStatement statement = null;
		ResultSet result = null;

		try {
			pool = Commons.getConnectionPool();
			connection = pool.aquireConnection();

			// first get the dataset
			statement = connection
					.prepareStatement("SELECT * FROM massive.continuous_id_subscriptions WHERE dataset_id=? ");
			statement.setInt(1, massivedataset);
			result = statement.executeQuery();
			while(result.next()){
				user_names.add(result.getString("user_id"));
			}
		} catch (Exception error) {
			throw new RuntimeException("Error Checking subscription.", error);
		} finally {
			pool.close(statement, connection);
		}
		
		return user_names;
	}
	
	/**
	 * @param user
	 * @param massivedataset
	 * @return 0 for no sub, 1 for sub
	 */
	public static int check_subscription(String user, int massivedataset) {
		ConnectionPool pool = null;
		Connection connection = null;

		// retrieve dataset row from the database
		PreparedStatement statement = null;
		ResultSet result = null;

		try {
			pool = Commons.getConnectionPool();
			connection = pool.aquireConnection();

			// first get the dataset
			statement = connection
					.prepareStatement("SELECT * FROM massive.continuous_id_subscriptions WHERE user_id=? AND dataset_id=? ");
			statement.setString(1, user);
			statement.setInt(2, massivedataset);
			result = statement.executeQuery();
			if (result.next() == false) {
				logger.info("no sub");
				return 0;
			} else {
				logger.info("yes sub " + result.getString("user_id")
						+ " to " + result.getString("dataset_id"));
			
				return 1;
			}

		} catch (Exception error) {
			throw new RuntimeException("Error Checking subscription.", error);
		} finally {
			pool.close(statement, connection);
		}
	}
	
	/**
	 * @param user
	 * @param massivedataset
	 * @return 0 success, 1 failure
	 */
	public static int del_subscription(String user, int massivedataset){
		ConnectionPool pool = null;
		Connection connection = null;
		
		// retrieve dataset row from the database
		PreparedStatement statement = null;
		
		try {
			pool = Commons.getConnectionPool();
			connection = pool.aquireConnection();
			
			//Checking if already present
			if(check_subscription(user, massivedataset) == 0){
				//Already present, no need to add.
				logger.info("Cannot Delete, Not Present");
				return 0;
			}
			
			// first get the dataset
			statement = connection.prepareStatement(
	        "DELETE FROM massive.continuous_id_subscriptions WHERE user_id=? AND dataset_id=?");
			    statement.setString(1, user);
			    statement.setInt(2, massivedataset);
			int insertion = statement.executeUpdate();
			logger.info("Remove Sub " + user + " " + massivedataset);
			logger.info("Deletion " + insertion);
			if(insertion != 1){
				//ERROR
				return -1;
			}
		} catch (Exception error) {
			throw new RuntimeException("Error adding subscription: There was " +
			        "an error inserting the spectrum row into the database.", error);
		} finally {
			pool.close(statement, connection);
		}
		
		return 0;
	}
	
	
	/**
	 * @param user
	 * @param massivedataset
	 * @return 0 success, -1 failure
	 */
	public static int add_subscription(String user, int massivedataset){
		ConnectionPool pool = null;
		Connection connection = null;
		
		// retrieve dataset row from the database
		PreparedStatement statement = null;
		
		try {
			pool = Commons.getConnectionPool();
			connection = pool.aquireConnection();
			
			//Checking if already present
			if(check_subscription(user, massivedataset) == 1){
				//Already present, no need to add.
				logger.info("Already Present");
				return 0;
			}
			
			// first get the dataset
			statement = connection.prepareStatement(
			        "INSERT INTO massive.continuous_id_subscriptions (user_id, dataset_id) " +
			        "VALUES(?, ?)");
			    statement.setString(1, user);
			    statement.setInt(2, massivedataset);
			int insertion = statement.executeUpdate();
			logger.info("Add Sub " + user + " " + massivedataset);
			logger.info("Insertion " + insertion);
			if(insertion != 1){
				//ERROR
				return -1;
			}
			
		} catch (Exception error) {
			throw new RuntimeException("Error adding subscription: There was " +
			        "an error inserting the spectrum row into the database.", error);
		} finally {
			pool.close(statement, connection);
		}
		
		return 0;
	}
	
	/**
	 * @param user
	 * @return list of subscribed datasets
	 */
	public static List<Integer> get_all_user_subscriptions(String user){
		ConnectionPool pool = null;
		Connection connection = null;
		
		List<Integer> subscribed_datasets = new ArrayList<Integer>();

		// retrieve dataset row from the database
		PreparedStatement statement = null;
		ResultSet result = null;

		try {
			pool = Commons.getConnectionPool();
			connection = pool.aquireConnection();

			// first get the dataset
			statement = connection
					.prepareStatement("SELECT * FROM massive.continuous_id_subscriptions WHERE user_id=?");
			statement.setString(1, user);
			
			logger.info(statement.toString());
			
			result = statement.executeQuery();
			while(result.next() != false){
				subscribed_datasets.add(result.getInt("dataset_id"));
			}

		} catch (Exception error) {
			throw new RuntimeException("Error Checking subscription.", error);
		} finally {
			pool.close(statement, connection);
		}
		
		return subscribed_datasets;
	}
	
	
	/**
	 * @param user
	 * @return list of subscribed datasets
	 */
	public static Map<Integer, List<String> > get_all_subscriptions(){
		ConnectionPool pool = null;
		Connection connection = null;
		Map<Integer, List<String> > all_subscriptions= new HashMap<Integer, List<String> >();

		// retrieve dataset row from the database
		PreparedStatement statement = null;
		ResultSet result = null;

		try {
			pool = Commons.getConnectionPool();
			connection = pool.aquireConnection();

			// first get the dataset
			statement = connection
					.prepareStatement("SELECT * FROM massive.continuous_id_subscriptions");
			
			logger.info(statement.toString());
			
			result = statement.executeQuery();
			while(result.next() != false){
				Integer dataset_id = result.getInt("dataset_id");
				String user_id = result.getString("user_id");
				if(!all_subscriptions.containsKey(dataset_id)){
					all_subscriptions.put(dataset_id, new ArrayList<String>());
				}
				all_subscriptions.get(dataset_id).add(user_id);
			}

		} catch (Exception error) {
			throw new RuntimeException("Error Checking subscription.", error);
		} finally {
			pool.close(statement, connection);
		}
		
		return all_subscriptions;
	}
	
	
}
