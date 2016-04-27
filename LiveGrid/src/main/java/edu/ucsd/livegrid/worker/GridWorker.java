package edu.ucsd.livegrid.worker;

import java.io.File;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UIKeyboardInteractive;
import com.jcraft.jsch.UserInfo;

import edu.ucsd.saint.commons.xml.XmlUtils;

public class GridWorker implements Worker{

	private String name, account, address;
	private File publicKey, privateKey;
	private boolean valid; 
	private String serverAddr; // address of a Dapper server
	private int serverPort; // port for the Dapper server
	private Map<String, List<String>> requests;

	private static final Logger logger = LoggerFactory.getLogger(GridWorker.class);
	
	protected static Document getPolicySpec(File folder){
		File spec = new File(folder, "policy.xml");
		try{
			if(spec.isFile())
				return (spec.exists() && spec.isFile()) ?
					XmlUtils.parseXML(spec.getAbsolutePath()) : null;
		}
		catch(Exception e){
			logger.error("[" + spec.getAbsolutePath() + "] is missing or corrupted", e);
		}
		return XmlUtils.createXML();
	}

	public GridWorker(File policyFolder){
		valid = false;
		if(!policyFolder.isDirectory()) return;
		Document policy= getPolicySpec(policyFolder);

		Element eName = XmlUtils.getElement(policy, "name");
		Element eAddress = XmlUtils.getElement(policy, "grid-address");
		Element eAccount = XmlUtils.getElement(policy, "grid-account");
		name = (eName!= null) ? eName.getTextContent() : policyFolder.getAbsolutePath();
		if(eName == null || eAccount == null){
			logger.error("[{}/policy.xml] is invalid; elements are missing", policyFolder.getAbsolutePath());
			return;
		}
		name = eName.getTextContent();
		account = eAccount.getTextContent();
		address = eAddress.getTextContent();
		privateKey = new File(policyFolder, "key.rsa");
		publicKey = new File(policyFolder, "key.pub");
		if(!publicKey.exists())
			logger.error("Public key [{}] does not exist", publicKey.getAbsolutePath());
		if(!privateKey.exists()){
			logger.error("Private key [{}] does not exist", privateKey.getAbsolutePath());
			return;
		}
		valid = true;
		requests = new HashMap<String, List<String>>();
	}

	public String getName(){
		return name;
	}
	
	public String getAccount(){
		return account;
	}
	
	public String getAddress(){
		return address;
	}
	
	public File getPrivateKey(){
		return privateKey;
	}
	
	public File getPublicKey(){
		return publicKey;
	}


	public String getServerAddr() {
		return serverAddr;
	}

	public int getServerPort() {
		return serverPort;
	}

	public void setFlowEngine(String addr, int port) {
		this.serverAddr = addr;
		this.serverPort = port;
	}
	
	protected void setValid(boolean valid){
		this.valid = valid;
	}

	public boolean isValid(){
		return valid;
	}
	
	public static class DefaultUserInfo implements UserInfo, UIKeyboardInteractive {
		public String getPassword() { return null; }
		public boolean promptYesNo(String str) { return true; }
		public String getPassphrase() { return null; }
		public boolean promptPassphrase(String message) { return false; }
		public boolean promptPassword(String message) { return false; }
		public void showMessage(String message) { }
		public String[] promptKeyboardInteractive(String destination,
				String name, String instruction, String[] prompt, boolean[] echo) {
			return null;
		}
	}
	
	private void sshGridFrontEnd(String command){
		List<String> commands = new LinkedList<String>();
		commands.add(command);
		invokeFrontend(commands);
	}

	private void invokeFrontend(List<String> commands){
		if(!isValid())
			return;

		JSch jsch = new JSch();
		Session session = null;
		Channel channel = null;
		try{
			jsch.addIdentity(getPrivateKey().getAbsolutePath());
			session = jsch.getSession(getAccount(), getAddress(), 22);
			session.setUserInfo(new DefaultUserInfo());
			session.connect();
			channel=session.openChannel("exec");
			PipedInputStream in = new PipedInputStream();
			PrintStream print = new PrintStream(new PipedOutputStream(in));
			channel.setInputStream(in);
			Scanner scanner = new Scanner(channel.getInputStream());
			channel.connect();

			for(String command: commands)
				print.printf(command + "\n");
			print.flush();
			print.close();

			StringBuffer buffer = new StringBuffer("SSH log\n");
			while (scanner.hasNextLine())
				buffer.append(scanner.nextLine()).append('\n');
			logger.info(buffer.toString());
			channel.disconnect();
			session.disconnect();
		}
		catch(Exception e){
			logger.info("Failed to call SSH", e);
		}
		finally{
			if(channel != null && channel.isConnected())
				channel.disconnect();
			if(session != null && session.isConnected())
				session.disconnect();
		}
	}
	
	public void terminateZombies(){
		sshGridFrontEnd("qdel");
	}
	
	public void issueRequest(String subject, String action){
		if(!requests.containsKey(subject))
			requests.put(subject, new LinkedList<String>());
		requests.get(subject).add(action);
	}
	
	public Set<String> getSubjects(){
		return requests.keySet();
	}
	
	public void fulfillRequests(){
		if(requests.isEmpty()) return;
		List<String> commands = new LinkedList<String>();
		commands.add("qsub");
		int counter = 0;
outer:		while(!requests.isEmpty()){
			List<String> toRemove = new LinkedList<String>(); 
			for(String subject: requests.keySet()){
				List<String> actions = requests.get(subject);
				counter ++;
				String action = actions.remove(0);
					commands.add(String.format(
						"%s.%s 1 %s %s", subject, action, serverAddr, serverPort));
				if(actions.isEmpty())
					toRemove.add(subject);
				if(counter > 50)
					break outer; 
			}
			for(String subject: toRemove)
				requests.remove(subject);
		}
		if(!commands.isEmpty())
			invokeFrontend(commands);
		requests.clear();
	}
}
