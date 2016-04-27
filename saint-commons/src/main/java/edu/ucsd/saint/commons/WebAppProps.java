package edu.ucsd.saint.commons;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.InvalidPropertiesFormatException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebAppProps implements ServletContextListener {
	private static Logger logger = LoggerFactory.getLogger(WebAppProps.class);

	private static Properties props = new Properties();
	private static File config = null;
	private static String base = null;
	
	static{
		File dummy = null;
		try {
			URL url = WebAppProps.class.getResource("/.dummy");
			if(url != null)
				dummy = new File(WebAppProps.class.getResource("/.dummy").toURI());
		} catch (URISyntaxException e) {
			logger.error("Failed to load webapp property file", e);
		}
		while(dummy != null && !dummy.getName().equals("WEB-INF"))
			dummy = dummy.getParentFile();
		if(dummy != null){
			File webinf = dummy.getParentFile();
			config = new File(webinf.getParentFile(), webinf.getName() + ".xml");
			logger.info("Webapp config file: {}", config.getAbsolutePath());
			loadProperties();
		}
		base = System.getProperty("WebAppProps.basepath");
		if(base == null)
			base = System.getProperty("user.dir");
	}
	
	public static boolean loadProperties(){
		synchronized(WebAppProps.class){
			if(config != null && config.exists())
				try {
					props.loadFromXML(new FileInputStream(config));
					substituteProperties(props);
					logger.info("Successfully loading webapp property file");
					return true;
				} catch (InvalidPropertiesFormatException e) {
					logger.error("Webapp property file is invalid", e);
				} catch (FileNotFoundException e) {
					logger.error("Webapp property file is missing", e);
				} catch (IOException e) {
					logger.error("Failed to load webapp property file", e);
				}
		}
		return false;
	}
	
	private static void substituteProperties(Properties props){
		Pattern SUB_PATTERN = Pattern.compile("\\$\\{([\\w\\.\\-]+)\\}");
		List<String> keys = new LinkedList<String>();
		for(Object obj: props.keySet())
			keys.add((String)obj);
		
		for(String key: keys){ 
			String val = props.getProperty(key);
			String newVal = val;
			Matcher matcher = SUB_PATTERN.matcher(val);
			while (matcher.find()) {
				String sysKey = matcher.group(1);
				String sysProp =  System.getProperty(sysKey);
				if(sysProp != null)
					newVal = newVal.replace(matcher.group(), sysProp);
			}
			if(!newVal.equals(val))
				props.setProperty(key, newVal);
		}
	}
	
	public static String get(String name){
		synchronized(WebAppProps.class){
			return props.getProperty(name);
		}
	}

	public static String get(String name, String def){
		synchronized(WebAppProps.class){
			return props.getProperty(name, def);
		}
	}

	public static String getPath(String name){
		synchronized(WebAppProps.class){
			return props.containsKey(name) ?
				FilenameUtils.concat(base, props.getProperty(name)).replace('\\', '/') : null;
		}
	}

	public static String getPath(String name, String def){
		synchronized(WebAppProps.class){
			return props.containsKey(name) ?
				FilenameUtils.concat(base, props.getProperty(name)).replace('\\', '/') : def;
		}
	}

	public void contextInitialized(ServletContextEvent event) {
/*		ServletContext context = event.getServletContext();
		config = new File(context.getRealPath("") + ".xml");
		logger.info("Webapp config file: {}", config.getAbsolutePath());
		loadProperties();*/
	}

	public void contextDestroyed(ServletContextEvent event) {
	}
}
