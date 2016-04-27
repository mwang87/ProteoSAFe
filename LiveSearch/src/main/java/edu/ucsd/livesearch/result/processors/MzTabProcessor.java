package edu.ucsd.livesearch.result.processors;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsd.livesearch.parameter.ResourceManager;
import edu.ucsd.livesearch.result.parsers.GroupedHit;
import edu.ucsd.livesearch.result.parsers.MzTabResult;
import edu.ucsd.livesearch.result.parsers.Result;
import edu.ucsd.livesearch.result.parsers.ResultHit;
import edu.ucsd.livesearch.result.parsers.TabularResult;

public class MzTabProcessor
implements ResultProcessor
{
	/*========================================================================
	 * Constants
	 *========================================================================*/
	private static final Logger logger =
		LoggerFactory.getLogger(MzTabProcessor.class);
	private static final String MZTAB_MODIFICATION_STRING_FORMAT =
		"{position}{Parameter}-{Modification or Substitution identifier}" +
		"|{neutral loss}";
	private static final Pattern CV_TERM_PATTERN = Pattern.compile(
		"^\\[([^,]*),\\s*([^,]*),\\s*\"?([^\"]*)\"?,\\s*([^,]*)\\]$");
	private static final Pattern CV_ACCESSION_PATTERN = Pattern.compile(
		"^(.*?:\\d*)$");
	private static final Pattern MZTAB_MODIFICATION_PATTERN = Pattern.compile(
		"(\\d+.*?(?:\\|\\d+.*?)*)-" +
		"((?:\\[[^,]*,\\s*[^,]*,\\s*\"?[^\"]*\"?,\\s*[^,]*\\])|" +	// CV param
		"(?:[^,]*))");
	private static final Pattern MZTAB_POSITION_PATTERN = Pattern.compile(
		"^(?:(\\d+)" +
		"(\\[[^,]*,\\s*[^,]*,\\s*\"?[^\"]*\"?,\\s*[^,]*\\])?" +		// CV param
		")+(?:\\|(\\d+)" +
		"(\\[[^,]*,\\s*[^,]*,\\s*\"?[^\"]*\"?,\\s*[^,]*\\])?)*$"	// CV param
	);
	private static final Pattern MZTAB_CHEMMOD_PATTERN = Pattern.compile(
		"^CHEMMOD:(.*)$");
	private static final Pattern ONTOLOGY_PTM_PATTERN = Pattern.compile(
		"^\\[.*\\]\\s*\\[(.*?)\\].*$");
	private static final Map<String, String> ONTOLOGY_PTMS;
	// initialize PTM map from the server-side ontology
	static {
		ONTOLOGY_PTMS = new LinkedHashMap<String, String>();
		Map<String, String> ptms = ResourceManager.getResource("modification");
		if (ptms != null) {
			for (String cvTerm : ptms.keySet()) {
				String value = ptms.get(cvTerm);
				if (value == null)
					continue;
				Matcher matcher = ONTOLOGY_PTM_PATTERN.matcher(value);
				if (matcher.matches()) try {
					String mass = matcher.group(1);
					// convert to double just to be sure it's a valid number
					Double.parseDouble(mass);
					ONTOLOGY_PTMS.put(cvTerm, mass);
				} catch (NumberFormatException error) {}
			}
		}
	}
	
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	public void processHit(ResultHit hit, Result result) {
		if (hit == null)
			return;
		else if (result == null) {
			setInvalid(hit, "Result instance is null.");
			return;
		}
		// properly recurse into grouped hits
		if (hit instanceof GroupedHit) {
			for (ResultHit memberHit : ((GroupedHit)hit).getMemberHits())
				processHit(memberHit, result);
			return;
		}
		// try to see if the hit was already set as invalid, by the converter
		String validity = hit.getFieldValue("opt_global_valid");
		if (validity != null && validity.equalsIgnoreCase("INVALID"))
			setInvalid(hit, hit.getFieldValue("opt_global_invalid_reason"));
		// process the "search_engine" field to
		// display a more human-readable value
		Collection<ImmutablePair<String, String>> cvNames = null;
		try {
			cvNames = parseMzTabCVListString(
				hit.getFieldValue("search_engine"));
		} catch (InvalidMzTabColumnValueException error) {}
		if (cvNames != null && cvNames.isEmpty() == false) {
			StringBuffer searchEngines = new StringBuffer();
			boolean first = true;
			for (ImmutablePair<String, String> searchEngine : cvNames) {
				// delimit search engines after the first with
				// commas and spaces, for human readability
				if (first == false)
					searchEngines.append(", ");
				else first = false;
				searchEngines.append(searchEngine.getKey());
			}
			hit.setFieldValue("search_engine", searchEngines.toString());
		}
		// process the "search_engine_score" field to display
		// separate dynamic columns for each score
		cvNames = null;
		try {
			cvNames = parseMzTabCVListString(
				hit.getFieldValue("search_engine_score"));
		} catch (InvalidMzTabColumnValueException error) {}
		if (cvNames != null && cvNames.isEmpty() == false)
			for (ImmutablePair<String, String> score : cvNames)
				hit.setAttribute(String.format("%s%s",
					MzTabResult.DYNAMIC_COLUMN_PREFIX,
					score.getKey()), score.getValue());
		// get the last instance of MzTabResult in this parser chain
		MzTabResult mzTabResult = null;
		if (result instanceof MzTabResult)
			mzTabResult = (MzTabResult)result;
		else if (result instanceof TabularResult)
			for (Result previous : ((TabularResult)result).getPreviousResults())
				if (previous instanceof MzTabResult)
					mzTabResult = (MzTabResult)previous;
		// account for jmzTab's incorrect way of reporting search engine scores
		int i = 1;
		while (true) {
			if (mzTabResult == null)
				break;
			String score = mzTabResult.getSearchEngineScore(i);
			if (score == null)
				break;
			String value =
				hit.getFieldValue(String.format("search_engine_score[%d]", i));
			if (value == null)
				break;
			hit.setAttribute(
				String.format(
					"%s%s", MzTabResult.DYNAMIC_COLUMN_PREFIX, score), value);
			i++;
		}
		// process "modifications" column to figure out exactly which mods
		// are present, and where they are in the peptide sequence
		String mods = hit.getFieldValue("modifications");
		Collection<String> modRefs = null;
		if (mods != null && mods.trim().equals("") == false &&
			mods.trim().equalsIgnoreCase("null") == false) {
			// build a map of mass offsets to insert
			// into the modified peptide string
			modRefs = new ArrayList<String>();
			Matcher matcher = MZTAB_MODIFICATION_PATTERN.matcher(mods);
			while (matcher.find())
				modRefs.add(String.format(
					"%s-%s", matcher.group(1), matcher.group(2)));
		}
		// process the "sequence" column, along with the set of modifications
		// just determined, to generate a modified peptide string
		String value = hit.getFieldValue("sequence");
		if (value != null) {
			// ensure that the "modified_sequence" attribute is present,
			// even if there are no mods for this row
			hit.setAttribute("modified_sequence", value);
			if (modRefs != null) {
				Map<Integer, Double> masses =
					new LinkedHashMap<Integer, Double>(modRefs.size());
				for (String mod : modRefs) {
					try {
						parseModMass(mod.trim(), value, masses);
					} catch (InvalidMzTabColumnValueException error) {
						//logger.error(error.getMessage());
						setInvalid(hit, error.getMessage());
						continue;
					}
				}
				// build modified peptide sequence by
				// looking up all the referenced mods
				StringBuffer modifiedSequence = new StringBuffer(value);
				for (int site : masses.keySet()) {
					String mass = getFormattedMass(masses.get(site));
					insertMzTabModIntoPeptideString(
						site, mass, value, modifiedSequence);
				}
				// add modified sequence as a hit attribute
				hit.setAttribute(
					"modified_sequence", modifiedSequence.toString());
			}
		}
		// dynamically display all "opt_global" columns, other than validity
		// (which is treated and displayed specially)
		for (String fieldName : hit.getFieldNames()) {
			if (fieldName.trim().startsWith("opt_global_")) {
				String suffix = fieldName.trim().substring(11);
				if (suffix.equalsIgnoreCase("valid") == false &&
					suffix.equalsIgnoreCase("invalid_reason") == false)
					hit.setAttribute(String.format("%s%s",
						MzTabResult.DYNAMIC_COLUMN_PREFIX, suffix),
						hit.getFieldValue(fieldName));
			}
		}
		// if all went well, then this row is valid
		validity = hit.getAttribute("valid");
		if (validity == null)
			setValid(hit);
	}
	
	/*========================================================================
	 * Local exception classes
	 *========================================================================*/
	@SuppressWarnings("serial")
	private class InvalidMzTabColumnValueException
	extends Exception {
		/*====================================================================
		 * Constructor
		 *====================================================================*/
		public InvalidMzTabColumnValueException(String message) {
			super(message);
		}
	}
	
	/*========================================================================
	 * Convenience methods
	 *========================================================================*/
	private Collection<ImmutablePair<String, String>> parseMzTabCVListString(
		String mzTabCVListString
	) throws InvalidMzTabColumnValueException {
		if (mzTabCVListString == null)
			return null;
		// an mzTab CV list string should consist of a pipe-delimited
		// ("|") list of square bracket-enclosed ("[]") CV tuples
		String[] cvTerms = mzTabCVListString.split("\\|");
		if (cvTerms == null || cvTerms.length < 1)
			throw new InvalidMzTabColumnValueException(String.format(
				"mzTab CV list string [%s] does not conform to the " +
				"required string format of a pipe-delimited (\"|\") " +
				"list of square bracket-enclosed (\"[]\") CV tuples.",
				mzTabCVListString));
		Collection<ImmutablePair<String, String>> cvAccessions =
			new LinkedHashSet<ImmutablePair<String, String>>(cvTerms.length);
		for (int i=0; i<cvTerms.length; i++) {
			String cvTerm = cvTerms[i];
			Matcher matcher = CV_TERM_PATTERN.matcher(cvTerm);
			if (matcher.matches() == false)
				throw new InvalidMzTabColumnValueException(String.format(
					"CV term element %d [%s] of mzTab CV list string [%s] " +
					"does not conform to the required string format of a " +
					"square bracket-enclosed (\"[]\") CV tuple:\n%s",
					i + 1, cvTerm, mzTabCVListString,
					"[cvLabel, accession, name, value]"));
			else cvAccessions.add(new ImmutablePair<String, String>(
				matcher.group(3), matcher.group(4)));
		}
		if (cvAccessions == null || cvAccessions.isEmpty())
			return null;
		else return cvAccessions;
	}
	
	private void parseModMass(
		String mzTabModString, String unmodifiedSequence,
		Map<Integer, Double> masses
	) throws InvalidMzTabColumnValueException {
		if (mzTabModString == null || unmodifiedSequence == null ||
			masses == null)
			return;
		Matcher matcher = MZTAB_MODIFICATION_PATTERN.matcher(mzTabModString);
		if (matcher.find() == false)
			throw new InvalidMzTabColumnValueException(String.format(
				"mzTab modification string [%s] does not conform to " +
				"the required string format, as defined in the mzTab " +
				"format specification, section 5.8:\n%s",
				mzTabModString, MZTAB_MODIFICATION_STRING_FORMAT));
		String position = matcher.group(1);
		String identifier = matcher.group(2);
		// if this is a neutral loss declaration (which we
		// assume to be the case if it's a CV declaration
		// enclosed by square brackets), then ignore it
		if (identifier != null && CV_TERM_PATTERN.matcher(identifier).matches())
			return;
		// validate the mod's {position} element
		else if (position == null || position.trim().equalsIgnoreCase("null")) {
			throw new InvalidMzTabColumnValueException(String.format(
				"A missing or \"null\" value was found in the \"{position}\" " +
				"element of mzTab modification string [%s]. Therefore, this " +
				"modification cannot be unambiguously written into the " +
				"modified peptide string.", mzTabModString));
		}
		matcher = MZTAB_POSITION_PATTERN.matcher(position);
		if (matcher.matches() == false)
			throw new InvalidMzTabColumnValueException(String.format(
				"The \"{position}\" element [%s] of mzTab modification " +
				"string [%s] does not conform to the required string " +
				"format, as defined in the mzTab format specification, " +
				"section 5.8.", position, mzTabModString));
		else if (position.indexOf('|') >= 0)
			throw new InvalidMzTabColumnValueException(String.format(
				"The \"{position}\" element [%s] of mzTab modification " +
				"string [%s] contains one or more pipe (\"|\") " +
				"characters, indicating that the modification's site " +
				"is ambiguous. Therefore, this modification cannot be " +
				"unambiguously written into the modified peptide string.",
				position, mzTabModString));
		// try to extract the integer site position of the referenced mod
		int site;
		try {
			site = Integer.parseInt(matcher.group(1));
		} catch (NumberFormatException error) {
			throw new InvalidMzTabColumnValueException(String.format(
				"The \"{position}\" element [%s] of mzTab modification " +
				"string [%s] could not be parsed into a proper integer " +
				"site index. Therefore, this modification cannot be " +
				"unambiguously written into the modified peptide string.",
				position, mzTabModString));
		}
		// make sure that the position is within the bounds
		// of the original peptide sequence's length
		if (site < 0 || site > unmodifiedSequence.length())
			throw new InvalidMzTabColumnValueException(String.format(
				"The \"{position}\" element [%s] of mzTab modification " +
				"string [%s] was parsed into an integer of value %d. This " +
				"position falls outside the bounds of the affected peptide " +
				"[%s] (length %d). Therefore, this modification cannot be " +
				"unambiguously written into the modified peptide string.",
				position, mzTabModString, site, unmodifiedSequence,
				unmodifiedSequence.length()));
		// try to match the mod's {Modification or Substitution identifier}
		// element against the set of recognized identifier formats, and
		// use that to determine or extract the modification mass 
		String mass = null;
		matcher = CV_ACCESSION_PATTERN.matcher(identifier);
		if (matcher.matches())
			mass = ONTOLOGY_PTMS.get(matcher.group(1));
		else {
			matcher = MZTAB_CHEMMOD_PATTERN.matcher(identifier);
			if (matcher.matches())
				mass = matcher.group(1);
			else throw new InvalidMzTabColumnValueException(String.format(
				"The \"{Modification or Substitution identifier}\" element " +
				"[%s] of mzTab modification string [%s] was not recognized " +
				"as a valid identifier format, as defined in the mzTab " +
				"format specification, section 5.8. Therefore, this " +
				"modification cannot be unambiguously written into the " +
				"modified peptide string.", identifier, mzTabModString));
		}
		// if no mass could be extracted, then this mod can't be written
		double massValue;
		try {
			massValue = Double.parseDouble(mass);
		} catch (Throwable error) {
			throw new InvalidMzTabColumnValueException(String.format(
				"The \"{Modification or Substitution identifier}\" element " +
				"[%s] of mzTab modification string [%s] could not be " +
				"evaluated into a proper numerical mass value. Therefore, " +
				"this modification cannot be unambiguously written into " +
				"the modified peptide string.", identifier, mzTabModString));
		}
		// if the mass is 0, ignore this mod
		if (massValue == 0.0)
			return;
		// add this mass offset to the map
		Double current = masses.get(site);
		if (current == null)
			masses.put(site, massValue);
		// be sure to use BigDecimal for the addition operation,
		// since doubles alone result in lame precision issues
		else masses.put(site, BigDecimal.valueOf(current).add(
			BigDecimal.valueOf(massValue)).doubleValue());
	}
	
	private StringBuffer insertMzTabModIntoPeptideString(
		int site, String mass, String unmodifiedSequence,
		StringBuffer modifiedSequence
	) {
		if (mass == null || unmodifiedSequence == null ||
			modifiedSequence == null)
			return modifiedSequence;
		// if the site is 0, then it's an N-term mod, so just put it there
		if (site == 0) {
			modifiedSequence.insert(0, mass);
			return modifiedSequence;
		}
		// otherwise, iterate through the modified peptide string
		// to find the correct position to write the mod mass
		int count = 0;
		for (int i=0; i<modifiedSequence.length(); i++) {
			char current = modifiedSequence.charAt(i);
			// only count original amino acid characters from the
			// unmodified sequence when counting up to the position
			// to be modified by this mass offset
			if (current == unmodifiedSequence.charAt(count)) {
				count++;
				// we're 1-based now since we just incremented
				if (count == site) {
					// insert the mass offset into the
					// modified sequence string
					modifiedSequence.insert(i + 1, mass);
					break;
				}
			}
		}
		return modifiedSequence;
	}
	
	private void setValid(ResultHit hit) {
		if (hit == null)
			return;
		else hit.setAttribute("valid", "VALID");
	}
	
	private void setInvalid(ResultHit hit, String reason) {
		if (hit == null)
			return;
		else if (reason != null)
			hit.setAttribute("valid", String.format("INVALID!%s", reason));
		else hit.setAttribute("valid", "INVALID");
	}
	
	private String getFormattedMass(Double mass) {
		if (mass == null)
			return null;
		String formattedMass;
		if (mass == (int)mass.doubleValue())
			formattedMass = String.format("%d", (int)mass.doubleValue());
		else formattedMass = String.format("%s", mass.toString());
		// prepend a "+" if this is a non-negative mass offset
		if (mass >= 0.0 && formattedMass.startsWith("+") == false)
			formattedMass = "+" + formattedMass;
		return formattedMass;
	}
}
