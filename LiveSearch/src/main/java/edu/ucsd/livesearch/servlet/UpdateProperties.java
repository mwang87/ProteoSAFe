package edu.ucsd.livesearch.servlet;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.ucsd.saint.commons.WebAppProps;
import edu.ucsd.saint.commons.xml.Wrapper;
import edu.ucsd.saint.commons.xml.XmlUtils;

/**
 * Servlet implementation class UpdateProperties
 */
public class UpdateProperties extends HttpServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3980561752227358036L;

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		boolean successful = false;
		if(ServletUtils.isAuthenticated(request.getSession()))
			successful = WebAppProps.loadProperties();
		response.setContentType("text/xml");
		Wrapper D = new Wrapper();
		XmlUtils.printXML(D.E("update-properties", D.E("updated", Boolean.toString(successful))),
			response.getOutputStream());
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}

}
