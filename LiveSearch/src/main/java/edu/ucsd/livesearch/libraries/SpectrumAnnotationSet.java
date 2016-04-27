package edu.ucsd.livesearch.libraries;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public class SpectrumAnnotationSet {
	public List <SpectrumAnnotation> Annotation_List = new ArrayList<SpectrumAnnotation>();
	
	public boolean Add_Annotation(SpectrumAnnotation addition){
		Annotation_List.add(addition);
		return true;
	}
	
	public SpectrumAnnotation Get_Annotation_By_Index(int index){
		if(index >= Annotation_List.size() || index < 0){
			return null;
		}
		return Annotation_List.get(index);
	}
	
	
	
	public String toJSON(){
		StringBuilder out = new StringBuilder();
		
		
		out.append(get_header() + "\n");
		
		for(SpectrumAnnotation annot : Annotation_List){
			out.append(annot.toString() + "\n");
		}
		
		return out.toString();
	}
	
	public static String get_header(){
		StringBuilder out = new StringBuilder();
		
		out.append("Compound_Name\t");
		out.append("Ion_Source\t");
		out.append("Instrument\t");
		out.append("Compound_Source\t");
		out.append("PI\t");
		out.append("Data_Collector\t");
		out.append("Adduct\t");
		out.append("Scan\t");
		out.append("Precursor_MZ\t");
		out.append("ExactMass\t");
		out.append("Charge\t");
		out.append("CAS_Number\t");
		out.append("Pubmed_ID\t");
		out.append("Smiles\t");
		out.append("INCHI\t");
		out.append("INCHI_AUX\t");
		out.append("Library_Class\t");
		out.append("SpectrumID\t");
		out.append("IonMode\t");
		out.append("UpdateWorkflowName\t");
		out.append("LibraryQualityString\t");
		out.append("TaskID");
		
		return out.toString();
	}
	
	
	
}

