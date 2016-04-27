package edu.ucsd.livesearch.dataset;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.ws.WebServiceException;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import edu.ucsd.livesearch.account.AccountManager;
import edu.ucsd.livesearch.parameter.ResourceManager;
import edu.ucsd.livesearch.publication.Publication;
import edu.ucsd.livesearch.servlet.BaseServlet;
import edu.ucsd.livesearch.servlet.QuerySpecies;
import edu.ucsd.livesearch.servlet.QuerySpecies.Species;
import edu.ucsd.livesearch.task.Task;
import edu.ucsd.livesearch.task.TaskManager;
import edu.ucsd.livesearch.util.Commons;
import edu.ucsd.livesearch.util.FileIOUtils;
import edu.ucsd.livesearch.util.WorkflowParameterUtils;
import edu.ucsd.saint.commons.ConnectionPool;
import edu.ucsd.saint.commons.SaintFileUtils;
import edu.ucsd.saint.commons.WebAppProps;
import edu.ucsd.saint.commons.http.HttpParameters;

@SuppressWarnings("serial")
public class DatasetPublisher
extends BaseServlet
{
	/*========================================================================
	 * Constants
	 *========================================================================*/
	private static final Logger logger =
		LoggerFactory.getLogger(DatasetPublisher.class);
	private static final Pattern PX_ID_PATTERN =
		Pattern.compile("^R?PX(D|T)\\d{6}$");
	private static final Pattern PX_ID_LINE_PATTERN =
		Pattern.compile("^identifier=(R?PX(?:D|T)\\d{6})$");
	private static final Pattern PX_RESULT_PATTERN =
		Pattern.compile("^result=(.*)$");
	private static final Properties properties = new Properties();
	public static final File PUBLIC_REPOSITORY_ROOT;
	static {
		// determine public repository root
		String repositoryRoot = WebAppProps.getPath("livesearch.massive.path");
		if (repositoryRoot == null) {
			String error =
				"Could not find the MassIVE dataset repository root.";
			logger.error(error);
			throw new RuntimeException(error);
		}
		PUBLIC_REPOSITORY_ROOT = new File(repositoryRoot, "public");
		if (PUBLIC_REPOSITORY_ROOT == null ||
			PUBLIC_REPOSITORY_ROOT.isDirectory() == false ||
			PUBLIC_REPOSITORY_ROOT.canRead() == false) {
			String error = String.format("Could not access the public " +
				"MassIVE dataset repository root assumed to be found at [%s].",
				PUBLIC_REPOSITORY_ROOT.getAbsolutePath());
			logger.error(error);
			throw new RuntimeException(error);
		}
		// load ProteomeXchange properties
		try {
			properties.load(
				DatasetPublisher.class.getResourceAsStream(
					"ProteomeXchange.properties"));
		} catch (IOException error) {
			logger.error("There was an error loading " +
				"the ProteomeXchange properties file", error);
			throw new RuntimeException(error);
		}
	}
	
	/*========================================================================
	 * Servlet methods
	 *========================================================================*/
	/**
	 * <p>Accepts and processes queries to download MassIVE dataset
	 * ProteomeXchange XML messages.
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
		
		// get the indicated dataset ID
		String datasetID = parameters.getParameter("dataset");
		if (datasetID == null) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST,
				"You must specify the ID of a valid MassIVE dataset in " +
				"order to generate a ProteomeXchange XML document for it.");
			return;
		}
		
		// retrieve the dataset
		Dataset dataset = DatasetManager.queryDatasetByID(datasetID);
		if (dataset == null) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST,
				String.format("Could not find MassIVE dataset with ID [%s].",
				datasetID));
			return;
		}
		
		// generate ProteomeXchange XML file for this dataset
		File pxFile = getProteomeXchangeXMLFile(dataset);
		if (pxFile == null || pxFile.canRead() == false) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
				String.format("There was an error writing the " +
				"generated ProteomeXchange XML message for dataset [%s] " +
				"to a task file.", datasetID));
			return;
		} else if (processProteomeXchangeXML(pxFile, false, false) == false) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
				String.format("The generated ProteomeXchange XML " +
				"message for dataset [%s] did not pass validation.",
				datasetID));
			return;
		}
		
		// write the validated XML to the response
		String xml = FileIOUtils.readFile(pxFile);
		response.setContentType("application/xml");
		response.setContentLength(xml.length());
		out.print(xml);
	}
	
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	public static synchronized boolean makeDatasetPublic(
		Dataset dataset, String user
	) {
		if (dataset == null)
			return false;
		// only attempt this operation if the dataset is not already public
		if (dataset.isPrivate() == false)
			return true;
		// set up a system task to properly log this operation
		String datasetID = dataset.getDatasetIDString();
		Map<String, Collection<String>> parameters =
			new LinkedHashMap<String, Collection<String>>(3);
		WorkflowParameterUtils.setParameterValue(
			parameters, "dataset", datasetID);
		String description =
			String.format("Making dataset [%s] public", datasetID);
		Task task = TaskManager.createSystemTask(
			user, "MAKE-DATASET-PUBLIC", description, parameters);
		List<String> errors = new ArrayList<String>();
		try {
			// move private dataset directory to the public dataset space
			File datasetDirectory =
				new File(dataset.getRepositoryPath(), datasetID);
			if (datasetDirectory == null ||
				datasetDirectory.isDirectory() == false ||
				datasetDirectory.canRead() == false ||
				datasetDirectory.canWrite() == false)
				throw new RuntimeException(String.format(
					"Could not access the repository directory for " +
					"dataset [%s] to make it public.", datasetID));
			String privatePath = datasetDirectory.getAbsolutePath();
			File publicDatasetDirectory =
				new File(PUBLIC_REPOSITORY_ROOT, datasetID);
			if (datasetDirectory.renameTo(publicDatasetDirectory) == false)
				throw new RuntimeException(String.format(
					"Could not move the repository directory [%s] " +
					"for dataset [%s] to its public location [%s].",
					datasetDirectory.getAbsolutePath(), datasetID,
					publicDatasetDirectory.getAbsolutePath()));
			// create symbolic link in the dataset's old private
			// location to its new public location
			SaintFileUtils.makeLink(
				publicDatasetDirectory, new File(privatePath));
			// set dataset's "private" flag in the database to false
			dataset.setPrivate(false);
			if (DatasetManager.updateDataset(dataset) == false)
				throw new RuntimeException(String.format(
					"There was an error making dataset [%s] public.",
					datasetID));
			// set dataset user's password to the public dataset password
			try {
				AccountManager.getInstance().updatePassword(
					datasetID, DatasetManager.PUBLIC_DATASET_PASSWORD);
			} catch (Throwable error) {
				throw new RuntimeException(String.format(
					"There was an error updating the password for " +
					"dataset [%s] to make it public.", datasetID), error);
			}
			return true;
		} catch (Throwable error) {
			String message = String.format(
				"There was an error making dataset [%s] public: %s",
				datasetID, error.getMessage());
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
	
	public static String getProteomeXchangeID(
		boolean reprocessed, boolean forceTest
	) {
		// web service URL
		String url = properties.getProperty("px.service.url");
		if (url == null) {
			logger.error("Could not find URL for ProteomeXchange web service.");
			return null;
		}
		// get PX web service parameters from properties
		List<NameValuePair> parameters = getBasicPXParameters(forceTest);
		// web service method - "requestID"
		parameters.add(new BasicNameValuePair("method", "requestID"));
		// dataset originality
		if (reprocessed)
			parameters.add(new BasicNameValuePair("reprocessed", "yes"));
		// set up and launch web service HTTP request
		CloseableHttpClient client = null;
		BufferedReader reader = null;
		try {
			client = HttpClientBuilder.create().build();
			HttpPost post = new HttpPost(url);
			post.setEntity(new UrlEncodedFormEntity(parameters));
			HttpResponse response = client.execute(post);
			int status = response.getStatusLine().getStatusCode();
			String content = EntityUtils.toString(response.getEntity());
			// report web service response
			StringBuffer report =
				new StringBuffer("Invoked ProteomeXchange web service at [");
			report.append(url);
			report.append("] to request a new dataset accession number:");
			report.append("\nGot a response of ");
			report.append(status);
			report.append(", with the following content:\n----------\n");
			report.append(content);
			report.append("\n----------");
			logger.info(report.toString());
			// if status was 200, parse the output to get the returned ID
			if (status == 200) {
				reader = new BufferedReader(new InputStreamReader(
					new ByteArrayInputStream(content.getBytes())));
				String line = null;
				while ((line = reader.readLine()) != null) {
					Matcher matcher = PX_ID_LINE_PATTERN.matcher(line);
					if (matcher.matches())
						return matcher.group(1);
				}
				// if no PX ID was found in the response, return null
				return null;
			}
			// otherwise, report the error message
			else throw new WebServiceException(String.format(
				"Error invoking PX web service to request accession number:" +
				"\nStatus Code = %d\nResponse = [%s]", status, content));
		} catch (Throwable error) {
			logger.error(error.getMessage(), error);
			return null;
		} finally {
			try { client.close(); } catch (Throwable error) {}
			try { reader.close(); } catch (Throwable error) {}
		}
	}
	
	public static boolean insertProteomeXchangeID(
		Integer datasetID, String pxID
	) {
		if (datasetID == null || pxID == null)
			return false;
		// insert dataset annotation row into database
		ConnectionPool pool = null;
		Connection connection = null;
		PreparedStatement statement = null;
		try {
			pool = Commons.getConnectionPool();
			connection = pool.aquireConnection();
			statement = connection.prepareStatement(
				"INSERT INTO dataset_annotations (dataset_id, name, value) " +
				"VALUES(?, ?, ?)");
			statement.setInt(1, datasetID);
			statement.setString(2, "px_accession");
			statement.setString(3, pxID);
			int insertion = statement.executeUpdate();
			if (insertion != 1)
				throw new RuntimeException(String.format(
					"The ProteomeXchange ID insert statement " +
					"returned a value of \"%d\".", insertion));
		} catch (Throwable error) {
			throw new RuntimeException("Error inserting a ProteomeXchange " +
				"ID annotation row into the database.", error);
		} finally {
			pool.close(statement);
		}
		// verify dataset annotation insertion
		ResultSet result = null;
		try {
			statement = connection.prepareStatement(
				"SELECT * FROM dataset_annotations " +
				"WHERE dataset_id=? AND name=?");
			statement.setInt(1, datasetID);
			statement.setString(2, "px_accession");
			result = statement.executeQuery();
			if (result.next() == false)
				throw new RuntimeException("No ProteomeXchange ID annotation " +
					"row was found when querying immediately after insertion.");
			else return true;
		} catch (Throwable error) {
			throw new RuntimeException("Error verifying a ProteomeXchange " +
				"ID annotation row after insertion.", error);
		} finally {
			pool.close(result, statement, connection);
		}
	}
	
	public static Document generateProteomeXchangeXML(
		Dataset dataset, String pxID
	) {
		// verify dataset and PX ID
		if (dataset == null)
			throw new NullPointerException(
				"Cannot generate ProteomeXchange XML for a null dataset.");
		else if (isValidPXID(pxID) == false)
			throw new IllegalArgumentException(String.format(
				"Cannot generate ProteomeXchange XML with invalid ID %s.",
				pxID != null ? ("\"" + pxID + "\"") : "null"));
		// get associated task
		Task task = TaskManager.queryTask(dataset.getTaskID());
		// get document builder
		DocumentBuilderFactory factory =
			DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = null;
		try {
			builder = factory.newDocumentBuilder();
		} catch (ParserConfigurationException error) {
			logger.error("Error instantiating XML DocumentBuilder", error);
			return null;
		}
		
		// create new document
		Document document = builder.newDocument();
		// create and populate the root node, <ProteomeXchangeDataset>
		Element root = document.createElement("ProteomeXchangeDataset");
		Attr attribute = document.createAttribute("xmlns:xsi");
		attribute.setNodeValue(
			properties.getProperty("ProteomeXchangeDataset.xsi",
			"http://www.w3.org/2001/XMLSchema-instance"));
		root.setAttributeNode(attribute);
		attribute = document.createAttribute("xsi:noNamespaceSchemaLocation");
		attribute.setNodeValue(
			properties.getProperty("ProteomeXchangeDataset.schemaLocation",
			"proteomeXchange-1.2.0.xsd"));
		root.setAttributeNode(attribute);
		attribute = document.createAttribute("formatVersion");
		attribute.setNodeValue(
			properties.getProperty("ProteomeXchangeDataset.formatVersion",
			"1.2.0"));
		root.setAttributeNode(attribute);
		attribute = document.createAttribute("id");
		attribute.setNodeValue(pxID);
		root.setAttributeNode(attribute);
		document.appendChild(root);
		
		// create and populate the first child node, <CvList>
		Element element = document.createElement("CvList");
		// extract the information for all <Cv> elements from properties file
		int i = 0;
		while (true) {
			i++;
			String cvID = properties.getProperty("Cv." + i + ".id");
			if (cvID == null)
				break;
			String cvFullName = properties.getProperty("Cv." + i + ".fullName");
			String cvURI = properties.getProperty("Cv." + i + ".uri");
			if (cvFullName == null || cvURI == null) {
				logger.warn(String.format("ProteomeXchange CV properties " +
					"require values for both \"fullName\" and \"uri\", " +
					"since these are required attributes in the XML. " +
					"Ignoring CV entry with ID [%s].", cvID));
				continue;
			}
			String cvVersion = properties.getProperty("Cv." + i + ".version");
			Element cv = document.createElement("Cv");
			attribute = document.createAttribute("id");
			attribute.setNodeValue(cvID);
			cv.setAttributeNode(attribute);
			attribute = document.createAttribute("fullName");
			attribute.setNodeValue(cvFullName);
			cv.setAttributeNode(attribute);
			attribute = document.createAttribute("uri");
			attribute.setNodeValue(cvURI);
			cv.setAttributeNode(attribute);
			if (cvVersion != null) {
				attribute = document.createAttribute("version");
				attribute.setNodeValue(cvVersion);
				cv.setAttributeNode(attribute);
			}
			element.appendChild(cv);
		}
		root.appendChild(element);
		
		// create and populate the second child node, <DatasetSummary>
		element = document.createElement("DatasetSummary");
		attribute = document.createAttribute("announceDate");
		attribute.setNodeValue(
			new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
		element.setAttributeNode(attribute);
		attribute = document.createAttribute("hostingRepository");
		attribute.setNodeValue(
			properties.getProperty("DatasetSummary.hostingRepository",
			"MassIVE"));
		element.setAttributeNode(attribute);
		attribute = document.createAttribute("title");
		attribute.setNodeValue(
			StringEscapeUtils.escapeXml10(task.getDescription()));
		element.setAttributeNode(attribute);
		// add child nodes of <DatasetSummary>
		Map<String, Collection<String>> taskParameters =
			WorkflowParameterUtils.extractParameters(task);
		// <Description>
		Element child = document.createElement("Description");
		String description = dataset.getDescription();
		// check params.xml for backwards compatibility
		// with old datasets, if necessary
		if (description == null)
			description = WorkflowParameterUtils.getParameter(
				taskParameters, "dataset.comments");
		if (description != null)
			child.setTextContent(StringEscapeUtils.escapeXml10(description));
		element.appendChild(child);
		// <ReviewLevel>
		child = document.createElement("ReviewLevel");
		Collection<Publication> publications = dataset.getPublications();
		if (publications == null || publications.isEmpty())
			child.appendChild(getCvParam(document,
				"ReviewLevel.unpublished.cvRef",
				"ReviewLevel.unpublished.accession",
				"ReviewLevel.unpublished.name"));
		else child.appendChild(getCvParam(document,
			"ReviewLevel.published.cvRef",
			"ReviewLevel.published.accession",
			"ReviewLevel.published.name"));
		element.appendChild(child);
		// <RepositorySupport>
		child = document.createElement("RepositorySupport");
		if (dataset.isComplete())
			child.appendChild(getCvParam(document,
				"RepositorySupport.complete.cvRef",
				"RepositorySupport.complete.accession",
				"RepositorySupport.complete.name"));
		else child.appendChild(getCvParam(document,
			"RepositorySupport.partial.cvRef",
			"RepositorySupport.partial.accession",
			"RepositorySupport.partial.name"));
		element.appendChild(child);
		root.appendChild(element);
		
		// create and populate the third child node, <DatasetIdentifierList>
		element = document.createElement("DatasetIdentifierList");
		// ProteomeXchange <DatasetIdentifier>
		child = document.createElement("DatasetIdentifier");
		child.appendChild(getCvParam(document,
			"DatasetIdentifier.px.cvRef",
			"DatasetIdentifier.px.accession",
			"DatasetIdentifier.px.name", pxID));
		element.appendChild(child);
		// MassIVE <DatasetIdentifier>
		child = document.createElement("DatasetIdentifier");
		child.appendChild(getCvParam(document,
			"DatasetIdentifier.massive.cvRef",
			"DatasetIdentifier.massive.accession",
			"DatasetIdentifier.massive.name", dataset.getDatasetIDString()));
		element.appendChild(child);
		root.appendChild(element);
		
		// create and populate the fourth child node, <DatasetOriginList>
		element = document.createElement("DatasetOriginList");
		// TODO: actually determine this dataset's original/reprocessed status
		child = document.createElement("DatasetOrigin");
		child.appendChild(getCvParam(document,
			"DatasetOrigin.original.cvRef",
			"DatasetOrigin.original.accession",
			"DatasetOrigin.original.name"));
		element.appendChild(child);
		root.appendChild(element);
		
		// create and populate the fifth child node, <SpeciesList>
		element = document.createElement("SpeciesList");
		// add user-specified <Species> nodes
		String speciesList = dataset.getSpecies();
		for (String speciesID : speciesList.split(";")) {
			// if this value is a CV term, extract its ID to look
			// up in the NCBI taxonomy ontology in the database
			if (speciesID.startsWith("NEWT:"))
				speciesID = "NCBITaxon:" + speciesID.substring(5);
			int ncbiTaxID = -1;
			if (speciesID.startsWith("NCBITaxon:")) try {
				ncbiTaxID = Integer.parseInt(speciesID.substring(10));
			} catch (NumberFormatException error) {}
			// determine scientific name, if available; otherwise, we assume
			// it's just the literal value entered by the user
			String scientificName = speciesID;
			String commonName = null;
			if (ncbiTaxID >= 0) {
				Species species = QuerySpecies.querySpecies(ncbiTaxID);
				if (species != null) {
					scientificName = species.getScientificName();
					commonName = species.getCommonName();
				}
			}
			// write XML element
			child = document.createElement("Species");
			child.appendChild(getCvParam(document,
				"Species.scientific.cvRef",
				"Species.scientific.accession",
				"Species.scientific.name", scientificName));
			if (commonName != null)
				child.appendChild(getCvParam(document,
					"Species.common.cvRef",
					"Species.common.accession",
					"Species.common.name", commonName));
			if (ncbiTaxID >= 0)
				child.appendChild(getCvParam(document,
					"Species.ncbi.cvRef",
					"Species.ncbi.accession",
					"Species.ncbi.name", Integer.toString(ncbiTaxID)));
			element.appendChild(child);
		}
		root.appendChild(element);
		
		// create and populate the sixth child node, <InstrumentList>
		element = document.createElement("InstrumentList");
		// add user-specified <Instrument> nodes
		String genericInstrumentCvRef =
			properties.getProperty("Instrument.generic.cvRef");
		String genericInstrumentAccession =
			properties.getProperty("Instrument.generic.accession");
		String genericInstrumentName =
			properties.getProperty("Instrument.generic.name");
		Map<String, String> resource =
			ResourceManager.getResource("instrument");
		String instrumentList = dataset.getInstrument();
		i = 1;
		for (String instrument : instrumentList.split(";")) {
			// determine instrument cvParam attributes
			String cvRef = null;
			String accession = null;
			String name = null;
			String value = null;
			String resourceItem = resource.get(instrument);
			// if this instrument was not found in the ontology, then the
			// recorded value is a custom user input, and should be reported
			// with the generic CV term
			if (resourceItem == null) {
				cvRef = genericInstrumentCvRef;
				accession = genericInstrumentAccession;
				name = genericInstrumentName;
				value = instrument;
			}
			// if this instrument was found in the ontology, then the recorded
			// value is a CV term ID, and should be reported with a proper
			// CV term for this ontology record
			else {
				String[] tokens = instrument.split(":");
				if (tokens == null || tokens.length != 2) {
					logger.warn(String.format("\"Instrument\" ontology " +
						"record IDs should be stored in the format " +
						"<cvRef>:<cvID>. Ignoring ontology record with ID " +
						"[%s].", instrument));
					continue;
				} else cvRef = tokens[0];
				accession = instrument;
				name = ResourceManager.getResourceName(resourceItem);
			}
			// write XML element
			child = document.createElement("Instrument");
			attribute = document.createAttribute("id");
			attribute.setNodeValue(String.format("Instrument_%d", i));
			child.setAttributeNode(attribute);
			// manually create cvParam node, since its accession and
			// name are directly extracted from the instrument value
			Element cvParam = document.createElement("cvParam");
			attribute = document.createAttribute("cvRef");
			attribute.setNodeValue(cvRef);
			cvParam.setAttributeNode(attribute);
			attribute = document.createAttribute("accession");
			attribute.setNodeValue(accession);
			cvParam.setAttributeNode(attribute);
			attribute = document.createAttribute("name");
			attribute.setNodeValue(name);
			cvParam.setAttributeNode(attribute);
			if (value != null) {
				attribute = document.createAttribute("value");
				attribute.setNodeValue(value);
				cvParam.setAttributeNode(attribute);
			}
			child.appendChild(cvParam);
			element.appendChild(child);
			i++;
		}
		root.appendChild(element);
		
		// create and populate the seventh child node, <ModificationList>
		element = document.createElement("ModificationList");
		// add <cvParam> nodes for all user-specified modifications
		String genericModificationCvRef =
			properties.getProperty("Modification.generic.cvRef");
		String genericModificationAccession =
			properties.getProperty("Modification.generic.accession");
		String genericModificationName =
			properties.getProperty("Modification.generic.name");
		resource = ResourceManager.getResource("modification");
		String modificationList = dataset.getModification();
		if (modificationList == null || modificationList.trim().equals(""))
			element.appendChild(getCvParam(document,
				"ModificationList.noPTMs.cvRef",
				"ModificationList.noPTMs.accession",
				"ModificationList.noPTMs.name"));
		else for (String modification : modificationList.split(";")) {
			// determine modification cvParam attributes
			String cvRef = null;
			String accession = null;
			String name = null;
			String value = null;
			String resourceItem = resource.get(modification);
			// if this modification was not found in the ontology, then the
			// recorded value is a custom user input, and should be reported
			// with the generic CV term
			if (resourceItem == null) {
				cvRef = genericModificationCvRef;
				accession = genericModificationAccession;
				name = genericModificationName;
				value = modification;
			}
			// if this modification was found in the ontology, then the recorded
			// value is a CV term ID, and should be reported with a proper
			// CV term for this ontology record
			else {
				String[] tokens = modification.split(":");
				if (tokens == null || tokens.length != 2) {
					logger.warn(String.format("\"Modification\" ontology " +
						"record IDs should be stored in the format " +
						"<cvRef>:<cvID>. Ignoring ontology record with ID " +
						"[%s].", modification));
					continue;
				} else cvRef = tokens[0];
				accession = modification;
				name =
					ResourceManager.getModificationResourceName(resourceItem);
			}
			// manually create cvParam node, since its accession and
			// name are directly extracted from the modification value
			Element cvParam = document.createElement("cvParam");
			attribute = document.createAttribute("cvRef");
			attribute.setNodeValue(cvRef);
			cvParam.setAttributeNode(attribute);
			attribute = document.createAttribute("accession");
			attribute.setNodeValue(accession);
			cvParam.setAttributeNode(attribute);
			attribute = document.createAttribute("name");
			attribute.setNodeValue(name);
			cvParam.setAttributeNode(attribute);
			if (value != null) {
				attribute = document.createAttribute("value");
				attribute.setNodeValue(value);
				cvParam.setAttributeNode(attribute);
			}
			element.appendChild(cvParam);
		}
		root.appendChild(element);
		
		// create and populate the eight child node, <ContactList>
		element = document.createElement("ContactList");
		// add <Contact> node for dataset submitter
		Map<String, String> user =
			AccountManager.getInstance().getProfile(task.getUser());
		if (user == null || user.isEmpty()) {
			// TODO: report error
		}
		child = document.createElement("Contact");
		attribute = document.createAttribute("id");
		attribute.setNodeValue("project_submitter");
		child.setAttributeNode(attribute);
		child.appendChild(getCvParam(document,
			"Contact.submitter.cvRef",
			"Contact.submitter.accession",
			"Contact.submitter.name"));
		child.appendChild(getCvParam(document,
			"Contact.name.cvRef",
			"Contact.name.accession",
			"Contact.name.name", user.get("realname")));
		child.appendChild(getCvParam(document,
			"Contact.email.cvRef",
			"Contact.email.accession",
			"Contact.email.name", user.get("email")));
		child.appendChild(getCvParam(document,
			"Contact.affiliation.cvRef",
			"Contact.affiliation.accession",
			"Contact.affiliation.name", user.get("organization")));
		element.appendChild(child);
		// add <Contact> node for principal investigator
		child = document.createElement("Contact");
		attribute = document.createAttribute("id");
		attribute.setNodeValue("principal_investigator");
		child.setAttributeNode(attribute);
		child.appendChild(getCvParam(document,
			"Contact.pi.cvRef",
			"Contact.pi.accession",
			"Contact.pi.name"));
		child.appendChild(getCvParam(document,
			"Contact.name.cvRef",
			"Contact.name.accession",
			"Contact.name.name", dataset.getPI()));
		element.appendChild(child);
		root.appendChild(element);
		
		// create and populate the ninth child node, <PublicationList>
		element = document.createElement("PublicationList");
		if (publications == null || publications.isEmpty()) {
			child = document.createElement("Publication");
			attribute = document.createAttribute("id");
			attribute.setNodeValue("pending");
			child.setAttributeNode(attribute);
			child.appendChild(getCvParam(document,
				"Publication.pending.cvRef",
				"Publication.pending.accession",
				"Publication.pending.name"));
			element.appendChild(child);
		} else {
			i = 1;
			for (Publication publication : publications) {
				child = document.createElement("Publication");
				// set publication's ID
				String id = null;
				String pmid = publication.getPMID();
				// PubMed ID might be empty, but not null; treat both the same
				if (pmid.trim().equals(""))
					pmid = null;
				if (pmid == null) {
					id = String.format("Unpublished_Paper_%d", i);
					i++;
				} else id = "PMID" + pmid;
				attribute = document.createAttribute("id");
				attribute.setNodeValue(id);
				child.setAttributeNode(attribute);
				if (pmid != null)
					child.appendChild(getCvParam(document,
						"Publication.pmid.cvRef",
						"Publication.pmid.accession",
						"Publication.pmid.name", pmid));
				child.appendChild(getCvParam(document,
					"Publication.reference.cvRef",
					"Publication.reference.accession",
					"Publication.reference.name",
					publication.getFormattedReferenceString()));
				element.appendChild(child);
			}
		}
		root.appendChild(element);
		
		// create and populate the tenth child node, <KeywordList>
		element = document.createElement("KeywordList");
		// TODO: put keywords in the database, retrieve
		// them directly from the Dataset object
		String keywordList = WorkflowParameterUtils.getParameter(
			taskParameters, "dataset.keywords");
		// write "MassIVE" as a keyword if none were specified by the user
		if (keywordList == null || keywordList.trim().equals(""))
			element.appendChild(getCvParam(document,
				"KeywordList.keyword.cvRef",
				"KeywordList.keyword.accession",
				"KeywordList.keyword.name", "MassIVE"));
		else for (String keyword : keywordList.split(";")) {
			element.appendChild(getCvParam(document,
				"KeywordList.keyword.cvRef",
				"KeywordList.keyword.accession",
				"KeywordList.keyword.name", keyword));
		}
		root.appendChild(element);
		
		// create and populate the eleventh child node, <FullDatasetLinkList>
		element = document.createElement("FullDatasetLinkList");
		// add <FullDatasetLink> node for MassIVE web URL
		child = document.createElement("FullDatasetLink");
		child.appendChild(getCvParam(document,
			"FullDatasetLink.web.cvRef",
			"FullDatasetLink.web.accession",
			"FullDatasetLink.web.name", getDatasetWebURL(task)));
		element.appendChild(child);
		// add <FullDatasetLink> node for MassIVE FTP URL
		child = document.createElement("FullDatasetLink");
		child.appendChild(getCvParam(document,
			"FullDatasetLink.ftp.cvRef",
			"FullDatasetLink.ftp.accession",
			"FullDatasetLink.ftp.name", getDatasetFTPURL(dataset)));
		element.appendChild(child);
		root.appendChild(element);
		
		return document;
	}
	
	public static File writeProteomeXchangeXMLFile(
		Document document, Dataset dataset
	) {
		if (document == null || dataset == null)
			return null;
		// get dataset task
		Task task = dataset.getTask();
		// write document to task directory
		File file = task.getPath(String.format(
			"ProteomeXchange/%s.xml", dataset.getDatasetIDString()));
		PrintWriter writer = null;
		try {
			writer = new PrintWriter(file);
			writer.print(FileIOUtils.printXML(document));
			return file;
		} catch (Exception error) {
			getLogger().error("Cannot create parameter file", error);
			return null;
		} finally {
			if (writer != null)
				writer.close();
		}
	}
	
	public static boolean processProteomeXchangeXML(
		File xmlFile, boolean forceTest, boolean submit
	) {
		if (xmlFile == null || xmlFile.canRead() == false)
			return false;
		// determine which operation is being done, for log messages
		String operation = null;
		if (submit)
			operation = "submit";
		else operation = "validate";
		// web service URL
		String url = properties.getProperty("px.service.url");
		if (url == null) {
			logger.error("Could not find URL for ProteomeXchange web service.");
			return false;
		}
		// get PX web service parameters from properties
		List<NameValuePair> parameters = getBasicPXParameters(forceTest);
		// web service method - "validateXML" or "submitDataset",
		// depending on the argument mode
		if (submit)
			parameters.add(new BasicNameValuePair("method", "submitDataset"));
		else parameters.add(new BasicNameValuePair("method", "validateXML"));
		// add PX XML file and other parameters to the POST request
		MultipartEntityBuilder multipart = MultipartEntityBuilder.create();
		multipart.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
		multipart.addPart("ProteomeXchangeXML", new FileBody(xmlFile));
		for (NameValuePair parameter : parameters)
			multipart.addTextBody(parameter.getName(), parameter.getValue());
		// set up and launch web service HTTP request
		CloseableHttpClient client = null;
		BufferedReader reader = null;
		try {
			client = HttpClientBuilder.create().build();
			HttpPost post = new HttpPost(url);
			post.setEntity(multipart.build());
			HttpResponse response = client.execute(post);
			int status = response.getStatusLine().getStatusCode();
			String content = EntityUtils.toString(response.getEntity());
			// report web service response
			StringBuffer report =
				new StringBuffer("Invoked ProteomeXchange web service at [");
			report.append(url);
			report.append("] to ");
			report.append(operation);
			report.append(" a ProteomeXchange XML message:");
			report.append("\nGot a response of ");
			report.append(status);
			report.append(", with the following content:\n----------\n");
			report.append(content);
			report.append("\n----------");
			logger.info(report.toString());
			// if status was 200, parse the output to get the result
			if (status == 200) {
				reader = new BufferedReader(new InputStreamReader(
					new ByteArrayInputStream(content.getBytes())));
				String line = null;
				while ((line = reader.readLine()) != null) {
					Matcher matcher = PX_RESULT_PATTERN.matcher(line);
					if (matcher.matches()) {
						String result = matcher.group(1);
						if (result != null &&
							result.equalsIgnoreCase("SUCCESS"))
							return true;
						else return false;
					}
				}
				// if no result line was found in the response, return false
				return false;
			}
			// otherwise, report the error message
			else throw new WebServiceException(String.format(
				"Error invoking PX web service to %s ProteomeXchange " +
				"XML:\nStatus Code = %d\nResponse = [%s]",
				operation, status, content));
		} catch (Throwable error) {
			logger.error(error.getMessage(), error);
			return false;
		} finally {
			try { client.close(); } catch (Throwable error) {}
			try { reader.close(); } catch (Throwable error) {}
		}
	}
	
	public static File getProteomeXchangeXMLFile(Dataset dataset) {
		if (dataset == null)
			return null;
		
		// get basic web service parameters
		String datasetID = dataset.getDatasetIDString();
		boolean forceTest = false;
		if ("test".equalsIgnoreCase(dataset.getOwner()))
			forceTest = true;
		
		// ensure that this dataset has a ProteomeXchange ID
		String pxID = dataset.getAnnotation("px_accession");
		if (pxID == null) {
			pxID = DatasetPublisher.getProteomeXchangeID(false, forceTest);
			if (pxID == null) {
				logger.error(String.format("There was an error retrieving a " +
					"ProteomeXchange ID for dataset [%s].", datasetID));
				return null;
			} else if (insertProteomeXchangeID(dataset.getDatasetID(), pxID)
				== false) {
				logger.error(String.format("There was an error inserting " +
					"ProteomeXchange ID [%s] into the database record " +
					"for dataset [%s].", pxID, datasetID));
				return null;
			} else dataset.setAnnotation("px_accession", pxID);
		}
		
		// generate ProteomeXchange XML for this dataset
		Document pxXML = null;
		try {
			pxXML = DatasetPublisher.generateProteomeXchangeXML(dataset, pxID);
		} catch (Throwable error) {
			logger.error(String.format("There was an error generating a " +
				"ProteomeXchange XML message for dataset [%s].", datasetID),
				error);
			return null;
		}
		
		// validate generated ProteomeXchange XML
		if (pxXML == null) {
			logger.error(String.format("There was an error generating a " +
				"ProteomeXchange XML message for dataset [%s].", datasetID));
			return null;
		} else return writeProteomeXchangeXMLFile(pxXML, dataset);
	}
	
	public static boolean publishDatasetToProteomeXchange(Dataset dataset) {
		if (dataset == null)
			return false;
		
		// get basic web service parameters
		String datasetID = dataset.getDatasetIDString();
		boolean forceTest = false;
		if ("test".equalsIgnoreCase(dataset.getOwner()))
			forceTest = true;
		
		// generate ProteomeXchange XML file for this dataset
		File pxFile = getProteomeXchangeXMLFile(dataset);
		if (pxFile == null || pxFile.canRead() == false) {
			logger.error(String.format("There was an error writing the " +
				"generated ProteomeXchange XML message for dataset [%s] " +
				"to a task file.", datasetID));
			return false;
		} else if (processProteomeXchangeXML(pxFile, forceTest, false)
			== false) {
			logger.error(String.format("The generated ProteomeXchange XML " +
				"message for dataset [%s] did not pass validation.",
				datasetID));
			return false;
		}
		
		// submit validated XML to ProteomeXchange
		if (processProteomeXchangeXML(pxFile, forceTest, true) == false) {
			logger.error(String.format("There was an error publishing the " +
				"generated ProteomeXchange XML message for dataset [%s].",
				datasetID));
			return false;
		} else return true;
	}
	
	public static boolean isValidPXID(String pxID) {
		if (pxID == null)
			return false;
		else return PX_ID_PATTERN.matcher(pxID).matches();
	}
	
	public static String getDatasetWebURL(Task task) {
		if (task == null)
			return null;
		String webURL = WebAppProps.get(
			"livesearch.site.url", "http://massive.ucsd.edu/ProteoSAFe");
		// append trailing slash, if one is not already present
		if (webURL.charAt(webURL.length() - 1) != '/')
			webURL += "/";
		// append status page URL portion
		webURL += "status.jsp?task=" + task.getID();
		return webURL;
	}
	
	public static String getDatasetFTPURL(Dataset dataset) {
		if (dataset == null)
			return null;
		String id = dataset.getDatasetIDString();
		String ftpURL = "ftp://";
		// if the dataset is private, prepend its ID as the FTP username
		if (dataset.isPrivate())
			ftpURL += id + "@";
		ftpURL += WebAppProps.get(
			"livesearch.ftp.host.address", "massive.ucsd.edu");
		// if the dataset is public, append its ID as the FTP root directory
		if (dataset.isPrivate() == false)
			ftpURL += "/" + id;
		return ftpURL;
	}
	
	/*========================================================================
	 * Convenience methods
	 *========================================================================*/
	private static List<NameValuePair> getBasicPXParameters(boolean forceTest) {
		// get PX web service parameters from properties
		List<NameValuePair> parameters = new ArrayList<NameValuePair>(6);
		// MassIVE partner repository username
		String partner = properties.getProperty("px.service.partner");
		if (partner == null) {
			logger.error(
				"Could not find username for ProteomeXchange web service.");
			return null;
		} else parameters.add(new BasicNameValuePair("PXPartner", partner));
		// MassIVE partner repository password
		String password = properties.getProperty("px.service.authentication");
		if (password == null) {
			logger.error(
				"Could not find password for ProteomeXchange web service.");
			return null;
		} else parameters.add(
			new BasicNameValuePair("authentication", password));
		// web service test mode
		String testMode = null;
		if (forceTest)
			testMode = "yes";
		else testMode = properties.getProperty("px.service.test", "no");
		parameters.add(new BasicNameValuePair("test", testMode));
		// web service verbosity
		parameters.add(new BasicNameValuePair("verbose",
			properties.getProperty("px.service.verbose", "no")));
		return parameters;
	}
	
	private static Element getCvParam(
		Document document, String cvRef, String accession, String name
	) {
		return getCvParam(document, cvRef, accession, name, null);
	}
	
	private static Element getCvParam(
		Document document, String cvRef, String accession, String name,
		String value
	) {
		if (document == null || cvRef == null ||
			accession == null || name == null)
			return null;
		Element cvParam = document.createElement("cvParam");
		Attr attribute = document.createAttribute("cvRef");
		attribute.setNodeValue(properties.getProperty(cvRef));
		cvParam.setAttributeNode(attribute);
		attribute = document.createAttribute("accession");
		attribute.setNodeValue(properties.getProperty(accession));
		cvParam.setAttributeNode(attribute);
		attribute = document.createAttribute("name");
		attribute.setNodeValue(properties.getProperty(name));
		cvParam.setAttributeNode(attribute);
		if (value != null) {
			attribute = document.createAttribute("value");
			attribute.setNodeValue(value);
			cvParam.setAttributeNode(attribute);
		}
		return cvParam;
	}
}
