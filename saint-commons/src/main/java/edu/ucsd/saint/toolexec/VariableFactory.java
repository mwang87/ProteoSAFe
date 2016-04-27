package edu.ucsd.saint.toolexec;

import java.util.Collection;
import java.util.LinkedList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import edu.ucsd.saint.toolexec.AbstractVariable;
import edu.ucsd.saint.toolexec.ArgumentVariable;
import edu.ucsd.saint.toolexec.ExecEnvironment;
import edu.ucsd.saint.toolexec.FileHandle;
import edu.ucsd.saint.toolexec.PlaceHolder;
import edu.ucsd.saint.toolexec.PrefixedVariable;
import edu.ucsd.saint.toolexec.SimpleVariable;
import edu.ucsd.saint.toolexec.SuffixedVariable;
import edu.ucsd.saint.toolexec.Variable;
import edu.ucsd.saint.toolexec.VariableFactory;


public class VariableFactory {

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(VariableFactory.class);

	private ExecEnvironment environment; 

	VariableFactory(ExecEnvironment environment){
		this.environment = environment;
	}
	
	private boolean isSuffixed(Element statement){
		if(!statement.getAttribute("extension").isEmpty())
			return true;
		if(!statement.getAttribute("datatype").isEmpty()){
			Element decl = environment.getDataTypeDecl(
				statement.getAttribute("datatype"));
			if(decl != null)
				return decl.getAttribute("convention").equals("suffix set");
		}
		return false;
	}

	private boolean isPrefixed(Element statement){
		if(!statement.getAttribute("datatype").isEmpty()){
			Element decl = environment.getDataTypeDecl(statement.getAttribute("datatype"));
			if(decl != null)
				return decl.getAttribute("convention").equals("common prefix");
		}
		return false;
	}

	private Collection<FileHandle> parseHandles(Collection<String> input){
		Collection<FileHandle> handles = new LinkedList<FileHandle>();
		int index = 0;
		for(String str: input){
			int pos = str.indexOf(':');
			if(pos != -1){
				//int index = Integer.parseInt(str.substring(0, pos));
				String name = str.substring(pos + 1);
				handles.add(new FileHandle(name, index++));
			}
			else handles.add(new FileHandle(str, index++));
		}
		return handles;
	}
	
	// create variable object for input port/parameter
	public AbstractVariable createInputVariable(String requirement, String base, Collection<String> input){
		Element statement = environment.getRequirementStmt(requirement); 
		return createInputVariable(statement, base, input);
	}

	public AbstractVariable createInputVariable(Element statement, String base, Collection<String> input){
		Collection<FileHandle> handles = parseHandles(input);
		
		AbstractVariable var = null;
		if(isSuffixed(statement))		var = new SuffixedVariable(environment, statement, base, handles);
		else if(isPrefixed(statement))	var = new PrefixedVariable(environment, statement, base, handles);
		else						var = new SimpleVariable(environment, statement, base, handles);
		environment.putVariable(var.getName(), var);
		return var;
	}

	// create variable object for output port/parameter
	public AbstractVariable createOutputVariable(String product, String base){
		Element statement = environment.getProductionStmt(product);
		return createOutputVariable(statement, base);
	}
	
	public AbstractVariable createOutputVariable(Element statement, String base){
		AbstractVariable var = null;
		if(isSuffixed(statement))		var = new SuffixedVariable(environment, statement, base);
		else if(isPrefixed(statement))	var = new PrefixedVariable(environment, statement, base);
		else var =						new SimpleVariable(environment, statement, base);
		environment.putVariable(var.getName(), var);
		return var;
	}

	public Variable createArgumentVariable(String name, String value){
		Variable var = new ArgumentVariable(name, value);
		environment.putVariable(name, var);		
		return var;
	}

	public Variable createPlaceHolder(String name, String value){
		Variable var = new PlaceHolder(name, value);
		environment.putVariable(name, var);
		return var;
	}
}
