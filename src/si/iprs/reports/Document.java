package si.iprs.reports;

import java.util.*;
import java.io.*;

/**
 * POJ for a document filed in a case (odločba, sklep o ustavitvi postopka, zapisnik, etc.)
 *
 */
public class Document {
    private File src;
    private File merged, html, mergedHtml;

    private boolean nascent = true;

    private Date documentDate;
    private String caseNumber;
    private String zavezanec, opis;
    private String nadzornik;
    private Date datumDodelitve;
    
    public Document(File src) {
        this.src = src;
    }
    
    public File getSrc() {
        return src;
    }    
    
    public File getMerged() {
        return merged;
    }

    public void setMerged(File merged) {
        this.merged = merged;
    }
    
    public File getHtml() {
        return html;
    }
    
    public void setHtml(File html) {
        this.html = html;
    }
    
    public File getMergedHtml() {
        return mergedHtml;
    }
    
    public void setMergedHtml(File mergedHtml) {
        this.mergedHtml = mergedHtml;
    }        






    public Document(String prijatelj, String zavezanec, String opis, String nadzornik, Date datumDodelitve) {}

}
