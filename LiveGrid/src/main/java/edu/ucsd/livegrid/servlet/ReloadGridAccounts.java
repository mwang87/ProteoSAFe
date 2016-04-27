package edu.ucsd.livegrid.servlet;


import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.w3c.dom.Element;

import edu.ucsd.livegrid.GridPlanner;
import edu.ucsd.saint.commons.xml.Wrapper;
import edu.ucsd.saint.commons.xml.XmlUtils;

/**
 * Servlet implementation class QueryTargetGrids
 */
public class ReloadGridAccounts extends HttpServlet {
       
    /**
	 * 
	 */
	private static final long serialVersionUID = -6880125221975657682L;

	/**
     * @see HttpServlet#HttpServlet()
     */
    public ReloadGridAccounts() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		GridPlanner planner = GridPlanner.getInstance();
		boolean successful = planner.discoverWorkers();
		Wrapper D = new Wrapper();
		Element result = D.E("reload-accounts",
			D.E("successful", Boolean.toString(successful)));
		response.setContentType("text/xml");
		XmlUtils.printXML(result, response.getOutputStream());
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}
}
