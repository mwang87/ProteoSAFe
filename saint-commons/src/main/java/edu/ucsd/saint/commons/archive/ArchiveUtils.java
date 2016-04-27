package edu.ucsd.saint.commons.archive;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.io.FilenameUtils;

public class ArchiveUtils {
	public static Archive loadArchive(FileItem item) throws IOException{
		String filename = FilenameUtils.getName(item.getName());
		String normalized = filename.toLowerCase();
		InputStream input = item.getInputStream();
		CompressionType compression = CompressionType.byExtension(normalized);
		return loadArchive(compression, input);
	}

	public static Archive loadArchive(CompressionType type, InputStream input)
		throws IOException{
		switch(type){
		case TAR:	return new TarArchive(input);
		case ZIP:	return new ZipArchive(input);
		case BZIP2:	case TBZ: return new BZipArchive(input);
		case GZIP:	case TGZ: return new GZipArchive(input);
		default:	return new PlainArchive(input);
		}
	}
	
	public static List<ArchiveEntry> extract(Archive archive, File folder)
		throws IOException {
		List<ArchiveEntry> entries = new LinkedList<ArchiveEntry>();
		ArchiveEntry entry = archive.getNextEntry();				
		while(entry != null){					
			File file = new File(folder, entry.getFilename());
			file.getParentFile().mkdirs();
			archive.read(file); // this is resource related
			archive.closeEntry();
			entries.add(entry);
			entry = archive.getNextEntry();
		}
		return entries;
	}

	public static void compress(Archiver archiver, File folder)
		throws IOException{
		File files[] = folder.listFiles();
		if(files.length == 0) return;
		try{
			for(File file: files){
				if(!file.isFile()) continue;
				ArchiveEntry entry = new ArchiveEntry(file.getName(), file);
				archiver.putNextEntry(entry);
				archiver.write(entry.getFile());
				archiver.closeEntry();
			}
		}
		finally{
			archiver.close();
		}
	}

	public static Archiver createArchiver(OutputStream out, CompressionType type) throws IOException{
		switch(type){
		case ZIP:	return new ZipArchiver(out);
		case BZIP2:	case TBZ: return new BZipArchiver(out);
		case TGZ: case GZIP: return new GZipArchiver(out);
		case TAR: 	return new TarArchiver(out);
		default:	return new PlainArchiver(out); 
		}
	}
}
