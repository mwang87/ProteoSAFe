package edu.ucsd.livesearch.parameter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.transform.TransformerException;

import org.apache.xpath.XPathAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.ucsd.livesearch.task.OnDemandLoader;
import edu.ucsd.livesearch.task.OnDemandOperation;
import edu.ucsd.livesearch.task.Task;
import edu.ucsd.livesearch.util.FileIOUtils;

public class GenerateMasses
implements OnDemandOperation
{
	/*========================================================================
	 * Constants
	 *========================================================================*/
	private final Logger logger =
		LoggerFactory.getLogger(GenerateMasses.class);
	
	/*========================================================================
	 * Properties
	 *========================================================================*/
	private File masses;
	private File parameters;
	
	/*========================================================================
	 * Constructor
	 *========================================================================*/
	public GenerateMasses(Task task) {
		masses = task.getPath("misc/masses.txt");
		parameters = task.getPath("params/params.xml");
		// ensure that parameters file is present
		LegacyParameterConverter paramsLoader =
			new LegacyParameterConverter(task);
		if (OnDemandLoader.load(paramsLoader) == false)
			throw new IllegalArgumentException("Error instantiating " +
				"GenerateMasses: no valid parameter file could be found " +
				"or generated from the argument task.");
	}
	
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	public boolean execute() {
		// generate amino acid masses file
		try {
			masses.createNewFile();
			FileWriter writer = new FileWriter(masses);
			String contents = generateMassesFile();
			writer.write(contents);
			writer.close();
			return true;
		} catch (IOException error) {
			logger.debug("Generation of amino acid masses file FAILED",
				error);
		}
		return false;
	}
	
	public boolean resourceExists() {
		return masses.exists();
	}
	
	public boolean resourceDated() {
		if (masses.exists() == false || parameters.exists() == false)
			return false;
		else return masses.lastModified() < parameters.lastModified(); 
	}
	
	public String getResourceName() {
		return masses.getAbsolutePath(); 
	}
	
	public File getMassesFile() {
		return masses;
	}
	
	public File getParametersFile() {
		return parameters;
	}
	
	/*========================================================================
	 * Convenience methods
	 *========================================================================*/
	private String generateMassesFile() {
		// build default amino acid masses map
		Map<String, String> masses = new HashMap<String, String>(20);
		// TODO: maybe these constants should be externalized?
		masses.put("A", "71.037113787");
		masses.put("R", "156.101111026");
		masses.put("D", "115.026943031");
		masses.put("N", "114.042927446");
		masses.put("C", "103.009184477");
		masses.put("E", "129.042593095");
		masses.put("Q", "128.058577510");
		masses.put("G", "57.021463723");
		masses.put("H", "137.058911861");
		masses.put("I", "113.084063979");
		masses.put("L", "113.084063979");
		masses.put("K", "128.094963016");
		masses.put("M", "131.040484605");
		masses.put("F", "147.068413915");
		masses.put("P", "97.052763851");
		masses.put("S", "87.032028409");
		masses.put("T", "101.047678473");
		masses.put("W", "186.079312952");
		masses.put("Y", "163.063328537");
		masses.put("V", "99.068413915");
		// update masses with user-specified fixed PTMS, if any
		Document params = null;
		try {
			// build XML document from parameters file
			params = FileIOUtils.parseXML(parameters);
			if (params != null) {
				// get cysteine mass
				Node cysteine = XPathAPI.selectSingleNode(params,
					"//parameter[@name='cysteine_protease.cysteine']");
				if (cysteine != null) {
					String mass = cysteine.getTextContent();
					if (mass != null) {
						if (mass.equals("c57"))
							addMass("C", "57.021463723", masses);
						else if (mass.equals("c58"))
							addMass("C", "58.005479308", masses);
						else if (mass.equals("c99"))
							addMass("C", "99.068413915", masses);
					}
				}
				// traverse XML document for custom PTM entries
				NodeList ptms = XPathAPI.selectNodeList(params,
					"//parameter[@name='ptm.custom_PTM']");
				if (ptms != null) {
					for (int i=0; i<ptms.getLength(); i++) {
						String ptm = ptms.item(i).getTextContent();
						// user-specified PTMs are assumed to be encoded as
						// a string in the format <mass>,<residue>,<type>
						String[] fields = ptm.split(",");
						if (fields.length != 3) {
							// TODO: report error
							continue;
						}
						// if this user-specified PTM is fixed, add it
						else if (fields[2] != null && fields[2].equals("fix")) {
							String aminoAcids = fields[1];
							// if the user specified an asterisk "*",
							// add the specified mass to all amino acids
							if (aminoAcids.contains("*"))
								for (String aminoAcid : masses.keySet())
									addMass(aminoAcid, fields[0], masses);
							// otherwise just add the specified mass to the
							// specified set of amino acids
							else for (int j=0; j<aminoAcids.length(); j++)
								addMass(
									Character.toString(aminoAcids.charAt(j)),
									fields[0], masses
								);
						}
					}
				}
			}
		} catch (TransformerException error) {
			// TODO: report error
		}
		// generate masses file content string
		StringBuffer contents = new StringBuffer();
		contents.append(String.format("%d\n", masses.size()));
		for (String aminoAcid : masses.keySet()) {
			contents.append(aminoAcid);
			contents.append("=");
			contents.append(masses.get(aminoAcid));
			contents.append("\n");
		}
		return contents.toString();
	}
	
	private void addMass(String aminoAcid, String addedMass,
		Map<String, String> masses) {
		if (aminoAcid == null || addedMass == null ||
			masses == null || masses.size() < 1)
			return;
		try {
			double mass = Double.parseDouble(masses.get(aminoAcid));
			mass += Double.parseDouble(addedMass);
			masses.put(aminoAcid, Double.toString(mass));
		} catch (NumberFormatException error) {}
	}
}
