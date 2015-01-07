package si.iprs.reports;

import java.io.*;
import java.util.*;
import java.net.*;
import javax.xml.transform.*;
import javax.xml.transform.sax.*;
import javax.xml.transform.stream.*;
import org.xml.sax.SAXException;
import org.apache.tika.sax.*;
import org.apache.tika.cli.*;
import org.apache.tika.io.*;
import org.apache.tika.parser.*;
import org.apache.tika.detect.*;
import org.apache.tika.metadata.*;
import org.apache.tika.exception.*;
import org.apache.commons.logging.*;

/**
 * Converts .doc/.docx file into html using the Apache Tika library 
 *
 * The q&amp; way: take the org.apache.tika.cli.TikaCLI class and just copy the needed bootstrapping code here.
 */

public class DocToHtml {
    private static final Log logger = LogFactory.getLog(TikaCLI.class);

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: java -cp .. DocToHtml <doc or docx file>");
            System.exit(-1);
        }
        
        callTika(new File(args[0]).getCanonicalFile());
    }


    public static void callTika(File doc, File html) throws IOException {
        String s = DocToHtml.callTika(doc);

        FileWriter out = new FileWriter(html);
        try {
            out.write(s);
        } finally {
            out.flush();
            out.close();
        }
    }

    /**
     * Convert the .doc/.docx file into html
     */
    public static String callTika(File doc) throws IOException {
        // initialize the tika framework
        ParseContext context = new ParseContext();
        Detector detector = new DefaultDetector();
        Parser parser = new AutoDetectParser(detector);
        context.set(Parser.class, parser);        

        // set the input and output streams
        Metadata metadata = new Metadata();
        URL url = doc.toURI().toURL();
        InputStream input = TikaInputStream.get(url, metadata);        
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        // go!
        String detectedCharset = null;
        try {
            boolean prettyPrint = false; // don't add extra newlines
            String encoding = null; // autodetect
            TransformerHandler tHandler = getTransformerHandler(output, "html", encoding, prettyPrint);            
            org.xml.sax.ContentHandler handler = 
                new org.apache.tika.sax.ExpandedTitleContentHandler(tHandler);
            logger.info("Parsing url " + url);
            parser.parse(input, handler, metadata, context);
            detectedCharset = tHandler.getTransformer().getOutputProperty(OutputKeys.ENCODING);

        } catch (TransformerConfigurationException tce) {
            throw new IOException(tce);
        } catch (SAXException se) {
            throw new IOException(se);
        } catch (TikaException te) {
            throw new IOException(te);
        } finally {
            input.close();
            output.flush();
        }

        if (detectedCharset == null) {
            return output.toString(); // try our luck with the platforms default enc
        } else {
            return output.toString(detectedCharset);
        }
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
