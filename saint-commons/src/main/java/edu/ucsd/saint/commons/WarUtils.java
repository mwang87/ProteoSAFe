package edu.ucsd.saint.commons;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

public class WarUtils {
	
	/*
	 * A property file which bears the same name as the .war file  
	 * 
	 */
	
	public static Properties loadFromWarRoot(Class<?> clazz) throws Exception{
		Properties props = new Properties();
		File dummy = new File(clazz.getResource("/.dummy").toURI());
		while(!dummy.getName().equals("WEB-INF"))
			dummy = dummy.getParentFile();
		File war = dummy.getParentFile();
		File cfg = new File(war.getParentFile(), war.getName() + ".xml");
		props.loadFromXML(new FileInputStream(cfg));
		return props;
	}
}
