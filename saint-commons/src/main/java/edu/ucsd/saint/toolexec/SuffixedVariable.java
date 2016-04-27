package edu.ucsd.saint.toolexec;


import java.io.File;
import java.io.FilenameFilter;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import edu.ucsd.saint.commons.Helpers;
import edu.ucsd.saint.commons.xml.XmlUtils;
import edu.ucsd.saint.toolexec.AbstractVariable;
import edu.ucsd.saint.toolexec.ExecEnvironment;
import edu.ucsd.saint.toolexec.FileHandle;
import edu.ucsd.saint.toolexec.SuffixedVariable;


public class SuffixedVariable extends AbstractVariable {

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(SuffixedVariable.class);
	private final List<String> extensions;
	private final FilenameFilter filter;
	private final Collection<FileHandle> identifiers;
	private final String base;
	
	SuffixedVariable(ExecEnvironment env, Element statement, String base) {
		super(env, statement);
		this.base = base;

		String extensionSpec = statement.getAttribute("extension");
		identifiers = new LinkedList<FileHandle>();
		extensions = new LinkedList<String>();
		// boolean variable that indicates the allowed extensions are listed explicitly or not
		boolean directListing = !extensionSpec.isEmpty();

		if(directListing)
			for(String ext: extensionSpec.split("\\|"))
				extensions.add(ext.toLowerCase());
		else{
			Element emt = environment.getDataTypeDecl(getDatatype());
			for(Element field: XmlUtils.getChildElements(emt))
				if(field.getNodeName().equals("file"))
					extensions.add(field.getAttribute("extension").toLowerCase());
		}
		filter = new FilenameFilter (){
			public boolean accept(File dir, String filename) {
				return extensions.contains(FilenameUtils.getExtension(filename).toLowerCase());
			}};

		if(namingRequired()){
			String matchNameWith = statement.getAttribute("matchNameWith");
			Collection<FileHandle> ids = environment.getIdentifiers(matchNameWith);
			String given = null;
			if(ids.size() == 1){
				String name = ids.iterator().next().getName();
				String subname = name.split(";")[0];
				given = FilenameUtils.getBaseName(subname);
			}
			if(given == null)
				given = Helpers.getUUID(false);
			identifiers.add(
				new FileHandle(
					concat(base, given  + "." +  extensions.get(0)),
					0));
		}
	}

	SuffixedVariable(ExecEnvironment env, Element statement, String base, Collection<FileHandle> input) {
		this(env, statement, base);
		if(input == null) return;
		for(FileHandle handle: input){
			String filenames = handle.getName();
			int index = handle.getIndex(); 
			for(String name: filenames.split(";")){
				boolean accepted = extensions.contains(
					FilenameUtils.getExtension(name).toLowerCase());
				if(accepted)
					identifiers.add(new FileHandle(name, index));				
			}
		}
	}

	public void matchContext(File context){
		if(namingRequired() || !context.exists() || !context.isDirectory())
			return;
		int i = 0;
		for(File file: context.listFiles(filter))
			identifiers.add(
				new FileHandle(
					concat(base, file.getName()), i++));
	}

	public String evaluate(String token){
		switch(getType()){
		case FILE:
			String result = identifiers.isEmpty() ? null : identifiers.iterator().next().getName();
			return result;
		case FOLDER:
			return base;
		default:
			return null;
		}
	}

	public Collection<FileHandle> getIdentifiers(String token) {
		return identifiers;
	}
}
