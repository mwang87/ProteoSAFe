package edu.ucsd.livesearch.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.TreeSet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsd.livesearch.util.Commons;
import edu.ucsd.saint.commons.ConnectionPool;
import edu.ucsd.saint.commons.http.HttpParameters;

@SuppressWarnings("serial")
public class QuerySpecies
extends BaseServlet
{
	/*========================================================================
	 * Constants
	 *========================================================================*/
	private static final Logger logger =
		LoggerFactory.getLogger(QuerySpecies.class);
	
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	/**
	 * <p>Accepts and processes queries for species data from the NCBI taxonomy
	 * ontology.
	 * 
	 * <p>By convention, a GET request to this servlet is assumed to be a
	 * request to read data only.  No creation, update, or deletion of
	 * server resources is handled by this method.
	 * 
	 * <p>This method implements the <code>dojox.data.FileStore</code>
	 * protocol defined at
	 * <code>http://docs.dojocampus.org/dojox/data/FileStore/protocol</code>.
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
		PrintWriter out = response.getWriter();
		
		// prepare JSON output
		StringBuffer output = new StringBuffer("[");
		
		// retrieve query term
		String term = parameters.getParameter("term");
		if (term == null) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST,
				"You must specify a valid query term to search species.");
			return;
		}
		
		// perform query
		Collection<Species> query = querySpecies(term);
		if (query != null) {
			for (Species species : query)
				output.append("\n\t").append(species.getJSON()).append(",");
		}
		
		// chomp trailing comma, if present, and close the JSON output
		if (output.charAt(output.length() - 1) == ',') {
			output.setLength(output.length() - 1);
			output.append("\n");
		}
		output.append("]");
			
		// write JSON to the servlet output stream
		response.setContentType("application/json");
		response.setContentLength(output.length());
		out.print(output.toString());
	}
	
	public static Collection<Species> querySpecies(String term) {
		Connection connection = null;
		PreparedStatement statement = null;
		ResultSet result = null;
		ConnectionPool pool = null;
		Collection<Species> species =
			new TreeSet<Species>(new SpeciesComparator());
		try {
			// initialize database connection
			pool = Commons.getConnectionPool();
			connection = pool.aquireConnection();
			// query species
			StringBuffer query = new StringBuffer(
				"SELECT ncbi_taxid, scientific_name, common_name FROM species");
			if (term != null) {
				query.append(" WHERE scientific_name LIKE '%")
					.append(term).append("%'").append(" OR common_name LIKE '%")
					.append(term).append("%'");
			}
			statement = connection.prepareStatement(query.toString());
			result = statement.executeQuery();
			while (result.next()) {
				species.add(new Species(
					result.getString("ncbi_taxid"),
					result.getString("scientific_name"),
					result.getString("common_name"))
				);
			}
		} catch (Throwable error) {
			logger.error(
				"Failed to query species from the NCBI taxonomy ontology",
				error);
			return null;
		} finally {
			pool.close(result, statement, connection);
		}
		// only return initialized collection if it's not empty
		if (species == null || species.isEmpty())
			return null;
		else return species;
	}
	
	public static Species querySpecies(Integer ncbiTaxID) {
		if (ncbiTaxID == null)
			return null;
		Connection connection = null;
		PreparedStatement statement = null;
		ResultSet result = null;
		ConnectionPool pool = null;
		try {
			// initialize database connection
			pool = Commons.getConnectionPool();
			connection = pool.aquireConnection();
			// query species
			statement = connection.prepareStatement(
				"SELECT ncbi_taxid, scientific_name, common_name " +
				"FROM species " +
				"WHERE ncbi_taxid=?"
			);
			statement.setString(1, "NCBITaxon:" + ncbiTaxID);
			result = statement.executeQuery();
			if (result.next() == false)
				return null;
			else return new Species(
				result.getString("ncbi_taxid"),
				result.getString("scientific_name"),
				result.getString("common_name")
			);
		} catch (Throwable error) {
			logger.error(String.format("There was an error querying the " +
				"NCBI taxonomy ontology for species \"NCBITaxon:%s\".",
				ncbiTaxID), error);
			return null;
		} finally {
			pool.close(result, statement, connection);
		}
	}
	
	public static Integer extractNCBITaxID(String speciesCVTerm) {
		if (speciesCVTerm == null)
			return null;
		Integer ncbiTaxID = null;
		try {
			// check "NEWT" prefix for legacy entries
			if (speciesCVTerm.startsWith("NEWT:"))
				ncbiTaxID = Integer.parseInt(speciesCVTerm.substring(5));
			else if (speciesCVTerm.startsWith("NCBITaxon:"))
				ncbiTaxID = Integer.parseInt(speciesCVTerm.substring(10));
		} catch (NumberFormatException error) {}
		return ncbiTaxID;
	}
	
	/*========================================================================
	 * Convenience classes
	 *========================================================================*/
	/**
	 * Struct to maintain fields of a single species entry
	 * from the NCBI taxonomy ontology
	 */
	public static class Species {
		/*====================================================================
		 * Properties
		 *====================================================================*/
		private String id;
		private String scientificName;
		private String commonName;
		
		/*====================================================================
		 * Constructors
		 *====================================================================*/
		public Species(String id, String scientificName, String commonName) {
			if (id == null)
				throw new NullPointerException(
					"NCBI taxonomy ID cannot be null.");
			else if (scientificName == null)
				throw new NullPointerException(
					"Species scientific name cannot be null.");
			this.id = id;
			this.scientificName = scientificName;
			this.commonName = commonName;
		}
		
		/*====================================================================
		 * Public interface methods
		 *====================================================================*/
		@Override
		public boolean equals(Object other) {
			if (other == null || other instanceof Species == false)
				return false;
			else return toString().equals(other.toString());
		}
		
		@Override
		public int hashCode() {
			return toString().hashCode();
		}
		
		@Override
		public String toString() {
			return getJSON();
		}
		
		/*====================================================================
		 * Property accessor methods
		 *====================================================================*/
		public String getId() {
			return id;
		}
		
		public String getScientificName() {
			return scientificName;
		}
		
		public String getCommonName() {
			return commonName;
		}
		
		public String getJSON() {
			// initialize this species entry as a JSON hash
			StringBuffer json = new StringBuffer("{");
			// add NCBI taxonomy ID as the hash's "value" property
			json.append("\"value\":\"").append(getId()).append("\"");
			// add scientific name as the hash's "name" and "label" properties
			json.append(",\"name\":\"")
				.append(getScientificName()).append("\"");
			json.append(",\"label\":\"")
				.append(getScientificName()).append("\"");
			// if present, append common name as the hash's "desc" property
			String commonName = getCommonName();
			if (commonName != null)
				json.append(",\"desc\":\"").append(commonName).append("\"");
			// close the JSON hash and return it
			json.append("}");
			return json.toString();
		}
	}
	
	/**
	 * Comparator to ensure that species are returned in the proper order
	 */
	public static class SpeciesComparator
	implements Comparator<Species> {
		/*====================================================================
		 * Constants
		 *====================================================================*/
		public static final String NCBI_HUMAN_TAXID = "NCBITaxon:9606";
		
		/*====================================================================
		 * Comparator methods
		 *====================================================================*/
		public int compare(Species s1, Species s2) {
			if (s1 == null && s2 == null)
				return 0;
			else if (s1 == null)
				return -1;
			else if (s2 == null)
				return 1;
			// special case for human species, which should always be first
			String id1 = s1.getId();
			if (id1 == null || id1.equalsIgnoreCase(NCBI_HUMAN_TAXID))
				return -1;
			String id2 = s2.getId();
			if (id2 == null || id2.equalsIgnoreCase(NCBI_HUMAN_TAXID))
				return 1;
			// otherwise just sort by ID
			Integer code1 = extractNCBITaxID(id1);
			if (code1 == null)
				return -1;
			Integer code2 = extractNCBITaxID(id2);
			if (code2 == null)
				return 1;
			else return code1 - code2;
		}
	}
}
