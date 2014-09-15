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

public class ParseInspectionsList {
    public static final Log logger = 
        LogFactory.getLog(ParseInspectionsList.class);
    private boolean debug = false;
    private String debugFilename = "zadeve.html";

    private File listFile;
    private File documentsDir;
    private int includeLastNYears = 3;
    private String html;

    public static void main(String[] args) throws Exception {
        ParseInspectionsList parser = new ParseInspectionsList();
        parser.parse();
    }

    /**
     * Create a new parser, load all the config from the properties file
     *
     */
    public ParseInspectionsList() throws Exception {
        this(null);
    }

    /**
     * Create a new parser, load all the config from the properties file
     * but use a specific list file. Good for testing.
     *
     */
    public ParseInspectionsList(File listFile) throws Exception {
        this.listFile = listFile;

        loadProperties();
        loadList();
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

        if (this.listFile == null) {
            String listFilename = props.getProperty("list_filename");
            if (listFilename == null)
                throw new FileNotFoundException("o_O list_filename (.doc " +
                " tabela z dodeljenimi primeri) ni definirana v properties " +
                " datoteki '" + propertiesFilename + "'"); 
            this.listFile = new File(listFilename);
        }
        if (! this.listFile.exists())
            throw new FileNotFoundException("o_O list file '" + 
                this.listFile.getCanonicalPath() + "' could not be found");

        // todo: document dirs (zapisniki, sklepi o neuvedbi, sklepi o ustavitvi, odločbe)

        int includeLastNYears = Integer.parseInt(props.getProperty("include_last_n_years", "3"));
        if (includeLastNYears > 10 || includeLastNYears < 1) {
            logger.warn("o_O invalid include_last_n_years property. Should be an int in [1,10]. Defaulting to 1.");
            includeLastNYears = 1;
        }
        this.includeLastNYears = includeLastNYears;
    }

    /**
     * Convert .doc into html.
     *
     * If debug mode is one, and a pre-prepared .html file is in place, that will be used instead
     * because the Tika conversion takes quite a while.
     */
    protected void loadList() throws Exception {
        if (debug) {
            File debugFile = new File(this.debugFilename);
            if (debugFile.exists()) {
                logger.info("Loading case list from debug file: " + debugFile.getCanonicalPath());
                this.html = new Scanner(debugFile, "UTF-8").
                    useDelimiter("\\A").next();
                return;
            }
        }

        this.html = DocToHtml.callTika(listFile);
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
        org.jsoup.nodes.Document soup = Jsoup.parse(this.html);
        List<Case> cases = new ArrayList<Case>();
        
        // get the tables
        Elements tables = soup.select("table");
        int numTables = tables.size();
        if (numTables == 0)
            throw new NoSuchElementException("o_O Could not find <table> "+
                "elements within the list file");
        if (numTables < 2)
            logger.warn("o_O There seem to be to little tables (" + numTables +
                ") in the list file");
        
        // throw away the last table (the tamplate for opening new cases)
        Element lastTable = tables.get(numTables -1);
        String lastHtml = lastTable.html();
        if (lastHtml.contains("Vrsta zadeve") && lastHtml.contains("Subjekt zadeve:")) {
            tables.remove(lastTable); 
        } else {
            logger.warn("o_O The last table doesn't look like a new case template.");
        }

        // get the relevant tabels (usually for the last couple of years)
        if (includeLastNYears > tables.size())
            includeLastNYears = tables.size();
        List<Element> tablesToParse = tables.subList(tables.size() - includeLastNYears - 1, tables.size());

        // do the harlem shake
        for (Element table : tablesToParse) {
            Elements rows = table.select("tr");
            Iterator<Element> rowsIterator = rows.iterator();
            if (rowsIterator.hasNext())
                rowsIterator.next(); // disregard first row (heading)

            while (rowsIterator.hasNext()) {
                Element row = rowsIterator.next();
                Elements cols = row.select("td");
                if (cols.size() != 5) {
                    logger.warn("o_O we found a row that doesn't have the usual 5 columns, skipping: " + row.html());
                    continue;
                }

                // the columns. the first one (#) is kapput for some reason, and
                // we don't need it anyway
                String prijavitelj = cols.get(1).text().trim();
                String zavezanecOpis = cols.get(2).text().trim();
                String nadzornikInSteviloPrimerov = cols.get(3).text().trim();
                String datumDodelitveString = cols.get(4).text().trim();
                boolean hasWarnings = false;                    

                // date first. if the date isn't valid just throw away the row.
                Date datumDodelitve = null;
                SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yy");
                try {           
                    datumDodelitve = sdf.parse(datumDodelitveString);
                } catch (ParseException pe) {
                    if (rowsIterator.hasNext()) { // isn't last line
                        logger.warn("o_O we found a row with an invalid date, and it doesn't seem to be the last one: " + row.html(), pe);
                    }
                    continue;            
                }

                // zavezanec, short case description
                // there's a dash seperating the two, and since this was entered by hand,
                // there may be problems with parsing. warn if so.
                String zavezanec = null;
                String opis = null;
                String[] separators = new String[] { " - ", " - ", " – " };
                for (String separator : separators) {
                    String[] parts = zavezanecOpis.split(separator);
                    if (parts.length != 2) {
                        continue;
                    } else {
                        zavezanec = parts[0].trim();
                        opis = parts[1].trim();             
                    }
                }
                if (zavezanec == null) {
                    logger.warn("o_O list parsing warning: found a row where I can't " +
                        "decisively split the zavezanec and the opis strings from '" + 
                        zavezanecOpis + "'. Consider fixing this row:\n" + formatForLog(row.html()));
                    hasWarnings = true;

                    // try again with more separators. This will propbably yield mistakes but the user has been warned
                    separators = new String[] { " - ", " - ", " – ", "\\s*-\\s*", "\\s*-\\s*", "\\s*–\\s*" };
                    for (String separator : separators) {
                        String[] parts = zavezanecOpis.split(separator);
                        if (parts.length == 2) {
                            zavezanec = parts[0];
                            opis = parts[1];             
                        } else {
                            int i = zavezanecOpis.lastIndexOf(separator);
                            if (i == -1) {
                                zavezanec = zavezanecOpis;
                            } else {
                                zavezanec = zavezanecOpis.substring(0, i);
                                opis = parts[parts.length -1];
                            } 
                        }
                    }
                }

                // nadzornikInSteviloPrimerov
                // also separated by dash, similar story.
                String nadzornik = null;
                String separator = "\\s*-\\s*";
                String[] parts = nadzornikInSteviloPrimerov.split(separator, 2);
                if (parts.length != 2) {
                    logger.warn("o_O list parsing warning: found a row where I can't " +
                        "decisively split the nadzornik and their case load from '" + 
                        nadzornikInSteviloPrimerov + "'. Consider fixing this row: " + row.html());
                    nadzornik = nadzornikInSteviloPrimerov;
                    hasWarnings = true;
                } else {
                    nadzornik = parts[0];
                }
     
                // all done
                Case c = new Case(prijavitelj, zavezanec, opis, nadzornik, datumDodelitve);
                cases.add(c);
            }
        }

        // some sanity checking
        checkNadzorniki(cases);

        for (Case c : cases) {
            System.out.println(String.format("GUCCI\t'%s'\t'%s'\t'%s'\t'%s'\t'%s'\t'%s'", 
                "?", c.getDatumDodelitve(), c.getZavezanec(), c.getOpis(), c.getPrijavitelj(), c.getNadzornik()));
        }
    }

    /**
     * Check for spelling mistakes in nadzorniki's names
     */
    protected void checkNadzorniki(List<Case> cases) {
        // count up the number of cases given to each nadzornik
        Map<String, Integer> caseload = new HashMap<String, Integer>();
        for (Case c : cases) {
            String nadzornik = c.getNadzornik();
            if (caseload.containsKey(nadzornik)) {
                caseload.put(nadzornik, caseload.get(nadzornik)+1);
            } else {
                caseload.put(nadzornik, 1);
            }          
        }

        Iterator<Map.Entry<String, Integer>> it = caseload.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Integer> pair = it.next();
            logger.info(String.format("Nadzornik '%s' was assigned '%s' cases", pair.getKey(), pair.getValue()));
            if (pair.getValue() < 5)
                logger.warn(String.format("o_O probably a miss-spelled nadzornik name: '%s' (%s uses)", pair.getKey(), pair.getValue()));
        }        
        
    }

    /**
     * Indent the html a bit for prettier logging
     */
    private String formatForLog(String html) {
        String[] lines = html.split("\\n");
        for (String line: lines) {
            line = "  " + line;
        }

        return String.join("\n", lines);
    }

    public void parseAsText(String inspectionsListHtml) {
        String lastModified = null;

        Scanner scanner = new Scanner(inspectionsListHtml);

        boolean start = false;
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
        
            if (line.startsWith("<meta name=\"modified\"")) {
                lastModified = line.substring(0, line.lastIndexOf("\""));
                lastModified = lastModified.substring(lastModified.lastIndexOf("\"") +1);
            }


        }

    }

}
