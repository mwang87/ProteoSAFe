package edu.ucsd.livegrid.worker;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dapper.client.Client;
import edu.ucsd.saint.commons.Helpers;

public class LocalWorker implements Worker {
	
	private Logger logger = LoggerFactory.getLogger(LocalWorker.class);

	private Thread monitor = null;
	
	private InetSocketAddress server;
	private int maxClients;
	private Map<String, Client> clients; // a collection of Dapper clients, indexed by client domains.  
	private Map<String, List<String>> requests; // execution requests	
	
	public LocalWorker(int maxClients){
		this.clients = Collections.synchronizedMap(new HashMap<String, Client>());
		this.requests = new HashMap<String, List<String>>();
		this.maxClients = maxClients;
		monitor = new Thread("LiveGrid: LocalWorker monitor"){
			public void run(){
				doMonitor();
			}
		};
		Helpers.startAsDaemon(monitor);
	}
	
	@Override
	public String getName() {
		return "[local]";
	}

	@Override
	public boolean isValid() {
		return true; // local worker is always valid
	}

	@Override
	public void setFlowEngine(String addr, int port) {
		server = new InetSocketAddress(addr, port);
	}

	@Override
	public Set<String> getSubjects(){
		return requests.keySet();
	}

	@Override
	public void issueRequest(String subject, String action){
		if(!requests.containsKey(subject))
			requests.put(subject, new LinkedList<String>());
		requests.get(subject).add(action);
	}
	
	@Override
	public void fulfillRequests() {
		if(requests.isEmpty()) return;
		int counter = 0;
outer:		while(!requests.isEmpty()){
			List<String> toRemove = new LinkedList<String>(); 
			for(String subject: requests.keySet()){
				if(counter > 10 || clients.size() > maxClients)
					break outer;			
				List<String> actions = requests.get(subject);
				if(actions.isEmpty())
					toRemove.add(subject);
				else{
					String action = actions.remove(0);
					String domain = subject + "." + action;
					if(clients.containsKey(domain))
						continue;
					clients.put(domain, new Client(server, domain){
							public void run(){
								try{
									super.run();
								}
								catch(Throwable th){
									this.close();
									logger.info("Client thread ended at throwable", th);
								}
							}
						}
					);
					logger.info("#(Local Clients) [{}], maxClients [{}]", clients.size(), maxClients);
					counter ++;
				}
			}
			for(String subject: toRemove)
				requests.remove(subject);
		}
		requests.clear();
	}

	public void monitorClients(){
		List<String> toRemove = new LinkedList<String>();
		for(Map.Entry<String, Client> entry: clients.entrySet()){
			Client client = entry.getValue();
			if(!client.isAlive())
				toRemove.add(entry.getKey());
		}
		for(String domain: toRemove){
			Client client = clients.remove(domain);
			client.close();
		}
	}

	@Override
	public void terminateZombies() {
	}
	
	private void doMonitor() {
		try {
			while (true) {
				Thread.sleep((int)(30000L));
				List<String> toRemove = new LinkedList<String>();
				for(Map.Entry<String, Client> entry: clients.entrySet()){
					Client client = entry.getValue();
					if(!client.isAlive())
						toRemove.add(entry.getKey());
				}
				for(String domain: toRemove){
					Client client = clients.remove(domain);
					client.close();
				}
			}
		} catch (InterruptedException e) {
			logger.info("LiveGrid monitor ends");
		} catch (Exception e) {
			logger.error("Failed to monitor grid resources", e);
		}
	}

	public Collection<String> getClientNames(){
		return clients.keySet();
	}
}
