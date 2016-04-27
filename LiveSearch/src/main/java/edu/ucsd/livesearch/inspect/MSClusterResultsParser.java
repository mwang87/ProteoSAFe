package edu.ucsd.livesearch.inspect;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Writer;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import edu.ucsd.livesearch.ResultsParser;
import edu.ucsd.livesearch.inspect.InspectResult.Hit;
import edu.ucsd.livesearch.task.Task;
import edu.ucsd.livesearch.task.TaskManager.TaskStatus;

/**
 * Parser implementation for Inspect/MSAlign using MSCluster
 * 
 * @author jjcarver
 */
public class MSClusterResultsParser
implements ResultsParser
{
	/*========================================================================
	 * Properties
	 *========================================================================*/
	private Task task;
	private InspectResult result;
	
	/*========================================================================
	 * Constructors
	 *========================================================================*/
	public MSClusterResultsParser(Task task) {
		setTask(task);
	}
	
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	public final String getResultType() {
		return task.getFlowName().toLowerCase();
	}
	
	public final String getDownloadType() {
		return "inspect";
	}
	
	public final long size() {
		return getResult().size();
	}
	
	public final boolean available() {
		return isValid(getTask());
	}
	
	public final boolean ready() {
		return getResult().ready();
	}
	
	public final void writeHitsJS(Writer writer, String variable) {
		InspectUtils.writeHitsJS(writer, variable, getTask());
	}
	
	public final void writeHitsJS(Writer writer, String table, Long hit) {
		if (table != null && table.equals("cluster_matches")) {
			if (hit == null) {
				//TODO: report error
				return;
			} else writeClusterMatchesJS(writer, hit);
		} else InspectUtils.writeHitsJS(writer, "hits", getTask());
	}
	
	public final void writeErrorsJS(Writer writer, String variable) {
		
	}
	
	/*========================================================================
	 * Public utility methods
	 *========================================================================*/
	public static final boolean isValid(Task task) {
		return (task != null && task.getStatus() != TaskStatus.NONEXIST &&
				task.getFlowName().matches("MSC-INSPECT|MSC-MSALIGN"));
	}
	
	/*========================================================================
	 * Property accessor methods
	 *========================================================================*/
	protected final Task getTask() {
		return task;
	}
	
	protected final void setTask(Task task) {
		if (task == null)
			throw new IllegalArgumentException("InspectResultsParser " +
				"objects must be initialized with a non-null task.");
		this.task = task;
		this.result = new InspectResult(task);
	}
	
	protected final InspectResult getResult() {
		return result;
	}
	
	public final String[][][] writeClusterMatchesJS(Writer writer, long position) {
/**/
String[][][] hits = new String[2][][];
/**/
		InspectResult result = getResult();
		result.seek(position);
		Hit hit = null;
		try {
			hit = result.next();
		} catch (Throwable error) {
			System.err.println("There was an error reading the hit.");
		} finally {
/**/
hits[0] = new String[1][3];
hits[0][0][0] = hit.getInternalName();
hits[0][0][1] = hit.getFieldValue("SpecFilePos");
hits[0][0][2] = hit.getFieldValue("Annotation");
/**/
			String filename = hit.getInternalName();
			filename = filename.replaceFirst("mgf", "clust.txt");
			RandomAccessFile mappingFile = null;
			try {
				try {
					mappingFile =
						new RandomAccessFile(
							getTask().getPath("mgf/" + filename), "r");
				} catch (FileNotFoundException error) {
					System.err.println("There was an error reading " +
						"the mapping file.");
					error.printStackTrace();
				} finally {
					if (mappingFile == null) {
						System.err.println("The mapping file is null.");
						return null;
					}
				}
				long scan;
				try {
					scan = Long.parseLong(hit.getFieldValue("Scan#"));
				} catch (NumberFormatException error) {
					System.err.println("There was an error reading " +
						"the hit's scan number.");
					return null;
				}
				String block = null;
				// read up to the specified scan number
				for (int i=0; i<=scan; i++) {
					block = readMapping(mappingFile);
				}
				if (block == null) {
					System.err.println("The scan that was read is null!");
					return null;
				} else {
					String[] lines = block.split("\n");
					Map<Long, Set<Long>> memberSpectra =
						new HashMap<Long, Set<Long>>(lines.length);
/**/
hits[1] = new String[lines.length-1][2];
/**/
					for (int i=1; i<lines.length; i++) {
						// get file index
						String[] columns = lines[i].split("\t");
						long fileIndex;
						try {
							fileIndex = Long.parseLong(columns[0]);
						} catch (NumberFormatException error) {
							System.err.println("There was an error reading " +
								"the hit's file index.");
							return null;
						}
						// get spectrum file using index from "specList" mapping
						File specList = getTask().getPath("specList/");
						String[] specListFiles = null;
						try {
							specListFiles = specList.list();
						} catch (SecurityException error) {
							System.err.println("There was an error reading " +
								"the files in the \"specList\" directory.");
							error.printStackTrace();
						} finally {
							if (specListFiles == null) {
								System.err.println("The \"specList\" " +
									"directory could not be found.");
								return null;
							}
						}
						String spectrumFilename = null;
						for (int j=0; j<specListFiles.length; j++) {
							RandomAccessFile specListFile = null;
							try {
								specListFile =
									new RandomAccessFile(
										getTask().getPath("specList/" +
											specListFiles[j]), "r");
							} catch (FileNotFoundException error) {
								System.err.println("\"specList\" file \"" +
									specListFiles[j] +
									"\" could not be found.");
								error.printStackTrace();
								continue;
							} finally {
								if (specListFile == null) {
									System.err.println("\"specList\" file \"" +
										specListFiles[j] +
										"\" could not be read.");
									continue;
								}
							}
							// read first line of specList file, which should
							// be its starting file index
							long startIndex;
							try {
								startIndex =
									Long.parseLong(specListFile.readLine());
							} catch (IOException error) {
								System.err.println("There was an error " +
									"reading the first line of \"specList\" " +
									"file \"" + specListFiles[j] + "\".");
								error.printStackTrace();
								continue;
							} catch (NumberFormatException error) {
								System.err.println("The first line of " +
									"\"specList\" file \"" + specListFiles[j] +
									"\" was not an integer file index, " +
									"as expected.");
								continue;
							}
							// only read this file if its start index isn't
							// beyond the index of the file we want
							if (startIndex > fileIndex)
								continue;
							else startIndex = fileIndex - startIndex;
							// loop through the file until reaching the desired
							// file index
							String line = null;
							for (int k=0; k<startIndex; k++) {
								try {
									if (specListFile.readLine() == null)
										break;
								} catch (IOException error) {
									System.err.println("There was an error " +
										"reading line " + (k + 1) +
										"of \"specList\" file \"" +
										specListFiles[j] + "\".");
									error.printStackTrace();
									continue;
								}
							}
							try {
								line = specListFile.readLine();
							} catch (IOException error) {
								System.err.println("There was an error " +
									"reading name of spectrum file with " +
									"index " + j + " of \"specList\" file \"" +
									specListFiles[j] + "\".");
								error.printStackTrace();
								continue;
							}
							if (line == null || line.trim().equals(""))
								continue;
							else {
								// remove path directories from filename,
								// if present
								int slash = line.lastIndexOf("/");
								if (slash >= 0 && slash < (line.length() - 1))
									spectrumFilename =
										line.substring(slash + 1);
								else spectrumFilename = line;
								break;
							}
						}
/**/
String uploadName = getTask().queryOriginalName(spectrumFilename);
if (uploadName != null)
	hits[1][i-1][0] = uploadName;
else hits[1][i-1][0] = spectrumFilename;
hits[1][i-1][1] = columns[1];
/**/
						Set<Long> spectra = memberSpectra.get(fileIndex);
						if (spectra == null)
							spectra = new HashSet<Long>();
						long spectrumNumber;
						try {
							spectrumNumber = Long.parseLong(columns[1]);
						} catch (NumberFormatException error) {
							System.err.println("There was an error reading the hit's spectrum number.");
							return null;
						}
						spectra.add(spectrumNumber);
						memberSpectra.put(fileIndex, spectra);
					}
//					for (long fileIndex : memberSpectra.keySet()) {
//						String spectrumFilename = "" + fileIndex;
//						int leadingZeros = 5 - spectrumFilename.length();
//						for (int i=0; i<leadingZeros; i++)
//							spectrumFilename = "0" + spectrumFilename;
//						spectrumFilename += ".mzXML";
//						try {
//							File source = getTask().getPath("spec/" +
//								spectrumFilename);
//							File sorted = getTask().getPath("cluster.sorted");
//							InspectResult clusterResult =
//								new InspectResult(getTask(), source, sorted);
//								getResult();		
//							try {
//								int counter = 0;
//								write(writer, "<script language='javascript' type='text/javascript'>%n");
//								write(writer, "/* <![CDATA[ */%n");
//								write(writer, "hits[\"cluster_matches\"] = [%n");
//								for(Hit hit: result){
//									write(writer, "  {", variable);
//									for(String field: result.getFieldNames())
//										write(writer, "'%s':%s,", field, quote(hit.getFieldValue(field)));
//									write(writer, "ProteinID:%s,", quote(hit.getProteinID()));
//									write(writer, "Internal :%s,", quote(hit.getInternalName()));
//									write(writer, "Position :%d,",   hit.getPosition());
//									write(writer, "Counter  :'H%d'", counter++);
//									write(writer, "  },%n");
//								}
//								write(writer, "];%n");
//								write(writer, "/* ]]> */ %n");
//								write(writer, "</script>%n");
//							}
//							catch(IOException e) {
//								e.printStackTrace();
//							}
//							finally {
//								result.close();
//							}
//						} catch (Throwable error) {
//							System.err.println("There was an error parsing " +
//								"the original spectrum file: " +
//								error.getMessage());
//							return null;
//						}
//					}
				}
			} finally {
				if (mappingFile != null) try {
					mappingFile.close();
				} catch (IOException error) {}
			}
		}
/**/
return hits;
/**/
	}
	
	private String readMapping(RandomAccessFile mappingFile) {
		String block = "";
		String line;
		do {
			line = null;
			try {
				line = mappingFile.readLine();
			} catch (IOException error) {}
			if (line == null || line.trim().equals(""))
				break;
			else block += line + "\n";
		} while (true);
		// chomp trailing newline
		if (block.length() > 0)
			block = block.substring(0, block.length() - 1);
		if (block.trim().equals(""))
			return null;
		else return block;
	}
	
	@SuppressWarnings("unused")
	private void write(Writer writer, String format, Object ... args)
			throws IOException {
		writer.write(String.format(format, args));
	}
	
	public Document parseXML(String filename) {
		if (filename == null) {
			//TODO: report error
			throw new RuntimeException("1");
			//return null;
		}
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = null;
		try {
			builder = factory.newDocumentBuilder();
		} catch (ParserConfigurationException error) {
			//TODO: report error
			throw new RuntimeException("2");
			//return null;
		}
		InputSource source = null;
		URL resource = MSClusterResultsParser.class.getResource(filename);
		if (resource == null) {
			//TODO: report error
			throw new RuntimeException("3");
			//return null;
		} else try {
			System.out.println("File = " + resource.getFile());
			source =
				new InputSource(new FileReader(new File(resource.getFile())));
		} catch (FileNotFoundException error) {
			//TODO: report error
			throw new RuntimeException("4");
			//return null;
		}
		Document document = null;
		try {
			document = builder.parse(source);
		} catch (IOException error) {
			//TODO: report error
			throw new RuntimeException("5");
		} catch (SAXException error) {
			//TODO: report error
			throw new RuntimeException("6");
		}
		return document;
	}
}
