package edu.ucsd.livesearch.storage;

import java.util.Collection;
import java.util.LinkedList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import edu.ucsd.saint.commons.WebAppProps;
import edu.ucsd.saint.commons.xml.XmlUtils;

public class SequenceRepository {

	private static final Logger logger = LoggerFactory.getLogger(SequenceRepository.class);

	public static Collection<SequenceFile> getSequences(){
		Collection<SequenceFile> sequences = new LinkedList<SequenceFile>();
	    boolean success = false;
	    
	    String context = WebAppProps.get("liveadmin.service.url");
	    String service = WebAppProps.get("liveadmin.service.querySequences");
	    String url = context + "/" + service; 

	    HttpClient client = new DefaultHttpClient();
	    try{
	 		int maxRetry = 3;
	    	client = new DefaultHttpClient(); 		
		    HttpGet method = new HttpGet(url);
	        for(int i = 0; i < maxRetry && !success; i++){
		        HttpResponse response = client.execute(method);
		        StatusLine status = response.getStatusLine();
		        if (status.getStatusCode() == 200)
		        	success = true;
		        HttpEntity entity = response.getEntity();
		        Document doc = XmlUtils.parseXML(entity.getContent());
		        entity.getContent().close();
		        for(Element seq: XmlUtils.getElements(doc, "sequence")){
		        	String code = XmlUtils.getElement(seq, "code").getTextContent();
		        	String display = XmlUtils.getElement(seq, "display").getTextContent();
		        	sequences.add(new SequenceFile(code, display));
		        }
	        }
	    }catch(Exception e){
	    	logger.trace("Failed to query sequences", e);
	    	e.printStackTrace();
	    }
	    finally{
	    	client.getConnectionManager().shutdown();
	    }
		return sequences;
	}
}
