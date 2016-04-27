package edu.ucsd.livesearch.util;

import java.util.Collection;
import java.util.HashSet;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsd.saint.commons.WebAppProps;

public class Notifier
{
	/*========================================================================
	 * Constants
	 *========================================================================*/
	private static final Logger logger = LoggerFactory.getLogger(Notifier.class);
	
	/*========================================================================
	 * Properties
	 *========================================================================*/
	private String sender;
	private String name;
	InternetAddress[] recipients;
	
	/*========================================================================
	 * Constructors
	 *========================================================================*/
	public Notifier()
	throws AddressException {
		this(WebAppProps.get("livesearch.admin.contacts"));
	}
	
	public Notifier(String addresses)
	throws AddressException {
		if (addresses == null)
			throw new NullPointerException("Notifier objects must be " +
				"initialized with a non-null list of recipient addresses.");
		// split addresses by whitespace, and add them individually
		Collection<InternetAddress> recipients = new HashSet<InternetAddress>();
        for (String address : addresses.split("\\s"))
        	recipients.add(new InternetAddress(address));
        this.recipients = recipients.toArray(new InternetAddress[recipients.size()]);
		sender = WebAppProps.get("livesearch.admin.sender");
		name = WebAppProps.get(
			"livesearch.admin.title", "UCSD/CCMS ProteoSAFe");
	}
	
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	public boolean notify(String subject, String content) {
		if (subject == null)
			subject = "";
		if (content == null)
			content = "";
		try {
		    Properties properties = System.getProperties();
		    properties.put("mail.smtp.host", "localhost");
		    Session session = Session.getDefaultInstance(properties);
		    Message message = new MimeMessage(session);
		    message.setFrom(new InternetAddress(sender, name));
		    message.setRecipients(Message.RecipientType.TO, recipients);
	        message.setSubject(subject);
	        message.setText(content);
	        Transport.send(message);
	        return true;
		} catch (Throwable error) {
			logger.error("Error sending notification message", error);
			return false;
		}
	}
}
