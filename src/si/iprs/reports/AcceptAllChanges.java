package si.iprs.reports;

import java.io.*;
import java.util.*;
import org.apache.commons.io.*;
import org.apache.commons.logging.*;
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
    public static final Log logger =
            LogFactory.getLog(AcceptAllChanges.class);
            	
	/**
	 * Get path to merged file
	 *
	 */
    public File getMergedFile(File file) throws IOException {
        return new File(file.getCanonicalFile().getParentFile(),
            FilenameUtils.getBaseName(file.getName()) + ".merged.docx");
	}

    public File acceptAllChanges(File original) throws IOException {
        return acceptAllChanges(original, getMergedFile(original), false);

    }

	public File acceptAllChanges(File original, File merged, boolean overwrite) throws IOException {
		original = original.getCanonicalFile();
	
		String ext = FilenameUtils.getExtension(original.getName());
		if (! ("doc".equalsIgnoreCase(ext) || "docx".equalsIgnoreCase(ext)) )
			throw new IOException("Not a .doc/.docx file (ext = " + ext + "): " + original);
		
		if (merged.exists() && merged.length() > 0 && !overwrite)
			return merged;

		try {	
			logger.info(String.format("Accepting all changes in %s, output will be %s ..", original, merged));
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
