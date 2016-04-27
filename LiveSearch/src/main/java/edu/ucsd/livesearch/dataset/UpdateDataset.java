package edu.ucsd.livesearch.dataset;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsd.livesearch.servlet.BaseServlet;
import edu.ucsd.livesearch.task.Task;
import edu.ucsd.livesearch.task.TaskManager;
import edu.ucsd.livesearch.util.WorkflowParameterUtils;
import edu.ucsd.saint.commons.http.HttpParameters;

@SuppressWarnings("serial")
public class UpdateDataset
extends BaseServlet
{
	/*========================================================================
	 * Constants
	 *========================================================================*/
	private static final Logger logger =
		LoggerFactory.getLogger(UpdateDataset.class);
	
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	@Override
	protected void doPost(
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
		
		// get dataset ID
		String id = parameters.getParameter("dataset");
		if (id == null) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST,
				"You must specify the ID of a valid dataset to update it.");
			return;
		}
		
		// get dataset and associated task
		Dataset dataset = DatasetManager.queryDatasetByID(id);
		if (dataset == null) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST,
				String.format("No dataset could be found with ID [%s].", id));
			return;
		}
		Task task = dataset.getTask();
		
		// verify that the currently authenticated user
		// has permission to update this dataset
		String user = getUser();
		if (isAdministrator() == false && dataset.isOwnedBy(user) == false) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST,
				String.format("User [%s] does not have permission to update " +
					"dataset with ID [%s].", user, id));
			return;
		}
		
		// set up a system task to properly log this operation
		Map<String, Collection<String>> systemTaskParameters =
			new LinkedHashMap<String, Collection<String>>(15);
		WorkflowParameterUtils.setParameterValue(
			systemTaskParameters, "dataset", id);
		String systemTaskDescription =
			String.format("Updating metadata for dataset [%s]", id);
		Task systemTask = null;
		List<String> errors = new ArrayList<String>();
		boolean titleUpdated = false;
		boolean datasetUpdated = false;
		try {
			// update the "description" field of the "tasks" table in the DB
			String title = parameters.getParameter("desc");
			String oldTitle = task.getDescription();
			if (title != null && title.equals(oldTitle) == false) {
				titleUpdated = true;
				WorkflowParameterUtils.setParameterValue(
					systemTaskParameters, "title.previous", oldTitle);
				WorkflowParameterUtils.setParameterValue(
					systemTaskParameters, "title", title);
				if (TaskManager.setComment(task, title) == false)
					throw new RuntimeException(String.format(
						"There was a problem updating the the " +
						"\"description\" field for task [%s], belonging to " +
						"dataset [%s].", task.getID(), id));
			}
			
			// update the fields of the "datasets" table in the DB
			// TODO: ensure that required parameters are present!
			// description
			String description = parameters.getParameter("dataset.comments");
			String oldDescription = dataset.getDescription();
			if (description != null &&
				description.equals(oldDescription) == false) {
				datasetUpdated = true;
				WorkflowParameterUtils.setParameterValue(
					systemTaskParameters, "description.previous",
					oldDescription);
				WorkflowParameterUtils.setParameterValue(
					systemTaskParameters, "description", description);
				dataset.setDescription(description);
			}
			// species
			String species = parameters.getParameter("dataset.species");
			String oldSpecies = dataset.getSpecies();
			if (species != null && species.equals(oldSpecies) == false) {
				datasetUpdated = true;
				WorkflowParameterUtils.setParameterValue(
					systemTaskParameters, "species.previous", oldSpecies);
				WorkflowParameterUtils.setParameterValue(
					systemTaskParameters, "species", species);
				dataset.setSpecies(species);
			}
			// instrument
			String instrument = parameters.getParameter("dataset.instrument");
			String oldInstrument = dataset.getInstrument();
			if (instrument != null &&
				instrument.equals(oldInstrument) == false) {
				datasetUpdated = true;
				WorkflowParameterUtils.setParameterValue(
					systemTaskParameters, "instrument.previous", oldInstrument);
				WorkflowParameterUtils.setParameterValue(
					systemTaskParameters, "instrument", instrument);
				dataset.setInstrument(instrument);
			}
			// modification
			String modification =
				parameters.getParameter("dataset.modification");
			String oldModification = dataset.getModification();
			if (modification != null &&
				modification.equals(oldModification) == false) {
				datasetUpdated = true;
				WorkflowParameterUtils.setParameterValue(
					systemTaskParameters, "modification.previous",
					oldModification);
				WorkflowParameterUtils.setParameterValue(
					systemTaskParameters, "modification", modification);
				dataset.setModification(modification);
			}
			// principal investigator
			String pi = parameters.getParameter("dataset.pi");
			String oldPI = dataset.getPI();
			if (pi != null && pi.equals(oldPI) == false) {
				datasetUpdated = true;
				WorkflowParameterUtils.setParameterValue(
					systemTaskParameters, "pi.previous", oldPI);
				WorkflowParameterUtils.setParameterValue(
					systemTaskParameters, "pi", pi);
				dataset.setPI(pi);
			}
			// only execute a DB operation if something actually changed
			if (datasetUpdated &&
				DatasetManager.updateDataset(dataset) == false)
				throw new RuntimeException(
					"Update dataset operation returned false.");
		} catch (Throwable error) {
			String message = String.format(
				"There was an error updating metadata for dataset [%s]: %s",
				id, error.getMessage());
			logger.error(message, error);
			errors.add(message);
			response.sendError(
				HttpServletResponse.SC_INTERNAL_SERVER_ERROR, message);
			return;
		} finally {
			// finalize task, if an update actually took place
			if (titleUpdated || datasetUpdated) {
				systemTask = TaskManager.createSystemTask(
					user, "UPDATE-DATASET-METADATA",
					systemTaskDescription, systemTaskParameters);
				if (errors.isEmpty()) {
					TaskManager.setDone(systemTask);
				} else systemTask.setFailures(errors);
			}
		}
		
		response.sendRedirect(
			String.format("status.jsp?task=%s", task.getID()));
	}
}
