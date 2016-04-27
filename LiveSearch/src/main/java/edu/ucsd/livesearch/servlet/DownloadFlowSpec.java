package edu.ucsd.livesearch.servlet;


import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Scanner;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsd.saint.commons.WebAppProps;

public class DownloadFlowSpec extends HttpServlet{
	/**
	 * 
	 */
	private static final long serialVersionUID = -8304973827611755034L;
	
	private static final Logger logger = LoggerFactory.getLogger(DownloadFlowSpec.class); 

	public void doGet(HttpServletRequest req, HttpServletResponse res)
		throws ServletException, IOException {
		String flow = req.getParameter("flow");
		String type = req.getParameter("spec");
		File spec = new File(WebAppProps.getPath("livesearch.flow.spec.path"), flow);
		spec = new File(spec, type + ".xml");
		//TODO: verify that the filename is legal and safe to prevent filename injection
		if(spec.exists()){
			res.setContentType("text/xml");
			res.addHeader("Content-Disposition","attachment; filename=\"" + spec.getName() + "\"");
			Scanner scanner = null;
			String downloadUrl = String.format("http://%s:%d%s/Download",
				req.getServerName(), req.getServerPort(), req.getContextPath());
			String uploadUrl = String.format("http://%s:%d%s/Upload",
					req.getServerName(), req.getServerPort(), req.getContextPath());
			try{
				scanner = new Scanner(spec);
				PrintStream printer = new PrintStream(res.getOutputStream());
				while(scanner.hasNextLine()){
					String line = scanner.nextLine()
						.replaceAll(
							"\\{livesearch\\.download\\}", downloadUrl)
						.replaceAll("\\{livesearch\\.upload\\}", uploadUrl);
					printer.println(line);
				}
			}
			catch(Exception e){
				logger.error("Failed to write specification [{}] to outputstream", spec.getAbsolutePath());
			}
			finally{
				if(scanner != null)
					scanner.close();
			}
		}
	}
	  
	public void doPost(HttpServletRequest req, HttpServletResponse res)
			throws ServletException, IOException {
		doGet(req, res);
	}
}
