import java.util.*;
import java.io.*;

/**
 * POJ for a document filed in a case (odločba, sklep o ustavitvi postopka, zapisnik, etc.)
 *
 */
public class Document {
    private File file;


    private Date documentDate;
    private String caseNumber;

    private String zavezanec, opis;
    private String nadzornik;
    private Date datumDodelitve;
    


    public Document(String prijatelj, String zavezanec, String opis, String nadzornik, Date datumDodelitve) {}

}
