package edu.ucsd.livesearch;

import java.io.Writer;

/**
 * Generic workflow results parser interface for the CCMS ProteoSAFe
 * application.  Implementations of this interface are assumed to be
 * properly initialized with a Task of the correct type.
 * 
 * @author Jeremy Carver
 */
public interface ResultsParser
{
	public String getResultType();
	
	public String getDownloadType();
	
	public long size();
	
	public boolean available();
	
	public boolean ready();
	
	public void writeHitsJS(Writer writer, String table);
	
	public void writeHitsJS(Writer writer, String table, Long hit);
	
	public void writeErrorsJS(Writer writer, String variable);
}
