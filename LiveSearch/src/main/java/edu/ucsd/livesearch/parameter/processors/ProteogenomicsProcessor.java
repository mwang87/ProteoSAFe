package edu.ucsd.livesearch.parameter.processors;

import java.io.File;
import java.io.FilenameFilter;
import java.util.List;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsd.livesearch.parameter.ParameterProcessor;
import edu.ucsd.livesearch.task.Task;
import edu.ucsd.livesearch.task.TaskBuilder;

public class ProteogenomicsProcessor
implements ParameterProcessor
{
	/*========================================================================
	 * Constants
	 *========================================================================*/
	@SuppressWarnings("unused")
	private static final Logger logger =
		LoggerFactory.getLogger(ProteogenomicsProcessor.class);
	
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	/**
	 * Validates the database file-related parameters submitted by the user
	 * from the CCMS ProteoSAFe web application input form for Proteogenomics
	 * workflow tasks, and stages the associated files to the appropriate
	 * task directories.
	 * 
	 * @param builder	an {@link TaskBuilder} object representing the building
	 * 					state of the task whose parameters are to be processed
	 * 
	 * @return			the {@link List} of error messages encountered
	 * 					during processing,
	 * 					null if processing completed successfully
	 */
	public List<String> processParameters(TaskBuilder builder) {
		if (builder == null)
			return null;
		Task task = builder.getTask();
		if (task == null)
			return null;
		List<String> errors = new Vector<String>();
		
		// TODO: this functionality should be handled by a more generic
		// XML parameter specification mechanism
		// hard-code FDR parameter for PValue step
		builder.setParameterValue("FDR.FDR", "0.01");
		// hard-code FPR parameter for MS-GF step
		builder.setParameterValue("FPR.FPR", "1.0");
		
//		// get selected organism-related databases
//		String organism = parameters.getParameter("db.DB");
//		if (organism == null || organism.equals("None")) {
//			errors.add("Organism unspecified");
//			return errors;
//		}
//		File organismFolder = new File(Commons.SEQUENCE_PATH, organism);
//		if (organismFolder == null || organismFolder.canRead() == false ||
//			organismFolder.isDirectory() == false) {
//			errors.add("Could not find database files for organism \"" +
//				organism + "\"");
//			return errors;
//		}
//		try {
//			// get common contaminants database file
//			String contaminants = parameters.getParameter("db.contaminants");
//			if ("on".equals(contaminants)) {
//				File trieFolder = task.getPath("contaminants/");
//				File srcTrie = new File(Commons.SEQUENCE_PATH,
//					"CommonContaminants.shuffle.trie");
//				File srcIndex = new File(Commons.SEQUENCE_PATH,
//					"CommonContaminants.shuffle.index");
//				SaintFileUtils.makeLink(srcTrie, trieFolder);
//				SaintFileUtils.makeLink(srcIndex, trieFolder);
//			}
//			
//			// get organism sequence database files
//			File subfolder = new File(organismFolder, "ORFs");
//			// unshuffled files
//			ProteogenomicsFilter unshuffled = new ProteogenomicsFilter(false);
//			File sequenceFolder = task.getPath("org/");
//			for (File file : subfolder.listFiles(unshuffled))
//				SaintFileUtils.makeLink(file, sequenceFolder);
//			// shuffled files
//			ProteogenomicsFilter shuffled = new ProteogenomicsFilter(true);
//			sequenceFolder = task.getPath("org_rs/");
//			for (File file : subfolder.listFiles(shuffled))
//				SaintFileUtils.makeLink(file, sequenceFolder);
//			
//			// get known proteome database files
//			subfolder = new File(organismFolder, "Proteome");
//			// unshuffled working files
//			ProteogenomicsFilter workingUnshuffled =
//				new ProteogenomicsFilter(false, "working");
//			sequenceFolder = task.getPath("known/");
//			for (File file : subfolder.listFiles(workingUnshuffled))
//				SaintFileUtils.makeLink(file, sequenceFolder);
//			// shuffled working files
//			ProteogenomicsFilter workingShuffled =
//				new ProteogenomicsFilter(true, "working");
//			sequenceFolder = task.getPath("known_rs/");
//			for (File file : subfolder.listFiles(workingShuffled))
//				SaintFileUtils.makeLink(file, sequenceFolder);
//			// unshuffled filtered files
//			ProteogenomicsFilter filteredUnshuffled =
//				new ProteogenomicsFilter(false, "filtered");
//			sequenceFolder = task.getPath("filtered/");
//			for (File file : subfolder.listFiles(filteredUnshuffled))
//				SaintFileUtils.makeLink(file, sequenceFolder);
//			// shuffled filtered files
//			ProteogenomicsFilter filteredShuffled =
//				new ProteogenomicsFilter(true, "filtered");
//			sequenceFolder = task.getPath("filtered_rs/");
//			for (File file : subfolder.listFiles(filteredShuffled))
//				SaintFileUtils.makeLink(file, sequenceFolder);
//			
//			// get splice graph files
//			subfolder = new File(organismFolder, "Graphs");
//			// unshuffled splice graph files
//			sequenceFolder = task.getPath("ms2db/");
//			for (File file : subfolder.listFiles(unshuffled))
//				SaintFileUtils.makeLink(file, sequenceFolder);
//			// shuffled splice graph files
//			sequenceFolder = task.getPath("ms2db_rs/");
//			for (File file : subfolder.listFiles(shuffled))
//				SaintFileUtils.makeLink(file, sequenceFolder);
//		} catch(Exception error) {
//			logger.error("Error making soft link for " +
//				"Proteogenomics database file:", error);
//			errors.add("There was a problem downloading database files " +
//				"for organism \"" + organism + "\"");
//		}

		if (errors.size() < 1)
			return null;
		else return errors;
	}
	
	/*========================================================================
	 * Convenience classes
	 *========================================================================*/
	@SuppressWarnings("unused")
	private static class ProteogenomicsFilter
	implements FilenameFilter
	{
		/*====================================================================
		 * Properties
		 *====================================================================*/
		// filtration mode
		private boolean shuffled;
		// filter regular expressions
		private String shuffledFilter;
		private String unshuffledFilter;
		
		/*====================================================================
		 * Constructors
		 *====================================================================*/
		public ProteogenomicsFilter(boolean shuffled) {
			setShuffled(shuffled);
			setKnown(null);
		}
		
		public ProteogenomicsFilter(boolean shuffled, String known) {
			setShuffled(shuffled);
			setKnown(known);
		}
		
		/*====================================================================
		 * Public interface methods
		 *====================================================================*/
		public boolean accept(File directory, String name) {
			if (name == null)
				return false;
			else if (name.matches(shuffledFilter))
				return shuffled;
			else if (name.matches(unshuffledFilter))
				return !shuffled;
			else return false;
		}
		
		/*====================================================================
		 * Property accessor methods
		 *====================================================================*/
		public boolean isShuffled() {
			return shuffled;
		}
		
		public final void setShuffled(boolean shuffled) {
			this.shuffled = shuffled;
		}
		
		public final void setKnown(String known) {
			buildFilters(known);
		}
		
		/*====================================================================
		 * Convenience methods
		 *====================================================================*/
		private void buildFilters(String known) {
			// build filter regular expressions
			StringBuffer shuffledFilter = new StringBuffer(".+");
			StringBuffer unshuffledFilter = new StringBuffer(".+");
			boolean isKnown =
				(known != null && known.matches("filtered|working"));
			if (isKnown) {
				shuffledFilter.append(known);
				shuffledFilter.append(".+");
				unshuffledFilter.append(known);
				unshuffledFilter.append(".+");
			}
			shuffledFilter.append("\\.RS\\.");
			unshuffledFilter.append("\\.");
			// known proteome database files are always tries
			if (isKnown) {
				shuffledFilter.append("(trie|index)");
				unshuffledFilter.append("(trie|index)");
			} else {
				shuffledFilter.append("(trie|ms2db|index)");
				unshuffledFilter.append("(trie|ms2db|index)");
			}
			// set filters
			this.shuffledFilter = shuffledFilter.toString();
			this.unshuffledFilter = unshuffledFilter.toString();
		}
	}
}
