package edu.ucsd.livesearch.result.processors;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import org.apache.commons.io.FilenameUtils;

import edu.ucsd.livesearch.result.parsers.Result;
import edu.ucsd.livesearch.result.parsers.ResultHit;

public class MsClusterProcessor
implements ResultProcessor
{
	/*========================================================================
	 * Properties
	 *========================================================================*/
	private String fileField;
	private String scanField;
	
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	public void processHit(ResultHit hit, Result result) {
		if (hit == null || result == null)
			return;
		// extract filename from original result file column value
		String fileField = getFileField();
		if (fileField == null)
			return;
		String filename = hit.getFirstFieldValue(fileField);
		if (filename == null)
			filename = hit.getAttribute(fileField);
		if (filename == null)
			return;
		// extract cluster ID from filename, which should be the base name
		String clusterFileID = FilenameUtils.getBaseName(filename);
		if (clusterFileID == null)
			return;
		// extract scan number from original result file column value
		String scanField = getScanField();
		if (scanField == null)
			return;
		String scan = hit.getFirstFieldValue(scanField);
		if (scan == null)
			scan = hit.getAttribute(scanField);
		if (scan == null)
			return;
		// parse scan into number
		int scanNumber;
		try {
			scanNumber = Integer.parseInt(scan);
		} catch (NumberFormatException error) {
			return;
		}
		// retrieve cluster ID from clustered mgf file
		String clusterID = getClusterID(
			result.getTask().getPath("mgf/" + clusterFileID + ".mgf"),
			scanNumber);
		if (clusterID == null)
			return;
		// add cluster ID as a special hit attribute
		hit.setAttribute("cluster", clusterID);
	}
	
	/*========================================================================
	 * Property accessor methods
	 *========================================================================*/
	public String getFileField() {
		return fileField;
	}
	
	public void setFileField(String fileField) {
		this.fileField = fileField;
	}
	
	public String getScanField() {
		return scanField;
	}
	
	public void setScanField(String scanField) {
		this.scanField = scanField;
	}
	
	/*========================================================================
	 * Convenience methods
	 *========================================================================*/
	private String getClusterID(File mgfFile, int scan) {
		if (mgfFile == null || mgfFile.canRead() == false || scan < 0)
			return null;
		// read mgf file until the proper scan is located
		int lastScan = -1;
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(mgfFile));
			while (true) {
				String line = reader.readLine();
				if (line == null)
					break;
				else if (line.startsWith("BEGIN IONS"))
					lastScan++;
				else if (lastScan == scan && line.startsWith("TITLE=")) {
					return line.substring(6).trim();
				}
			}
		} catch (Throwable error) {
			return null;
		} finally {
			if (reader != null) try {
				reader.close();
			} catch (Throwable error) {}
		}
		// if the file ended before the proper scan's
		// title could be found, return null
		return null;
	}
}
