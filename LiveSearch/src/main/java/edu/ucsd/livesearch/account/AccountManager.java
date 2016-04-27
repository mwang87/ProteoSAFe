package edu.ucsd.livesearch.account;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsd.livesearch.dataset.Dataset;
import edu.ucsd.livesearch.servlet.ServletUtils;
import edu.ucsd.livesearch.storage.FileManager;
import edu.ucsd.livesearch.util.Commons;
import edu.ucsd.saint.commons.ConnectionPool;
import edu.ucsd.saint.commons.WebAppProps;

public class AccountManager
{
	/*========================================================================
	 * Constants
	 *========================================================================*/
	public static AccountManager manager;
	private static final Logger logger;
	private static final String[] SYNCHRONIZED_HOSTS;
	static {
		logger = LoggerFactory.getLogger(AccountManager.class);	
		ConnectionPool pool = new ConnectionPool(
			"java:comp/env/" + WebAppProps.get("livesearch.jdbc.account"));
		manager = new AccountManager(pool);
		// extract synchronized hosts from comma-delimited property string
		String syncHosts = WebAppProps.getPath("livesearch.sync.hosts", "");
		if (syncHosts == null || syncHosts.trim().equals(""))
			SYNCHRONIZED_HOSTS = null;
		else SYNCHRONIZED_HOSTS = syncHosts.split(",");
	}
	// reserved usernames - regular users should not be able to register these
	// TODO: this list should be encoded in an external configuration file
	private static final String[] RESERVED_USERNAMES = new String[]{
		"ccms", "massive", "gnps", "proteosafe", "livesearch", "registered",
		"user", "guest", "test", "demo", "public_data", "speclibs",
		"continuous", "converter"
	};
	
	/*========================================================================
	 * Properties
	 *========================================================================*/
	private ConnectionPool pool;
	
	/*========================================================================
	 * Constructors
	 *========================================================================*/
	private AccountManager(ConnectionPool pool){
		this.pool = pool;
	}
	
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	public static AccountManager getInstance(){
		return manager;
	}
	
	public boolean accountExists(String user)
	throws Exception {
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			conn = pool.aquireConnection();
			stmt = conn.prepareStatement("SELECT * FROM users WHERE user_id=?");
			stmt.setString(1, user);
			rs = stmt.executeQuery();
			return rs.next();
		} catch (Throwable error) {
			logger.warn("Failed to check account existence", error);
		} finally {
			pool.close(rs, stmt, conn);
		}
		throw new Exception("Failed to check account existence");
	}

	public boolean tryRegister(
		String user, String password, Map<String, String> params
	) throws Exception {		
		if (accountExists(user))
			return false;
		else if (isValidUsername(user) == false) {
			String message = String.format(
				"Account registration failed: username \"%s\" is not valid.",
				user);
			logger.error(message);
			throw new IllegalArgumentException(message);
		}
		Connection conn = null;
		PreparedStatement stmt = null;
		try {
			conn = pool.aquireConnection();
			String fieldString = "user_id, salt, hash, create_time";
			String valueString = "?, ?, PASSWORD(?), NOW()";
			Collection<String> values = new LinkedList<String>();
			values.add(user);
			values.add(scramble(password));
			values.add(password);
			for(Entry<String, String> entry: params.entrySet()) {
				fieldString += ", " + entry.getKey();
				valueString += ", ?";
				values.add(entry.getValue());
			}
			stmt = conn.prepareStatement(
				String.format("INSERT INTO users(%s) VALUES(%s)",
				fieldString, valueString));
			int i = 1;
			for (String value: values)
				stmt.setString(i++, value);
			if (stmt.executeUpdate() == 1) {
				syncUserSpaces(user);
				return true;
			} else throw new RuntimeException(
				"Account registration database update did not succeed.");
		} catch (Throwable error) {
			String message = String.format(
				"There was an error registering account \"%s\".", user);
			logger.error(message, error);
			throw new Exception(message, error);
		} finally {
			pool.close(stmt, conn); 
		}
	}
	
	public boolean authenticate(String user, String password)
	throws Exception {
		if (canLogin(user) == false)
			throw new IllegalArgumentException(
				String.format("User \"%s\" is restricted - " +
				"this user should not be logging in!", user));
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			conn = pool.aquireConnection();
			stmt = conn.prepareStatement(
				"SELECT salt FROM users WHERE user_id=?");
			stmt.setString(1, user);
			rs = stmt.executeQuery();
			if (!rs.next())
				return false;
			String salt = rs.getString("salt");
			return checkScramble(password, salt);
		} catch (Throwable error) {
			logger.warn("Failed authentication", error);
		} finally {
			pool.close(rs, stmt, conn);
		}
		throw new Exception("Failed authentication due to reasons unknown");
	}

	public boolean checkRole(String user, String role) {
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			conn = pool.aquireConnection();
			stmt = conn.prepareStatement(
				"SELECT * FROM roles WHERE user_id=? AND role=?");
			stmt.setString(1, user);
			stmt.setString(2, role);
			rs = stmt.executeQuery();
			return rs.next();
		} catch (Throwable error) {
			logger.warn("Failed to check roles", error);
			return false;
		} finally {
			pool.close(rs, stmt, conn);
		}
	}
	
	public Map<String, String> getProfile(String user){
		Map<String, String>result = new HashMap<String, String>();
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try{
			conn = pool.aquireConnection();
			stmt = conn.prepareStatement(
				String.format("SELECT * FROM users WHERE user_id=?"));
			stmt.setString(1, user);
			rs = stmt.executeQuery();
			if(rs.next()){
				result.put("realname",	rs.getString("realname"));
				result.put("email",		rs.getString("email"));
				result.put("organization", rs.getString("organization"));
				result.put("hash",		rs.getString("hash"));
			}
		}
		catch(Throwable th){
			logger.warn("Failed to get user profiles", th);
		}
		finally{
			pool.close(rs, stmt, conn);
		}
		return result;
	}

	public boolean updateProfile(
		String user, String password, Map<String, String> params
	) throws Exception {
		Connection conn = null;
		PreparedStatement stmt = null;
		try {
			conn = pool.aquireConnection();
			StringBuffer assignments = new StringBuffer();
			Collection<String> values = new LinkedList<String>();
			// update password salt/hash only when the user provides a
			// non-blank value; otherwise do not update password salt/hash
			if (password.equals("") == false) {
				assignments.append("salt=?, hash=PASSWORD(?)");
				// base64 converter may or may not append /r/n;
				// trim the encrypted value to prevent inconsistency
				values.add(scramble(password).trim());
				values.add(password);
			}
			for (Entry<String, String> entry: params.entrySet()) {
				// only prepend a comma if previous values have been added
				if (assignments.length() > 0)
					assignments.append(", ");
				assignments.append(entry.getKey()).append("=?");
				values.add(entry.getValue());
			}
			values.add(user);
			stmt = conn.prepareStatement(
				String.format("UPDATE users SET %s WHERE user_id=?",
				assignments));
			int i = 1;
			for (String value: values)
				stmt.setString(i++, value);
			if (stmt.executeUpdate() == 1)
				return true;
		} catch (Throwable error) {
			logger.info("Failed to update user profile", error);
		} finally {
			pool.close(stmt, conn);
		}
		throw new Exception(
			"Failed to update user profile for reasons unknown");
	}
	
	public boolean updatePassword(String user, String password)
	throws Exception {
		Connection conn = null;
		PreparedStatement stmt = null;
		try {
			conn = pool.aquireConnection();
			stmt = conn.prepareStatement(
				"UPDATE users SET salt=?, hash=PASSWORD(?) WHERE user_id=?");
			stmt.setString(1, scramble(password).trim());
			stmt.setString(2, password);
			stmt.setString(3, user);
			if (stmt.executeUpdate() == 1)
				return true;
		} catch(Throwable th){
			logger.warn("Failed to update hash code", th);
		} finally {
			pool.close(stmt, conn);
		}
		// if the update did not complete successfully, throw an exception
		throw new Exception(
			"Failed to update hash code due to reasons unknown.");
	}
	
	public Map<String, Boolean> getWorkflowAccessibility(String user) {
		// set up query properties
		Map<String, Boolean> accessibility = new HashMap<String, Boolean>();
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		// perform query
		try {
			conn = pool.aquireConnection();
			stmt = conn.prepareStatement("SELECT * FROM access_rights " +
				"WHERE resource LIKE 'workflow:%'");
			rs = stmt.executeQuery();
			while (rs.next()) {
				String resource = rs.getString("resource");
				if (resource != null && resource.length() > 9) {
					// strip off leading "workflow:"
					resource = resource.substring(9);
					// if no user is logged in, then all restricted
					// workflows are inaccessible
					if (user == null)
						accessibility.put(resource, false);
					else {
						// check to see if this workflow
						// is already present in the map
						Boolean accessible = accessibility.get(resource);
						// if it is not present, or if accessibility for this
						// user hasn't been confirmed yet, check it
						if (accessible == null || accessible == false) {
							String subject = rs.getString("subject");
							if (verifyAccessibility(user, subject))
								accessibility.put(resource, true);
							else accessibility.put(resource, false);
						}
					}
				}
			}
		} catch (Throwable error) {
			logger.error(String.format(
				"Error querying accessibility of workflows for user \"%s\":",
				user), error);
		} finally {
			pool.close(stmt, conn, rs);
		}
		if (accessibility.size() < 1)
			return null;
		else return accessibility;
	}
	
	@SuppressWarnings("resource")
	public boolean isWorkflowAccessible(String workflow, String user)
	throws Exception {
		if (workflow == null)
			return false;
		// if the user is an admin, then all workflows are accessible
		else if (isAdministrator(user))
			return true;
		// guest users are explicitly disabled from submitting
		// any workflow tasks on the MassIVE server
		else if (user != null && user.toLowerCase().startsWith("guest") &&
			ServletUtils.isMassIVESite())
			return false;
		// otherwise, check the database for accessibility
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		// perform query
		try {
			conn = pool.aquireConnection();
			stmt = conn.prepareStatement(
				"SELECT * FROM access_rights WHERE resource=?");
			stmt.setString(1, String.format("workflow:%s", workflow));
			rs = stmt.executeQuery();
			// if there are no access rights entries in the database for
			// this workflow, then it is accessible to everyone
			if (rs.next() == false)
				return true;
			// if this workflow is restricted to certain users,
			// and no user was provided, then it is not accessible
			else if (user == null)
				return false;
			// otherwise return true only if this user is registered
			// as having access to this workflow in the database
			else do {
				String subject = rs.getString("subject");
				if (verifyAccessibility(user, subject))
					return true;
			} while (rs.next());
		} catch (Throwable error) {
			logger.error(String.format(
				"Error querying accessibility of workflow \"%s\" " +
				"for user \"%s\":", workflow, user), error);
		} finally {
			pool.close(stmt, conn, rs);
		}
		return false;
	}
	
	public static boolean isRegistered(String user) {
		if (user == null)
			return false;
		else try {
			return getInstance().accountExists(user);
		} catch (Exception error) {
			return false;
		}
	}
	
	public static boolean isAdministrator(String user) {
		if (user == null)
			return false;
		else return getInstance().checkRole(user, "administrator");
	}
	
	public static boolean isReservedUsername(String user) {
		if (user == null)
			return false;
		else if (Dataset.isValidDatasetIDString(user))
			return true;
		else for (String reserved : RESERVED_USERNAMES)
			if (user.toLowerCase().startsWith(reserved))
				return true;
		return false;
	}
	
	public static boolean isValidUsername(String user) {
		if (user == null)
			return false;
		else if (isReservedUsername(user))
			return false;
		else return true;
	}
	
	public static boolean isActiveUsername(String user) {
		if (user == null)
			return false;
		else if (user.matches("Guest\\.(\\w)+-(\\d)+"))
			return true;
		else return isRegistered(user);
	}
	
	public static boolean canLogin(String user) {
		if (user == null)
			return false;
		else if (Dataset.isValidDatasetIDString(user))
			return false;
		else return true;
	}
	
	public boolean updateHash(String user, String password) throws Exception{
		Connection conn = null;
		PreparedStatement stmt = null;
		try{
			conn = pool.aquireConnection();
			stmt = conn.prepareStatement("UPDATE users SET salt=?, hash=PASSWORD(?) WHERE user_id=?");
			stmt.setString(1, scramble(password));
			stmt.setString(2, password);
			stmt.setString(3, user);
			if(stmt.executeUpdate() == 1)
				return true;
		}
		catch(Throwable th){
			logger.warn("Failed to update password hash/salt", th);
		}
		finally{
			pool.close(stmt, conn);
		}
		throw new Exception("Failed to update password hash/salt due to reasons unknown");
	}

	public static Collection<String> queryGroups(String identity){
		Collection<String> groups = new LinkedList<String>();
		if(identity == null) return groups;
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		ConnectionPool pool = null;
		try{
			pool = Commons.getConnectionPool();
			conn = pool.aquireConnection();
			stmt = conn.prepareStatement("SELECT * FROM groups WHERE user_id=?");
			stmt.setString(1, identity);
			rs = stmt.executeQuery();
			while(rs.next())
				groups.add(rs.getString("group_id"));
		}
		catch(Throwable th){
			logger.error("Error querying group_id for user " + identity, th);
		}
		finally{
			pool.close(rs, stmt, conn);
		}
		return groups;
	}

	private static final String PEM_BASE64_DIGITS = 
		"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
	
	private static String scramble(String plaintext){
		String preamble = "";
		Random r = new Random( );
		for(int i = 0; i < 10; i++)
			preamble += PEM_BASE64_DIGITS.charAt(r.nextInt(64));
		return preamble + encrypt(preamble + plaintext);
	}
	
	private static boolean checkScramble(String plaintext, String salt){
		String preamble = salt.substring(0, 10);
		String challenge = preamble + encrypt(preamble + plaintext);
		logger.info("Salt     : [{}]", salt.trim());
		logger.info("Challenge: [{}]", challenge.trim());
		return salt.trim().equals(challenge.trim());
	}

	private static String encrypt(String plaintext){
	    MessageDigest md = null;
	    try{
			md = MessageDigest.getInstance("SHA-256");
			md.update(plaintext.getBytes("UTF-8"));
		    byte raw[] = md.digest();
		    String hash = new String((new Base64()).encode(raw));
		    return hash;
	    }
	    catch(NoSuchAlgorithmException e){return null; }
	    catch(UnsupportedEncodingException e){return null; }
	}
	
	private static boolean verifyAccessibility(
		String user, String accessibility
	) {
		if (user == null || accessibility == null)
			return false;
		// this workflow is accessible to this user if
		// it's accessible to all registered users
		else if (accessibility.equals("user:registered"))
			return true;
		// otherwise this workflow is only accessible
		// to this user if it's explicitly assigned
		else if (accessibility.equals("user:" + user))
			return true;
		else return false;
	}
	
	private void syncUserSpaces(String user) {
		if (user == null)
			return;
		// sync the user's local private data spaces
		FileManager.syncFTPSpace(user);
		FileManager.syncUserSpace(user);
		// sync the user's remote private data spaces
		if (SYNCHRONIZED_HOSTS == null)
			return;
		for (String host : SYNCHRONIZED_HOSTS) {
			CloseableHttpClient client = null;
	        CloseableHttpResponse response = null;
		    try {
				client = HttpClients.createDefault();
				HttpPost post = new HttpPost(String.format(
					"http://%s/user/SyncUserSpace", host));
		    	List<NameValuePair> params = new ArrayList<NameValuePair>(1);
		        params.add(new BasicNameValuePair("user", user));
		        post.setEntity(new UrlEncodedFormEntity(params));
		        response = client.execute(post);
		        // TODO: check response, react appropriately
		    } catch (Throwable error) {
		        // TODO: react appropriately
		    } finally {
		    	try { client.close(); } catch (Throwable error) {}
		    	try { response.close(); } catch (Throwable error) {}
		    }
		}
	}
	
	// admin function to recover lost passwords
	public static void main(String[] args) {
		String username = "";
		String password = "password";
		String sql = String.format("UPDATE users " +
			"SET salt='%s', hash=PASSWORD('%s') WHERE user_id='%s';",
			scramble(password), password, username);
		System.out.println(sql);
	}
}
