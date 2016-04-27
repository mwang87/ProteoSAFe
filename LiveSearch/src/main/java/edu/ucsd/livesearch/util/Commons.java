package edu.ucsd.livesearch.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import javax.mail.internet.AddressException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsd.livesearch.account.AccountManager;
import edu.ucsd.livesearch.dataset.Dataset;
import edu.ucsd.livesearch.notification.DatasetSubscriberNotification;
import edu.ucsd.livesearch.notification.GenericTaskCompletion;
import edu.ucsd.livesearch.notification.MassIVENotification;
import edu.ucsd.livesearch.notification.WorkflowNotification;
import edu.ucsd.livesearch.subscription.SubscriptionManager;
import edu.ucsd.livesearch.task.Task;
import edu.ucsd.saint.commons.ConnectionPool;
import edu.ucsd.saint.commons.WebAppProps;

public class Commons
{
	public static final String RESOURCE_PATH;
	public static final String SEQUENCE_PATH;

	private static final Logger logger;
	private static final ConnectionPool connPool;
	private static Map<String, String> proteinDesc = new HashMap<String, String>();
	private static VersionTuple version;

	static {
		logger = LoggerFactory.getLogger(Commons.class);
		logger.info("Begin initializing ProteoSAFe Commons");

		loadVersion();
		loadProteinDesc();
		
		RESOURCE_PATH = WebAppProps.getPath("livesearch.resources.path");
		SEQUENCE_PATH = WebAppProps.getPath("livesearch.sequences.path");
		
		connPool = new ConnectionPool(
				"java:comp/env/" + WebAppProps.get("livesearch.jdbc.main"));
		
		logger.info("End initializing ProteoSAFe Commons");
	}

	private static void loadVersion(){
		Scanner scanner = null;
		version = new VersionTuple();
		try{		
			File file = new File(Commons.class.getResource("/.version").toURI());
			logger.info("{}: {}", file.getAbsolutePath(), file.exists());			
			scanner = new Scanner(file);
			if(scanner.hasNextLine()){
				String text = scanner.nextLine();
				version = new VersionTuple(text);
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}
		finally{
			if(scanner != null) scanner.close();
		}
	}

	private static void loadProteinDesc(){
		try{
			File file = new File(WebAppProps.getPath("livesearch.util.path"), "protein_desc");
			if(!file.exists()) return;
			for(File f: file.listFiles()){
				Scanner scanner = new Scanner(f);
				while(scanner.hasNextLine()){
					String[] tokens = scanner.nextLine().split("\\t", 2);
					proteinDesc.put(tokens[0], tokens[1]);
				}
				scanner.close();
			}
		}
		catch(Exception e){
			logger.error("Failed to initialize protein description", e);
			e.printStackTrace();
		}
	}

	public static String getDescription(String code){
		return proteinDesc.get(code);
	}

	public static VersionTuple getVersion(){
		return version;
	}
	
	public static void closeStream(OutputStream out){
		try{
			if(out != null) out.close();
		} catch(IOException e){
			logger.error("Failed to close output stream", e);
		}
	}

	public static void closeStream(InputStream in){
		try{
			if(in != null) in.close();
		} catch(IOException e){
			logger.error("Failed to close input stream", e);
		}
	}


	public static enum NullType{
		INTEGER(Types.INTEGER), STRING(Types.VARCHAR), DOUBLE(Types.DOUBLE);

		public final int value;

		private NullType(int type){
			value = type;
		}
	}

	public static void executeProcess(File directory, Writer writer, ProcessBuilder pb)
	throws IOException{
		pb.redirectErrorStream(true);
		pb.directory(directory);
		Process proc = pb.start();
		Scanner scanner = new Scanner(proc.getInputStream());
		if(writer != null)
			while(scanner.hasNextLine()){
				writer.write(scanner.nextLine());
				writer.write('\n');
			}
		else while(scanner.hasNextLine())
			scanner.nextLine();
	}
	
	public static void executeProcess(File directory, File output, ProcessBuilder pb)
	throws IOException{
		PrintWriter writer = null; 
		try{
			if(output != null)
				writer = new PrintWriter(output);
			executeProcess(directory, writer, pb);
		}
		finally{
			if(writer != null)
				writer.close();
		}
	}

	public static ConnectionPool getConnectionPool(){
		return connPool;
	}

	public static int executeUpdate(String sql, Object ... args){
		Connection conn = null;
		PreparedStatement stmt = null;
		try{
			conn = connPool.aquireConnection();
			stmt = conn.prepareStatement(sql);
			for(int i = 0; i < args.length; i++){
				Object arg = args[i];
				if(arg == null)
					stmt.setNull(i+1, Types.NULL);
				else if(arg instanceof Integer)
					stmt.setInt(i+1, (Integer)arg);
				else if(arg instanceof Long)
					stmt.setLong(i+1, (Long)arg);
				else if(arg instanceof Double)
					stmt.setDouble(i+1, (Double)arg);
				else stmt.setString(i+1, arg.toString());
			}
			return stmt.executeUpdate();
		}
		catch(SQLException e){
			logger.error(String.format(
				"SQL Error [code=%d, state=%s, message=%s]%n",
					e.getErrorCode(), e.getSQLState(), e.getLocalizedMessage()), e);
		}
		finally{
			connPool.close(stmt, conn);
		}
		return 0;
	}
	
	public static boolean sendEmail(String recipients, String subject, String content) {
		Notifier notifier = null;
		try {
			notifier = new Notifier(recipients);
		} catch (AddressException error) {
			logger.error("Error sending notification email", error);
			return false;
		}
        return notifier.notify(subject, content);
	}
	
	public static void sendCompletionEmail(Task task, String status) {
		// only send an email if notifications are enabled
		boolean enabled = Boolean.valueOf(
			WebAppProps.get("livesearch.email.enabled", "true"));
		if (enabled == false)
			return;
		String recipient = task.getNotification();
		// retrieve proper email implementation for this workflow
		WorkflowNotification email = null;
		String workflow = task.getFlowName();
		// TODO: this should be a lookup of the email implementation registered
		// in this workflow's result.xml, if any - otherwise generic
		if (workflow.equals("MASSIVE"))
			email = new MassIVENotification(task, status);
		else email = new GenericTaskCompletion(task, status);
		// send email
        if (sendEmail(recipient, email.getSubject(), email.getContent()))
        	logger.info("Notification mail was sent to [{}] for task [{}]",
        		recipient, task.getID());
        else logger.error(
        	"Failed to send notification mail to [{}] for task [{}]",
        	recipient, task.getID());
	}
	
	public static void sendSubscriberEmails(
		Dataset dataset, String operation, String message
	) {
		if (dataset == null)
			return;
		// only send emails if notifications are enabled
		boolean enabled = Boolean.valueOf(
			WebAppProps.get("livesearch.email.enabled", "true"));
		if (enabled == false)
			return;
		// gather list of subscribers
		List<String> subscribers = null;
		try {
			subscribers = SubscriptionManager.get_all_dataset_subscription(
				dataset.getDatasetID());
		} catch (Throwable error) {
		} finally {
			if (subscribers == null || subscribers.size() < 1)
				return;
		}
		// gather list of subscriber email addresses
		AccountManager accounts = AccountManager.getInstance();
		StringBuffer addresses = new StringBuffer();
		for (String subscriber : subscribers) {
			Map<String, String> user = accounts.getProfile(subscriber);
			if (user != null) {
				String address = user.get("email");
				if (address != null && address.trim().equals("") == false) {
					// prepend a space if this isn't the first email in the list
					if (addresses.length() > 0)
						addresses.append(" ");
					addresses.append(address);
				}
			}
		}
		if (addresses.length() < 1)
			return;
		// retrieve proper email implementation for this workflow
		WorkflowNotification email =
			new DatasetSubscriberNotification(dataset, operation, message);
		// send email
        if (sendEmail(
        		addresses.toString(), email.getSubject(), email.getContent()))
        	logger.info("Subscriber notification email " +
        		"was sent to [{}] for dataset [{}]",
        		addresses.toString(), dataset.getDatasetIDString());
        else logger.error("Failed to send subscriber " +
        	"notification email to [{}] for dataset [{}]",
        	addresses.toString(), dataset.getDatasetIDString());
	}
	
	public static void contactAdministrator(String content) {
		boolean enabled = Boolean.valueOf(WebAppProps.get("livesearch.email.enabled", "true"));
		if(!enabled) return;
		String emails = WebAppProps.get("livesearch.admin.contacts");
    	String host = WebAppProps.get("livesearch.host.address");
        String subject = "[SEVERE] ProteoSAFe exception at " + host;
        if (sendEmail(emails, subject, content))
        	logger.info("Administrator notification mail sent.");
        else logger.error(
        	String.format("Failed to notify administrators. Message=[%s]", content));
	}
}
