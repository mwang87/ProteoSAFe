package edu.ucsd.livegrid;

import java.io.File;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.math.NumberUtils;
import org.apache.http.HttpEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import edu.ucsd.livegrid.worker.LocalWorker;
import edu.ucsd.livegrid.worker.GridWorker;
import edu.ucsd.livegrid.worker.PersonalWorker;
import edu.ucsd.livegrid.worker.Worker;
import edu.ucsd.livegrid.worker.WorkflowWorker;
import edu.ucsd.saint.commons.Helpers;
import edu.ucsd.saint.commons.WebAppProps;
import edu.ucsd.saint.commons.http.HttpGetAgent;
import edu.ucsd.saint.commons.http.SteadyRetry;
import edu.ucsd.saint.commons.xml.XmlUtils;

public class GridPlanner {

	private static final Logger logger = LoggerFactory.getLogger(GridPlanner.class);

	private static GridPlanner planner;

	public static GridPlanner getInstance() {
		return planner;
	}

	static {
		planner = new GridPlanner();
	}

	private Thread planning = null;
	private Thread monitor = null;
	private Worker shared = null;
	private Map<String, PersonalWorker> personal = null;
	private Map<String, WorkflowWorker> flowWorker = null;
	private Map<String, Worker> assignments = null;

	private GridPlanner() {
		discoverWorkers();
		assignments = new HashMap<String, Worker>();
		if(WebAppProps.get("livegrid.shared.type").equals("local")){
			File path = new File(WebAppProps.getPath("livegrid.local.path"));
			if(!path.isDirectory())
				return;
			if(System.getProperty("ccms.config.path") == null)
				System.setProperty("ccms.config.path", new File(path, "conf").getAbsolutePath());
			logger.info("Property [ccms.config.path] = {}", System.getProperty("ccms.config.path"));
			if(System.getProperty("ccms.tool.path") == null)
				System.setProperty("ccms.tool.path", new File(path, "tools").getAbsolutePath());
			logger.info("Property [ccms.tool.path] = {}", System.getProperty("ccms.tool.path"));
		}
	}

	public boolean discoverWorkers() {
		String	addr	= WebAppProps.get("liveflow.engine.address");
		int		port 	= NumberUtils.toInt(WebAppProps.get("liveflow.engine.port"), 10100);
		File	folder	= new File(WebAppProps.getPath("livegrid.policy.path"));
		String	type	= WebAppProps.get("livegrid.shared.type");
		int		maxClients = NumberUtils.toInt(WebAppProps.get("livegrid.local.maxClients"), 3);

		File commonFolder = new File(folder, "common");
		shared = type.equals("local") ? new LocalWorker(maxClients) : new GridWorker(commonFolder);
		personal = Collections.synchronizedMap(new HashMap<String, PersonalWorker>());
		File userFolder = new File(folder, "user");
		if (userFolder.isDirectory())
			for (File u : userFolder.listFiles())
				addPersonalWorker(u);
		flowWorker = Collections.synchronizedMap(new HashMap<String, WorkflowWorker>());
		File flowFolder = new File(folder, "workflow");
		if (flowFolder.isDirectory())
			for (File f : flowFolder.listFiles())
				addWorkflowWorker(f);

		if (shared.isValid())
			shared.setFlowEngine(addr, port);
		for(Worker worker: personal.values())
			worker.setFlowEngine(addr, port);
		for(Worker worker: flowWorker.values())
			worker.setFlowEngine(addr, port);
		
		return true;
	}


	public PersonalWorker addPersonalWorker(File folder) {
		PersonalWorker account = new PersonalWorker(folder);
		if (account.isValid() && account.getOwner() != null)
			synchronized (personal) {
				personal.put(account.getOwner(), account);
			}
		return account;
	}

	public WorkflowWorker addWorkflowWorker(File folder) {
		WorkflowWorker account = new WorkflowWorker(folder);
		if (account.isValid() && account.getFlowName() != null)
			synchronized (personal) {
				flowWorker.put(account.getFlowName(), account);
			}
		return account;
	}

	public void start() {
		if (planning == null) {
			planning = new Thread("LiveGrid: Planner") {
				public void run() {
					doPlanning();
				}
			};
			Helpers.startAsDaemon(planning);
		}
		if (monitor == null) {
			monitor = new Thread("LiveGrid: Monitor") {
				public void run() {
					doMonitor();
				}
			};
			Helpers.startAsDaemon(monitor);
		}
	}

	public void stop() {
		if (planning != null) {
			planning.interrupt();
			planning = null;
		}
		if (monitor != null) {
			monitor.interrupt();
			monitor = null;
		}
	}

	private void doMonitor() {
		try {
			logger.info("LiveGrid monitor begins");
			double period = NumberUtils.toDouble(WebAppProps
					.get("livegrid.grid.monitorPeriod"), 10);
			while (true) {
				Thread.sleep((int)(60000L * period));
				logger.info("Current monitor thread: [{}:{}]", Thread
						.currentThread(), Thread.currentThread().getName());
				Collection<Worker> workers = new LinkedList<Worker>();
				if (shared.isValid())
					workers.add(shared);
				synchronized (personal) {
					workers.addAll(personal.values());
				}
				synchronized (flowWorker) {
					workers.addAll(flowWorker.values());
				}
				for (Worker worker : workers)
					worker.terminateZombies();
			}
		} catch (InterruptedException e) {
			logger.info("LiveGrid monitor ends");
		} catch (Exception e) {
			logger.error("Failed to monitor grid resources", e);
		}
	}

	private void doPlanning() {
		try {
			logger.info("LiveGrid Planner begins");
			double period = NumberUtils.toDouble(WebAppProps
					.get("livegrid.grid.allocatePeriod"), 1);
			while (true) {
				Thread.sleep((int)(60000L * period));
				logger.debug("Planning begins");
				Collection<Worker> targets = gatherRequests();
				fulfillRequests(targets);
				updateAssignments(targets);
				logger.debug("Planning ends");
			}
		} catch (InterruptedException e) {
			logger.info("LiveGrid Planner ends");
		} catch (Exception e) {
			logger.error("Failed to plan grid resources", e);
		}
	}

	private Collection<Worker> gatherRequests() {
		Set<Worker> targets = new HashSet<Worker>();
		for (Element task : getReadyTasks()) {
			String id = task.getAttribute("id");
			Worker worker = decideWorker(task);
			if (worker != null) {
				for(Element node: XmlUtils.getElements(task, "node")){
					String name = node.getAttribute("name");
					if(!name.isEmpty())
						worker.issueRequest(id, name);
				}
				targets.add(worker);
			} else
				logger.error("NULL grid worker found for task {}", id);
		}
		return targets;
	}

	private Worker decideWorker(Element task) {
		String id = task.getAttribute("id");
		if (assignments.containsKey(id))
			return assignments.get(id);
		String flow = task.getAttribute("flow");
		String user = task.getAttribute("user");
		synchronized (flowWorker) {
			if (flowWorker.containsKey(flow))
				return flowWorker.get(flow);
		}
		synchronized (personal) {
			if (personal.containsKey(user))
				return personal.get(user);
		}
		return shared;
	}

	private void fulfillRequests(Collection<Worker> targets) {
		for (Worker worker : targets)
			worker.fulfillRequests();
	}

	private void updateAssignments(Collection<Worker> targets) {
		assignments.clear();
		for (Worker worker : targets)
			for (String subject : worker.getSubjects())
				assignments.put(subject, worker);
	}

	private List<Element> getReadyTasks() {
		List<Element> tasks = getTaskStatus();
		Map<String, Element> ownerInfo = getTaskOwners();
		for (Element task : tasks) {
			String id = task.getAttribute("id");
			Element info = ownerInfo.get(id);
			if (info == null)
				continue;
			NamedNodeMap attrs = info.getAttributes();
			for (int i = 0; i < attrs.getLength(); i++) {
				Node attr = attrs.item(i);
				String name = attr.getNodeName();
				String value = attr.getNodeValue();
				task.setAttribute(name, value);
			}
		}
		return tasks;
	}

	private List<Element> getTaskStatus() {
		String context = WebAppProps.get("liveflow.service.url");
		String service = WebAppProps.get("liveflow.service.readyNodes");
		String url = context + "/" + service;
		HttpGetAgent agent = new HttpGetAgent(new SteadyRetry(0, 3));
		try {
			HttpEntity entity = agent.execute(url);
			InputStream input = entity.getContent();
			Document doc = XmlUtils.parseXML(input);
			input.close();
			return XmlUtils.getElements(doc, "task");
		} catch (Exception e) {
			logger.info("Failed to query task status on " + context, e);
			return new LinkedList<Element>();
		} finally {
			agent.close();
		}
	}

	private Map<String, Element> getTaskOwners() {
		Map<String, Element> taskInfo = new HashMap<String, Element>();
		String service = WebAppProps.get("livesearch.service.taskInfo");
		String sites[] = WebAppProps.get("livesearch.service.sites", "").split("\\s");
		HttpGetAgent agent = new HttpGetAgent(new SteadyRetry(0, 3));
		try {
			for (String site : sites) {
				if (site.isEmpty())
					continue;
				String url = site + "/" + service;
				try {
					HttpEntity entity = agent.execute(url);
					InputStream input = entity.getContent();
					Document doc = XmlUtils.parseXML(input);
					input.close();
					for (Element task : XmlUtils.getElements(doc, "task"))
						taskInfo.put(task.getAttribute("id"), task);
				} catch (Exception e) {
					logger.info("Failed to query task owners on " + site, e);
				}
			}
		} finally {
			agent.close();
		}
		return taskInfo;
	}
	
	public Collection<String> getClientNames(){
		if(shared != null && shared instanceof LocalWorker)
			return ((LocalWorker)shared).getClientNames();
		return new LinkedList<String>();
	}
}
