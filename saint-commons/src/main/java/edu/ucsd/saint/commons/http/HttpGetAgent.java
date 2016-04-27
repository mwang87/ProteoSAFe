package edu.ucsd.saint.commons.http;


import java.io.IOException;
import java.io.InputStream;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.io.ChunkedInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpGetAgent
{
	private final Logger logger = LoggerFactory.getLogger(HttpGetAgent.class);
	
	private RetryPolicy policy;
	private HttpClient client;
	private HttpResponse response;
	
	public HttpGetAgent(RetryPolicy policy){
		this.policy = policy;
		this.client = new DefaultHttpClient();
	}

	public HttpEntity execute(String url)
	throws Exception {
		policy.reset();
	    HttpGet method = new HttpGet(url);
		Exception e = null;
		StatusLine status = null;
		while (policy.keepTrying()) {
			try {
				logger.info("GET begins [{}]", url);
				response = client.execute(method);
				logger.info("GET ends   [{}]", url);
				HttpEntity entity = response.getEntity();
				status = response.getStatusLine();
				if (status.getStatusCode() == 200)
					return entity;
				Thread.sleep(policy.waitingPeriod());
	    	} catch(Exception error) {
	    		e = error;
	    	}
	    }
	    if (status != null)
	    	throw new IOException(String.format(
	        	"Unsuccessful execution of GET method.%n" +
	        	"Response: [%d] %s", 
	        	status.getStatusCode(), status.getReasonPhrase()));
	    if (e != null)
	    	throw e;
	    throw (new IOException(
	    	"Failed to perform GET method for unknown reasons"));
	}
	
	public HttpResponse getResponse() {
		return response;
	}
	
	public String getHeader(String header) {
		if (header == null || response == null)
			return null;
		Header[] headers = response.getHeaders(header);
		if (headers == null || headers.length < 1)
			return null;
		else return headers[0].getValue();
	}
	
	public String getFooter(String footer) {
		if (footer == null || response == null)
			return null;
		try {
			InputStream input = response.getEntity().getContent();
			if (input == null || input instanceof ChunkedInputStream == false)
				return null;
			Header[] footers = ((ChunkedInputStream)input).getFooters();
			for (Header header : footers)
				if (header.getName().equals(footer))
					return header.getValue();
		} catch (Throwable error) {
			return null;
		}
		return null;
	}
	
	public boolean close() {
		client.getConnectionManager().shutdown();
		return true;
	}
}
