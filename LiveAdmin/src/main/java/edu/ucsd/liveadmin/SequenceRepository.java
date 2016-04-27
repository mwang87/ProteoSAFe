package edu.ucsd.liveadmin;

import java.io.File;
import java.io.FilenameFilter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsd.saint.commons.ConnectionPool;
import edu.ucsd.saint.commons.WebAppProps;

public class SequenceRepository {

	private static Lock read, write;
	private static Map<String, SequenceFile> sequences;
	private static final Logger logger;
	
	static{
		logger = LoggerFactory.getLogger(SequenceRepository.class);
		ReentrantReadWriteLock rw = new ReentrantReadWriteLock();
		read = rw.readLock();
		write = rw.writeLock();
		updateSequences();
		logger.info("Done initializing sequence repository");		
	}
	
	public static void updateSequences(){
		sequences = new HashMap<String, SequenceFile>();
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		ConnectionPool pool = null;
		write.lock();
		try{
			Map<String, SequenceFile> tempMap = new TreeMap<String, SequenceFile>();
			pool = Commons.getConnectionPool();
			conn = pool.aquireConnection();
			stmt = conn.createStatement();
			rs = stmt.executeQuery("SELECT * FROM sequences");
			while(rs.next()){
				String code = rs.getString("code");
				String display = rs.getString("display");
				String version = resolveVersion(code);
				if(version != null){
					String realCode = code + "." + version;
					String realDisplay = String.format("%s (%s, %s)", display, code, version); 
					tempMap.put(code, new SequenceFile(realCode, realDisplay));
					logger.debug("Sequence [{}] found", realDisplay);
				}
			}
			sequences = tempMap;
		}
		catch(Throwable th){
			logger.error("Failed to update protein sequence databases", th);
		}
		finally{			
			write.unlock();
			pool.close(rs, stmt, conn);
		}
	}
	
	private static class TrieFilter implements FilenameFilter{
		String code;
		public TrieFilter(String code){
			this.code = code;
		}
		public boolean accept(File dir, String name) {
			return name.startsWith(code) && name.endsWith(".trie") && !name.endsWith(".shuffle.trie") ;
		}		
	}
	
	public static String resolveVersion(String code){
		String sequencePath = WebAppProps.getPath("liveadmin.sequences.path");
		File repos = new File(sequencePath);
		File latest = null;
		
		if(repos.exists() && repos.isDirectory()){
			File files[] = repos.listFiles(new TrieFilter(code));
			if(files.length > 0){
				latest = files[0];
				for(File f: files)
					if(f.getName().compareTo(latest.getName()) > 0)
						latest = f;
			}
		}
		if(latest == null) return null;
		String name = latest.getName();
		return name.substring(code.length() + 1, name.length() - ".trie".length());
	}
	
	
	public static Collection<SequenceFile> getSequences(){
		read.lock();
		try{
			return sequences.values();
		}finally{read.unlock();}
	}
}
