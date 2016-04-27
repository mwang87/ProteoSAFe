package edu.ucsd.liveflow.servlet;





import java.util.Collection;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import dapper.server.ServerProcessor.FlowProxy;
import dapper.server.flow.FlowNode;
import dapper.server.flow.FlowStatus;
import dapper.server.flow.LogicalNode;
import dapper.server.flow.LogicalNodeStatus;

import edu.ucsd.liveflow.FlowEngineFacade;
import edu.ucsd.liveflow.FlowEngineFacade.WorkflowRecord;
import edu.ucsd.saint.commons.xml.Wrapper;
import edu.ucsd.saint.commons.xml.XmlUtils;

public class QueryReadyNodes extends HttpServlet {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -1459032105895722229L;
	private final Logger logger = LoggerFactory.getLogger(QueryReadyNodes.class);

	/**
	 * 
	 */

	public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException{
		logger.debug(String.format("Query ready nodes"));
		Wrapper d = new Wrapper(XmlUtils.createXML());
		Collection<WorkflowRecord> records =
			FlowEngineFacade.getFacade().getWorkflows();
		Element tasks = d.E("tasks"); 
		try{
			res.setContentType("text/xml");
			for(WorkflowRecord record: records){
				FlowProxy proxy = record.getProxy();
				if(proxy.getFlow().getStatus() != FlowStatus.EXECUTE)
					continue;
				int count = 0;
				Element taskEmt = d.E("task",
						d.A("id", record.getTask()),
						d.A("flow", record.getFlow()));			
				for(LogicalNode node: proxy.getFlow().getNodes()){
					int remaining = node.getDependencyCountDown().getRemaining().size();
					LogicalNodeStatus status = node.getStatus();
					if(status == LogicalNodeStatus.PENDING_EXECUTE ||
						(status == LogicalNodeStatus.PENDING_DEPENDENCY && remaining == 0)
					)
						for(FlowNode n: node.getFlowNodes()){
							taskEmt.appendChild(
								d.E("node", d.A("name", n.getName())));
							count++;
						}
				}
				taskEmt.setAttribute("count", Integer.toString(count));
				tasks.appendChild(taskEmt);
			}
			XmlUtils.printXML(tasks, res.getOutputStream());
		}
		catch(Exception e){
			logger.error(String.format( 
				"Failed to query nodes"), e);
		}

	}
	
	public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException{
		doGet(req, res);
	}
}
