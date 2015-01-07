package si.iprs.reports;

import java.io.*;
import java.util.*;
import java.net.*;
import java.text.*;
import java.util.regex.*;
import java.util.*;
import org.apache.commons.io.*;
import org.apache.commons.io.filefilter.*;
import org.apache.commons.logging.*;
import org.jsoup.*;
import org.jsoup.select.*;
import org.jsoup.nodes.*;

/**
 * ParseInspectionsList - parse a .doc with a list of open cases into
 * a mysql database
 * 
 * The inspector general maintains a MS Word (.doc) document with a table of
 * new cases (one fresh table per calendar year). We need to parse out a list
 * of those cases.
 * 
 * The columns are:<ol>
 * <li>#</li>
 * <li>prijavitelj</li>
 * <li>zavezanec - opis</li>
 * <li>nadzornik - case load</li>
 * <li>datum dodelitve</li></ol>
 * 
 * Note that this document was not made to be machine-readables. There 
 * are a handful of typos that should be fixed and that the script will
 * attempt to warn you of. Also, At the very end, there is extra table 
 * with a template for opening case. That we discard.
 *
 * The way we parse is we use Apache Tika (a q&d hack of their TikaCLI class)
 * to convert the .doc to html. We then use jsoup to parse out the tables. This
 * appears to be easier as the .doc is saved in an older (propriatery) word format
 * and Apache POI doesn't have the methods to extract the tables directly.
 * Converting the document to .docx etc does not appear to be an option atm. Also,
 * we'll be using a lot of doc-to-html later on when parsing case documents.
 * It's just easier that way.
 *
 * Java was chosen because the solution has to work on a rather dated computer platform
 * (WINXP) and that platform is just horrid for python etc.
 *
 * @author ikolar
 */

public class ParseDocuments {
    public static final Log logger = 
        LogFactory.getLog(ParseDocuments.class);
    private boolean debug = false;

	private static final String[] documentDirs = new String[] { "zapisniki_dir", "neuvedbe_dir", "ustavitve_dir", "odlocbe_dir", "prekrski_dir", "destination_dir" };
    private Map<String, File> sourceDirs = new HashMap<String, File>();
    private File destinationDir;

    public static void main(String[] args) throws Exception {
        ParseDocuments parser = new ParseDocuments();
        parser.prepareDocuments();
    }

    /**
     * Create a new parser, load all the config from the properties file
     * but use a specific list file. Good for testing.
     *
     */
    public ParseDocuments() throws Exception {
        loadProperties();
    }

    /**
     * Load the properties. The method will complain if anything of note
     * is missing.
     *
     */
    protected void loadProperties() throws IOException {
        Properties props = new Properties();
        String propertiesFilename = "porocila.properties";
        InputStream in = getClass().getResourceAsStream(propertiesFilename);
        if (in == null)
            throw new FileNotFoundException("o_O no properties file '" +
                propertiesFilename + "' could be found");
        props.load(in);
        in.close();

        this.debug = "1".equals(props.getProperty("debug"));

        // todo: document dirs (zapisniki, sklepi o neuvedbi, sklepi o ustavitvi, odločbe)
	    logger.info("Checking whether the document directories are properly defined in the properties file ..");
	    boolean critical = false;
	    for (String d : documentDirs) {
		    if (props.getProperty(d) == null) {
			    logger.error("o_O property '" + d + 
				    "' not defined in properties file " + propertiesFilename);
			    critical = true;
                continue;
		    } 
            
            File dir = new File(props.getProperty(d));
            if (! dir.isDirectory()) {
			    logger.error("o_O directory '" + dir.getCanonicalPath() + 
                    " for property '" + d + "' does not exist");
			    critical = true;
                  
		    } else {
                if ("destination_dir".equals(d))
                    destinationDir = dir;
                else
                    sourceDirs.put(d, dir); 
            }
	    }
        if (critical) {
            throw new FileNotFoundException("o_O one or more document dirs are undefined and/or don't exist." + 
                " Check the log for details.");
        }
    }

    /**
     * Parse the html obtained from the case list .doc.
     * 
     * The .doc is comprised of several tables with a list of open cases, one table per year.
     * Fields are #, prijavitelj, zavezanec - opis zadeve, nadzornik, datum.
     * 
     * The last table is a template for opening new cases and should be ignored.
     *
     */
    public void parse() throws Exception {
    }
    
    /**
      * Prepare documents from all source directories for parsing.
      */
    public List<Document> prepareDocuments() throws IOException {
        List<Document> allDocs = new ArrayList<Document>();

        for (Map.Entry<String, File> source : sourceDirs.entrySet()) {
            File sourceDir = source.getValue();
            allDocs.addAll(prepareDocuments(sourceDir, destinationDir));

        }

        return allDocs;
    }    
    
    /**
     *
     */  
    public List<Document> prepareDocuments(File sourceDir, File destinationDir) throws IOException {
        logger.info(String.format("Preparing docs, sourceDir = %s, destinationDir = %s", sourceDir, destinationDir)); 

        // find the source files
        Collection<File> sources = FileUtils.listFiles(
            sourceDir,
            new IOFileFilter() {
                public boolean accept(File file) {
                    return accept(file.getParentFile(), file.getName());
                }

                public boolean accept(File dir, String name) {
                    if (FilenameUtils.getBaseName(name).toLowerCase().endsWith(".merged"))
                        return false;
                    
                    String ext = FilenameUtils.getExtension(name);
                    if (! ("doc".equalsIgnoreCase(ext) || "docx".equalsIgnoreCase(ext)) )
                        return false;

                    return true;
                }
            },
            TrueFileFilter.INSTANCE
        );

        // see who needs what        
        List<Document> docs = new ArrayList<Document>();
        List<Document> toMerge = new ArrayList<Document>();
        List<Document> toHtml = new ArrayList<Document>();
        List<Document> toMergedHtml = new ArrayList<Document>();
        for (File src : sources) {
            File outdir = new File(destinationDir, sourceDir.getName());
            String base = src.getName();
            
            Document doc = new Document(src);
            doc.setMerged(new File(outdir, base + ".merged.docx"));
            doc.setHtml(new File(outdir, base + ".html"));
            doc.setMergedHtml(new File(outdir, base + ".merged.html"));
            docs.add(doc);

            if (! doc.getMerged().exists())
                toMerge.add(doc);
            if (! doc.getHtml().exists())
                toHtml.add(doc);
            if (! doc.getMergedHtml().exists())
                toMergedHtml.add(doc);
            
            FileUtils.forceMkdir(doc.getMerged().getParentFile());
        }
        logger.info(String.format("Need to prepare %d merges, %d htmls, %d mergedHtmls", toMerge.size(), toHtml.size(), toMergedHtml.size()));

        AcceptAllChanges acceptor = new AcceptAllChanges();
        for (Document doc : toMerge) {
            acceptor.acceptAllChanges(doc.getSrc(), doc.getMerged(), false);
        }

        for (Document doc : toHtml) {
            DocToHtml.callTika(doc.getSrc(), doc.getHtml());
        }
        
        for (Document doc : toMergedHtml) {
            if (! doc.getMerged().exists()) {
                logger.warn("o_O can't make merged html file if merged file " + doc.getMerged() + " doesn't exist ..");
                continue;
            }
            DocToHtml.callTika(doc.getMerged(), doc.getMergedHtml());
        }                            

        return docs;
    }
}
