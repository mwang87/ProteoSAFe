package edu.ucsd.livesearch.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import edu.ucsd.livesearch.account.AccountManager;
import edu.ucsd.saint.commons.xml.Wrapper;
import edu.ucsd.saint.commons.xml.XmlUtils;

/**
 * Servlet implementation class AuthenticateUser
 */
public class AuthenticateUser extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private Logger logger = LoggerFactory.getLogger(AuthenticateUser.class);
       
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		AccountManager manager = AccountManager.getInstance();
		Wrapper D = new Wrapper(XmlUtils.createXML());
		Element authenticated = D.E("authenticated", D.T("false"));
		Element profile = D.E("user-profile", authenticated);;
		try{
			String user = request.getParameter("user");
			String password = request.getParameter("password");
			if(manager.authenticate(user, password)){
				authenticated.setTextContent("true");
//				for(Entry<String, String> entry: manager.getProfile(user).entrySet())
//					profile.appendChild(
//						D.E(entry.getKey(), D.T(entry.getValue())));
			}
		}
		catch(Exception e){
			logger.error("Failed to authenticate user", e);
		}
		response.setContentType("text/xml");
		XmlUtils.printXML(profile, response.getOutputStream());
	}
}
