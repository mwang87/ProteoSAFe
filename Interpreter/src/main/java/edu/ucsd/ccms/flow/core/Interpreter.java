package edu.ucsd.ccms.flow.core;


import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

import dapper.server.flow.DummyEdge;
import dapper.server.flow.Flow;
import dapper.server.flow.FlowBuilder;
import dapper.server.flow.FlowEdge;
import dapper.server.flow.FlowNode;
import dapper.server.flow.HandleEdge;
import dapper.ui.Program;
import edu.ucsd.saint.commons.xml.Wrapper;
import edu.ucsd.saint.commons.xml.XmlUtils;

@Program(arguments = { // specified a number of arguments passed in by the caller of the constructor
	"flow", // path to the flow specification
	"binding", // path to the binding specification
	"tool", // path to the workflow-specific tool specification (optional, may be null)
	"arguments" // additional arguments separated by semicolons; each in the form "[key]=[value]"
})

public class Interpreter implements FlowBuilder{

	private String flowName;
	private String workflow;
	private Document flowSpec, bindingSpec, toolSpec;
	private Map<String, String> arguments;

	private Map<String, Element> objects;
	private Map<String, Element> actions;
	private Map<String, Element> bindingsByAction;
	private Map<String, Element> bindingsByObject;

	private Map<String, FlowNode> nodes;

	private Logger logger = LoggerFactory.getLogger(Interpreter.class); 
	
	public Interpreter(String[] args){ // 'args' are as specified by the 'arguments' of the @Program annotation  
		File flowPath = new File(args[0]);
		File bindingPath = new File(args[1]);
		File toolPath = null;
		if (args[2] != null)
			toolPath = new File(args[2]);
		flowSpec = XmlUtils.parseXML(flowPath.getAbsolutePath());
		bindingSpec = XmlUtils.parseXML(bindingPath.getAbsolutePath());
		if (toolPath != null) try {
			toolSpec = XmlUtils.parseXML(toolPath.getAbsolutePath());
		} catch (Throwable error) {}
		arguments = new HashMap<String, String>();
		loadObjectMapping();
		loadActionMapping();
		loadBindingMapping();
		addDataflowInfo();
		for (String pair: args[3].split(";")) {
			int index = pair.indexOf('=');
			if (index != -1)
				arguments.put(
					pair.substring(0, index), pair.substring(index + 1));
		}
		// 'task' is a required argument, which specified the task id  
		flowName = String.format("%s[%s]",
			flowSpec.getDocumentElement().getAttribute("name"),
			arguments.get("task"));
		workflow = flowSpec.getDocumentElement().getAttribute("name");
	}

	private void loadObjectMapping(){
		objects = new HashMap<String, Element>();
		for(Element e: XmlUtils.getElements(flowSpec, "collection", "object"))
			objects.put(e.getAttribute("name"), e);
	}

	private void loadActionMapping(){
		actions = new HashMap<String, Element>();
		for(Element e: XmlUtils.getElements(flowSpec, "action"))
			actions.put(e.getAttribute("name"), e);
	}

	private void loadBindingMapping(){
		bindingsByAction = new HashMap<String, Element>();
		bindingsByObject = new HashMap<String, Element>();
		for(Element binding: XmlUtils.getElements(bindingSpec, "bind")){
			String action = binding.getAttribute("action");
			String object = binding.getAttribute("object");
			if(!action.isEmpty())
				bindingsByAction.put(action, binding);
			else if(!object.isEmpty())
				bindingsByObject.put(object, binding);
		}
	}

	private void addDataflowInfo(){
		Wrapper doc = new Wrapper(flowSpec);
		for(Element action: actions.values()){
			String actionName = action.getAttribute("name");
			boolean isMultiple = "multiple".equals(action.getAttribute("multiplicity"));
			for(Element dependence: XmlUtils.getChildElements(action)){
				/* The "object"/"collection" attributes of <input>/<output> are now uniformly described
				 * by two separate attributes "type" and "object" where the former distinguish "collection" 
				 * from "object" and the latter specified the target data entity 
				 */
				boolean isCollection = !dependence.getAttribute("collection").isEmpty();
				dependence.setAttribute("isCollection", Boolean.toString(isCollection));
				if(isCollection){
					dependence.setAttribute("object", dependence.getAttribute("collection"));
					dependence.removeAttribute("collection");
				}
				
				// Create <sink>/<source> which will bring essential information from <action> to <object>
				Element dataflow = null;
				String dependenceType = dependence.getNodeName();
				if(dependenceType.equals("input"))
					dataflow = doc.E("sink");
					// means the associated object "sinks" to this action node  				
				else if (dependenceType.equals("output")) dataflow = doc.E("source");
					// means the associated object is "sourced" from this action node
				else return; // TODO: should throw an exception instead
				dataflow.setAttribute("action", actionName);
				dataflow.setAttribute("multiplicity", isMultiple ? "multiple" : "single");

				/* copy all attributes of this <input></output> element to <sink>/<source>
				 * (except the "object" attribute because it's redundant with the "name" 
				 *  attribute of the target <object>)
				 */				
				NamedNodeMap attrs = dependence.getAttributes();
				for(int i = 0; i < attrs.getLength(); i++){
					Attr attr = (Attr)attrs.item(i);
					if(attr.getNodeName().equals("object")){
						// bring the <sink>/<source> to the associated <object>  
						Element emtObj = objects.get(attr.getNodeValue());
						if(emtObj != null) emtObj.appendChild(dataflow);
						else logger.info("object [{}] is not found", attr.getNodeValue());
					}
					else dataflow.setAttributeNode((Attr)attr.cloneNode(false));
				}
			}
		}
	}

	public void build(Flow flow, List<FlowEdge> in, List<FlowNode> out) {
		flow.setAttachment(arguments.get("task"));
		populateFlowNodes(flow);
		populateFlowEdges(flow);
	}

	public void populateFlowNodes(Flow flow){
		nodes = new HashMap<String, FlowNode>();
		String task = arguments.get("task");
		Wrapper d = new Wrapper(flowSpec);
		FlowNode execNode = new FlowNode(ActionExec.class.getName());
		FlowNode parallelNode = new FlowNode(Dispatcher.class.getName());
//		execNode.setDomainPattern(".*");
//		parallelNode.setDomainPattern(".*");
		
		Element objBindings = d.E("objects");
		for(Element binding: bindingsByObject.values())
			objBindings.appendChild(flowSpec.adoptNode(binding.cloneNode(true)));

		for(Element action: actions.values()){
			// brings the action node the information from a binding element that it is associated with  
			String name = action.getAttribute("name");
			Element actionBinding = bindingsByAction.get(name);
			if(actionBinding == null)
				logger.info("binding is not found for action [{}]", name);
			String tool = actionBinding.getAttribute("tool");
			String type = actionBinding.getAttribute("type");
			if(!tool.isEmpty())
				action.setAttribute("tool", tool);
			if(!type.isEmpty())
				action.setAttribute("type", type);

			// create an XML element to contain all relevant information: id, arguments, <action>, <binding> 			
			Element params = d.E("parameters", d.A("workflow", workflow), d.A("id", name), action, actionBinding,
				objBindings);
			for(Entry<String, String> entry: arguments.entrySet())
				params.appendChild(d.E("argument", 
					d.A("name", entry.getKey()), d.A("value", entry.getValue())));
			// add tool.xml snippet for this workflow, if present
			if (toolSpec != null)
				params.appendChild(d.E("tool", toolSpec.getDocumentElement()));
			// create a node for the action and add it to the map 
			boolean isParallel =
				action.getAttribute("multiplicity").equals("multiple")  &&
				action.getAttribute("type").equals("parallel");
			//	TODO: The combination "multiple+non"  and "parallel" is undefined
			FlowNode node = (isParallel ? parallelNode: execNode)
				.clone().setName(name).setAttachment(name)
				.setParameters(params).setTimeout(86400000000l)
				.setDomainPattern(task + "." + name);
			flow.add(node);
			nodes.put(name, node);
		}
	}

	public void populateFlowEdges(Flow flow){
		for(Element obj: objects.values()){
			String objName = obj.getAttribute("name");
			for(Element source: XmlUtils.getElements(obj, "source")){
				String  sourceName = source.getAttribute("action");
				String  sourcePort = source.getAttribute("port");
				boolean sourceMultiplicity = source.getAttribute("multiplicity").equals("multiple");
				FlowNode sourceNode = nodes.get(sourceName);
				if(sourceNode == null)
					logger.info("Source node [{}] is not found", sourceName);
				Element sourceParams = (Element)sourceNode.getParameters();
				Wrapper D = new Wrapper(sourceParams);

				for(Element sink: XmlUtils.getElements(obj, "sink")){
					String  sinkName = sink.getAttribute("action");
					String  sinkPort = sink.getAttribute("port");
					boolean sinkUnfolding = sink.getAttribute("transformation").equals("unfolding");
					FlowNode sinkNode = nodes.get(sinkName);
					if(sourceNode == null)
						logger.info("Sink node [{}] is not found", sinkName);
					FlowEdge edge = null;
					edge = sourceMultiplicity?
						new DummyEdge(sourceNode, sinkNode) :
						new HandleEdge(sourceNode, sinkNode).setExpandOnEmbed(sinkUnfolding);
					String  edgeName = String.format("%s:%b(%s.%s,%s.%s)",
						objName, sinkUnfolding, sourceName, sourcePort, sinkName, sinkPort);
					flow.add(edge.setName(edgeName));

					sourceParams.appendChild(D.E("edge",
						D.A("object", objName), D.A("unfolding", "" + sinkUnfolding),
						D.A("sourceAction", sourceName), D.A("sourcePort", sourcePort),
						D.A("sinkAction", sinkName), D.A("sinkPort", sinkPort)));
				}
			}
		}
	}

	public String toString(){
		return flowName;
	}
}
