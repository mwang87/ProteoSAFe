package edu.ucsd.ccms.flow.core;

import java.net.URL;
import java.util.Date;
import java.util.List;

import org.apache.log4j.xml.DOMConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import dapper.codelet.Codelet;
import dapper.codelet.CodeletUtilities;
import dapper.codelet.InputHandleResource;
import dapper.codelet.OutputHandleResource;
import dapper.codelet.Resource;
import edu.ucsd.ccms.flow.action.Action;
import edu.ucsd.ccms.flow.action.DownloadAction;
import edu.ucsd.ccms.flow.action.ToolExecAction;
import edu.ucsd.ccms.flow.action.UploadAction;
import edu.ucsd.saint.commons.xml.XmlUtils;

public class ActionExec implements Codelet{

	private final Logger logger = LoggerFactory.getLogger(ActionExec.class);

	static{
		URL log4j = ActionExec.class.getClassLoader().getResource("log4j.xml");
		if(log4j != null)
			DOMConfigurator.configure(log4j);
	}

	public void run(List<Resource> in, List<Resource> out, Node parameters)
		throws Exception{
		logger.info("{}: ActionExec", new Date());
		try{
			List<InputHandleResource> input = CodeletUtilities.filter(in, InputHandleResource.class);
			List<OutputHandleResource> output = CodeletUtilities.filter(out, OutputHandleResource.class);
	
			Action action = null;
			Element params = (Element)parameters;
			Element binding = XmlUtils.getElement(params, "bind");
			String type = binding.getAttribute("type");
	
			if(!binding.getAttribute("tool").isEmpty())
				action = new ToolExecAction(input, output, params);
			else if(type.equals("download"))
				action = new DownloadAction(input, output, params);
			else if(type.equals("upload"))
				action = new UploadAction(input, output, params);
			if(action != null) action.run();
		}
		catch(Exception e){
			logger.info("Exception running [ActionExec]", e);
			throw e;
		}
	}
}
