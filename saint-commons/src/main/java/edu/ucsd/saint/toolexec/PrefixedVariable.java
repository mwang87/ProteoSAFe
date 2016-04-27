package edu.ucsd.saint.toolexec;



import java.io.File;
import java.io.FileFilter;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.FileFileFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import edu.ucsd.saint.commons.Helpers;
import edu.ucsd.saint.commons.xml.XmlUtils;
import edu.ucsd.saint.toolexec.AbstractVariable;
import edu.ucsd.saint.toolexec.ExecEnvironment;
import edu.ucsd.saint.toolexec.FileHandle;

public class PrefixedVariable extends AbstractVariable {
	protected Collection<String> prefixes;
	protected Collection<String> suffixes;
	private   String base;

	private static Logger logger = LoggerFactory.getLogger(PrefixedVariable.class);

	PrefixedVariable(ExecEnvironment env, Element statement, String base) {
		super(env, statement);
		this.base = base;
		suffixes = new HashSet<String>();
		prefixes = new HashSet<String>();
		Element emt = environment.getDataTypeDecl(getDatatype());
		for(Element field: XmlUtils.getChildElements(emt))
			if(field.getNodeName().equals("file"))
				suffixes.add(field.getAttribute("extension"));
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
			prefixes.add(given);
		}
	}

	PrefixedVariable(ExecEnvironment env, Element statement, String base, Collection<FileHandle> input) {
		this(env, statement, base);
		if(input == null) return;
		for(FileHandle handle: input){
			String id = handle.getName();
			String names[] = id.split(";");
			if(names.length < 1) continue;
			int begin = names[0].lastIndexOf('/');
			int end = names[0].lastIndexOf('.');
			if(begin == -1 || end == -1) continue;
			String prefix = names[0].substring(begin + 1, end);
			prefixes.add(prefix);
		}
	}


	public void matchContext(File context){
		if(namingRequired() || !context.exists() || !context.isDirectory())
			return;
		Set<String> prefixCandidates = new HashSet<String>();
		Set<String> matchAgainst = new HashSet<String>();
		FileFilter filter = FileFileFilter.FILE;
		for(File file: context.listFiles(filter)){
			String name = file.getName();
			int last = name.lastIndexOf('.');
			if(last != -1){
				String prefix = name.substring(0, last);
				prefixCandidates.add(prefix);
				matchAgainst.add(name);
			}
		}
	CAND_LOOP:
		for(String candidate: prefixCandidates){
			for(String suffix: suffixes)
				if(!matchAgainst.contains(candidate + "." + suffix))
					continue CAND_LOOP;
			prefixes.add(candidate);
		}
	}

	public String evaluate(String token){
//		logger.info("Prefix variable [{}] with type: [{}]", getName(), getType());
		if(getType() == Type.FILE){
//			logger.info("Suffix [{}] is valid: [{}]", token, suffixes.contains(token));
			if(suffixes.contains(token) && !prefixes.isEmpty())
				return concat(base, prefixes.iterator().next() + "." + token);
		}
		return base;
	}

	public Collection<FileHandle> getIdentifiers(String token){
		Collection<FileHandle> result = new LinkedList<FileHandle>();
		for(String pre: prefixes)
			logger.info("prefix: [{}]", pre);
		for(String suf: suffixes)
			logger.info("suffixes: [{}]", suf);
		int i = 0;
		if(token != null && suffixes.contains(token))
			for(String prefix: prefixes)
				result.add(new FileHandle(
						concat(base, prefix + "." + token), i++));
		else
			for(String prefix: prefixes){
				StringBuffer buffer = new StringBuffer();
				for(String suffix: suffixes)
					buffer.append(concat(base, prefix + "." + suffix))
						  .append(";");
				result.add(new FileHandle(buffer.toString(), i++));
			}
		return result;
	}
}
