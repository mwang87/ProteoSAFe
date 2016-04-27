package edu.ucsd.liveadmin;

import java.io.File;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;

import edu.ucsd.saint.commons.ConnectionPool;
import edu.ucsd.saint.commons.WebAppProps;

public class Commons {

	private static final Logger logger = LoggerFactory.getLogger(Commons.class);
	private static final ConnectionPool connPool;

	static{
		System.setProperty(DOMImplementationRegistry.PROPERTY,
			"org.apache.xerces.dom.DOMXSImplementationSourceImpl");
		logger.info("Begin initializing LiveAdmin Commons");
		connPool = new ConnectionPool(
				"java:comp/env/" + WebAppProps.get("liveadmin.jdbc.main"));		
		logger.info("End initializing LiveAdmin Commons");
	}
	

	public static ConnectionPool getConnectionPool(){
		return connPool;
	}


	public static void executeProcess(File directory, PrintStream out, ProcessBuilder pb)
	throws IOException{
		pb.redirectErrorStream(true);
		if(directory != null)
			pb.directory(directory);
		Process proc = pb.start();
		Scanner scanner = new Scanner(proc.getInputStream());
		if(out!= null)
			while(scanner.hasNextLine()){
				out.println(scanner.nextLine());
			}
		else while(scanner.hasNextLine())
			scanner.nextLine();
	}
}
