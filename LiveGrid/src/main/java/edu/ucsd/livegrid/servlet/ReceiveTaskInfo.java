package edu.ucsd.livegrid.servlet;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsd.saint.commons.http.HttpParameters;

/**
 * Servlet implementation class for Servlet: ReceiveTaskInfo
 *
 */
 public class ReceiveTaskInfo extends javax.servlet.http.HttpServlet implements javax.servlet.Servlet {

	private static final long serialVersionUID = 9137990752324914541L;
	private static Logger logger = LoggerFactory.getLogger(ReceiveTaskInfo.class);
	
	/* (non-Java-doc)
	 * @see javax.servlet.http.HttpServlet#HttpServlet()
	 */
	public ReceiveTaskInfo() {
		super();
	}   	
	
	/* (non-Java-doc)
	 * @see javax.servlet.http.HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	}  	
	
	/* (non-Java-doc)
	 * @see javax.servlet.http.HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try{
			HttpParameters params = new HttpParameters(request, null);
			logger.info("Receive task info: {}", params.getParameter("info"));

		}
		catch(Exception e){
		}
	}   	  	    
}
 