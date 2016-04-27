package edu.ucsd.saint.toolexec;

import java.io.File;
import java.util.Collection;
import java.util.LinkedList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import edu.ucsd.saint.commons.Helpers;
import edu.ucsd.saint.toolexec.AbstractVariable;
import edu.ucsd.saint.toolexec.ExecEnvironment;
import edu.ucsd.saint.toolexec.FileHandle;

public class SimpleVariable extends AbstractVariable{
	protected Collection<FileHandle> identifiers;

	private String base;

	private static Logger logger = LoggerFactory.getLogger(SimpleVariable.class);

	SimpleVariable(ExecEnvironment env, Element statement, String base){
		super(env, statement);
		this.base = base;
		
		identifiers = new LinkedList<FileHandle>();
		if(namingRequired())
			identifiers.add(
				new FileHandle( 
					String.format("%s/%s", base, Helpers.getUUID(false)),
					0));
	}

	SimpleVariable(ExecEnvironment env, Element statement, String base, Collection<FileHandle> input){
		this(env, statement, base);
		if(input == null) return;
		for(FileHandle handle: input)
			identifiers.add(handle);
	}

	private final class Counter{
		int value;

		public Counter(){this(0);}

		public Counter(int value){this.value = value;}
		
		@SuppressWarnings("unused")
		public int get(){ return value; }

		@SuppressWarnings("unused")
		public void increase(){ value++; }

		public int getAndIncrease(){
			int ret = value;
			value++;
			return ret;
		}
	}

	private void matchContext(File folder, String baseName, Counter counter){
		for(File file: folder.listFiles()){
			String name = concat(baseName, file.getName());
			if(file.isFile()){
				identifiers.add(
					new FileHandle(name, counter.getAndIncrease()));
				logger.info("SimpleVariable.matchContext  [{}]", name);
			}
			else if(file.isDirectory())
				matchContext(file, name, counter);
		}
	}
	
	public void matchContext(File context){
		if(namingRequired() || !context.exists() || !context.isDirectory())
			return;
		matchContext(context, base, new Counter());
	}

	public String evaluate(String token){
		switch(getType()){
		case FILE:
			return identifiers.isEmpty() ? null : identifiers.iterator().next().getName();
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
