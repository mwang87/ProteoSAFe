package edu.ucsd.livegrid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;

public class Commons {

	private static final Logger logger = LoggerFactory.getLogger(Commons.class);

	static{
		logger.info("Begin initializing LiveGrid Commons");
		System.setProperty(DOMImplementationRegistry.PROPERTY,
			"org.apache.xerces.dom.DOMXSImplementationSourceImpl");
		logger.info("End initializing LiveGrid Commons");
	}
}
