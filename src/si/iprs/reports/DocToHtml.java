package si.iprs.reports;

import java.io.*;
import java.util.*;
import java.net.*;
import javax.xml.transform.*;
import javax.xml.transform.sax.*;
import javax.xml.transform.stream.*;
import org.apache.tika.sax.*;
import org.apache.tika.cli.*;
import org.apache.tika.io.*;
import org.apache.tika.parser.*;
import org.apache.tika.detect.*;
import org.apache.tika.metadata.*;
import org.apache.commons.logging.*;

/**
 * Converts .doc file into html using the apache tika library.
 *
 * The q&d way: tha the org.apache.tika.cli.TikaCLI class and just copy the needed bootstrapping code here.
 */

public class DocToHtml {
    private static final Log logger = LogFactory.getLog(TikaCLI.class);

    public static void main(String[] args) throws Exception {
        String html = callTika(new File(args[0]).getCanonicalFile());
        System.out.print(html);
    }

    /**
     * Convert the .doc/.docx file into html
     */
    public static String callTika(File file) throws Exception {
        logger.info("Initializing Tika parser");
        // initialize the tika framework
        ParseContext context = new ParseContext();
        Detector detector = new DefaultDetector();
        Parser parser = new AutoDetectParser(detector);
        context.set(Parser.class, parser);        

        // set the input and output streams
        Metadata metadata = new Metadata();
        URL url = file.toURI().toURL();
        InputStream input = TikaInputStream.get(url, metadata);        
        OutputStream output = new ByteArrayOutputStream();

        // go!
        try {
            boolean prettyPrint = false; // don't add extra newlines
            String encoding = null; // autodetect
            org.xml.sax.ContentHandler handler = 
                new org.apache.tika.sax.ExpandedTitleContentHandler(getTransformerHandler(output, "html", encoding, prettyPrint));
            logger.info("Parsing url " + url);
            parser.parse(input, handler, metadata, context);

        } finally {
            input.close();
            output.flush();
        }

        logger.info("Parsing url " + url + " done");
        return output.toString();
    }

    /**
     * Returns a transformer handler that serializes incoming SAX events
     * to XHTML or HTML (depending the given method) using the given output
     * encoding.
     *
     * @see <a href="https://issues.apache.org/jira/browse/TIKA-277">TIKA-277</a>
     * @param output output stream
     * @param method "xml" or "html"
     * @param encoding output encoding,
     *                 or <code>null</code> for the platform default
     * @return {@link System#out} transformer handler
     * @throws TransformerConfigurationException
     *         if the transformer can not be created
     */
    private static TransformerHandler getTransformerHandler(
            OutputStream output, String method, String encoding, boolean prettyPrint)
            throws TransformerConfigurationException {
        SAXTransformerFactory factory = (SAXTransformerFactory)
                SAXTransformerFactory.newInstance();
        TransformerHandler handler = factory.newTransformerHandler();
        handler.getTransformer().setOutputProperty(OutputKeys.METHOD, method);
        handler.getTransformer().setOutputProperty(OutputKeys.INDENT, prettyPrint ? "yes" : "no");
        if (encoding != null) {
            handler.getTransformer().setOutputProperty(
                    OutputKeys.ENCODING, encoding);
        }
        handler.setResult(new StreamResult(output));
        return handler;
    }
}
