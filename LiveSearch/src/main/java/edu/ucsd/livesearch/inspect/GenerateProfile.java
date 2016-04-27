package edu.ucsd.livesearch.inspect;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsd.livesearch.task.OnDemandOperation;
import edu.ucsd.livesearch.task.Task;
import edu.ucsd.livesearch.util.FormatUtils;

public class GenerateProfile implements OnDemandOperation{
	private final Logger logger = LoggerFactory.getLogger(GenerateProfile.class);
	
	private Task	task;
	private File	profile;
	private double	pmtlr, iontlr, maxPtmMass;
	private String	protease, instrument;
	private int		maxPTM;
	private boolean blind, shuffled;

	private List<String> sequences;
	private List<String> modifications;

	public GenerateProfile(Task task){
		this.task = task;
		profile = task.getPath("profile.txt");
		shuffled = blind = false;
		pmtlr = iontlr = maxPtmMass = Double.NaN;
		maxPTM = -1;
		protease = instrument = null;
		sequences = new LinkedList<String>();
		modifications = new LinkedList<String>();
	}
	
	public String getResourceName() {
		return task.getID() + ".profile";
	}

	public boolean execute() {
		PrintWriter writer = null;
		try{
			loadCommandConfig();
			loadDatabaseConfig();
			writer = new PrintWriter(profile);
			writer.printf("Instrument,%s%n", instrument);
			writer.printf("Protease, %s%n", protease);
			writer.printf("Parent mass tolerance, %f Daltons%n", pmtlr);
			writer.printf("Ion tolerance, %f Daltons%n", iontlr);
			if(!blind){
				writer.printf("Maximum number of PTM allowed per peptide, %d%n", maxPTM);
				for(String ptm: modifications)
					writer.printf("PTM, %s%n", ptm);
			}
			else{
				writer.printf("Blind search%n");
				writer.printf("Maximum modification mass allowed, %f Daltons%n", maxPtmMass);
			}
			
			for(String spec: task.queryUploadsByPurpose("spectrum"))
				writer.printf("Spectrum, %s%n", spec);
			for(String db: sequences)
				writer.printf("Sequence, %s%n", db);
			writer.printf(
				"Results were filtered at a spectrum-level pvalue of 0.05, measured by %s%n", 
				shuffled ?
					"hits to the decoy database" :
					"modeling an F-score to a mixture model (Keller et al., 2002. Anal Chem)");
			return true;
		}
		catch(Exception e){
			logger.warn(FormatUtils.formatExceptoin(e, 
				"Failed to load profile [%s]", profile.getAbsolutePath()));
			return false;			
		}
		finally{
			if(writer != null) writer.close();
		}		
	}

	private void  loadCommandConfig() throws Exception {
		File conf = task.getPath("params/commands.in");
		if(!conf.exists())
			throw new Exception("The command file does not exist.");
		Scanner scanner = new Scanner(new BufferedReader(new FileReader(conf)));
		try{
			while(scanner.hasNextLine())
				parseCommand(scanner.nextLine());
		}
		finally{
			scanner.close();
		}
	}

	private void parseCommand(String line){
		if(line.equals("# shuffled")){
			shuffled = true;
			return;
		}
		String parts[] = line.split(",");
		String command = parts[0];
		if(command.equals("PMTolerance"))
			pmtlr = NumberUtils.toDouble(parts[1], Double.NaN);
		else if(command.equals("IonTolerance"))
			iontlr = NumberUtils.toDouble(parts[1], Double.NaN);
		else if(command.equals("protease"))
			protease = parts[1];
		else if(command.equals("instrument"))
			instrument = parts[1];
		else if(command.equals("mods"))
			maxPTM = NumberUtils.toInt(parts[1], -1);
		else if(command.equals("MaxPTMSize"))
			maxPtmMass = NumberUtils.toDouble(parts[1], Double.NaN);
		else if(command.equals("blind"))
			blind = true;
		else if(command.equals("mod"))
			modifications.add(String.format(
				"delta mass: %s Da, affected: %s, type: %s", parts[1], parts[2],parts[3]));
	}
	
	private void loadDatabaseConfig(){
		for(String trie: task.getPath("trie/").
				list(new SuffixFileFilter(".trie")))
			sequences.add(FilenameUtils.getBaseName(trie).
				replaceAll(".shuffle", ""));
		for(String fasta: task.getPath("fasta/").
				list(new SuffixFileFilter(".fasta")))
			sequences.add(task.queryOriginalName(
				FilenameUtils.getBaseName(fasta) + ".fasta"));
	}

	public boolean resourceExists() {
		return profile.exists();
	}

	public boolean resourceDated(){
		return false;
	}
	
	public File getFile(){
		return profile;
	}
}
