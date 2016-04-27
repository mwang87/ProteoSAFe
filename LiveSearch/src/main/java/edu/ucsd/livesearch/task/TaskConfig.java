package edu.ucsd.livesearch.task;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import org.apache.commons.io.filefilter.SuffixFileFilter;

import edu.ucsd.livesearch.task.NullTask;

public class TaskConfig {
//	private Collection<String> databases;
	private boolean shuffled;
	private Task task;

	TaskConfig(Task task){
		this.task = task;
		if(task instanceof NullTask) return;
//		databases = new LinkedList<String>();
		shuffled = false;
		loadCommands(task);
		//loadSpectrum(task);
//		loadSequences(task);
	}

	private void loadCommands(Task task){
		try{
			File conf = task.getPath("commands.in");
			if(!conf.exists()) return;
			Scanner scanner = new Scanner(new BufferedReader(new FileReader(conf)));
			while(scanner.hasNextLine()){
				String line = scanner.nextLine();
				if(line.equals("# shuffled"))
					shuffled = true;
			}
		}
		catch(IOException e){}
	}

	public boolean isShuffled(){
		return shuffled;
	}

//	private void loadSequences(Task task){
//		this.task = task;
//		try{
//			File conf = task.getPath("databases.in");
//			if(!conf.exists()) return;
//			Scanner scanner = new Scanner(new BufferedReader(new FileReader(conf)));
//			while(scanner.hasNextLine()){
//				String line = scanner.nextLine();
//				if(line.startsWith("db,"))
//					databases.add(line.substring(3));
//			}
//		}
//		catch(IOException e){}		
//	}

	private static FilenameFilter filter = new SuffixFileFilter(".trie");

	public List<String> getSequences(){
		File folder = task.getPath("trie/");
		if(folder.exists())
			return Arrays.asList(folder.list(filter));
		return new LinkedList<String>();
	}
	
	public List<File> getSeqFiles(){
		File folder = task.getPath("trie/");
		if(folder.exists())
			return Arrays.asList(folder.listFiles(filter));
		return new LinkedList<File>();
	}
}
