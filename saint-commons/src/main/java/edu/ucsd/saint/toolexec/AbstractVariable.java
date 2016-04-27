package edu.ucsd.saint.toolexec;



import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import edu.ucsd.saint.toolexec.AbstractVariable;
import edu.ucsd.saint.toolexec.ExecEnvironment;
import edu.ucsd.saint.toolexec.Variable;


public abstract class AbstractVariable implements Variable{

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(AbstractVariable.class); 

	private String name;
	private final Type type;
	private final String datatype;
	private boolean namingRequired;
	protected ExecEnvironment environment;

	AbstractVariable(ExecEnvironment env, Element statement){
		environment = env;
		name = StringUtils.defaultIfEmpty(statement.getAttribute("name"), statement.getAttribute("object"));
		type = Type.valueOf(statement.getAttribute("type").toUpperCase()); 
		datatype = statement.getAttribute("datatype");
		namingRequired = 
			statement.getNodeName().equals("produce") &&
			(type == Type.FILE) &&
			statement.getAttribute("naming").equals("explicit");
	}
	
	public String getName() { return name; }

	public Type getType() { return type; }

	public String getDatatype() { return datatype; }

	protected boolean namingRequired(){ return namingRequired; }
	
	public void setNamingRequired(boolean required){
		this.namingRequired = this.namingRequired && required; 
	}

	protected static String concat(String base, String name){
		return StringUtils.isEmpty(base) ? name : base + '/' + name;
	}
}
