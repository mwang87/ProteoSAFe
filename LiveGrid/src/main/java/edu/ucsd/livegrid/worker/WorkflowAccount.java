package edu.ucsd.livegrid.worker;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import edu.ucsd.saint.commons.xml.XmlUtils;

public class WorkflowAccount extends GridWorker {
	public String getFlowName() {
		return flow;
	}

	private String flow;
	private Logger logger = LoggerFactory.getLogger(WorkflowAccount.class);

	public WorkflowAccount(File policyFolder) {
		super(policyFolder);
		flow = null;
		Document policy = getPolicySpec(policyFolder);
		Element emtFlow = XmlUtils.getElement(policy, "workflow-name");
		if(emtFlow != null)
			flow = emtFlow.getTextContent();
		logger.info("Workflow grid policy for [{}] is loaded", flow);
	}

	
}
