package edu.ucsd.livesearch.result.plugin;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;
import java.util.Vector;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsd.livesearch.inspect.InspectUtils;
import edu.ucsd.livesearch.parameter.GenerateMasses;
import edu.ucsd.livesearch.servlet.BaseServlet;
import edu.ucsd.livesearch.task.NullTask;
import edu.ucsd.livesearch.task.OnDemandLoader;
import edu.ucsd.livesearch.task.OnDemandOperation;
import edu.ucsd.livesearch.task.Task;
import edu.ucsd.livesearch.task.TaskManager;
import edu.ucsd.livesearch.task.TaskManager.TaskStatus;
import edu.ucsd.livesearch.util.Commons;
import edu.ucsd.saint.commons.IOUtils;
import edu.ucsd.saint.commons.WebAppProps;
import edu.ucsd.saint.commons.http.HttpParameters;

@SuppressWarnings("serial")
public class DownloadPSMImage
extends BaseServlet
{
	/*========================================================================
	 * Constants
	*========================================================================*/
	private static final Logger logger =
		LoggerFactory.getLogger(DownloadPSMImage.class);
	
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	/**
	 * <p>Accepts and processes queries to generate PSM images and related
	 * data, and stream it back to the client.
	 * 
	 * <p>By convention, a GET request to this servlet is assumed to be a
	 * request to read data only.  No creation, update, or deletion of
	 * server resources is handled by this method.
	 * 
	 * @param request	an {@link HttpServletRequest} object that contains
	 * 					the request the client has made of the servlet
	 * 
	 * @param response	an {@link HttpServletResponse} object that contains
	 * 					the response the servlet sends to the client
	 * 
	 * @throws IOException		if an input or output error is detected
	 * 							when the servlet handles the GET request
	 * 
	 * @throws ServletException	if the request for the GET could not be
	 * 							handled
	 */
	@Override
	protected void doGet(
		HttpServletRequest request, HttpServletResponse response
	) throws ServletException, IOException {
		// initialize properties
		try {
			initialize(request, false);
		} catch (ServletException error) {
			getLogger().error(
				"Error initializing servlet properties from request", error);
			response.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return;
		} catch (Throwable error) {
			getLogger().error(
				"Error initializing servlet properties from request", error);
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		}
		HttpParameters parameters = getParameters();
		ServletOutputStream out = response.getOutputStream();
		
		// retrieve relevant parameters
		String taskID = parameters.getParameter("task");
		String filename = parameters.getParameter("file");
		String peptide = parameters.getParameter("peptide");
		String fragmentation = parameters.getParameter("fragmentation");
		String type = parameters.getParameter("type");
		String force = parameters.getParameter("force");
		String trim = parameters.getParameter("trim");
		String inspect = parameters.getParameter("annotation-style-inspect");
		
		// how to identify the spectrum from the originating file?
		String index = parameters.getParameter("index");
		String scan = parameters.getParameter("scan");
		String spectrumid = parameters.getParameter("spectrumid");
		
		// retrieve task
		Task task = TaskManager.queryTask(taskID);
		
		// examine "type" parameter to determine the nature of this request
		boolean thumbnail = false;
		boolean text = false;
		if (type != null) {
			if (type.equals("thumbnail"))
				thumbnail = true;
			else if (type.equals("text"))
				text = true;
		}
		
		// attempt to retrieve proper file
		File file = null;
		try {
			file = generatePSMImage(
				task, filename, index, scan, spectrumid, peptide,
				fragmentation, thumbnail, text, Boolean.parseBoolean(force),
				Boolean.parseBoolean(trim), Boolean.parseBoolean(inspect));
		} catch (Throwable error) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
		
		// write appropriate response
		if (file == null)
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		else {
			response.addHeader("Content-Disposition", "inline");
			if (text) {
				response.setContentType("text/html");
				writeText(file, out);
			} else {
				response.setContentType("image/png");
				writeImage(file, out);
			}
		}
	}
	
	public static File generatePSMImage(
		Task task, String filename, String index, String scan,
		String spectrumid, String peptide, String fragmentation,
		boolean thumbnail, boolean text, boolean force, boolean trim,
		boolean inspect
	) throws FileNotFoundException {
		// instantiate image file loader
		PSMImageGenerator loader = null;
		try {
			loader = new PSMImageGenerator(
				task, filename, index, scan, spectrumid, peptide,
				fragmentation, thumbnail, text, force, trim, inspect);
		} catch (FileNotFoundException error) {
			logger.error(error.getMessage());
			throw error;
		} catch (Throwable error) {
			throw new RuntimeException(error);
		}
		// load image
		if (OnDemandLoader.load(loader)) {
			if (text)
				return loader.getTextFile();
			else return loader.getImageFile();
		} else return null;
	}
	
	private static void writeImage(File image, ServletOutputStream output)
	throws IOException {
		BufferedInputStream input = null;
		try {
			input = new BufferedInputStream(new FileInputStream(image));
			IOUtils.copyStream(input, output);
		} finally {
			if (input != null)
				input.close();
		}
	}
	
	private static void writeText(File text, ServletOutputStream output)
	throws IOException {
		Scanner scanner = null;
		try {
			output.println("<html><head>");
			output.println("<link href=\"../styles/inline.css\" " +
				"rel=\"stylesheet\" type=\"text/css\"/>");
			output.println("</head><body>");
			scanner = new Scanner(
				new BufferedInputStream(new FileInputStream(text)));
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				if (line.equals(""))
					break;				
				if (line.startsWith("MQScore") ||
					line.startsWith("Length") ||
					line.startsWith("NTT") ||
					line.startsWith("Total Cut Score") ||
					line.startsWith("Median Cut Score"))
					continue;
				output.println(line + "<br/>");				
			}
			output.println("</body></html>");
		} catch(FileNotFoundException error) {
			logger.error(String.format("File not found: [%s]", text));
		} finally {
			if (scanner != null)
				scanner.close();
		}		
	}
	
	private static class PSMImageGenerator
	implements OnDemandOperation
	{
		/*====================================================================
		 * Constants
		 *====================================================================*/
		private final Logger logger =
			LoggerFactory.getLogger(PSMImageGenerator.class);
		
		/*====================================================================
		 * Properties
		 *====================================================================*/
		private String resource;
		private Integer index;
		private Integer scan;
		private String spectrumid;
		private String peptide;
		private String fragmentation;
		private String workflow;
		private File spectrum;
		private File image;
		private File annotation;
		private File log;
		private boolean thumbnail;
		private boolean text;
		private boolean force;
		private boolean trim;
		private boolean inspect;
		private GenerateMasses masses;
		
		/*====================================================================
		 * Constructor
		 *====================================================================*/
		public PSMImageGenerator(
			Task task, String filename, String index, String scan,
			String spectrumid, String peptide, String fragmentation,
			boolean thumbnail, boolean text, boolean force, boolean trim,
			boolean inspect
		) throws FileNotFoundException {
			// evaluate task
			if (task == null)
				throw new NullPointerException("Task cannot be null.");
			else if (task instanceof NullTask)
				throw new IllegalArgumentException(
					"Task cannot be an instance of \"NullTask\".");
			else if (TaskStatus.DONE.equals(task.getStatus()) == false)
				throw new IllegalArgumentException("Task must be " +
					"successfully completed to process its results.");
			// set workflow
			workflow = task.getFlowName();
			// set spectrum file
			if (filename == null)
				throw new NullPointerException(
					"Spectrum filename cannot be null.");
			spectrum = new File(filename);
			if (spectrum.exists() == false)
				throw new FileNotFoundException(
					String.format("Spectrum file \"%s\" does not exist.",
						spectrum.getAbsolutePath()));
			if (spectrum.isFile() == false)
				throw new IllegalArgumentException(
					String.format("Spectrum file \"%s\" must be a normal " +
						"(non-directory) file.", spectrum.getAbsolutePath()));
			else if (spectrum.canRead() == false)
				throw new IllegalArgumentException(
					String.format("Spectrum file \"%s\" must be readable.",
						spectrum.getAbsolutePath()));
			// set spectrum index, if present
			if (index != null) try {
				this.index = Integer.parseInt(index);
			} catch (NumberFormatException error) {
				throw new IllegalArgumentException(
					"Spectrum index must be an integer.");
			}
			// validate index number
			if (this.index != null) {
				// spectrum indices are expected to be 1-based, so if this task
				// is associated with a workflow that is known to output 0-based
				// indices, increment the index appropriately
				if (workflow != null && workflow.matches("PEPNOVO"))
					this.index++;
				// if a spectrum index is less than 1, then it is invalid
				if (this.index < 1)
					this.index = null;
				/*
				 * special rule for MS-GFDB, implemented in
				 * response to email thread dated 10/30/12
				 */
				// if the workflow is MS-GFDB and the file is mzXML,
				// ignore the index and use the scan number
				String extension = FilenameUtils.getExtension(filename);
				if (workflow != null && workflow.equals("MSGFDB") &&
					extension != null && extension.equalsIgnoreCase("mzXML"))
					this.index = null;
			}
			// if index is not available or invalid, then try the scan number
			if (this.index == null && scan != null) try {
				this.scan = Integer.parseInt(scan);
			} catch (NumberFormatException error) {
				throw new IllegalArgumentException(
					"Scan number must be an integer.");
			}
			// validate scan number
			if (this.scan != null) {
				// if the task is associated with an InsPecT-based workflow and
				// a .mgf input file, then the output will have 0-based indices,
				// so the scan number must be incremented
				String extension = FilenameUtils.getExtension(filename);
				if (workflow != null &&
					workflow.matches(
						"INSPECT|MSALIGN|MSALIGN-CONVEY|" +
						"MSC-INSPECT|MSC-MSALIGN|PROTEOGENOMICS|PEPNOVO") &&
					extension != null && extension.equalsIgnoreCase("mgf"))
					this.scan++;
				// if a scan number is less than 1, then it is invalid
				if (this.scan < 1)
					this.scan = null;
			}
			// if index and scan are not available or invalid,
			// then try the "spectrum ID"
			if (this.index == null && this.scan == null &&
				spectrumid != null)
				this.spectrumid = spectrumid;
			// validate spectrum ID
			if (this.spectrumid != null) {
				// TODO: do something
			}
			// ensure that some spectrum identifier was found and validated
			if (this.scan == null && this.index == null &&
				this.spectrumid == null)
				throw new NullPointerException("Either a scan number, " +
					"spectrum index or spectrum ID must be provided.");
			// set peptide string
			if (peptide == null)
				throw new NullPointerException("Peptide cannot be null.");
			List<String> peptides = splitPeptide(peptide);
			if (peptides != null && peptides.isEmpty() == false) {
				StringBuffer combinedPeptides = new StringBuffer();
				for (String peptideString : peptides) {
					combinedPeptides.append(peptideString);
					combinedPeptides.append("|");
				}
				combinedPeptides.setLength(combinedPeptides.length() - 1);
				this.peptide = combinedPeptides.toString();
			} else this.peptide = peptide;
			// set fragmentation method
			if (fragmentation != null && fragmentation.equalsIgnoreCase("etd"))
				this.fragmentation = "etd";
			else this.fragmentation = "cid";
			// set request type properties
			this.thumbnail = thumbnail;
			this.text = text;
			this.force = force;
			this.trim = trim;
			this.inspect = inspect;
			// uniquely identify the resource that this loader is dealing with
			String prefix =
				FilenameUtils.getBaseName(filename) + ".";
			if (this.index != null)
				prefix += "I" + this.index;
			else if (this.scan != null)
				prefix += "S" + this.scan;
			else if (this.spectrumid != null)
				prefix += "SpID" + this.spectrumid;
			// TODO: ensure that the peptide string contains
			// no illegal filename characters
			prefix += "." + peptide;
			resource = task + "." + prefix;
			if (thumbnail)
				resource += ".thumbnail";
			// set up target files
			String pathBase = "temp/image/" + prefix;
			image = task.getPath(pathBase + (thumbnail ? ".th" : "") + ".png");
			annotation = task.getPath(pathBase + (thumbnail ? ".th" : "") + ".txt");
			logger.info(annotation.getAbsolutePath());
			log = task.getPath(pathBase + (thumbnail ? ".th" : "") + ".log");
			// be sure amino acid masses file is present
			masses = new GenerateMasses(task);
			if (OnDemandLoader.load(masses) == false) {
				// TODO: report error
			}
		}
		
		/*========================================================================
		 * Public interface methods
		 *========================================================================*/
		public boolean execute() {
			try {
				// build command string
				List<String> arguments = new Vector<String>();
				File specPlot = new File(
					WebAppProps.getPath("livesearch.tools.path"),
					"specplot"
				);
				File tool = new File(specPlot, "bin/specplot");
				
				arguments.add(tool.getAbsolutePath());
				
				// determine file type, use proper argument
				String extension =
					FilenameUtils.getExtension(spectrum.getAbsolutePath());
				if (extension == null) {
					// TODO: report error
					return false;
				} else if (extension.equalsIgnoreCase("mgf")) {
					arguments.add("--mgf");
					arguments.add(spectrum.getAbsolutePath());
				} else if (extension.equalsIgnoreCase("mzxml")) {
					arguments.add("--mzxml");
					arguments.add(spectrum.getAbsolutePath());
				} else if (extension.equalsIgnoreCase("mzml") ||
					extension.toLowerCase().endsWith("gz")) {
					arguments.add("--mzxml");
					arguments.add(spectrum.getAbsolutePath());
					arguments.add("--pwiz");
				} else if (extension.equalsIgnoreCase("pkl")) {
					arguments.add("--pkl");
					arguments.add(spectrum.getAbsolutePath());
				} else if (extension.equalsIgnoreCase("dta")) {
					arguments.add("--dta");
					arguments.add(spectrum.getAbsolutePath());
				} else if (extension.equalsIgnoreCase("pklbin")) {
					arguments.add("--pklbin");
					arguments.add(spectrum.getAbsolutePath());
				}
				
				// specify spectrum identifier
				if (index != null) {
					arguments.add("--spectrum");
					arguments.add(Integer.toString(index));
				} else if (scan != null) {
					// if a scan number is specified, but the file is an
					// mgf file, then it cannot contain scan numbers
					// and therefore should be invoked with --spectrum
					if (extension.equalsIgnoreCase("mgf")) {
						arguments.add("--spectrum");
						arguments.add(Integer.toString(scan));
					} else {
						arguments.add("--spectrumscan");
						arguments.add(Integer.toString(scan));
					}
				} else if (spectrumid != null) {
					arguments.add("--spectrumid");
					arguments.add(spectrumid);
				} else throw new IllegalArgumentException("Either a scan " +
					"number, spectrum index or spectrum ID must be provided.");
				
				// specify peptide argument
				if(peptide.compareTo("*..*") != 0 && peptide.length() > 0){
					arguments.add("--peptide");
					arguments.add(InspectUtils.clean(peptide));							
				}
				else{
					logger.info("Empty Peptide Sentinal Found");
				}
				
				// supply amino acid masses file, if present
				if (masses.resourceExists()) {
					arguments.add("--aa");
					arguments.add(masses.getMassesFile().getAbsolutePath());
				}
				
				// shrink image if thumbnail is specified
				if (thumbnail) {
					arguments.add("--zoom");
					arguments.add("0.5");
				}
				
				if (trim) {
					arguments.add("--trim");
				}
				
				
				// if the workflow is of a type that produces results with
				// InsPecT-style peptide annotation, specify this
				if (workflow != null && (inspect ||
					workflow.matches(
						"INSPECT|MSALIGN|MSALIGN-CONVEY|" +
						"MSC-INSPECT|MSC-MSALIGN|PROTEOGENOMICS|" +
						"MSGFDB|MSGF_PLUS|MSPLIT|MODA|PEPNOVO|" +
						"MASSIVE|MASSIVE-COMPLETE")))
					arguments.add("--annotation-style-inspect");
				
				// specify fragmentation model, if necessary
				if (fragmentation.equals("etd")) {
					arguments.add("--annotation-model");
					arguments.add("model_etd.txt");
				}
				
//				// specify rendering font
//				arguments.add("--font");
//				arguments.add(new File(tool.getParentFile(),
//					"install/all/fonts/bitstream-vera").getAbsolutePath());
				
//				// specify that this is a verbose request
//				arguments.add("--verbose");
				
				// specify output file and directory arguments
				
				arguments.add("--outdir");
				arguments.add(image.getParent());
				
				List<String> img_parameters =  new Vector<String>();
				img_parameters.addAll(arguments);
				List<String> annot_parameters =  new Vector<String>();
				annot_parameters.addAll(arguments);
				
				img_parameters.add("--outfile");
				img_parameters.add(image.getName());
				annot_parameters.add("--annot");
				annot_parameters.add(annotation.getName());
				
				ProcessBuilder pb_img = new ProcessBuilder(img_parameters);
				ProcessBuilder pb_annot = new ProcessBuilder(annot_parameters);
				
				if(this.text == false){
					// build and execute command	
					logger.info("Executing IMG");
					Commons.executeProcess(tool.getParentFile(), log, pb_img);
				}
				else{
					logger.info("Executing TEXT");
					Commons.executeProcess(tool.getParentFile(), log, pb_annot);
				}
				
				
				
				// write executed command to the server log
				StringBuffer buffer = new StringBuffer();
				for (String token : pb_img.command())
					buffer.append(token).append(" ");
				buffer.append("\n");
				for (String token : pb_annot.command())
					buffer.append(token).append(" ");
				logger.info("\n\n" + buffer.toString() + "\n\n");
				return true;
			} catch (Throwable error) {
				logger.error("There was an error executing specplot", error);
			}
			return false;
		}
		
		public boolean resourceExists() {
			return text ? log.exists() : image.exists();
		}
		
		public boolean resourceDated() {
			// if this is a forced-reload request,
			// then the resource is always dated
			if (force)
				return true;
			// otherwise, check the resource file
			File resource = text ? log : image;
			if (resource.exists() == false || spectrum.exists() == false)
				return false;
			else return resource.lastModified() < spectrum.lastModified(); 
		}
		
		public String getResourceName() {
			return resource; 
		}
		
		public File getImageFile() {
			return image;
		}
		
		public File getTextFile() {
			return annotation;
		}
		
		/*====================================================================
		 * Convenience methods
		 *====================================================================*/
		private List<String> splitPeptide(String peptide) {
			if (peptide == null)
				return null;
			// parse peptide string into multiple peptides
			List<String> peptides = null;
			String[] splitPeptides = peptide.split("!");
			if (splitPeptides != null && splitPeptides.length > 0) {
				peptides = new Vector<String>(splitPeptides.length);
				// for each peptide string found, clean it
				for (String splitPeptide : splitPeptides)
					peptides.add(InspectUtils.clean(splitPeptide));
			}
			return peptides;
		}
	}
}
