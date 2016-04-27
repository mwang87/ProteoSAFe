package edu.ucsd.saint.commons;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConnectionPool {
	private DataSource pool;
	private int counter = 0;

	public static final Logger logger = LoggerFactory.getLogger(ConnectionPool.class);
	
	static{		
		logger.info("Loggers for [ConnectionPool] is initiated");
	}
	
	public ConnectionPool(String resourceJNDIName){
		pool = null;
		try {
			// Look up a DataSource, which is a connection pool created by
			// Tomcat using apache DBCP
			pool = (DataSource)new InitialContext().lookup(resourceJNDIName);
			if (pool == null)
				logger.info("<{}> is an unknown DataSource", resourceJNDIName);
		} catch (NamingException ne) {
			logger.info("Failed to load connection pool", resourceJNDIName);
		}
	}

	public synchronized Connection aquireConnection() throws SQLException{
		if(pool == null) throw new SQLException("Can't access DataSource");
		Connection conn = pool.getConnection( );
		logger.debug("\tAquire connection [count={}]:{}", ++counter, printStack());
		return conn;
	}
	
	public synchronized void yieldConnection(Connection conn) {
		try{
			if(conn != null){
				conn.close();
				logger.debug("\tYield connection [count={}]@{}", --counter, printStack());
			}
		}
		catch(SQLException e){
			logger.error(
				String.format(
					"SQL eError code=%d, state=%s, msg=%s%n",
					e.getErrorCode(), e.getSQLState(), e.getLocalizedMessage()));
		}
	}
	
	private static String printStack(){
		StackTraceElement elements[] = Thread.currentThread().getStackTrace();
		StringBuffer buffer = new StringBuffer();
		for(int i = 3; i < 5 && i < elements.length; i++){
			StackTraceElement element = elements[i];
			buffer.append("\n\t[")
				.append(element.getFileName())
				.append('.')
				.append(element.getMethodName())
				.append("@")
				.append(element.getLineNumber())
				.append("]");
		}
		return buffer.toString();
	}
	
	public void close(Object ... args){
		for(Object obj: args){
			if(obj instanceof Statement)
				closeStatement((Statement)obj);
			else if(obj instanceof ResultSet)
				closeResultSet((ResultSet)obj);
			else if(obj instanceof Connection)
				yieldConnection((Connection)obj);
		}
	}
	
	private static void closeStatement(Statement stmt){
		try{
			if(stmt != null)
				stmt.close();
		} catch (SQLException e){
			logger.info("Failed to close statement", e);
		}
	}
	
	private static void closeResultSet(ResultSet rs){
		try{
			if(rs != null)
				rs.close();
		} catch (SQLException e){
			logger.info("Failed to close ResultSet", e);
		}
	}
	
}
