package edu.ucsd.livesearch.libraries;

import java.sql.Timestamp;

import org.json.simple.JSONObject;

public class SpectrumAnnotation{

	private String SpectrumID;
	private String Compound_Name;
	private String Ion_Source;
	private String Instrument;
	private String Compound_Source;
	private String PI;
	private String Data_Collector;
	private String Adduct;
	private int Scan;
	private float Precursor_MZ;
	private float ExactMass;
	private int Charge;
	private String CAS_Number;
	private String Pubmed_ID;
	private String Smiles;
	private String INCHI;
	private String INCHI_AUX;
	private int Library_Class;
	private String Ion_Mode;
	private Timestamp create_time;
	private String task_id;
	private String user_id;
	



	

	

	public SpectrumAnnotation(){
		super();
		SpectrumID = "";
		Compound_Name = "";
		Ion_Source = "";
		Instrument = "";
		Compound_Source = "";
		PI = "";
		Data_Collector = "";
		Adduct = "";
		Scan = -1;
		Precursor_MZ = 0.f;
		ExactMass = 0.f;
		Charge = 0;
		CAS_Number = "";
		Pubmed_ID = "";
		Smiles = "";
		INCHI = "";
		INCHI_AUX = "";
		Library_Class = 0;
		Ion_Mode = "";
		task_id = "";
		
	}
	
	public SpectrumAnnotation(String spectrumID, String compound_Name, String ion_Source,
			String instrument, String compound_Source, String pI,
			String data_Collector, String adduct, int scan, float precursor_MZ,
			float exactMass, int charge, String cAS_Number, String pubmed_ID,
			String smiles, String iNCHI, String iNCHI_AUX, int library_Class, String ion_Mode) {
		super();
		SpectrumID = spectrumID;
		Compound_Name = compound_Name;
		Ion_Source = ion_Source;
		Instrument = instrument;
		Compound_Source = compound_Source;
		PI = pI;
		Data_Collector = data_Collector;
		Adduct = adduct;
		Scan = scan;
		Precursor_MZ = precursor_MZ;
		ExactMass = exactMass;
		Charge = charge;
		CAS_Number = cAS_Number;
		Pubmed_ID = pubmed_ID;
		Smiles = smiles;
		INCHI = iNCHI;
		INCHI_AUX = iNCHI_AUX;
		Library_Class = library_Class;
		Ion_Mode = ion_Mode;
	}

	private void trim_strings(){
		Compound_Name = Compound_Name.replaceAll("\t", "");
		Ion_Source = Ion_Source.replaceAll("\t", "");
		Instrument = Instrument.replaceAll("\t", "");
		Compound_Source= Compound_Source.replaceAll("\t", "");
		PI = PI.replaceAll("\t", "");
		Data_Collector = Data_Collector.replaceAll("\t", "");
		Adduct = Adduct.replaceAll("\t", "");
		Pubmed_ID = Pubmed_ID.replaceAll("\t", "");
		Smiles = Smiles.replaceAll("\t", "");
		INCHI = INCHI.replaceAll("\t", "");
		INCHI_AUX = INCHI_AUX.replaceAll("\t", "");
		SpectrumID = SpectrumID.replaceAll("\t", "");
		Ion_Mode = Ion_Mode.replaceAll("\t", "");
	}
	
	@Override
	public String toString(){
		trim_strings();
		StringBuilder result = new StringBuilder();
		result.append(Compound_Name + "\t");
		result.append(Ion_Source + "\t");
		result.append(Instrument + "\t");
		result.append(Compound_Source + "\t");
		result.append(PI + "\t");
		result.append(Data_Collector + "\t");
		result.append(Adduct + "\t");
		result.append(Scan + "\t");
		result.append(Precursor_MZ + "\t");
		result.append(ExactMass + "\t");
		result.append(Charge + "\t");
		result.append(CAS_Number + "\t");
		result.append(Pubmed_ID + "\t");
		result.append(Smiles + "\t");
		result.append(INCHI + "\t");
		result.append(INCHI_AUX + "\t");
		result.append(Library_Class + "\t");
		result.append(SpectrumID + "\t");
		result.append(Ion_Mode + "\t");
		
		//Writing out workflow name for library qualities
		switch(Library_Class){
			case 1:
				result.append("UPDATE-SINGLE-ANNOTATED-GOLD\tGold\t");
				break;
			case 2:
				result.append("UPDATE-SINGLE-ANNOTATED-SILVER\tSilver\t");
				break;
			case 3:
				result.append("UPDATE-SINGLE-ANNOTATED-BRONZE\tBronze\t");
				break;
			default:
				
		}
		
		result.append(task_id);
		
		return result.toString();
	}
	
	public String toJSON(){
		StringBuilder result = new StringBuilder();
		
		result.append("{ ");
		result.append(this.toJSON_intermediate());
		result.append(" }");
		
		return result.toString();
	}
	
	/**
	 * Returns the JSON string that useful for embedding in other pieces of JSON
	 * @return
	 */
	public String toJSON_intermediate(){
		StringBuilder result = new StringBuilder();
		
		result.append("\"Compound_Name\" : \"" + JSONObject.escape(Compound_Name) + "\" ,");
		result.append("\"Ion_Source\" : \"" + Ion_Source + "\" ,");
		result.append("\"Compound_Source\" : \"" + Compound_Source + "\" ,");
		result.append("\"Instrument\" : \"" + Instrument + "\" ,");
		result.append("\"PI\" : \"" + JSONObject.escape(PI) + "\" ,");
		result.append("\"Data_Collector\" : \"" + JSONObject.escape(Data_Collector) + "\" ,");
		result.append("\"Adduct\" : \"" + Adduct + "\" ,");
		result.append("\"Scan\" : \"" + Scan + "\" ,");
		result.append("\"Precursor_MZ\" : \"" + Precursor_MZ + "\" ,");
		result.append("\"ExactMass\" : \"" + ExactMass + "\" ,");
		result.append("\"Charge\" : \"" + Charge + "\" ,");
		result.append("\"CAS_Number\" : \"" + JSONObject.escape(CAS_Number) + "\" ,");
		result.append("\"Pubmed_ID\" : \"" + Pubmed_ID + "\" ,");
		result.append("\"Smiles\" : \"" + JSONObject.escape(Smiles) + "\" ,");
		result.append("\"INCHI\" : \"" + JSONObject.escape(INCHI) + "\" ,");
		result.append("\"INCHI_AUX\" : \"" + JSONObject.escape(INCHI_AUX) + "\" ,");
		result.append("\"Library_Class\" : \"" + Library_Class + "\" ,");
		result.append("\"SpectrumID\" : \"" + SpectrumID + "\" ,");
		result.append("\"Ion_Mode\" : \"" + Ion_Mode + "\" ,");
		result.append("\"create_time\" : \"" + create_time + "\" ,");
		result.append("\"task_id\" : \"" + task_id + "\" ,");
		result.append("\"user_id\" : \"" + user_id + "\"");
		
		return result.toString();
	}

	
	
	
	
	
	public int compare(SpectrumAnnotation compareto){
		
		if(SpectrumID.compareTo(compareto.SpectrumID) != 0){
			return 1;
		}
		if(Compound_Name.compareTo(compareto.Compound_Name) != 0){
			return 1;
		}
		if(Ion_Source.compareTo(compareto.Ion_Source) != 0){
			return 1;
		}
		if(Instrument.compareTo(compareto.Instrument) != 0){
			return 1;
		}
		if(Compound_Source.compareTo(compareto.Compound_Source) != 0){
			return 1;
		}
		if(PI.compareTo(compareto.PI) != 0){
			return 1;
		}
		if(Data_Collector.compareTo(compareto.Data_Collector) != 0){
			return 1;
		}
		if(Adduct.compareTo(compareto.Adduct) != 0){
			return 1;
		}
		if(CAS_Number.compareTo(compareto.CAS_Number) != 0){
			return 1;
		}
		if(Pubmed_ID.compareTo(compareto.Pubmed_ID) != 0){
			return 1;
		}
		if(Smiles.compareTo(compareto.Smiles) != 0){
			return 1;
		}
		if(INCHI.compareTo(compareto.INCHI) != 0){
			return 1;
		}
		if(INCHI_AUX.compareTo(compareto.INCHI_AUX) != 0){
			return 1;
		}
		if(Ion_Mode.compareTo(compareto.Ion_Mode) != 0){
			return 1;
		}
		/*if(Scan != compareto.Scan){
			return 1;
		}*/
		if(Precursor_MZ != compareto.Precursor_MZ){
			return 1;
		}
		if(ExactMass != compareto.ExactMass){
			return 1;
		}
		if(Charge != compareto.Charge){
			return 1;
		}
		if(Library_Class != compareto.Library_Class){
			return 1;
		}
		

		return 0;
	}
	
	public String getUser_id() {
		return user_id;
	}

	public void setUser_id(String user_id) {
		this.user_id = user_id;
	}
	
	public String getTask_id() {
		return task_id;
	}

	public void setTask_id(String task_id) {
		this.task_id = task_id;
	}
	
	public String getSpectrumID() {
		return SpectrumID;
	}

	public void setSpectrumID(String spectrumID) {
		SpectrumID = spectrumID;
	}

	public String getCompound_Name() {
		return Compound_Name;
	}

	public void setCompound_Name(String compound_Name) {
		Compound_Name = compound_Name;
	}

	public String getIon_Source() {
		return Ion_Source;
	}

	public void setIon_Source(String ion_Source) {
		Ion_Source = ion_Source;
	}

	public String getInstrument() {
		return Instrument;
	}

	public void setInstrument(String instrument) {
		Instrument = instrument;
	}

	public String getCompound_Source() {
		return Compound_Source;
	}

	public void setCompound_Source(String compound_Source) {
		Compound_Source = compound_Source;
	}

	public String getPI() {
		return PI;
	}

	public void setPI(String pI) {
		PI = pI;
	}

	public String getData_Collector() {
		return Data_Collector;
	}

	public void setData_Collector(String data_Collector) {
		Data_Collector = data_Collector;
	}

	public String getAdduct() {
		return Adduct;
	}

	public void setAdduct(String adduct) {
		Adduct = adduct;
	}

	public int getScan() {
		return Scan;
	}

	public void setScan(int scan) {
		Scan = scan;
	}

	public float getPrecursor_MZ() {
		return Precursor_MZ;
	}

	public void setPrecursor_MZ(float precursor_MZ) {
		Precursor_MZ = precursor_MZ;
	}

	public float getExactMass() {
		return ExactMass;
	}

	public void setExactMass(float exactMass) {
		ExactMass = exactMass;
	}

	public int getCharge() {
		return Charge;
	}

	public void setCharge(int charge) {
		Charge = charge;
	}

	public String getCAS_Number() {
		return CAS_Number;
	}

	public void setCAS_Number(String cAS_Number) {
		CAS_Number = cAS_Number;
	}

	public String getPubmed_ID() {
		return Pubmed_ID;
	}

	public void setPubmed_ID(String pubmed_ID) {
		Pubmed_ID = pubmed_ID;
	}

	public String getSmiles() {
		return Smiles;
	}

	public void setSmiles(String smiles) {
		Smiles = smiles;
	}

	public String getINCHI() {
		return INCHI;
	}

	public void setINCHI(String iNCHI) {
		INCHI = iNCHI;
	}

	public String getINCHI_AUX() {
		return INCHI_AUX;
	}

	public void setINCHI_AUX(String iNCHI_AUX) {
		INCHI_AUX = iNCHI_AUX;
	}

	public int getLibrary_Class() {
		return Library_Class;
	}

	public void setLibrary_Class(int library_Class) {
		Library_Class = library_Class;
	}

	public String getIon_Mode() {
		return Ion_Mode;
	}

	public void setIon_Mode(String ion_Mode) {
		Ion_Mode = ion_Mode;
	}
	
	public Timestamp getCreate_time() {
		return create_time;
	}
	
	public void setCreate_time(Timestamp create_time) {
		this.create_time = create_time;
	}

}
