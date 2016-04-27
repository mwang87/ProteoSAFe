package edu.ucsd.saint.commons.archive;

public enum CompressionType {
	NONE("",     "application/octet-stream", false),
	ZIP("zip",   "application/zip", true),
	TAR("tar",   "application/x-tar", true),
	GZIP("gz",   "application/x-gzip", true),
	TGZ("tgz",   "application/x-compressed", true),
	BZIP2("bz2", "application/x-bzip", true),
	TBZ("tbz",   "application/x-compressed", true);
	
	private final String extension;
	private final String mime;
	private final boolean isArchive;
	
	CompressionType(String extension, String mime, boolean archiveType){
		this.extension = extension;
		this.mime = mime;
		this.isArchive = archiveType;
	}
	
	public static CompressionType byTypeName(String name){
		name = name.toUpperCase();
		for(CompressionType t: values())
			if(t.toString().equals(name))
				return t;
		return NONE;
	}

	public static CompressionType byExtension(String filename){
		if(filename.endsWith(".tar"))
			return TAR;
		if(filename.endsWith(".zip"))
			return ZIP;
		if(filename.endsWith(".tgz") || filename.endsWith(".tar.gz") )
			return TGZ;
		if(filename.endsWith(".gz"))
			return GZIP;
		if(filename.endsWith("tar.bz2") || filename.endsWith(".tbz") || filename.endsWith(".tbz2"))
			return TBZ;
		if(filename.endsWith(".bz2"))
			return BZIP2;
		return NONE;
	}
	
	public String getExtension(){
		return extension;
	}
	
	public String getMime(){
		return mime;
	}
	
	public boolean isArchive(){
		return isArchive;
	}
}
