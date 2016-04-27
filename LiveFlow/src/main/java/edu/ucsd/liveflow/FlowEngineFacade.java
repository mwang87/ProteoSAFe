package edu.ucsd.liveflow;



import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.config.RequestConfig.Builder;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import shared.metaclass.JarRegistry;
import shared.metaclass.RegistryClassLoader;
import shared.util.Control;
import dapper.event.FlowEvent;
import dapper.event.FlowEvent.FlowEventType;
import dapper.server.Server;
import dapper.server.ServerProcessor.FlowProxy;
import dapper.server.flow.Flow;
import dapper.server.flow.FlowBuilder;
import dapper.server.flow.FlowStatus;
import dapper.ui.Program;
import edu.ucsd.saint.commons.Helpers;
import edu.ucsd.saint.commons.IOUtils;
import edu.ucsd.saint.commons.WebAppProps;
import edu.ucsd.saint.commons.http.HttpGetAgent;
import edu.ucsd.saint.commons.http.SteadyRetry;

public class FlowEngineFacade {

	private static final Logger logger = LoggerFactory
			.getLogger(FlowEngineFacade.class);

	public static class WorkflowRecord {
		private String task;
		private String flow;
		private String site;
		private FlowProxy proxy;
		private String layout;
		private String message;
		private int	errorCount;

		public WorkflowRecord(String task, String flow, String site) {
			this.task = task;
			this.flow = flow;
			this.site = site;
			this.proxy = null;
			this.message = null;
			errorCount = 0;
		}
		public String getTask() { return task; }
		public String getFlow() { return flow; }
		public String getSite() { return site; }
		
		public void setProxy(FlowProxy proxy) {
			this.proxy = proxy;
		}

		public FlowProxy getProxy(){
			return proxy;
		}
		
		public synchronized String getMessage() {
			return message;
		}

		public synchronized void setMessage(String message) {
			this.message = message;
		}
		
		public synchronized String getLayout(){
			return layout;
		}
		
		public synchronized FlowStatus getStatus(){
			return proxy.getFlow().getStatus();
		}
		
		public synchronized void purge() throws InterruptedException, ExecutionException{
			proxy.purge();
		}
		
		public synchronized void update(){
			try{
				proxy.refresh();
				Flow flow = proxy.getFlow();
				if(flow.getStatus() != FlowStatus.FAILED)
					layout = proxy.toString();
			}
			catch(Exception e){
				logger.info("Failed to refresh FlowProxy", e);
			}
		}
		
		public synchronized int incErrorCount(){
			return ++ errorCount;
		}
		
		public synchronized int getErrorCount(){
			return errorCount;
		}
		
		public String toString(){ return task; }
	}
	
	private static FlowEngineFacade facade;
	public static void start() {
		facade = new FlowEngineFacade();
		facade.startServices();
	}

	private Thread monitor = null;
	private Thread renderer = null;
	private Thread poller = null;

	private void startServices() {
		monitor = new Thread("LiveFlow: launcher") {
			public void run() {doMonitor();};
		};
		renderer = new Thread("LiveFlow: renderer"){
			public void run(){doRender();}
		};
		poller = new Thread("LiveFlow: poller"){
			public void run(){doPolling();}
		};
		
		Thread relaunch = new Thread("LiveFlow: reluancher") {
			public void run() {
				try {
					Thread.sleep(180000l);
					facade.requestRelaunch();
				} catch (Exception e) {
					logger.info("Reluancher failed", e);
				}
			}
		};
		Helpers.startAsDaemon(monitor);		
		Helpers.startAsDaemon(renderer);
		Helpers.startAsDaemon(poller);
		Helpers.startAsDaemon(relaunch);
	}

	public static void stop(){
		if(facade != null){
			facade.stopServices();
			facade = null;
		}
	}
	
	private void stopServices(){
		if(monitor != null){
			monitor.interrupt();
			monitor = null;
		}
		if(renderer != null){
			renderer.interrupt();
			renderer = null;
		}
		server.close();
	}

	public static FlowEngineFacade getFacade() {
		return facade;
	}

	private List<WorkflowRecord> getCompletedWorkflows() throws Exception {
		List<WorkflowRecord> completed = new LinkedList<WorkflowRecord>();
		Collection<WorkflowRecord> records = getWorkflows();
		if (records== null)
			return completed;
		for (WorkflowRecord record : records) {
			FlowStatus status = record.getStatus();
			if(!status.isExecuting()){
				completed.add(record);
				logger.info("Task [{}] is {}", record.getTask(), status);
			}
		}
		return completed;
	}

	private void doMonitor() {
		final int timer = 1;
		logger.info("Flow monitor thread begins");
		try {
			while (true) {
				synchronized (this) {
					this.wait(60000L * timer);
				}
				try {
					logger.debug("New monitor iteration begins");
					List<WorkflowRecord> completed = null;
					List<String> toRemove = new LinkedList<String>();
					completed = getCompletedWorkflows();

					for (WorkflowRecord record : completed) {
						if (notifyCompletion(record))
							cleanTaskFolder(record.getTask());
							toRemove.add(record.getTask());
					}
					synchronized (records) {
						for (String task : toRemove)
							records.remove(task);
					}
					logger.debug("Flow monitor iteration ends");
				} catch (Exception e) {
					logger.info(
							"Failed to get the list of completed workflows", e);
				}
			}
		} catch (InterruptedException e) {
			logger.info("Task monitor thread is interrupted", e);
		}
	}

	private boolean notifyCompletion(WorkflowRecord record) {
		String status = record.getStatus().toString();
		String task = record.getTask();
		logger.info(String.format("Notifying ProteoSAFe that " +
			"workflow task [%s] is completed, with status [%s].",
			task, status));
		// build POST URL
		String site = record.getSite();
		String service = WebAppProps.get("livesearch.service.notifyCompletion");
		String[] tokens = service.split("\\?");
		String url = site + "/" + tokens[0];
		// extract parameters from template URL, since they need to be POSTed
		Map<String, String> parameters = new LinkedHashMap<String, String>(2);
		if (tokens.length > 1) {
			String params = tokens[1];
			params = params.replace("{task}", task);
			params = params.replace("{status}", status);
			for (String param : params.split("&")) {
				String[] values = param.split("=");
				parameters.put(values[0], values[1]);
			}
		}
		StringBuffer log = new StringBuffer("Notification URL = [");
		log.append(url);
		if (parameters.isEmpty() == false) {
			log.append("?");
			for (String param : parameters.keySet()) {
				log.append(param).append("=");
				log.append(parameters.get(param)).append("&");
			}
			// chomp trailing "&"
			if (log.charAt(log.length() - 1) == '&')
				log.setLength(log.length() - 1);
		}
		logger.info(log.append("]").toString());
		// submit notification POST
		CloseableHttpClient client = null;
		CloseableHttpResponse response = null;
		try {
			// create and initialize HTTP objects
			client = HttpClientBuilder.create().build();
			HttpPost post = new HttpPost(url);
			MultipartEntityBuilder multipart = MultipartEntityBuilder.create();
			multipart.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
			// set HTTP request config parameters
			Builder config = RequestConfig.custom();
			config.setSocketTimeout(60000);
			config.setConnectTimeout(60000);
			post.setConfig(config.build());
			// add HTTP post parameters
			for (String parameter : parameters.keySet())
				multipart.addTextBody(parameter, parameters.get(parameter));
			String message = record.getMessage();
			if (message == null)
				message = "";
			multipart.addPart(
				"message", new StringBody(message, ContentType.TEXT_PLAIN));
			renderFor(record);
			File svg = new File(getTaskFolder(task), "layout.svg");
			if (svg.isFile())
				multipart.addPart("layout", new FileBody(svg));
			post.setEntity(multipart.build());
			response = client.execute(post);
			StatusLine statusLine = response.getStatusLine();
			response.getEntity().getContent().close();
			int code = statusLine.getStatusCode();
			if (code == 200) {
				logger.info(String.format(
					"Successfully notified [%s] of the completion status " +
					"[%s] for task [%s]", site, status, task));
				return true;
			} else logger.error(String.format(
				"Failed to notify [%s] of the completion status [%s] " +
				"for task [%s]: HTTP response returned status code [%d]",
				site, status, task, code));
		} catch (Throwable error) {
			logger.error(String.format(
				"Failed to notify [%s] of the completion status " +
				"[%s] for task [%s]", site, status, task), error);
		} finally {
			try { client.close(); } catch (Throwable error) {}
			try { response.close(); } catch (Throwable error) {}
		}
		return false;
	}

	private void requestRelaunch() {
		String service = WebAppProps
				.get("livesearch.service.relaunchFlows", "");
		HttpGetAgent agent = new HttpGetAgent(new SteadyRetry(120000, 5));
		try {
			for (String site : trustedSites) {
				String url = site + "/" + service;
				try {
					logger.info(String.format("Request [%s] to relaunch:%n %s",
							site, url));
					HttpEntity entity = agent.execute(url);
					entity.getContent().close();
				} catch (Exception e) {
					logger.info(String.format(
							"Failed to request [%s] to relaunch:%n %s", site,
							url), e);
				}
			}
		} finally {
			agent.close();
		}
	}

	private Server server;
	private Map<String, WorkflowRecord> records = new HashMap<String, WorkflowRecord>();
	private Collection<String> trustedSites;

	private FlowEngineFacade() {
		try {
			int port = Integer
					.parseInt(WebAppProps.get("liveflow.engine.port"));
			InetAddress addr = InetAddress.getLocalHost(); 
			server = new Server(addr, port);
			server.setAutoCloseIdle(true);
		} catch (IOException error) {
			logger.error("Error instantiating FlowEngineFacade.", error);
		} catch (Exception error) {
			logger.error("Error instantiating FlowEngineFacade.", error);
		}

		String sites[] = WebAppProps.get("livesearch.service.sites", "").split(
				"\\s");
		trustedSites = new HashSet<String>();
		for (String site : sites)
			if (!site.isEmpty())
				trustedSites.add(site);
	}

	public Collection<String> getTrustedSites() {
		return trustedSites;
	}

	public boolean isTrusted(String site) {
		return trustedSites.contains(site);
	}

	private static File getTaskFolder(String task){
		return new File(WebAppProps.getPath("liveflow.temp.path"), task);
	}

	private static void cleanTaskFolder(String task) {
		File folder = getTaskFolder(task);
		boolean success = FileUtils.deleteQuietly(folder);
		logger.info("Clean task foler [{}] {}", folder, success);
	}

	private static String fetchSpec(
		String from, String task, String flow, String spec
	) {
		File path = new File(
			getTaskFolder(task), String.format("%s.%s.xml", flow, spec));
		// TODO: verify whether the path is legal
		// safe to prevent filename injection
		String url =
			from + "/" + WebAppProps.get("livesearch.service.queryFlowSpec");
		url = url.replace("{flow}", flow);
		url = url.replace("{spec}", spec);

		boolean success = false;
		HttpEntity entity = null;
		try {
			OutputStream output = new FileOutputStream(path);
			HttpGetAgent agent = new HttpGetAgent(new SteadyRetry(0, 10));
			try {
				entity = agent.execute(url);
				entity.writeTo(output);
				success = true;
			} catch (IOException e) {
				logger.info(
						String.format("Failed to write spec to [%s]", path), e);
			} finally {
				agent.close();
				output.close();
			}
		} catch (Exception e) {
			logger.info(String.format(
					"Failed to download [%s] spec for workflow [%s]", spec,
					flow), e);
		}

		logger.info("Write spec to [{}] {}", path, success);
		return success ? path.getAbsolutePath() : null;
	}

	public boolean launchWorkflow(String task, String flow, String site)
	throws Exception {
		WorkflowRecord record = new WorkflowRecord(task, flow, site);
		try {
			synchronized (records) {
				if (!records.containsKey(task)) {
					getTaskFolder(task).mkdirs();
					String flowSpec = fetchSpec(site, task, flow, "flow");
					String bindingSpec = fetchSpec(site, task, flow, "binding");
					String toolSpec = fetchSpec(site, task, flow, "tool");
					if (flowSpec != null && bindingSpec != null) {
						String args[] = {
							flowSpec, bindingSpec, toolSpec, "task=" + task
						};
						File jar = new File(
							WebAppProps.getPath("liveflow.engine.package"));
						String clazz =
							WebAppProps.get("liveflow.engine.interpreter");
						FlowProxy proxy =
							runIntepreter(server, jar, clazz, args);
						logger.info("Task [{}] is launched successfully", task);
						record.setProxy(proxy);
						records.put(task, record);
					}
					return true;
				}
			}
		} catch (Exception e) {
			logger.error("Failed to launch task [" + task + "]", e);
		}
		return false;
	}

	public boolean purgeWorkflow(String task) {
		try {
			synchronized (records) {
				WorkflowRecord record = records.get(task);
				if (record != null) {
					record.purge();
					records.remove(task);
					logger.info("Workflow {} is purged", task);
					return true;
				}
			}
		} catch (Exception e) {
			logger.info("Failed to purge %s", task);
		}
		return false;
	}

	public int getNumInstances() {
		synchronized (records) {
			return records.size();
		}
	}

	public int getNumReadyNodes() {
		try {
			return server.getPendingCount();
		} catch (Exception e) {
			e.printStackTrace();
			return 0;
		}
	}

	public Collection<WorkflowRecord> getWorkflows() {
		synchronized (records) {			
			return records.values();
		}
	}

	public WorkflowRecord getWorkflow(String task) {
		synchronized (records) {
			return records.get(task);
		}
	}

	/**
	 * A {@link Pattern} for detecting binary class names.
	 */
	final public static Pattern BinaryNamePattern = Pattern.compile("(" //
			// Capture names of base classes.
			+ "(:?[a-zA-Z_][0-9a-zA-Z_]*/)*[a-zA-Z_][0-9a-zA-Z_]*"
			// Capture names of inner classes.
			+ "(:?\\$[0-9]+|\\$[a-zA-Z_][0-9a-zA-Z_]*)*" //
			+ ")\\.class");

	/**
	 * Loads {@link FlowBuilder}s from the given {@link ClassLoader}.
	 * 
	 * @throws ClassNotFoundException
	 *             when some class(es) could not be found.
	 */
	@SuppressWarnings("unchecked")
	final protected static List<Class<? extends FlowBuilder>> getBuilders( //
			ClassLoader cl, List<String> classNames)
			throws ClassNotFoundException {

		List<Class<? extends FlowBuilder>> builders = new ArrayList<Class<? extends FlowBuilder>>();
		for (String className : classNames) {
			Class<?> clazz = cl.loadClass(className);
			Program programAnnotation = clazz.getAnnotation(Program.class);
			if (programAnnotation != null) {
				Control.checkTrue(FlowBuilder.class.isAssignableFrom(clazz), //
						"Class must be able to build flows");
				builders.add((Class<? extends FlowBuilder>) clazz);
			}
		}
		return builders;
	}

	final protected static FlowProxy runIntepreter(Server server, File file,
			String targetName, String[] args)
			throws Exception {

		// Get the list of classes included in a jar files
		List<String> classNames = new ArrayList<String>();
		JarRegistry jr = new JarRegistry(new JarFile(file));
		final RegistryClassLoader rcl = new RegistryClassLoader();
		rcl.addRegistry(jr);
		for (String pathname : jr.getDataMap().keySet()) {
			Matcher m = BinaryNamePattern.matcher(pathname);
			if (m.matches())
				classNames.add(m.group(1).replace("/", "."));
		}
		// Look up the FlowBuilder class
		List<Class<? extends FlowBuilder>> builderClasses = getBuilders(rcl, classNames);
		Control.checkTrue(targetName != null, "Please specify a target class");
		Class<? extends FlowBuilder> targetClass = null;
		loop: for (final Class<? extends FlowBuilder> clazz : builderClasses) {
			if (targetName.equals(clazz.getName())) {
				targetClass = clazz;
				break loop;
			}
		}
		Control.checkTrue(targetClass != null, "Target class not found");

		// Check the number of arguments matches with the FlowBuilder class
		String[] argNames = targetClass.getAnnotation(Program.class)
				.arguments();
		if (argNames.length != args.length) {
			Formatter f = null;
			try {
				f = new Formatter();
				f.format("Target class = [%s]", targetClass.getName());
				f.format("\nExpecting arguments");
				for (String argName : argNames)
					f.format(" [%s]", argName);
				f.format("\nGot arguments");
				for (String arg : args)
					f.format(" [%s]", arg);
				throw new IllegalArgumentException(f.toString());
			} finally {
				try { f.close(); } catch (Throwable error) {}
			}
		}
		// instantiate the builder and let the server run it.
		FlowBuilder builder = targetClass.getConstructor(String[].class) //
				.newInstance(new Object[] { args });
		FlowProxy proxy = server.createFlow(builder, rcl, FlowEvent.F_ALL);
		return proxy;
	}

	private void doPolling(){
		try {
			BlockingQueue<FlowEvent<String, String>> queue = server.createFlowEventQueue();
			while (true) {
				logger.info("Begin taking");
				FlowEvent<String, String> event = queue.take();
				logger.info("End taking");
				FlowEventType type = event.getType();
				String task = event.getFlowAttachment();
				String node = event.getFlowNodeAttachment();
				WorkflowRecord record = getWorkflow(task);
				if(record == null) continue;
				switch (type){
				case FLOW_BEGIN: case FLOW_END: case FLOW_ERROR:
					record.update();
					requestRendering(record);
					logger.info("{}: [{}]", type.name(), task);
					break;

				case FLOW_NODE_ERROR:
					String msg = String.format(
						"Workflow node [%s] was forcibly terminated by the " +
						"computational back end.\nThe most likely reason is " +
						"for exceeding this node's memory usage quota.", node);
					Throwable error = event.getError();
					if (error != null) {
						Throwable cause = error.getCause();
						if (cause != null) {
							msg = cause.getMessage();
							if (msg == null)
								msg = cause.getLocalizedMessage();
						}
					}
					record.setMessage(msg);
					record.update();
					requestRendering(record);
					logger.info(
						String.format("FLOW_NODE_ERROR: [%s:%s]: %s", task, node, msg), error);
					if (record.incErrorCount() >= 8)
						record.purge();
					break;

				case FLOW_NODE_BEGIN: case FLOW_NODE_END:
					record.update();
					requestRendering(record);
					logger.info("{}: [{}]", type.name(), task + ":" + node);
				}
			}
		} catch (Exception e) {
			logger.error("Poller thread is interrupted", e);
		}
	}

	// RENDERING
	private Set<String> rendergRequests;

	private void doRender(){
		final int timer = 1;
		rendergRequests = Collections.synchronizedSet(new HashSet<String>());
		logger.info("Renderer thread begins");
		try {
			while (true) {
				synchronized (this) {
					this.wait(10000L * timer);
				}
				try {
					while(renderOne())
						;
				} catch (Exception e) {
					logger.info(
						"Failed to fulfill rendering requests", e);
				}
			}
		} catch (InterruptedException e) {
			logger.info("Renderer thread is interrupted", e);
		}
	}

	private boolean renderOne(){
		String task;
		boolean fulfilled = true;
		synchronized(rendergRequests){
			if(rendergRequests.isEmpty())
				return false;
			task = rendergRequests.iterator().next();
		}
		WorkflowRecord record = getWorkflow(task);
		if(record != null)
			fulfilled = renderFor(record);
		if(fulfilled)
			rendergRequests.remove(task);
		return true;
	}
	
	private void requestRendering(WorkflowRecord record){
		rendergRequests.add(record.getTask());
		logger.debug("Request rendering for task [{}]", record.getTask());
	}

	private boolean renderFor(WorkflowRecord record){
		try{
			synchronized(record){
				String directory = WebAppProps.getPath("liveflow.dot.path");
				String dotExec = StringUtils.isEmpty(directory)? "dot" : directory + "/dot";
				File folder = getTaskFolder(record.getTask());
				File svg = new File(folder, "layout.svg");
				File log = new File(folder, "layout.log");
				File dot = new File(folder, "layout.dot");

				String dotString = record.getLayout();
				if(dotString != null && !dotString.isEmpty()){
					IOUtils.dumpToFile(new ByteArrayInputStream(dotString.getBytes()), dot);
					ProcessBuilder pb = new ProcessBuilder(dotExec, "-Tsvg", "-o", 
						svg.getAbsolutePath(), dot.getAbsolutePath());
					if(directory != null)
						pb.directory(new File(directory));
					pb.redirectErrorStream(true);
					Process proc = pb.start();
					IOUtils.dumpToFile(proc.getInputStream(), log);
					proc.waitFor();
					return true;
				}
			}
		}
		catch(Exception e){
			logger.debug("Unable to generate svg for task [{}]", record.getTask());
		}
		return false;
	}	
}
