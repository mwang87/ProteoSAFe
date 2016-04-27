package edu.ucsd.livesearch.pepnovo;

public class PepnovoHit {
	private int index;
	private double prob, score, ngap, cgap, mh, charge;
	private String sequence;
	public PepnovoHit(String line, boolean noRankScore){
		int i = 0;
		String tokens[] = line.split("\\t");
		index = Integer.parseInt(tokens[i++]);
		if(noRankScore) prob = 0;
		else  prob = Double.parseDouble(tokens[i++]);
		try{ score = Double.parseDouble(tokens[i++]); }
		catch(NumberFormatException e){ score = 100000;}
		ngap = Double.parseDouble(tokens[i++]);
		cgap = Double.parseDouble(tokens[i++]);
		mh = Double.parseDouble(tokens[i++]);
		charge = Double.parseDouble(tokens[i++]);
		sequence = (i < tokens.length) ? tokens[i]: "";
	}
	public double getCharge() {
		return charge;
	}
	public double getCGap() {
		return cgap;
	}
	public int getIndex() {
		return index;
	}
	public double getMH() {
		return mh;
	}
	public double getNGap() {
		return ngap;
	}
	public double getProb() {
		return prob;
	}
	public double getScore() {
		return score;
	}
	public String getSequence() {
		return sequence;
	}
}
