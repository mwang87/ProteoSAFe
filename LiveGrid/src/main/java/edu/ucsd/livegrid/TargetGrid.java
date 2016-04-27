package edu.ucsd.livegrid;


import java.io.File;
import java.net.InetAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import edu.ucsd.saint.commons.xml.XmlUtils;

public class TargetGrid {
	private String name, scriptPath;
	private InetAddress address; 
	private boolean valid;
	private Logger logger = LoggerFactory.getLogger(TargetGrid.class);

	public TargetGrid(File gridFolder){
		valid = false;
		if(!gridFolder.isDirectory()) return;
		Document settings = getSettings(gridFolder);
		if(settings == null) return;
		
		Element eName = XmlUtils.getElement(settings, "name");
		Element eAddr = XmlUtils.getElement(settings, "grid-address");
		Element eScript = XmlUtils.getElement(settings, "script-path");
		String  path = gridFolder.getAbsolutePath();		
		if(eName == null || eAddr == null || eScript == null){
			logger.error("[{}/settings.xml] is invalid; elements are missing", path);
			return;
		}		
		name = eName.getTextContent();
		try {
			address = InetAddress.getByName(eAddr.getTextContent());
		}catch (Exception e) {
			logger.error("[" + path + "/settings.xml] is invalid; invalid address", e);
			return;
		}
		scriptPath = eScript.getTextContent();
		valid = true;
	}
	
	private Document getSettings(File folder){
		File settings = new File(folder, "settings.xml");
		try{
			if(settings.isFile())
				return (settings.exists() && settings.isFile()) ?
					XmlUtils.parseXML(settings.getAbsolutePath()) : null;
		}
		catch(Exception e){
			logger.error("[" + settings.getAbsolutePath() + "] is missing or corrupted", e);
		}
		return null;
	}
	
	public String getName() { return name; }
	public InetAddress getAddress() { return address; }
	public String getScriptPath() { return scriptPath; }
	public boolean isValid() { return valid; }
}
