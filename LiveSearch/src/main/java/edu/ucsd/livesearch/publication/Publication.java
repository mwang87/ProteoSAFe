package edu.ucsd.livesearch.publication;

import org.apache.commons.lang3.StringEscapeUtils;

public class Publication
{
	/*========================================================================
	 * Properties
	 *========================================================================*/
	private Integer id;
	private String  authors;
	private String  title;
	private String  citation;
	private String  abs;
	private String  pmid;
	private String  pmcid;
	
	/*========================================================================
	 * Constructors
	 *========================================================================*/
	public Publication(String authors, String title) {
		this(null, authors, title, null, null, null, null);
	}
	
	public Publication(
		Integer id, String authors, String title, String citation,
		String abs, String pmid, String pmcid
	) throws NullPointerException, IllegalArgumentException {
		// set database ID
		setId(id);
		// set required article information - authors and title
		setAuthors(authors);
		setTitle(title);
		// set optional article information: citation, abstract and PubMed IDs
		setCitation(citation);
		setAbstract(abs);
		setPMID(pmid);
		setPMCID(pmcid);
	}
	
	/*========================================================================
	 * Property accessor methods
	 *========================================================================*/
	public Integer getId() {
		return id;
	}
	
	public void setId(Integer id) {
		this.id = id;
	}
	
	public String getAuthors() {
		return authors;
	}
	
	public void setAuthors(String authors) {
		if (authors == null)
			throw new NullPointerException("\"authors\" cannot be null.");
		else this.authors = authors;
	}
	
	public String getTitle() {
		return title;
	}
	
	public void setTitle(String title) {
		if (title == null)
			throw new NullPointerException("\"title\" cannot be null.");
		else this.title = title;
	}
	
	public String getCitation() {
		return citation;
	}
	
	public void setCitation(String citation) {
		this.citation = citation;
	}
	
	public String getAbstract() {
		return abs;
	}
	
	public void setAbstract(String abs) {
		this.abs = abs;
	}
	
	public String getPMID() {
		return pmid;
	}
	
	public void setPMID(String pmid) {
		this.pmid = pmid;
	}

	public String getPMCID() {
		return pmcid;
	}

	public void setPMCID(String pmcid) {
		this.pmcid = pmcid;
	}
	
	public String getFormattedReferenceString() {
		// authors and title
		StringBuffer reference = new StringBuffer(appendDot(getAuthors()));
		reference.append(" ").append(appendDot(getTitle()));
		// article citation
		String citation = getCitation();
		if (citation != null)
			reference.append(" ").append(appendDot(citation));
		return reference.toString();
	}
	
	public String getHTMLFormattedReferenceString() {
		// authors and title
		StringBuffer reference =
			new StringBuffer("<span style=\"font-weight:bold;\">");
		reference.append(appendDot(getAuthors()));
		reference.append("</span><br/><span style=\"font-style:italic;\">");
		reference.append(appendDot(getTitle()));
		reference.append("</span>");
		// article citation
		String citation = getCitation();
		if (citation != null)
			reference.append("<br/>").append(appendDot(citation));
		return reference.toString();
	}
	
	public String getJSON() {
		// initialize this publication as a JSON hash
		StringBuffer json = new StringBuffer("{");
		// add "id" property
		json.append("\"id\":\"").append(getId()).append("\"");
		// add "authors" property
		json.append(",\"authors\":\"").append(
			StringEscapeUtils.escapeJson(getAuthors())).append("\"");
		// add "title" property
		json.append(",\"title\":\"").append(
			StringEscapeUtils.escapeJson(getTitle())).append("\"");
		// add "citation" property
		json.append(",\"citation\":\"").append(
			StringEscapeUtils.escapeJson(getCitation())).append("\"");
		// add "abstract" property
		json.append(",\"abstract\":\"").append(
			StringEscapeUtils.escapeJson(getAbstract())).append("\"");
		// add "pmid" property
		json.append(",\"pmid\":\"").append(getPMID()).append("\"");
		// add "pmcid" property
		json.append(",\"pmcid\":\"").append(getPMCID()).append("\"");
		// close the JSON hash and return it
		json.append("}");
		return json.toString();
	}
	
	@Override
	public String toString() {
		return getFormattedReferenceString();
	}
	
	/*========================================================================
	 * Convenience methods
	 *========================================================================*/
	private String appendDot(String value) {
		if (value == null)
			return null;
		else if (value.isEmpty())
			return ".";
		else if (value.charAt(value.length() - 1) == '.')
			return value;
		else return value + ".";
	}
}
