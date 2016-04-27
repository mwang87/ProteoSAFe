package edu.ucsd.livesearch.publication;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.stream.XMLStreamException;
import javax.xml.ws.WebServiceException;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.xpath.XPathAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.ucsd.livesearch.dataset.Dataset;
import edu.ucsd.livesearch.dataset.DatasetManager;
import edu.ucsd.livesearch.servlet.BaseServlet;
import edu.ucsd.livesearch.task.Task;
import edu.ucsd.livesearch.task.TaskManager;
import edu.ucsd.livesearch.util.Commons;
import edu.ucsd.livesearch.util.FileIOUtils;
import edu.ucsd.livesearch.util.WorkflowParameterUtils;
import edu.ucsd.saint.commons.ConnectionPool;
import edu.ucsd.saint.commons.http.HttpParameters;

@SuppressWarnings("serial")
public class PublicationManager
extends BaseServlet
{
	/*========================================================================
	 * Constants
	 *========================================================================*/
	private static final Logger logger =
		LoggerFactory.getLogger(PublicationManager.class);
	private static final String E_FETCH_URL =
		"http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi";
	
	/*========================================================================
	 * Servlet methods
	 *========================================================================*/
	/**
	 * <p>Accepts and processes queries for PubMed publication data.
	 * 
	 * <p>By convention, a GET request to this servlet is assumed to be a
	 * request to read data only.  No creation, update, or deletion of
	 * server resources is handled by this method.
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
		PrintWriter out = response.getWriter();
		
		// get the indicated publication's PubMed ID
		String pmid = parameters.getParameter("pmid");
		if (pmid == null) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST,
				"You must specify a valid PubMed ID to retrieve " +
				"a publication's NCBI eFetch XML.");
			return;
		}
		
		// fetch PubMed XML, parse into a Publication object
		Publication publication = null;
		try {
			publication = getPublicationFromPubMed(pmid);
		} catch (Throwable error) {
			getLogger().error(String.format("Error retrieving publication " +
				"with PubMed ID [%s] from the NCBI eFetch service",
				pmid), error);
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
				error.getMessage());
			return;
		} finally {
			if (publication == null) {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST,
					String.format("No NCBI eFetch data could be found for " +
						"publication with PubMed ID [%s]", pmid));
				return;
			}
		}
		
		// output the returned publication's fields as JSON
		String json = publication.getJSON();
		response.setContentType("application/json");
		response.setContentLength(json.length());
		out.print(json);
	}
	
	/**
	 * <p>Accepts and processes requests to add new publications associated
	 * with a specified dataset.
	 * 
	 * <p>By convention, a POST request to this servlet is assumed to be a
	 * request for data creation only.  No reading, update, or deletion of
	 * server resources is handled by this method.
	 * 
	 * @param request	an {@link HttpServletRequest} object that contains
	 * 					the request the client has made of the servlet
	 * 
	 * @param response	an {@link HttpServletResponse} object that contains
	 * 					the response the servlet sends to the client
	 * 
	 * @throws IOException		if an input or output error is detected
	 * 							when the servlet handles the POST request
	 * 
	 * @throws ServletException	if the request for the POST could not be
	 * 							handled
	 */
	@Override
	protected void doPost(
		HttpServletRequest request, HttpServletResponse response
	) throws ServletException, IOException {
		// initialize properties
		try {
			initialize(request, true);
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
		
		// verify authentication of user
		if (isAuthenticated() == false) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST,
				"You must be logged in to add a new publication to a dataset.");
			return;
		}
		
		// get associated dataset and task
		String datasetID = parameters.getParameter("dataset");
		if (datasetID == null) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST,
				"You must specify a valid dataset ID " +
				"to add a new publication to it.");
			return;
		}
		Dataset dataset = DatasetManager.queryDatasetByID(datasetID);
		if (dataset == null) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST,
				String.format("Could not find a dataset with ID [%s].",
				datasetID));
			return;
		}
		Task task = dataset.getTask();
		if (task == null) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST,
				String.format("Could not find a task for dataset with ID [%s].",
				datasetID));
			return;
		}
		
		// ensure that user has permission to add a publication to this dataset
		String user = getUser();
		if (isAdministrator() == false) {
			if (user.equals(task.getUser()) == false) {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST,
					String.format("User [%s] does not have permission " +
					"to add a new publication to dataset with ID [%s].",
					user, datasetID));
				return;
			}
		}
		
		// get form parameters
		String authors = parameters.getParameter("publication.authors");
		String title = parameters.getParameter("publication.title");
		String citation = parameters.getParameter("publication.citation");
		String abs = parameters.getParameter("publication.abstract");
		String pmid = parameters.getParameter("publication.pmid");
		String pmcid = parameters.getParameter("publication.pmcid");
		
		// initialize publication with parameters
		Publication publication =
			new Publication(null, authors, title, citation, abs, pmid, pmcid);

		// set up a system task to properly log this operation
		Map<String, Collection<String>> systemTaskParameters =
			new LinkedHashMap<String, Collection<String>>(7);
		WorkflowParameterUtils.setParameterValue(
			systemTaskParameters, "dataset", datasetID);
		String description =
			String.format("Adding publication to dataset [%s]", datasetID);
		Task systemTask = null;
		List<String> errors = new ArrayList<String>();
		try {
			// if this publication already exists, update the existing record
			Publication existing = queryPublicationByPMID(pmid);
			if (existing != null) {
				publication.setId(existing.getId());
				publication = updatePublication(publication);
			}
			// otherwise this is a new publication, so insert it as a new record
			else publication = insertPublication(publication);
			// now that the proper publication has been acquired,
			// add all of its relevant attributes to the log task
			pmid = publication.getPMID();
			if (pmid != null && pmid.trim().equals("") == false) {
				WorkflowParameterUtils.setParameterValue(
					systemTaskParameters, "pmid", pmid);
				description = String.format(
					"Adding publication [PMID%s] to dataset [%s]",
					pmid, datasetID);
			}
			pmcid = publication.getPMCID();
			if (pmcid != null && pmcid.trim().equals("") == false)
				WorkflowParameterUtils.setParameterValue(
					systemTaskParameters, "pmcid", pmcid);
			authors = publication.getAuthors();
			if (authors != null && authors.trim().equals("") == false)
				WorkflowParameterUtils.setParameterValue(
					systemTaskParameters, "authors", authors);
			title = publication.getTitle();
			if (title != null && title.trim().equals("") == false)
				WorkflowParameterUtils.setParameterValue(
					systemTaskParameters, "title", title);
			String reference = publication.getFormattedReferenceString();
			if (reference != null && reference.trim().equals("") == false)
				WorkflowParameterUtils.setParameterValue(
					systemTaskParameters, "reference", reference);
			// now assign this publication to the specified dataset
			if (addPublicationToDataset(publication, datasetID) == false)
				throw new RuntimeException(
					"Could not assign the publication to the dataset.");
		} catch (Throwable error) {
			String message = String.format("There was an error " +
				"adding a publication to dataset [%s]: %s",
				datasetID, error.getMessage());
			logger.error(message, error);
			errors.add(message);
			response.sendError(
				HttpServletResponse.SC_INTERNAL_SERVER_ERROR, message);
			return;
		} finally {
			// finalize task
			systemTask = TaskManager.createSystemTask(
				user, "ADD-DATASET-PUBLICATION",
				description, systemTaskParameters);
			if (errors.isEmpty())
				TaskManager.setDone(systemTask);
			else systemTask.setFailures(errors);
		}
		
		// redirect to this dataset's status page
		response.sendRedirect(
			String.format("status.jsp?task=%s", task.getID()));
	}
	
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	public static String getPubMedPublicationXML(String pmid)
	throws WebServiceException, XMLStreamException {
		if (pmid == null)
			return null;
		// submit HTTP request to NCBI E-Fetch web service
		CloseableHttpClient client = null;
		CloseableHttpResponse response = null;
		String responseText = null;
		try {
			// build HTTP request
			client = HttpClientBuilder.create().build();
			StringBuffer url = new StringBuffer(E_FETCH_URL);
			url.append("?db=pubmed&retmode=xml&id=");
			url.append(pmid);
			HttpGet request = new HttpGet(url.toString());
			// Execute HTTP request
			response = client.execute(request);
			responseText = EntityUtils.toString(response.getEntity());
			// if status code is not 200, handle error
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode != 200)
				throw new WebServiceException(
					"Error invoking the NCBI \"E-Fetch\" service: " +
					statusCode + " - " + responseText);
		} catch (WebServiceException error) {
			throw error;
		} catch (Throwable error) {
			throw new WebServiceException(error);
		} finally {
			try { client.close(); } catch (Throwable error) {}
			try { response.close(); } catch (Throwable error) {}
		}
		// return retrieved XML document
		return responseText;
	}
	
	public static Publication getPublicationFromPubMed(String pmid)
	throws WebServiceException, XMLStreamException {
		if (pmid == null)
			return null;
		// get publication XML from NCBI E-Fetch web service
		return getPublicationFromPubMed(
			FileIOUtils.parseXML(getPubMedPublicationXML(pmid)));
	}
	
	public static Publication getPublicationFromPubMed(Document eFetchResult)
	throws XMLStreamException {
		if (eFetchResult == null)
			return null;
		// parse XML to get publication properties
		StringBuffer authors = null;
		String title = null;
		String journal = null;
		String date = null;
		String epubDate = null;
		String volume = null;
		Integer issue = null;
		String pages = null;
		String abs = null;
		String pmid = null;
		String pmcid = null;
		try {
			// get top-level document PubmedArticle node
			Node pubmed = XPathAPI.selectSingleNode(eFetchResult,
				"/PubmedArticleSet/PubmedArticle");
			if (pubmed == null)
				throw new XMLStreamException("Could not find a required " +
					"<PubmedArticle> element in the XML document returned " +
					"by the NCBI \"E-Fetch\" service.");
			// get pmid
			Node citation =
				XPathAPI.selectSingleNode(pubmed, "MedlineCitation");
			if (citation == null)
				throw new XMLStreamException("Could not find a required " +
					"<MedlineCitation> element in the XML document returned " +
					"by the NCBI \"E-Fetch\" service.");
			Node pmidNode = XPathAPI.selectSingleNode(citation, "PMID");
			if (pmidNode == null)
				throw new XMLStreamException("Could not find a required " +
					"<PMID> element in the XML document returned by " +
					"the NCBI \"E-Fetch\" service.");
			else pmid = pmidNode.getTextContent().trim();
			// get pmcid, if present
			Node pmcidNode = XPathAPI.selectSingleNode(pubmed,
				"PubmedData/ArticleIdList/ArticleId[@IdType=\"pmc\"]");
			if (pmcidNode != null)
				pmcid = pmcidNode.getTextContent().trim();
			// get author list
			Node article = XPathAPI.selectSingleNode(citation, "Article");
			if (article == null)
				throw new XMLStreamException("Could not find a required " +
					"<Article> element in the XML document returned by " +
					"the NCBI \"E-Fetch\" service.");
			NodeList authorNodes =
				XPathAPI.selectNodeList(article, "AuthorList/Author");
			if (authorNodes == null || authorNodes.getLength() < 1)
				throw new XMLStreamException("Could not find any <Author> " +
					"elements in the XML document returned by the NCBI " +
					"\"E-Fetch\" service.");
			else {
				authors = new StringBuffer();
				for (int i=0; i<authorNodes.getLength(); i++) {
					Node author = authorNodes.item(i);
					Node lastName =
						XPathAPI.selectSingleNode(author, "LastName");
					if (lastName == null)
						throw new XMLStreamException(String.format(
							"Could not find a required <LastName> element " +
							"under <Author> element %d, in the XML document " +
							"returned by the NCBI \"E-Fetch\" service.", i));
					Node initials =
						XPathAPI.selectSingleNode(author, "Initials");
					if (initials == null)
						throw new XMLStreamException(String.format(
							"Could not find a required <Initials> element " +
							"under <Author> element %d, in the XML document " +
							"returned by the NCBI \"E-Fetch\" service.", i));
					authors.append(lastName.getTextContent().trim())
						.append(" ").append(initials.getTextContent().trim())
						.append(", ");
				}
				// chomp trailing ", "
				authors.setLength(authors.length() - 2);
			}
			// get title
			Node titleNode = XPathAPI.selectSingleNode(article, "ArticleTitle");
			if (titleNode == null)
				throw new XMLStreamException("Could not find a required " +
					"<ArticleTitle> element in the XML document returned by " +
					"the NCBI \"E-Fetch\" service.");
			else title = titleNode.getTextContent().trim();
			// get journal name
			Node journalNode = XPathAPI.selectSingleNode(article, "Journal");
			if (journalNode == null)
				throw new XMLStreamException("Could not find a required " +
					"<Journal> element in the XML document returned by " +
					"the NCBI \"E-Fetch\" service.");
			Node journalNameNode =
				XPathAPI.selectSingleNode(journalNode, "ISOAbbreviation");
			if (journalNameNode == null)
				throw new XMLStreamException("Could not find a required " +
					"<ISOAbbreviation> element under element <Journal> in " +
					"the XML document returned by the NCBI \"E-Fetch\" " +
					"service.");
			else journal = journalNameNode.getTextContent().trim();
			// get journal volume
			Node journalIssueNode =
				XPathAPI.selectSingleNode(journalNode, "JournalIssue");
			if (journalIssueNode != null) {
				Node volumeNode =
					XPathAPI.selectSingleNode(journalIssueNode, "Volume");
				if (volumeNode != null)
					volume = volumeNode.getTextContent().trim();
				// get journal issue
				Node issueNode =
					XPathAPI.selectSingleNode(journalIssueNode, "Issue");
				if (issueNode != null)
					issue = Integer.parseInt(issueNode.getTextContent().trim());
				// get publication date
				Node dateNode =
					XPathAPI.selectSingleNode(journalIssueNode, "PubDate");
				if (dateNode != null) {
					// first try to get "MedlineDate"
					Node medlineDateNode =
						XPathAPI.selectSingleNode(dateNode, "MedlineDate");
					if (medlineDateNode != null)
						date = medlineDateNode.getTextContent().trim();
					else {
						String year = null;
						String month = null;
						String day = null;
						// year is required
						Node yearNode =
							XPathAPI.selectSingleNode(dateNode, "Year");
						if (yearNode == null)
							throw new XMLStreamException("Could not find a " +
								"required <Year> element under element " +
								"<PubDate> in the XML document returned by " +
								"the NCBI \"E-Fetch\" service.");
						year = yearNode.getTextContent().trim();
						// month and day are optional
						Node monthNode =
							XPathAPI.selectSingleNode(dateNode, "Month");
						if (monthNode != null)
							month = monthNode.getTextContent().trim();
						Node dayNode =
							XPathAPI.selectSingleNode(dateNode, "Day");
						if (dayNode != null)
							day = dayNode.getTextContent().trim();
						// format date string from available time values
						date = getFormattedDate(year, month, day);
					}
				}
			}
			// get pages
			Node pagesNode = XPathAPI.selectSingleNode(article,
				"Pagination/MedlinePgn");
			if (pagesNode != null)
				pages = pagesNode.getTextContent().trim();
			// get e-publication date (if present)
			Node articleDateNode = XPathAPI.selectSingleNode(article,
				"ArticleDate[@DateType=\"Electronic\"]");
			if (articleDateNode != null) {
				// first try to get "MedlineDate"
				Node medlineDateNode =
					XPathAPI.selectSingleNode(articleDateNode, "MedlineDate");
				if (medlineDateNode != null)
					epubDate = medlineDateNode.getTextContent().trim();
				else {
					String year = null;
					String month = null;
					String day = null;
					// year is required
					Node yearNode =
						XPathAPI.selectSingleNode(articleDateNode, "Year");
					if (yearNode == null)
						throw new XMLStreamException("Could not find a " +
							"required <Year> element under element " +
							"<ArticleDate> in the XML document returned " +
							"by the NCBI \"E-Fetch\" service.");
					year = yearNode.getTextContent().trim();
					// month and day are optional
					Node monthNode =
						XPathAPI.selectSingleNode(articleDateNode, "Month");
					if (monthNode != null)
						month = monthNode.getTextContent().trim();
					Node dayNode =
						XPathAPI.selectSingleNode(articleDateNode, "Day");
					if (dayNode != null)
						day = dayNode.getTextContent().trim();
					// format date string from available time values
					epubDate = getFormattedDate(year, month, day);
				}
			}
			// get abstract
			Node abstractNode = XPathAPI.selectSingleNode(article,
				"Abstract/AbstractText");
			if (abstractNode!= null)
				abs = abstractNode.getTextContent().trim();
		} catch (XMLStreamException error) {
			throw error;
		} catch (Throwable error) {
			throw new XMLStreamException(error);
		}
		// populate Publication object with parsed XML properties
		return new Publication(null, authors.toString(), title,
			getCitation(journal, date, epubDate, volume, issue, pages),
			abs, pmid, pmcid);
	}
	
	public static Publication queryPublicationByID(Integer id) {
		if (id == null)
			return null;
		Collection<Publication> publications = queryPublications(
			"WHERE publication_id=?", Integer.toString(id));
		// this query should return at most one result
		if (publications == null || publications.isEmpty())
			return null;
		else return publications.iterator().next();
	}
	
	public static Publication queryPublicationByPMID(String pmid) {
		if (pmid == null)
			return null;
		Collection<Publication> publications = queryPublications(
			"WHERE pmid=?", pmid);
		// this query should return at most one result
		if (publications == null || publications.isEmpty())
			return null;
		else return publications.iterator().next();
	}
	
	public static Collection<Publication> queryPublicationsByDatasetID(
		Integer datasetID
	) {
		if (datasetID == null)
			return null;
		Collection<Publication> publications = queryPublications(
			", publications_has_datasets WHERE publication_id=" +
			"publications_has_datasets.publications_publication_id AND " +
			"publications_has_datasets.datasets_dataset_id=?",
			Integer.toString(datasetID));
		if (publications == null || publications.isEmpty())
			return null;
		else return publications;
	}
	
	public static Collection<Publication> queryPublicationsByDatasetID(
		String datasetID
	) {
		if (datasetID == null)
			return null;
		else return queryPublicationsByDatasetID(
			Dataset.parseDatasetIDString(datasetID));
	}
	
	public static Publication insertPublication(Publication publication)
	throws Exception {
		if (publication == null)
			return null;
		// any newly inserted publication should not already have an ID
		if (publication.getId() != null)
			throw new IllegalArgumentException(
				"Newly added publications cannot already have a database ID.");
		// insert publication row into database
		Connection connection = null;
		PreparedStatement statement = null;
		ResultSet result = null;
		ConnectionPool pool = null;
		try {
			pool = Commons.getConnectionPool();
			connection = pool.aquireConnection();
			statement = connection.prepareStatement(
				"INSERT INTO publications (authors, title, citation, " +
				"abstract, pmid, pmcid) VALUES(?, ?, ?, ?, ?, ?)",
				Statement.RETURN_GENERATED_KEYS);
			statement.setString(1, publication.getAuthors());
			statement.setString(2, publication.getTitle());
			statement.setString(3, publication.getCitation());
			statement.setString(4, publication.getAbstract());
			statement.setString(5, publication.getPMID());
			statement.setString(6, publication.getPMCID());
			int insertion = statement.executeUpdate();
			if (insertion != 1)
				throw new SQLException(String.format("The publication " +
					"insert statement returned a value of [%d].", insertion));
			// get resulting publication ID
			result = statement.getGeneratedKeys();
			if (result.next() == false)
				throw new SQLException("The publication insert statement " +
					"did not generate any auto increment keys.");
			int publicationId = result.getInt(1);
			if (publicationId < 1)
				throw new SQLException(String.format("The publication " +
					"insert statement returned an auto increment key of [%d].",
					publicationId));
			// add publication ID to publication and return it
			publication.setId(publicationId);
			return publication;
		} catch (Throwable error) {
			throw new RuntimeException("There was an error inserting " +
				"the publication row into the database.", error);
		} finally {
			pool.close(result, statement, connection);
		}
	}
	
	public static Publication updatePublication(Publication publication)
	throws Exception {
		if (publication == null)
			return null;
		// any updateable publication should already have an ID
		if (publication.getId() == null)
			throw new IllegalArgumentException(
				"Publications must already have a database ID to be updated.");
		// update publication row in database
		Connection connection = null;
		PreparedStatement statement = null;
		ResultSet result = null;
		ConnectionPool pool = null;
		try {
			pool = Commons.getConnectionPool();
			connection = pool.aquireConnection();
			statement = connection.prepareStatement(
				"UPDATE publications SET authors=?, title=?, citation=?, " +
				"abstract=?, pmid=?, pmcid=? WHERE publication_id=?");
			statement.setString(1, publication.getAuthors());
			statement.setString(2, publication.getTitle());
			statement.setString(3, publication.getCitation());
			statement.setString(4, publication.getAbstract());
			statement.setString(5, publication.getPMID());
			statement.setString(6, publication.getPMCID());
			statement.setInt(7, publication.getId());
			int update = statement.executeUpdate();
			if (update != 1)
				throw new SQLException(String.format("The publication " +
					"update statement returned a value of [%d].", update));
			else return publication;
		} catch (Throwable error) {
			throw new RuntimeException("There was an error inserting " +
				"the publication row into the database.", error);
		} finally {
			pool.close(result, statement, connection);
		}
	}
	
	public static boolean addPublicationToDataset(
		Publication publication, String datasetID
	) throws Exception {
		if (publication == null || datasetID == null)
			return false;
		// any addable publication should already have an ID
		if (publication.getId() == null)
			throw new IllegalArgumentException("Publications must already " +
				"have a database ID to be added to a dataset.");
		// verify that dataset exists
		Dataset dataset = DatasetManager.queryDatasetByID(datasetID);
		if (dataset == null)
			throw new IllegalArgumentException(String.format(
				"Error adding publication with ID [%d] to dataset " +
				"with ID [%s]: no dataset could be found with this ID.",
				publication.getId(), datasetID));
		// verify that this dataset doesn't already have this publication
		dataset = DatasetManager.queryDatasetPublications(dataset);
		Collection<Publication> publications = dataset.getPublications();
		if (publications != null) {
			for (Publication existing : publications)
				if (publication.getId().equals(existing.getId()))
					return true;
		}
		// insert publication-dataset join row into database
		Connection connection = null;
		PreparedStatement statement = null;
		ConnectionPool pool = null;
		try {
			pool = Commons.getConnectionPool();
			connection = pool.aquireConnection();
			statement = connection.prepareStatement(
				"INSERT INTO publications_has_datasets " +
				"(publications_publication_id, datasets_dataset_id) " +
				"VALUES(?, ?)");
			statement.setInt(1, publication.getId());
			statement.setInt(2, dataset.getDatasetID());
			int insertion = statement.executeUpdate();
			if (insertion != 1)
				throw new SQLException(String.format(
					"The publication-dataset join table insert statement " +
					"returned a value of [%d].", insertion));
			else return true;
		} catch (Throwable error) {
			throw new RuntimeException("There was an error inserting " +
				"the publication-dataset join row into the database.", error);
		} finally {
			pool.close(statement, connection);
		}
	}
	
	public static String getCitation(
		String journal, String date, String epubDate,
		String volume, Integer issue, String pages
	) {
		// journal title - required
		if (journal == null || journal.isEmpty())
			return null;
		else journal = appendDot(journal);
		StringBuffer citation = new StringBuffer(journal);
		// all citation fields must be present to make sense
		if (date != null && volume != null && issue != null && pages != null) {
			citation.append(" ").append(date);
			citation.append(";").append(volume);
			citation.append("(").append(issue).append(")");
			citation.append(":").append(appendDot(pages));
		}
		// epub date
		if (epubDate != null)
			citation.append(" Epub ").append(appendDot(epubDate));
		return citation.toString();
	}
	
	/*========================================================================
	 * Convenience methods
	 *========================================================================*/
	private static String getFormattedDate(
		String year, String month, String day
	) {
		// year is required
		if (year == null)
			return null;
		StringBuffer date = new StringBuffer(year);
		// month and day are optional
		if (month != null) {
			date.append(" ");
			// if this is an integer, convert it
			try {
				Calendar calendar = new GregorianCalendar();
				calendar.set(Calendar.MONTH, Integer.parseInt(month) - 1);
				date.append(new SimpleDateFormat("MMM")
					.format(calendar.getTime()));
			} catch (NumberFormatException error) {
				date.append(month);
			}
		}
		// strip leading 0's from day number, if any
		if (day != null)
			date.append(" ").append(day.replaceFirst("^0+(?!$)", ""));
		return date.toString();
	}
	
	private static String appendDot(String value) {
		if (value == null)
			return null;
		else if (value.isEmpty())
			return ".";
		else if (value.charAt(value.length() - 1) == '.')
			return value;
		else return value + ".";
	}
	
	private static Collection<Publication> queryPublications(
		String whereClause, String ... args
	) {
		Connection connection = null;
		PreparedStatement statement = null;
		ResultSet result = null;
		Collection<Publication> publications = new ArrayList<Publication>();
		ConnectionPool pool = null;
		try {
			// retrieve publications from the database
			pool = Commons.getConnectionPool();
			connection = pool.aquireConnection();
			statement = connection.prepareStatement(
				"SELECT publication_id, authors, title, citation, abstract, " +
				"pmid, pmcid FROM  publications " + whereClause +
				"ORDER BY publication_id DESC"
			);
			int i = 1;
			for (String arg : args)
				statement.setString(i++, arg);
			result = statement.executeQuery();
			logger.debug("Querying publications with statement: [{}]",
				statement);
			while (result.next()) {
				publications.add(
					new Publication(
						result.getInt("publication_id"),
						result.getString("authors"),
						result.getString("title"),
						result.getString("citation"),
						result.getString("abstract"),
						result.getString("pmid"),
						result.getString("pmcid")
					)
				);
			}
		} catch (Throwable error) {
			logger.error("Failed to query datasets with where clause", error);
			return null;
		} finally {
			pool.close(result, statement, connection);
		}
		return publications;
	}
}
