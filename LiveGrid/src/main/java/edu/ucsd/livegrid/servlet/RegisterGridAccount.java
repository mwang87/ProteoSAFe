package edu.ucsd.livegrid.servlet;



import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.KeyPair;

import edu.ucsd.livegrid.GridPlanner;
import edu.ucsd.livegrid.GridRepository;
import edu.ucsd.livegrid.TargetGrid;
import edu.ucsd.livegrid.worker.PersonalWorker;
import edu.ucsd.saint.commons.WebAppProps;
import edu.ucsd.saint.commons.xml.Wrapper;
import edu.ucsd.saint.commons.xml.XmlUtils;

/**
 * Servlet implementation class RegisterGridAccount
 */
public class RegisterGridAccount extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static Logger logger = LoggerFactory.getLogger(RegisterGridAccount.class);
	
    /**
     * @see HttpServlet#HttpServlet()
     */
    public RegisterGridAccount() {
        super();
    }

    public static void modifyAccessMode(File file, String mode){
		if(file.exists()){
			try{
				String cmds[] = new String[]{
						"chmod", mode, file.getAbsolutePath() 
					};
				Runtime.getRuntime().exec(cmds).waitFor();
			}
			catch(Exception e){
				logger.error(
					"Failed to modify access mode of [" + file.getAbsolutePath() + "] to " + mode);
			}
		}
    }
    
	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws IOException{
		String user = request.getParameter("user");
		String pass = request.getParameter("password");
		String account = request.getParameter("account");
		String addr = request.getRemoteAddr();
		TargetGrid grid = GridRepository.get(addr); 
		Wrapper D = new Wrapper(XmlUtils.createXML());
		Element registered = D.E("registered", D.T("false"));
		Element site = D.E("site-name", WebAppProps.get("system.name"));
		Element result = D.E("registry", registered, site);

		if(!authenticateUser(user, pass))
			result.appendChild(D.E("message", D.T("Invalid username or password")));
		else if (grid == null)
			result.appendChild(D.E("message", D.T("The system cannot recognize this grid")));
		else{
			Scanner scanner = null;
			try{
				File policyFolder = createUserPolicy(user, grid, account);
				PersonalWorker policy = GridPlanner.getInstance().addPersonalWorker(policyFolder);
				scanner = new Scanner(policy.getPublicKey());
				String keyLiteral = scanner.nextLine();
				registered.setTextContent("true");
				result.appendChild(D.E("public-key", D.T(keyLiteral)));
			}
			catch(Exception e){
				logger.error("Failed to register a grid account", e);
			}
			finally{
				if(scanner != null) scanner.close();
			}
		}
		response.setContentType("text/html");
		XmlUtils.prettyPrint(result, response.getOutputStream());
	}

	private boolean authenticateUser(String user, String password){
		String site = WebAppProps.get("livesearch.service.url");
		String service = WebAppProps.get("livesearch.service.authenticateUser");
		String url = site + "/" + service;
		logger.info("Target URL=[{}]", url);
		try{
		    List <NameValuePair> pairs = new LinkedList <NameValuePair>();
		    pairs.add(new BasicNameValuePair("user", user));
		    pairs.add(new BasicNameValuePair("password", password));

		    HttpClient client = new DefaultHttpClient();
		    HttpPost post = new HttpPost(url);
		    post.setEntity(new UrlEncodedFormEntity(pairs, HTTP.UTF_8));
		    HttpResponse response = client.execute(post);
	        StatusLine resStatus = response.getStatusLine();
	        if (resStatus.getStatusCode() != 200){
	        	logger.error("Failed to authenticate user [{}]; " +
	        			"http response status [{}]",
	        			user, resStatus.getStatusCode());
	        }else{
	        	HttpEntity entity = response.getEntity();
	        	Document doc = XmlUtils.parseXML(entity.getContent());
	        	entity.getContent().close();
	        	Element authenticated = XmlUtils.getElement(doc, "authenticated");
	        	if(authenticated != null)
	        		return "true".equals(authenticated.getTextContent());
	        }
		}
	    catch(Exception e){
	    	logger.error(String.format(
	    		"Failed to authenticate user [%s] ", user), e);
	    }
	    return false;
	}
	
    private static File createUserPolicy(String user, TargetGrid grid, String account) throws JSchException, IOException{
		File folder = new File(WebAppProps.getPath("livegrid.policy.path"), "user/" + user);
		if(!folder.exists())
			folder.mkdir();
		File pubKeyFile = new File(folder, "key.pub");
		File prvKeyFile = new File(folder, "key.rsa"); 
		File policyFile = new File(folder, "policy.xml");

 		JSch jsch = new JSch();
		KeyPair kpair = KeyPair.genKeyPair(jsch, KeyPair.RSA, 2048);
		kpair.setPassphrase("");
		kpair.writePrivateKey(prvKeyFile.getAbsolutePath());
		kpair.writePublicKey(pubKeyFile.getAbsolutePath(),
			String.format("%s[%s@%s]", user, account, grid.getAddress().getHostName()));
		kpair.dispose();
		modifyAccessMode(prvKeyFile, "600");
		
		logger.info("pub:[{}], prv:[{}]", pubKeyFile.getAbsolutePath(), prvKeyFile.getAbsolutePath());
		Wrapper d = new Wrapper(XmlUtils.createXML());
		Element result = d.E("user-grid-policy",
			d.E("name", d.T(account)),
			d.E("grid-address", d.T(grid.getAddress().getHostName())),
			d.E("grid-account", d.T(account)),
			d.E("owner", d.T(user))
		);
		XmlUtils.prettyPrint(result, new FileOutputStream(policyFile));
		return folder;
     }
}
