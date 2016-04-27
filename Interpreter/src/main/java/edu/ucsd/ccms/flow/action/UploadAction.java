package edu.ucsd.ccms.flow.action;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import dapper.codelet.InputHandleResource;
import dapper.codelet.OutputHandleResource;
import edu.ucsd.saint.commons.archive.ArchiveUtils;
import edu.ucsd.saint.commons.archive.Archiver;
import edu.ucsd.saint.commons.archive.CompressionType;
import edu.ucsd.saint.commons.xml.XmlUtils;
import edu.ucsd.saint.toolexec.Variable;
import edu.ucsd.saint.toolexec.VariableFactory;

public class UploadAction extends Action{

	private static final Logger logger = LoggerFactory.getLogger(UploadAction.class);

	private List<Element> queries;
	private List<Element> uploads;
	private String compression;
	private String url;
	private String contentQuery;
	private int maxRetry = 5;

	private void initBinding(Element params){
		Map<String, Element> inEmts = XmlUtils.getNamedMap(params, "input", "port");
		Element binding = XmlUtils.getElements(params, "bind").get(0);
		queries = new LinkedList<Element>();
		uploads = new LinkedList<Element>();
		VariableFactory factory = environment.getFactory();
		for(Element emt: XmlUtils.getChildElements(binding)){
			String name = emt.getNodeName();
			if(name.equals("url"))
				url = emt.getAttribute("value");
			else if(name.equals("compression"))
				compression = emt.getAttribute("type");
			else if(name.equals("query"))
				queries.add(emt);
			else if (name.equals("contentQuery"))
				contentQuery = emt.getAttribute("name");
			else if(name.equals("upload")){
				String port = emt.getAttribute("port");
				Element stmt = mergeElements("upload", emt, inEmts.get(port));
				uploads.add(stmt );
				List<String> identifiers = inputHandles.get(port);
				if(identifiers == null) identifiers = new LinkedList<String>();
				factory.createInputVariable(stmt, 
					stmt.getAttribute("object"), identifiers);
			}
		}
	}

	public UploadAction(List<InputHandleResource> in,
			List<OutputHandleResource> out, Element params) {
		super(in, out, params);
		initBinding(params);
	}

	public void run() throws Exception {
		for(Element upload: uploads){
			UploadOptions options = getUploadOptions(upload);
			String objName = upload.getAttribute("object");
	        Variable var = environment.getVariable(objName);

			try{
				uploadVariable(options, var, objName);
			}
			catch(Exception e){
				logger.info("Failed to upload [{}] to [{}] due to Exception[{}]",
						new Object[]{objName, url, e.getClass().getName()});
			}
		}

		PrintWriter writer = null;
		try{ 		
			writer = new PrintWriter(new File(getGlobalFolder(".info"), ".done"));
			writer.printf("Task [%s] is done.%n", taskName);
		}
		finally{
			writer.close();
		}
	}

	private static class UploadOptions{
		final String url, compression, contentQuery;
		final List<Element> queries;
		UploadOptions(String url, String compression, String contentQuery, List<Element> query){
			this.url = url;
			this.compression = compression;
			this.contentQuery = contentQuery;
			this.queries = query;
		}
	}

	public UploadOptions getUploadOptions(Element uploadStmt){
		String newUrl = url;
		String newComp = compression;
		String newCtntQry = contentQuery;
		List<Element> qry = new LinkedList<Element>(queries);
		for(Element emt: XmlUtils.getChildElements(uploadStmt)){
			String name = emt.getNodeName();
			if(name.equals("url"))
				newUrl = emt.getAttribute("value");
			else if(name.equals("compression"))
				newComp = emt.getAttribute("type");
			else if(name.equals("query"))
				qry.add(emt);
			else if (name.equals("contentQuery"))
				newCtntQry = emt.getAttribute("name");
		}
		return new UploadOptions(newUrl, newComp, newCtntQry, qry);
	}

	private void uploadVariable(UploadOptions options, Variable var, String object)
		throws IOException{
		String url = options.url;
		CompressionType type = CompressionType.byTypeName(options.compression);
		String ctntQry = options.contentQuery;

		HttpClient httpclient = new DefaultHttpClient();
        try{
	        HttpPost httppost = new HttpPost(url);
	        MultipartEntity reqEntity = new MultipartEntity();
	
			for(Element query: options.queries){
				String name = query.getAttribute("name");
				String valueRef = query.getAttribute("valueRef");
				String value = valueRef.isEmpty() ? query.getAttribute("value") : environment.evaluate(valueRef);
				reqEntity.addPart(name, new StringBody(value));
				logger.info("Query [{}]=[{}]", name, value);
			}
	        File folder = getGlobalFolder(object);
	        File compressed = new File(getGlobalFolder(), "result." + options.compression);
			FileOutputStream out = new FileOutputStream(compressed);
	        Archiver archiver = ArchiveUtils.createArchiver(out, type);
	        ArchiveUtils.compress(archiver, folder);
	        
	        reqEntity.addPart(ctntQry, new FileBody(compressed));
	        httppost.setEntity(reqEntity);
	        for(int i = 0; i < maxRetry; i++){
		        HttpResponse response = httpclient.execute(httppost);
		        HttpEntity resEntity = response.getEntity();
		        logger.info(
		        	"Upload [{}] to [{}]: [{}]",
		        		new Object[]{folder.getAbsolutePath(), url, response.getStatusLine()});
		        StatusLine status = response.getStatusLine();
		        if (status.getStatusCode() == 200 || resEntity != null) {
		            resEntity.getContent().close();
		            break;
		        }
		        try{
		        	Thread.sleep(180000);
		        }
		        catch(Exception e){
		        	logger.trace("Uploading thread is interrupted");
		        }
			}
        }
        finally{
        	httpclient.getConnectionManager().shutdown();
        }
	}
}
