package edu.ucsd.liveflow;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;

public class Commons {

    final static private Logger logger;
 

	static{
		System.setProperty(DOMImplementationRegistry.PROPERTY,
			"org.apache.xerces.dom.DOMXSImplementationSourceImpl");
		logger = LoggerFactory.getLogger(Commons.class);		
		logger.info("Begin initializing LiveFlow Commons");
		logger.info("End initializing LiveFlow Commons");
	}
	

	public static void closeStream(OutputStream out){
		try{
			if(out != null) out.close();
		} catch(IOException e){
			logger.error("Failed to close output stream");
		}
	}

	public static void closeStream(InputStream in){
		try{
			if(in != null) in.close();
		} catch(IOException e){
			logger.error("Failed to close input stream");
		}
	}
}
