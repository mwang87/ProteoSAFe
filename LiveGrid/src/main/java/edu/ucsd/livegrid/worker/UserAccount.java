package edu.ucsd.livegrid.worker;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import edu.ucsd.saint.commons.xml.XmlUtils;

public class UserAccount extends GridWorker {
	private String owner;
	private Logger logger = LoggerFactory.getLogger(UserAccount.class);
	
	public UserAccount(File policyFolder){
		super(policyFolder);
		owner = null;
		Document policy = getPolicySpec(policyFolder);
		Element emtOwner = XmlUtils.getElement(policy, "owner");
		if(emtOwner != null)
			owner = emtOwner.getTextContent();
		logger.info("User grid policy for [{}] is loaded", owner);
	}
	
	public String getOwner(){
		return owner;
	}
}
