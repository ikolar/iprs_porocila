import java.io.*;
import java.util.*;
import com.aspose.words.Document;
import com.aspose.words.SaveFormat;

/*
 * Helper class that uses the Aspose MS Word library to accept all changes
 * in word documents.  This way, when we need to extract their text later,
 * we'll only get the latest version and not the changes as well (super
 * annoying to parse btw).
 *
 * Run with filename to convert only that filename, or with -r to find and
 * convert all the .doc/.docx under the current working directory (be
 * careful with that btw).
 *
 * The converted filename will be <base>.converted.docx.
 *
 * @author ikolar
 */
public class AcceptAllChanges {
	/**
	 * Get original file extension
	 *
	 */
	protected String getExtension(File file) {
		int i = file.getName().lastIndexOf(".");
		if (i > 0)
			return file.getName().substring(i+1).toLowerCase();

		return null;		
	}
	
	/**
	 * Get path to merged file
	 *
	 */
	protected File getMergedFile(File file) {
		int i = file.getName().lastIndexOf(".");
		if (i > 0)
			return new File(file.getParentFile(), file.getName().substring(0, i) + ".merged.docx");
		
		return null;
	}


	public File acceptAllChanges(File original) throws IOException {
		original = original.getCanonicalFile();
	
		String ext = getExtension(original);
		if (! ("doc".equals(ext) || "docx".equals(ext)) )
			throw new IOException("Not a .doc/.docx file (ext = " + ext + "): " + original);
		
		File merged = getMergedFile(original);
		if (merged.exists())
			return merged;

		try {	
			Document doc = new Document(original.getCanonicalPath());
			doc.acceptAllRevisions();
			doc.save(merged.getCanonicalPath(), SaveFormat.DOCX);
			return merged;
		} catch (Exception e) {
			throw new IOException("Aspose lib reported an error while accepting all the changes: " + e.getMessage(), e);
		}
	}


	public static void main(String[] args) throws IOException {
		if (args.length < 1) {
			System.err.println("Usage: java AcceptAllChanges <msword document>");
			System.exit(-1);
		}
		
		AcceptAllChanges acceptor = new AcceptAllChanges();
		File merged = acceptor.acceptAllChanges(new File(args[0]));
		System.out.println(merged.getCanonicalFile());
	}
}
