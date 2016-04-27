package edu.ucsd.livesearch.inspect;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Scanner;

import edu.ucsd.livesearch.task.Task;
import edu.ucsd.livesearch.util.Commons;

public class InspectSummary {
	private Task task;
	private boolean found;
	private String protein, desc;
	private Collection<String> dist;
	public InspectSummary(Task task, String proteinId, String protein){
		this.task = task;
		this.found = false;
		this.protein = protein;
		this.desc = null;
		this.dist = new LinkedList<String>();
		load(proteinId);
	}
	
	private void load(String proteinId) { 		
//		GenerateSummary summary = new GenerateSummary(task);
//		if(!FileManager.loadOnDemand(summary)) return;
		if (proteinId != null)
			desc = Commons.getDescription(proteinId);
		Scanner scanner = null;				
		try {
			scanner = new Scanner(InspectUtils.getSummaryPath(task));			
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				if (line.contains(protein) == false)
					continue; 
				found = true;
				break;
			}
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();			
				if (line.equals(""))
					break;
				dist.add(line);
			}
		}
		catch(IOException e){}
		finally{
			if(scanner != null) scanner.close();
		}
	}
	
	public boolean found() {
		return found;
	}
	
	public String getProtein() {
		return protein;
	}
	
	public String getDescription() {
		return desc;
	}
	
	public Collection<String> getDistribution() {
		return dist;
	}
}
