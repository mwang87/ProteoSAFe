package edu.ucsd.livesearch.dataset;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsd.livesearch.account.AccountManager;
import edu.ucsd.livesearch.publication.PublicationManager;
import edu.ucsd.livesearch.servlet.BaseServlet;
import edu.ucsd.livesearch.task.Task;
import edu.ucsd.livesearch.task.TaskManager;
import edu.ucsd.livesearch.task.TaskManager.TaskStatus;
import edu.ucsd.livesearch.util.Commons;
import edu.ucsd.livesearch.util.WorkflowParameterUtils;
import edu.ucsd.saint.commons.ConnectionPool;
import edu.ucsd.saint.commons.WebAppProps;
import edu.ucsd.saint.commons.http.HttpParameters;

@SuppressWarnings("serial")
public class DatasetManager
extends BaseServlet
{
	/*========================================================================
	 * Constants
	 *========================================================================*/
	private static final Logger logger =
		LoggerFactory.getLogger(DatasetManager.class);
	public static final String PUBLIC_DATASET_PASSWORD = "a";
	public static final String DELETED_DATASET_PASSWORD = "stuCac8u";

	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	/**
	 * <p>Accepts and processes requests to make MassIVE datasets public.
	 *
	 * <p>By convention, a PUT request to this servlet is assumed to be a
	 * request for data update only.  No creation, reading, or deletion of
	 * server resources is handled by this method.
	 *
	 * @param request	an {@link HttpServletRequest} object that contains
	 * 					the request the client has made of the servlet
	 *
	 * @param response	an {@link HttpServletResponse} object that contains
	 * 					the response the servlet sends to the client
	 *
	 * @throws IOException		if an input or output error is detected
	 * 							when the servlet handles the PUT request
	 *
	 * @throws ServletException	if the request for the PUT could not be
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
			getLogger().error(
				"Error initializing servlet properties from request", error);
			response.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return;
		} catch (Throwable error) {
			getLogger().error(
				"Error initializing servlet properties from request", error);
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		}
		HttpParameters parameters = getParameters();
		String user = getUser();

		// retrieve task and associated dataset
		String taskID = parameters.getParameter("task");
		if (taskID == null || taskID.trim().equals("")) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST,
				"You must provide a dataset's task ID to make it public.");
			return;
		}
		Task task = TaskManager.queryTask(taskID);
		Dataset dataset = queryDatasetByTaskID(taskID);
		if (task == null || dataset == null) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST,
				String.format("Error making dataset public: dataset task " +
					"\"%s\" was not found in the database.", taskID));
			return;
		}
		String datasetID = dataset.getDatasetIDString();

		// verify authentication of user
		if (isAuthenticated() == false) {
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
				"You must be logged in to make datasets public.");
			return;
		} else if (user.equals(task.getUser()) == false &&
			isAdministrator() == false) {
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
				"You must be the owner of the dataset task or an " +
				"administrator to make a dataset public.");
			return;
		}

		// make dataset public
		if (DatasetPublisher.makeDatasetPublic(dataset, user) == false) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
				String.format("There was an error making dataset [%s] " +
					"public.", datasetID));
			return;
		}

		// attempt to publish the dataset to ProteomeXchange, if
		// the user indicated that the dataset should be submitted
		String pxStatus =
			WorkflowParameterUtils.getParameter(task, "dataset.px");
		if (pxStatus != null && pxStatus.equalsIgnoreCase("on")) try {
			if (DatasetPublisher.publishDatasetToProteomeXchange(dataset)
				== false)
				throw new RuntimeException(
					"DatasetPublisher.publishDatasetToProteomeXchange() " +
					"returned false.");
		} catch (Throwable error) {
			logger.error(String.format("There was an error publishing " +
				"dataset [%s] to ProteomeXchange.", datasetID), error);
		}

		// if dataset was successfully made public, redirect to the status page
		response.sendRedirect("status.jsp?task=" + task.getID());
	}

	public static Dataset queryDatasetByID(String id) {
		Map<Task, Dataset> datasets = queryDatasets("AND dataset_id=?",
			Integer.toString(Dataset.parseDatasetIDString(id)));
		// this query should return at most one result
		if (datasets == null || datasets.isEmpty())
			return null;
		else return datasets.get(datasets.keySet().iterator().next());
	}

	public static Dataset queryDatasetByTaskID(String taskID) {
		Map<Task, Dataset> datasets =
			queryDatasets("AND tasks.task_id=?", taskID);
		// this query should return at most one result
		if (datasets == null || datasets.isEmpty())
			return null;
		else return datasets.get(datasets.keySet().iterator().next());
	}

	public static Map<Task, Dataset> queryAllDatasets() {
		return queryDatasets("");
	}

	public static Map<Task, Dataset> queryDatasetsByPrivacy(
		boolean isPrivate
	) {
		return queryDatasets("AND private=?", isPrivate ? "1" : "0");
	}

	public static Map<Task, Dataset> queryOwnedDatasets(String user) {
		return queryDatasets("AND user_id=?", user);
	}

	public static Map<Task, Dataset> queryDatasetList(List<Integer> datasets_list){
		//Construct query string
		String query_string = "AND dataset_id in (";
		int query_counts = 0;
		String[] arguments = new String[datasets_list.size()];
		for(Integer dataset_id : datasets_list){
			query_counts += 1;
			arguments[query_counts - 1] = dataset_id.toString();
			if(query_counts == 1){
				query_string += "?" ;
			}
			else{
				query_string += ",?" ;
			}
		}

		query_string += ")";


		return queryDatasets(query_string, arguments);
	}

	public static Dataset queryDatasetPublications(Dataset dataset) {
		if (dataset == null)
			return null;
		dataset.setPublications(
			PublicationManager.queryPublicationsByDatasetID(
				dataset.getDatasetID()));
		return dataset;
	}

	public static boolean updateDataset(Dataset dataset) {
		if (dataset == null)
			throw new NullPointerException("Cannot update a null dataset.");
		// update all mutable dataset columns in the database
		ConnectionPool pool = null;
		Connection connection = null;
		PreparedStatement statement = null;
		try {
			pool = Commons.getConnectionPool();
			connection = pool.aquireConnection();
			// make this and the annotation update a single transaction
			connection.setAutoCommit(false);
			// first update the dataset row itself
			statement = connection.prepareStatement(
				"UPDATE datasets " +
				"SET description=?, file_count=?, file_size=?, species=?, " +
				"instrument=?, modification=?, pi=?, complete=?, private=? " +
				"WHERE dataset_id=?");
			statement.setString(1, dataset.getDescription());
			statement.setInt(2, dataset.getFileCount());
			statement.setLong(3, dataset.getFileSize());
			statement.setString(4, dataset.getSpecies());
			statement.setString(5, dataset.getInstrument());
			statement.setString(6, dataset.getModification());
			statement.setString(7, dataset.getPI());
			statement.setBoolean(8, dataset.isComplete());
			statement.setBoolean(9, dataset.isPrivate());
			statement.setInt(10, dataset.getDatasetID());
			int update = statement.executeUpdate();
			if (update != 1)
				throw new RuntimeException(String.format("The dataset " +
					"update statement returned a value of \"%d\".", update));
			// then update the dataset's annotations
			else if (updateDatasetAnnotations(dataset, connection) == false)
				throw new RuntimeException(
					"Could not update dataset annotations.");
			// commit the entire transaction
			connection.commit();
			return true;
		} catch (Throwable error) {
			try { connection.rollback(); } catch (Throwable rollbackError) {}
			logger.error(String.format(
				"There was an error updating dataset [%s].",
				dataset.getDatasetIDString()), error);
			return false;
		} finally {
			pool.close(statement, connection);
		}
	}

	public static boolean deleteDataset(Dataset dataset, String user) {
		if (dataset == null)
			throw new NullPointerException("Cannot delete a null dataset.");
		else if (user == null)
			throw new NullPointerException("Cannot properly log this dataset " +
				"deletion operation, since the argument user is null.");
		// set up a system task to properly log this operation
		String datasetID = dataset.getDatasetIDString();
		Map<String, Collection<String>> parameters =
			new LinkedHashMap<String, Collection<String>>(3);
		WorkflowParameterUtils.setParameterValue(
			parameters, "dataset", datasetID);
		String description = String.format("Deleting dataset [%s]", datasetID);
		Task task = TaskManager.createSystemTask(
			user, "DELETE-DATASET", description, parameters);
		List<String> errors = new ArrayList<String>();
		try {
			// a dataset normally should never be truly deleted;
			// it should simply be set to private and its FTP access removed
			boolean isPrivate = dataset.isPrivate();
			dataset.setPrivate(true);
			if (DatasetManager.updateDataset(dataset) == false) {
				String message = String.format(
					"There was an error making deleted dataset [%s] private.",
					datasetID);
				logger.error(message);
				errors.add(message);
				return false;
			}
			// if the dataset is public, then it needs to be moved back
			// out of the public FTP space of the MassIVE repository
			if (isPrivate == false) try {
				// first find and verify the public dataset directory
				File publicDatasetDirectory = new File(
					DatasetPublisher.PUBLIC_REPOSITORY_ROOT, datasetID);
				if (publicDatasetDirectory.exists() == false)
					throw new RuntimeException(String.format(
						"Could not find public dataset directory [%s].",
						publicDatasetDirectory.getAbsolutePath()));
				else if (publicDatasetDirectory.canWrite() == false)
					throw new RuntimeException(String.format(
						"Public dataset directory [%s] was " +
						"found, but could not be moved.",
						publicDatasetDirectory.getAbsolutePath()));
				// then find, verify and remove the public symlink
				// for this dataset in the private repository space
				File publicDatasetSymlink = new File(
					WebAppProps.getPath("livesearch.massive.path"), datasetID);
				if (publicDatasetSymlink.canWrite() == false ||
					FileUtils.isSymlink(publicDatasetSymlink) == false ||
					publicDatasetSymlink.delete() == false)
					throw new RuntimeException(String.format(
						"Could not delete private dataset symlink [%s].",
						publicDatasetSymlink.getAbsolutePath()));
				// if the symlink deletion was successful, then move
				// the public dataset to the private location
				else if (publicDatasetDirectory.renameTo(publicDatasetSymlink)
					== false)
					throw new RuntimeException(String.format(
						"Could not move public dataset directory [%s] " +
						"to private dataset location [%s].",
						publicDatasetDirectory.getAbsolutePath(),
						publicDatasetSymlink.getAbsolutePath()));
			} catch (Throwable error) {
				String message = String.format(
					"There was an error rearranging repository files to " +
					"accommodate deleting dataset [%s].", datasetID);
				logger.error(message, error);
				errors.add(message);
			}
			// FTP access can be removed by changing the dataset's password to
			// some special value corresponding to deleted datasets
			try {
				if (AccountManager.getInstance().updatePassword(
						datasetID, DELETED_DATASET_PASSWORD))
					return true;
				else throw new RuntimeException(
					"AccountManager.updatePassword() returned false.");
			} catch (Throwable error) {
				String message = String.format("There was an error updating " +
					"the password for deleted dataset [%s].", datasetID);
				logger.error(message, error);
				errors.add(message);
				return false;
			}
		} catch (Throwable error) {
			String message = String.format(
				"There was an error deleting dataset [%s].", datasetID);
			logger.error(message, error);
			errors.add(message);
			return false;
		} finally {
			// finalize task
			if (errors.isEmpty())
				TaskManager.setDone(task);
			else task.setFailures(errors);
		}
	}

	public static boolean deleteDatasetPermanently(Dataset dataset) {
		if (dataset == null)
			throw new NullPointerException("Cannot delete a null dataset.");
		// delete all mutable dataset columns in the database
		ConnectionPool pool = null;
		Connection connection = null;
		PreparedStatement statement = null;
		try {
			// get dataset's details before deleting
			int datasetID = dataset.getDatasetID();
			File datasetFolder = new File(
				dataset.getRepositoryPath(), dataset.getDatasetIDString());
			// retrieve database connection
			pool = Commons.getConnectionPool();
			connection = pool.aquireConnection();
			// make this and the annotation deletion a single transaction
			connection.setAutoCommit(false);
			// first delete all of this dataset's annotations
			statement = connection.prepareStatement(
				"DELETE FROM dataset_annotations " +
				"WHERE dataset_id=?");
			statement.setInt(1, datasetID);
			statement.executeUpdate();
			statement.close();
			// then delete all of this dataset's publications
			statement = connection.prepareStatement(
				"DELETE FROM publications_has_datasets " +
				"WHERE datasets_dataset_id=?");
			statement.setInt(1, datasetID);
			statement.executeUpdate();
			statement.close();
			// then delete the dataset row itself
			statement = connection.prepareStatement(
				"DELETE FROM datasets " +
				"WHERE dataset_id=?");
			statement.setInt(1, datasetID);
			int update = statement.executeUpdate();
			if (update != 1)
				throw new RuntimeException(String.format("The dataset " +
					"delete statement returned a value of \"%d\".", update));
			// commit the entire transaction
			connection.commit();
			// finally, delete the dataset's files on the file system
			if (datasetFolder.isDirectory() == false)
				throw new RuntimeException(String.format("Could not delete " +
					"dataset directory \"%s\": directory could not be found.",
					datasetFolder.getAbsolutePath()));
			FileUtils.deleteDirectory(datasetFolder);
			return true;
		} catch (Throwable error) {
			try { connection.rollback(); } catch (Throwable rollbackError) {}
			throw new RuntimeException(String.format(
				"There was an error deleting the dataset with id \"%d\".",
				dataset.getDatasetID()), error);
		} finally {
			pool.close(statement, connection);
		}
	}

	public static String getDatasetTaskListJSON(Map<Task, Dataset> datasets) {
		if (datasets == null || datasets.isEmpty())
			return "[]";
		// build JSON string
		StringBuffer json = new StringBuffer("[");
		int i = 0;
		for (Task task : datasets.keySet()) {
			Dataset dataset = datasets.get(task);
			TaskStatus status = task.getStatus();
			if (TaskStatus.DELETED.equals(status))
				continue;
			String desc = task.getDescription();
			if (desc == null || desc.trim().equals(""))
				desc = "";
			String hash = dataset.getAnnotation("tranche_hash");
			if (hash == null)
				hash = "";
			String px = dataset.getAnnotation("px_accession");
			if (px == null)
				px = "";
			json.append("\n\t{\"dataset\":\"");
			json.append(dataset.getDatasetIDString());
			json.append("\",\"datasetNum\":\"");
			json.append(dataset.getDatasetID());
			json.append("\",\"title\":\"");
			json.append(JSONObject.escape(desc));
			json.append("\",\"user\":\"");
			json.append(task.getUser());
			json.append("\",\"site\":\"");
			json.append(task.getSite());
			json.append("\",\"flowname\":\"");
			json.append(task.getFlowName());
			json.append("\",\"createdMillis\":\"");
			json.append(dataset.getCreatedMilliseconds());
			json.append("\",\"created\":\"");
			json.append(dataset.getCreatedString());
			json.append("\",\"fileCount\":\"");
			json.append(dataset.getFileCount());
			json.append("\",\"fileSizeKB\":\"");
			json.append(Math.round(dataset.getFileSize() / 1024.0));
			json.append("\",\"species\":\"");
			json.append(dataset.getSpecies());
			json.append("\",\"instrument\":\"");
			json.append(dataset.getInstrument());
			json.append("\",\"modification\":\"");
			json.append(dataset.getModification());
			json.append("\",\"pi\":\"");
			json.append(dataset.getPI());
			json.append("\",\"complete\":\"");
			json.append(dataset.isComplete());
			json.append("\",\"status\":\"");
			json.append(dataset.getSubmissionStatus());
			json.append("\",\"private\":\"");
			json.append(dataset.isPrivate());
//			json.append("\",\"converted\":\"");
//			json.append(dataset.isConvertedAndComputable());
			json.append("\",\"hash\":\"");
			json.append(JSONObject.escape(hash));
			json.append("\",\"px\":\"");
			json.append(JSONObject.escape(px));
			json.append("\",\"task\":\"");
			json.append(task.getID());
			json.append("\",\"id\":\"");
			json.append(i);
			json.append("\"},");
			i++;
		}
		// chomp trailing comma and close JSON array
		json.setLength(json.length() - 1);
		json.append("\n]");
		return json.toString();
	}

	/*========================================================================
	 * Convenience methods
	 *========================================================================*/
	private static Map<Task, Dataset> queryDatasets(
		String andClause, String ... args
	) {
		Connection connection = null;
		PreparedStatement statement = null;
		ResultSet result = null;
		Map<Task, Dataset> datasets = new LinkedHashMap<Task, Dataset>();
		ConnectionPool pool = null;
		try {
			// first get dataset/task combinations
			pool = Commons.getConnectionPool();
			connection = pool.aquireConnection();
			statement = connection.prepareStatement(
				"SELECT tasks.task_id, user_id, tasks.create_time, " +
				"begin_time, end_time, NOW() AS now, status, messages, " +
				"tasks.description AS description, tool, notification, " +
				"site, version, dataset_id, repo_path, " +
				"datasets.create_time AS dataset_create_time, " +
				"datasets.description AS dataset_description, " +
				"file_count, file_size, species, instrument, modification, " +
				"pi, complete, private FROM tasks, datasets " +
				"WHERE tasks.task_id = datasets.task_id AND status='DONE' " +
				andClause + "ORDER BY dataset_id DESC"
			);
			int i = 1;
			for (String arg : args)
				statement.setString(i++, arg);
			logger.info("Querying datasets with statement: [{}]", statement);
			result = statement.executeQuery();
			logger.info("Done with Query");
			while (result.next()) {
				Task task = TaskManager.populateTask(result);
				Dataset dataset = new Dataset(
					result.getInt("dataset_id"),
					result.getTimestamp("dataset_create_time"),
					result.getString("task_id"),
					result.getString("repo_path"),
					result.getString("dataset_description"),
					result.getInt("file_count"),
					result.getLong("file_size"),
					result.getString("species"),
					result.getString("instrument"),
					result.getString("modification"),
					result.getString("pi"),
					result.getBoolean("complete"),
					result.getBoolean("private"),
					task
				);
				datasets.put(task, dataset);
				// accommodate legacy tasks by ensuring that the dataset's
				// description (previously stored only in params.xml) is
				// correctly inserted into the database
//				String description = dataset.getDescription();
//				if (description == null) try {
//					//logger.info("Getting description");
//					description = WorkflowParameterUtils.getParameter(
//						task, "dataset.comments");
//					if (description != null &&
//						description.trim().equals("") == false) {
//						dataset.setDescription(description);
//						updateDataset(dataset);
//					}
//				} catch (Throwable error) {
//					logger.error(String.format("Could not retrieve the " +
//						"description for legacy dataset %s (task [%s]).",
//						dataset.getDatasetIDString(), task.getID()), error);
//				}
			}
			logger.info("Finished with Getting Dataset");
			// map datasets by ID
			Map<Integer, Dataset> datasetsById =
				new LinkedHashMap<Integer, Dataset>(datasets.size());
			for (Task task : datasets.keySet()) {
				Dataset dataset = datasets.get(task);
				datasetsById.put(dataset.getDatasetID(), dataset);
			}
			// then populate each dataset with its annotations
			// TODO: find a smarter way to filter annotations at the query
			// level, based on which datasets were already looked up
			pool.close(result, statement);
			statement = connection.prepareStatement(
				"SELECT * FROM dataset_annotations");
			logger.info("Querying annotations with statement: [{}]", statement);
			result = statement.executeQuery();
			while (result.next()) {
				Dataset dataset = datasetsById.get(result.getInt("dataset_id"));
				if (dataset == null)
					continue;
				else dataset.setAnnotation(
					result.getString("name"), result.getString("value"));
			}

		} catch (Throwable error) {
			logger.error("Failed to query datasets with where clause", error);
			return null;
		} finally {
			pool.close(result, statement, connection);
		}
		return datasets;
	}

	private static boolean updateDatasetAnnotations(
		Dataset dataset, Connection connection
	) {
		if (dataset == null)
			throw new NullPointerException("Cannot update a null dataset.");
		else if (connection == null)
			throw new NullPointerException(
				"Database connection must be non-null to update a dataset.");
		// retrieve current annotations
		Set<String> newAnnotations = dataset.getAnnotationNames();
		// compare new annotations to old ones, update as necessary
		PreparedStatement statement = null;
		ResultSet result = null;
		try {
			// retrieve old annotations from the database
			Map<String, String> oldAnnotations =
				new LinkedHashMap<String, String>();
			statement = connection.prepareStatement(
				"SELECT * FROM dataset_annotations WHERE dataset_id=?");
			statement.setInt(1, dataset.getDatasetID());
			result = statement.executeQuery();
			while (result.next())
				oldAnnotations.put(
					result.getString("name"), result.getString("value"));
			statement.close();
			// update database with any annotations that are new or changed
			int datasetID = dataset.getDatasetID();
			for (String name : newAnnotations) {
				String newValue = dataset.getAnnotation(name);
				String oldValue = oldAnnotations.get(name);
				// if there is no existing row for this annotation, add it
				if (oldValue == null)
					statement = connection.prepareStatement(
						"INSERT INTO dataset_annotations " +
						"(value, dataset_id, name) VALUES(?, ?, ?)");
				// if the existing value is stale, perform the update
				else if (oldValue.equals(newValue) == false)
					statement = connection.prepareStatement(
						"UPDATE dataset_annotations SET value=? " +
						"WHERE dataset_id=? AND name=?");
				// if the existing value matches the new one, skip it
				else continue;
				// update the annotation row
				statement.setString(1, newValue);
				statement.setInt(2, datasetID);
				statement.setString(3, name);
				int update = statement.executeUpdate();
				if (update != 1)
					throw new RuntimeException(String.format(
						"The dataset annotation update statement " +
						"returned a value of \"%d\".", update));
				else statement.close();
			}
			// delete any old annotations that are missing from the new set
//			for (String name : oldAnnotations.keySet()) {
//				if (newAnnotations.contains(name) == false) {
//					statement = connection.prepareStatement(
//						"DELETE FROM dataset_annotations " +
//						"WHERE dataset_id=? AND name=?");
//					statement.setInt(1, datasetID);
//					statement.setString(2, name);
//					int update = statement.executeUpdate();
//					if (update != 1)
//						throw new RuntimeException(String.format(
//							"The dataset annotation delete statement " +
//							"returned a value of \"%d\".", update));
//					else statement.close();
//				}
//			}
			return true;
		} catch (Throwable error) {
			throw new RuntimeException(String.format(
				"There was an error updating the annotations of " +
				"dataset \"%d\".", dataset.getDatasetID()), error);
		} finally {
			try { result.close(); } catch (Throwable error) {}
			try { statement.close(); } catch (Throwable error) {}
		}
	}
}
