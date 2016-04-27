package edu.ucsd.livesearch.libraries;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsd.livesearch.dataset.Dataset;
import edu.ucsd.livesearch.util.Commons;
import edu.ucsd.saint.commons.ConnectionPool;

public class SpectrumRatingManager {

	private static final Logger logger =
		LoggerFactory.getLogger(SpectrumRatingManager.class);
	
	/**
	 * Gets the rating for the particular match in the continuous ID, for a user and such
	 * @param spectrumID
	 * @param user_id
	 * @param dataset_id
	 * @param scan
	 * @return
	 */
	public static SpectrumRating Get_Spectrum_Rating_For_User_Dataset_Scan(String spectrumID, String user_id, String dataset_id, int scan){
		List<SpectrumRating> ratings = Get_All_Ratings_Per_Anything(spectrumID, dataset_id, scan, user_id);
		if(ratings.size() >= 1){
			logger.info("Rating Entry Size: " + ratings.size());
			return ratings.get(0);
		}
		else{
			logger.info("No Rating Entry");
			return null;
		}
	}
	
	/**
	 * Deleting Dataset Identification Rating
	 * @param rating
	 * @return
	 */
	public static int Delete_Spectrum_Rating_For_User_Dataset_Scan(SpectrumRating rating){
		//Database boilerplate
		PreparedStatement statement = null;
		ConnectionPool pool = null;
		Connection connection = null;
		
		if(Get_Spectrum_Rating_For_User_Dataset_Scan(AnnotationManager.SpectrumID_toString(rating.getSpectrum_id()), 
				rating.getUser_id(), 
				Dataset.generateDatasetIDString(rating.getDataset_id()), 
				rating.getScan()) != null){
			try {
				pool = Commons.getConnectionPool();
				connection = pool.aquireConnection();
				
				//Update
				statement = connection.prepareStatement(
	        	"DELETE FROM massive.dataset_identification_ratings WHERE spectrum_id=? AND dataset_id=? AND scan=? AND user_id=?");
				
				statement.setInt(1, rating.getSpectrum_id());
				statement.setInt(2, rating.getDataset_id());
				statement.setInt(3, rating.getScan());
				statement.setString(4, rating.getUser_id());
				
				int insertion = statement.executeUpdate();
				logger.info("Deleting Rating RetValue " + insertion);
			
			} catch (Exception error) {
				throw new RuntimeException("Error Inserting New Rating Continuous ID Rating", error);
			} finally {
				pool.close(statement, connection);
			}
		}
			
		return 0;
	}
	
	/**
	 * Write or updates an existing rating
	 * @param rating
	 * @return
	 */
	public static int Set_Spectrum_Rating_For_User_Dataset_Scan(SpectrumRating rating){
		//Database boilerplate
		PreparedStatement statement = null;
		ConnectionPool pool = null;
		Connection connection = null;
		
		if(Get_Spectrum_Rating_For_User_Dataset_Scan(AnnotationManager.SpectrumID_toString(rating.getSpectrum_id()), 
				rating.getUser_id(), 
				Dataset.generateDatasetIDString(rating.getDataset_id()), 
				rating.getScan()) == null){
			try {
				pool = Commons.getConnectionPool();
				connection = pool.aquireConnection();
				
				//Update
				statement = connection.prepareStatement(
	        	"INSERT INTO massive.dataset_identification_ratings (spectrum_id, dataset_id, scan, user_id, rating, date, task_id) " +
				"VALUES(?, ?, ?, ?, ?, ?, ?)");
				
				statement.setInt(1, rating.getSpectrum_id());
				statement.setInt(2, rating.getDataset_id());
				statement.setInt(3, rating.getScan());
				statement.setString(4, rating.getUser_id());
				statement.setInt(5, rating.getRating());
				statement.setTimestamp(6, rating.getRating_date());
				statement.setString(7, rating.getTask_id());
				
				int insertion = statement.executeUpdate();
				logger.info("Inserting Rating RetValue " + insertion);
			
			} catch (Exception error) {
				throw new RuntimeException("Error Inserting New Rating Continuous ID Rating", error);
			} finally {
				pool.close(statement, connection);
			}
		}
		else{
			try {
				pool = Commons.getConnectionPool();
				connection = pool.aquireConnection();
				
				//Update
				statement = connection.prepareStatement(
	        	"UPDATE massive.dataset_identification_ratings " +
	        	" SET spectrum_id=?, dataset_id=?, scan=?, user_id=?,rating=?,date=?, task_id=?" + 
	        	" WHERE spectrum_id=? AND dataset_id=? AND scan=? AND user_id=?");
				
				statement.setInt(1, rating.getSpectrum_id());
				statement.setInt(2, rating.getDataset_id());
				statement.setInt(3, rating.getScan());
				statement.setString(4, rating.getUser_id());
				statement.setInt(5, rating.getRating());
				statement.setTimestamp(6, rating.getRating_date());
				statement.setString(7, rating.getTask_id());
				
				statement.setInt(8, rating.getSpectrum_id());
				statement.setInt(9, rating.getDataset_id());
				statement.setInt(10, rating.getScan());
				statement.setString(11, rating.getUser_id());
				
				int insertion = statement.executeUpdate();
				logger.info("Updating Rating RetValue " + insertion);
			
			} catch (Exception error) {
				throw new RuntimeException("Error querying Spectrum Dataset Continuous ID Rating", error);
			} finally {
				pool.close(statement, connection);
			}
		}
		return 0;
	}
	
	/**
	 * Updates the comment for a particular SpectrumRating
	 * @param rating
	 * @return
	 */
	public static int Set_Spectrum_Rating_Comment(SpectrumRating rating){
		
		if(Get_Spectrum_Rating_For_User_Dataset_Scan(AnnotationManager.SpectrumID_toString(rating.getSpectrum_id()), 
				rating.getUser_id(), 
				Dataset.generateDatasetIDString(rating.getDataset_id()), 
				rating.getScan()) != null){
			//Do an update on that table
			//Database boilerplate
			PreparedStatement statement = null;
			ConnectionPool pool = null;
			Connection connection = null;
			try {
				pool = Commons.getConnectionPool();
				connection = pool.aquireConnection();
				
				//Update
				statement = connection.prepareStatement(
	        	"UPDATE massive.dataset_identification_ratings " +
	        	" SET comment=?" + 
	        	" WHERE spectrum_id=? AND dataset_id=? AND scan=? AND user_id=?");
				
				statement.setString(1, rating.getRating_comment());
				statement.setInt(2, rating.getSpectrum_id());
				statement.setInt(3, rating.getDataset_id());
				statement.setInt(4, rating.getScan());
				statement.setString(5, rating.getUser_id());
				
				int insertion = statement.executeUpdate();
				logger.info("Updating Rating Comment RetValue " + insertion);
				
				if(insertion == 0){
					return 1;
				}
			
			} catch (Exception error) {
				throw new RuntimeException("Error querying Spectrum Dataset Continuous ID Rating", error);
			} finally {
				pool.close(statement, connection);
			}
			return 0;
		}
		else{
			return 1;
		}
	}
	
	
	/**
	 * Makes queries based on all 4 quantities depending on which is null or not
	 * @param spectrum_id, can be null
	 * @param dataset_id, can be null
	 * @param scan, null is -1
	 * @param user_id, can be null
	 * @return
	 */
	public static List<SpectrumRating> Get_All_Ratings_Per_Anything(String spectrum_id, String dataset_id, int scan, String user_id){
		List<SpectrumRating> ratings = new ArrayList<SpectrumRating>();
		
		boolean get_all_ratings = false;
		
		//Checking if everything is null
		if(spectrum_id == null && dataset_id == null && scan == -1 && user_id == null){
			get_all_ratings = true;
		}
		
		ConnectionPool pool = null;
		Connection connection = null;
		
		// retrieve dataset row from the database
		PreparedStatement statement = null;
		ResultSet result = null;
		
		try {
			pool = Commons.getConnectionPool();
			connection = pool.aquireConnection();
			
			// first get the dataset
			int num_predicates = 0;
			String query_string = "";
			if(get_all_ratings == false)
				query_string =  "SELECT * FROM massive.dataset_identification_ratings WHERE ";
			else
				query_string =  "SELECT * FROM massive.dataset_identification_ratings ";
			if(spectrum_id != null){
				if(num_predicates > 0){
					query_string += " AND ";
				}
				query_string += "spectrum_id=?";
				num_predicates++;
			}
			
			if(dataset_id != null){
				if(num_predicates > 0){
					query_string += " AND ";
				}
				query_string += "dataset_id=?";
				num_predicates++;
			}
			
			if(scan > 0){
				if(num_predicates > 0){
					query_string += " AND ";
				}
				query_string += "scan=?";
				num_predicates++;
			}
			
			if(user_id != null){
				if(num_predicates > 0){
					query_string += " AND ";
				}
				query_string += "user_id=?";
				num_predicates++;
			}
			
			statement = connection.prepareStatement(query_string);
			
			//Setting value into query
			num_predicates = 0;
			
			if(spectrum_id != null){
				num_predicates++;
				statement.setInt(num_predicates, AnnotationManager.SpectrumID_fromString(spectrum_id));
			}
			
			if(dataset_id != null){
				num_predicates++;
				statement.setInt(num_predicates, Dataset.parseDatasetIDString(dataset_id));
			}
			
			if(scan > 0){
				num_predicates++;
				statement.setInt(num_predicates, scan);
			}
			
			if(user_id != null){
				num_predicates++;
				statement.setString(num_predicates, user_id);
			}
			
			
			result = statement.executeQuery();
			
			while(result.next() != false){
				SpectrumRating specrating = new SpectrumRating(result.getInt("spectrum_id"), result.getInt("dataset_id"), 
						result.getString("user_id"), result.getString("task_id"), result.getInt("scan"), 
						result.getInt("rating"), result.getTimestamp("date"), result.getString("comment"));
				ratings.add(specrating);
			}
		
		} catch (Exception error) {
			throw new RuntimeException("Error querying Spectrum Dataset Continuous ID Rating", error);
		} finally {
			pool.close(statement, connection);
		}
		
		return ratings;
	}
	
	/**
	 * Returns all the ratings for a given spectrum ID
	 * @param spectrumID
	 * @return
	 */
	public static List<SpectrumRating> Get_All_Ratings_Per_Spectrum(String spectrumID){
		return Get_All_Ratings_Per_Anything(spectrumID, null, -1, null);
	}
	
	/**
	 * Returns all the ratings for a given Dataset
	 * @param spectrumID
	 * @return
	 */
	public static List<SpectrumRating> Get_All_Ratings_Per_Dataset(String dataset_id){
		return Get_All_Ratings_Per_Anything(null, dataset_id, -1, null);
	}
	
	/**
	 * Dumping the ratings table, this is dangerous, becareful when calling
	 * @return
	 */
	public static List<SpectrumRating> Get_All_Ratings(){
		return Get_All_Ratings_Per_Anything(null, null, -1, null);
	}
}
