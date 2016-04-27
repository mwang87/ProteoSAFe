package edu.ucsd.ccms.flow.core;


import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import dapper.codelet.Resource;
import dapper.server.flow.EmbeddingCodelet;
import dapper.server.flow.Flow;
import dapper.server.flow.FlowEdge;
import dapper.server.flow.FlowNode;
import dapper.server.flow.HandleEdge;
import edu.ucsd.saint.commons.xml.XmlUtils;

public class Dispatcher implements EmbeddingCodelet {

	private static final Logger logger = LoggerFactory.getLogger(Dispatcher.class);

	private String nodeName;
	private String actionName;
	private Node parameters;
	private Map<String, String>arguments;

	public Dispatcher(){
		this.parameters = null;
	}

	public Node getEmbeddingParameters() {
		return this.parameters;
	}

	public void setEmbeddingParameters(Node parameters) {
		this.parameters = parameters;
	}

	public void run(List<Resource> inResources, List<Resource> outResources, Node parameters) throws Exception {
		setEmbeddingParameters(parameters);
		Thread.sleep(1000);
	}
	
	private Collection<FlowNode> instances;
	private Collection<String> needBroadcast;
	private FlowNode broadcast;

	public void build(Flow flow, List<FlowEdge> input, List<FlowNode> output) {
		Element emtParams = (Element)getEmbeddingParameters();
		Element emtAction = (Element)emtParams.getFirstChild();
		actionName = emtAction.getAttribute("name");
		arguments = new HashMap<String, String>();
		for(Element emt: XmlUtils.getElements(emtParams, "argument"))
			arguments.put(emt.getAttribute("name"), emt.getAttribute("value"));
		populateBroadcastNode(flow);
		populateNodesAndInputs(flow, input);
		populateJoinEdges(flow, output);
		populateBroadcastEdges(flow);
	}

	private void populateBroadcastNode(Flow flow){
		String task = arguments.get("task");
		String broadcastName = String.format("%s_broadcast", actionName);
		Element params = (Element)getEmbeddingParameters().cloneNode(true);
		params.setAttribute("id", broadcastName);
		broadcast = new FlowNode(Broadcast.class.getName())
			.setDomainPattern(task + "." + broadcastName)
			.setName(broadcastName)
			.setAttachment(broadcastName)
			.setParameters(params)
			.setTimeout(86400000000l);
	}

	private void populateNodesAndInputs(Flow flow, List<FlowEdge> input){
		instances = new LinkedList<FlowNode>();
		needBroadcast = new HashSet<String>();
		String task = arguments.get("task");
			
		/* multiple incoming edges may have the same name, but only one broadcast edge is required 
		 */
		
		FlowNode exec =new FlowNode(ActionExec.class.getName());

		for(FlowEdge in: input){
			String edgeName = in.getName();
			String tokens[] = edgeName.split("[,:\\.\\(\\)]");
			logger.info("Input:{}", edgeName);
			if(tokens[1].equals("true")){ //it's a multicast edge
				String name = actionName + "_" + instances.size();
				Element params = (Element)getEmbeddingParameters().cloneNode(true);
				params.setAttribute("id", name);
				FlowNode node = exec.clone()
					.setName(name)
					.setAttachment(name)
					.setParameters(params)
					.setTimeout(86400000000l)
					.setDomainPattern(task + "." + name);
				instances.add(node);
				flow.add(node);
				in.setV(node);
			}
			else{
				needBroadcast.add(edgeName);
				in.setV(broadcast);
			}
		}
	}

	private void populateJoinEdges(Flow flow,  List<FlowNode> output){
		Map<String, FlowNode>outMap = new HashMap<String, FlowNode>();
		Element params = (Element)getEmbeddingParameters();
		for(FlowNode out: output)
			outMap.put(out.getName(), out);
		for(Element out: XmlUtils.getElements(params , "edge")){
			String sinkAction	= out.getAttribute("sinkAction");
			String sinkPort		= out.getAttribute("sinkPort");
			String sourceAction = out.getAttribute("sourceAction");
			String sourcePort	= out.getAttribute("sourcePort");
			String object 		= out.getAttribute("object");
			boolean unfolding	= out.getAttribute("unfolding").equals("true");
			String edgeName = String.format("%s:%b(%s.%s, %s.%s)", object, unfolding,
					sourceAction, sourcePort, sinkAction, sinkPort);
			if(outMap.containsKey(sinkAction))
				for(FlowNode instance: instances)
					flow.add(new HandleEdge(instance, outMap.get(sinkAction))
						.setName(edgeName).setExpandOnEmbed(unfolding));
		}
	}

	private void populateBroadcastEdges(Flow flow){
		if(!needBroadcast.isEmpty()){
			flow.add(broadcast);
			for(String edgeName: needBroadcast){
				for(FlowNode i: instances){
					flow.add(new HandleEdge(broadcast, i)
							.setName(edgeName));
				}
			}
		}
	}

	@Override
	public String toString() {
		return nodeName;
	}
}
