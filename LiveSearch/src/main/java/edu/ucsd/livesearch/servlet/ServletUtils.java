package edu.ucsd.livesearch.servlet;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Date;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.JspWriter;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsd.livesearch.account.AccountManager;
import edu.ucsd.livesearch.dataset.Dataset;
import edu.ucsd.livesearch.dataset.DatasetManager;
import edu.ucsd.livesearch.inspect.InspectUtils;
import edu.ucsd.livesearch.inspect.ProteogenomicsUtils;
import edu.ucsd.livesearch.pepnovo.PepnovoUtils;
import edu.ucsd.livesearch.task.Task;
import edu.ucsd.livesearch.task.TaskManager.TaskStatus;
import edu.ucsd.livesearch.util.WorkflowParameterUtils;
import edu.ucsd.saint.commons.WebAppProps;

public class ServletUtils
{
	/*========================================================================
	 * Constants
	 *========================================================================*/
	@SuppressWarnings("unused")
	private static final Logger logger =
		LoggerFactory.getLogger(ServletUtils.class);
	
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	public static String URLEncode(String s) {
		try {
			return URLEncoder.encode(s, "UTF-8");
		} catch (UnsupportedEncodingException error) {
			return null;
		}
	}
	
	public static String URLDecode(String s) {
		try {
			return URLDecoder.decode(s, "UTF-8");
		} catch (UnsupportedEncodingException error) {
			return null;
		}
	}
	
	@SuppressWarnings("unchecked")
	public static Map<String, String> extractRequestParameters(
		HttpServletRequest request
	) {
		if (request == null)
			return null;
		Map<String, String> parameters = new LinkedHashMap<String, String>();
		try {
			Enumeration<String> parameterNames =
				(Enumeration<String>)request.getParameterNames();
			while (parameterNames.hasMoreElements()) {
				String name = parameterNames.nextElement();
				String value = request.getParameter(name);
				String decoded = URLDecode(value);
				if (decoded != null)
					parameters.put(name, decoded);
				else parameters.put(name, value);
			}
		} catch (Throwable error) {
			return null;
		}
		if (parameters.isEmpty())
			return null;
		else return parameters;
	}
	
	public static boolean isAuthenticated(HttpSession session) {
		return "true".equals(session.getAttribute("livesearch.authenticated"));
	}
	
	public static boolean isAdministrator(HttpSession session) {
		String identity = (String)session.getAttribute("livesearch.user");
		AccountManager manager = AccountManager.getInstance();
		return manager.checkRole(identity, "administrator");				
	}
	
	public static boolean sameIdentity(HttpSession session, String user) {
		String identity = (String)session.getAttribute("livesearch.user");
		if(identity == null || user == null) return false;
		return identity.equals(user);
	}
	
	public static boolean isMassIVESite() {
		String site = WebAppProps.get("livesearch.site.name");
		if (site != null && site.trim().equalsIgnoreCase("MassIVE"))
			return true;
		else return false;
	}
	
	public static boolean isMassIVEHost() {
		String[] hosts = {"MassIVE", "GNPS"};
		String site = WebAppProps.get("livesearch.site.name");
		for (String host : hosts)
			if (StringUtils.containsIgnoreCase(site, host))
				return true;
		return false;
	}
	
	public static String registerGuestUser(HttpSession session) {
		String user = String.format("Guest.%s-%d",
			session.getId(), System.currentTimeMillis());
		session.setAttribute("livesearch.user", user);
		return user;
	}
	
	public static String getCurrentURL(HttpServletRequest request){
		String url = request.getRequestURL().toString();
		return URLEncode(request.getQueryString() == null ?
			url : url + '?' + request.getQueryString());
	}
	
	@SuppressWarnings("rawtypes")
	public static void printSessionInfo(JspWriter out, HttpSession session)
	throws IOException{
		Object obj = session.getAttribute("testAttribute");
		if(obj == null)
			session.setAttribute("testAttribute", 0);
		else session.setAttribute("testAttribute", (Integer)obj + 1);
		out.println("<!--");
		out.println("Session  ID: " + session.getId());
		out.println("Created  At: " + new Date(session.getCreationTime()));
		out.println("Last Access: " + new Date(session.getLastAccessedTime()));
			for(Enumeration e = session.getAttributeNames(); e.hasMoreElements(); ){
				String name = (String)e.nextElement();
				out.println(String.format("%s=[%s]%n", name, session.getAttribute(name)));
			}
		out.println("-->");
	}
	
	@SuppressWarnings("incomplete-switch")
	public static String HTMLStatusBlock(
		Task task, HttpServletRequest request, HttpSession session,
		boolean showOptions
	) {
		// deal with legacy tasks that have no specified workflow
		String workflow = task.getFlowName();
		if (workflow == null)
			workflow = "N/A";
		
		StringBuffer buffer = new StringBuffer();
		String context	= request.getContextPath();
		String taskID = task.getID();
		TaskStatus status = task.getStatus();
		
		// determine if this task represents a MassIVE dataset
		Dataset dataset = null;
		boolean isDataset = false;
		boolean isPrivate = false;
		boolean pxActivated = false;
		if (workflow.toUpperCase().startsWith("MASSIVE"))
			dataset = DatasetManager.queryDatasetByTaskID(task.getID());
		if (dataset != null) {
			isDataset = true;
			isPrivate = dataset.isPrivate();
			String pxStatus =
				WorkflowParameterUtils.getParameter(task, "dataset.px");
			if (pxStatus != null && pxStatus.equalsIgnoreCase("on"))
				pxActivated = true;
		}
		
		// status link
		buffer.append(String.format(
			"<div><a href=\"%s/status.jsp?task=%s\">%s</a></div>",
			context, taskID, status));
		
		// option links
		if (showOptions) {
			boolean isAdmin = ServletUtils.isAdministrator(session);
			boolean isOwner =
				ServletUtils.sameIdentity(session, task.getUser());
			
			// only show the "Clone" link if this is not a dataset task
			if (workflow.toUpperCase().startsWith("MASSIVE") == false) {
				buffer.append(
					"\n<div style=\"text-align: left; float: left;\">");
				buffer.append(String.format(
					"[<a href=\"%s/index.jsp?task=%s\">Clone</a>]",
					context, taskID));
				buffer.append("</div>");
			} else if (status == TaskStatus.DONE) {
				if (isDataset && (isAdmin || isOwner)) {
					buffer.append(
						"\n<div style=\"text-align: left; float: left;\">");
					// make public link
					if (isPrivate) {
						buffer.append(String.format(
							"[<a href=\"%s/PublishDataset?task=%s\"",
							context, taskID));
						buffer.append(
							" onclick=\"return confirm('Are you sure? " +
							"If you make this dataset public, then it will " +
							"become viewable and downloadable by all users");
						if (pxActivated)
							buffer.append(", and will be publicly announced " +
								"via the ProteomeXchange consortium");
						buffer.append(".')\">");
						buffer.append("Make Public</a>]&nbsp;");
					}
					// update dataset link
					buffer.append(String.format(
						"[<a href=\"%s/updateDataset.jsp?task=%s\">",
						context, taskID));
						buffer.append("Add/Update Metadata</a>]&nbsp;");
					// add publication link
					buffer.append(String.format(
						"[<a href=\"%s/addPublication.jsp?task=%s\">",
						context, taskID));
						buffer.append("Add Publication</a>]");
					buffer.append("</div>");
				}
			}
			
			// admin/owner links
			buffer.append("\n<div style=\"text-align: right; float: right;\">");
			
			// "Suspend" link
			if (status == TaskStatus.RUNNING && isAdmin)
				buffer.append(String.format(
					"\n\t[<a href=\"%s/Suspend?task=%s\">Suspend</a>]",
					context, taskID));
			
			// "Restart" link
			switch (status) {
				case UPLOADING: case QUEUED: case SUSPENDED: case DONE: case FAILED:
					if (isOwner || isAdmin)
						buffer.append(String.format(
							"[<a href=\"%s/Restart?task=%s\">Restart</a>]", 
							context, taskID));
			}
			
			// "Delete" link
			// only delete if this is not a dataset, or if it's private;
			// not even owners can delete a public dataset
			if ((isOwner && (isDataset == false || isPrivate)) || isAdmin) {
				buffer.append(String.format(
					"[<a href=\"%s/Delete?task=%s\"", context, taskID));
				buffer.append(
					" onclick=\"return confirm('Are you sure? " +
					"If you delete this task, you will be unable to "+
					"recover it.')\">");
				buffer.append("Delete</a>]");
			}
			
			// end admin/owner links
			buffer.append("</div>");
		}
		
		return buffer.toString();
	}
	
	public static String HTMLBrowsingBlock(Task task, HttpServletRequest request){
		StringBuffer buffer = new StringBuffer();
		String flow = task.getFlowName();
		TaskStatus status = task.getStatus(); 
		if(status == TaskStatus.DONE){
			buffer.append("Result: ");
			if (flow != null) {
				if (flow.matches("INSPECT|MSALIGN|MSALIGN-CONVEY|" +
						"MSCLUSTER|MSC-INSPECT|MSC-MSALIGN"))
					buffer.append(InspectUtils.generateBrowseBlock(task, request));
				else if (flow.matches("PROTEOGENOMICS"))
					buffer.append(ProteogenomicsUtils.generateBrowseBlock(task, request));
				else if (flow.matches("PEPNOVO"))
					buffer.append(PepnovoUtils.generateBrowseBlock(task, request));
				// TODO: this is a hack
				else if (flow.matches("SPEC-ARCHIVE"))
					buffer.append(
						generateSpectralArchivesBrowseBlock(task, request));
			}
		}
		return buffer.toString();
	}
	
	public static String JSLogoBlock(String logoDiv, HttpServletRequest request, HttpSession session){
		StringBuffer buffer = new StringBuffer();
		buffer.append("initLogoBlock('").append(logoDiv).append("',\n")
			.append(isAuthenticated(session)).append(",\n")
			.append(isAdministrator(session)).append(",\n")
			.append(isMassIVEHost()).append(",\n")
			.append('\'').append(request.getContextPath()).append("',\n")
			.append('\'').append(getCurrentURL(request)).append("');\n");
		return buffer.toString();
	}
	
	public static String generateSpectralArchivesBrowseBlock(
		Task task, HttpServletRequest request
	) {
		StringBuffer buffer = new StringBuffer();
		if(task.getStatus() == TaskStatus.DONE){
			String context = request.getContextPath();
			String format = 
				String.format("<a href='%s/inspect/%%s?task=%s'>%%s</a>",
					context, task.getID());
			buffer.append("[ ");
			buffer.append(
				String.format(format, "result.jsp", "group by peptide"))
				.append(" ] \n");
		}
		return buffer.toString();
	}
}
