package edu.ucsd.livegrid;


import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import edu.ucsd.saint.commons.WebAppProps;

public class GridRepository {

	private static Map<String, TargetGrid> targets; 
	
	static{
		targets = new HashMap<String, TargetGrid>();
		File folder = new File(WebAppProps.getPath("livegrid.policy.path"), "target");
		for(File f: folder.listFiles())
			if(f.isDirectory()){
				TargetGrid grid = new TargetGrid(f);
				if(grid.isValid())
					targets.put(grid.getAddress().getHostAddress(), grid);
			}
	}
	
	public static TargetGrid get(String name){
		return targets.get(name);
	}

	public static Collection<TargetGrid> list(){
		return targets.values();
	}
}
