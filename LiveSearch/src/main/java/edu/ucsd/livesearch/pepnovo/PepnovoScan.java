package edu.ucsd.livesearch.pepnovo;

import java.util.Collection;
import java.util.LinkedList;

public class PepnovoScan{
	private String spectrumFile;
	private String title;
	private int scanNumber;
	private long index;
	private Collection<PepnovoHit> hits;
	public PepnovoScan(String spec, int scan, String title , long index){
		spectrumFile = spec;
		scanNumber = scan;
		this.title = title;
		hits = new LinkedList<PepnovoHit>();
		this.index = index;
	}
	public void addHit(PepnovoHit hit){
		hits.add(hit);
	}
	public Collection<PepnovoHit> getHits(){
		return hits;
	}
	public int getScanNumber() {
		return scanNumber;
	}
	public String getSpectrumFile() {
		return spectrumFile;
	}
	public String getTitle() {
		return title;
	}
	public long getIndex(){
		return index;
	}
}
