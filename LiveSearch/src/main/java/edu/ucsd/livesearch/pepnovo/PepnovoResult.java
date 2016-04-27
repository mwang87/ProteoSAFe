package edu.ucsd.livesearch.pepnovo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;

import edu.ucsd.livesearch.task.Task;

public class PepnovoResult {
	private boolean ready = false;
	private Collection<PepnovoScan> allScans = new LinkedList<PepnovoScan>();
		// each element is itself an array of hits for a specific scan
	private static Pattern HEADER_PATTERN = Pattern.compile(">> (\\d++) (?:(\\d++) )?(.*+)");
	private Task task;
	private boolean noRankScore = false;
	
	public PepnovoResult(Task task){
		this.task = task;
		File path = PepnovoUtils.getResultPath(task);
		ready = path.exists(); 		
		if(ready) parseOutput(path);
	}

	public boolean noRankScore(){
		return noRankScore;
	}

	private boolean parseOutput(File outputFile){
		Scanner scanner = null;
		try{
			scanner = new Scanner(new BufferedReader(new FileReader(outputFile)));
			int index = 0;
			while(scanner.hasNextLine()){
				String line = scanner.nextLine();
				while(line.equals("") && scanner.hasNextLine())
					line = scanner.nextLine();
				if(line.equals("")) break;
				index++;
				Matcher matcher = HEADER_PATTERN.matcher(line);
				if(!scanner.hasNextLine())
					break;
				line = scanner.nextLine(); // the heading line
				if(!line.startsWith("#Index"))
					continue;
				noRankScore = !line.contains("Prob") && !line.contains("RnkScr");
//				if(noRankScore) System.out.printf("[blah blah blah]%s%n", line);
				matcher.matches();
				String scanToken = matcher.group(2);
				int scanIndex = (scanToken == null)? -1 : Integer.parseInt(scanToken);
				String title = task.queryOriginalName(FilenameUtils.getName(matcher.group(3)));
				if(title == null) title = matcher.group(3);
				PepnovoScan scan = new PepnovoScan(matcher.group(1), scanIndex, title, index);				
				while((line = scanner.nextLine()) != null && !line.equals("")){					
					scan.addHit(new PepnovoHit(line, noRankScore));
				}
				allScans.add(scan);
			}
			return true;
		}
		catch(IOException e){
			return false;
		}
		finally{
			if(scanner != null) scanner.close();
		}
	}

	public boolean ready(){
		return ready;
	}
	
	public Collection<PepnovoScan> getScans(){
		return allScans;
	}
}
