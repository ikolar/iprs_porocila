import java.io.*;
import java.util.*;
import java.net.*;
import java.text.*;
import java.util.regex.*;
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

    private File zapisnikiDir, neuvedbeDir, ustavitveDir, odlocbeDir;

    public static void main(String[] args) throws Exception {
        ParseDocuments parser = new ParseDocuments();
        // parser.parse();
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
	    String[] documentDirs = new String[] { "zapisniki_dir", "neuvedbe_dir", "ustavitve_dir", "odlocbe_dir" };
	    boolean critical = false;
	    for (String d : documentDirs) {
		    if (props.getProperty(d) == null) {
			    logger.warn("o_O property '" + d + 
				    "' not defined in properties file " + propertiesFilename);
			    critical = true;
		    } else if (! new File(props.getProperty(d)).isDirectory()) {
			    logger.warn("o_O directory '" + new File(props.getProperty(d)).getCanonicalPath() + 
                    " for property '" + d + "' does not exist");
			    critical = true;
		    }
	    }
        if (critical) {
            throw new FileNotFoundException("o_O one or more document dirs are undefined and/or don't exist." + 
                " Check the log for details.");
        }
        logger.info("All ok :)");
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
}
