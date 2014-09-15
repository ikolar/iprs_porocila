import java.util.*;
import java.text.*;

/**
 * POJ for an inspection/misdemeanor case
 *
 */
public class Case {
    private static final SimpleDateFormat DF = new SimpleDateFormat("yyyy-MM-dd");

    private String[] orignals;
    private String prijavitelj;
    private String zavezanec, opis;
    private String nadzornik;
    private Date datumDodelitve;
    private boolean warnings = false;
    
    // extras
    private String caseNumber = null;
    private List documents = new ArrayList();

    // todo make this configurable
    private static Map<String, String> nadzornikiTypos = new HashMap<String, String>();
    static {
        nadzornikiTypos.put("Monika Benkovč", "Monika Benkovič");
        nadzornikiTypos.put("Monika Benkovč", "Monika Benkovič");
        nadzornikiTypos.put("Klenem Mišič", "Klemen Mišič");
        nadzornikiTypos.put("Marko Logar", "Jure Logar");
        nadzornikiTypos.put("Marijan Činč", "Marijan Čonč");
        nadzornikiTypos.put("Tana Slak", "Tanja Slak");
        nadzornikiTypos.put("Blaš Pavšič", "Blaž Pavšič");
    }

    
    /**
     *
     */
    public Case(String prijavitelj, String zavezanec, String opis, String nadzornik, Date datumDodelitve) {
        this.prijavitelj = prijavitelj;
        this.zavezanec = zavezanec;
        this.opis = opis;
        this.nadzornik = nadzornik;
        this.datumDodelitve = datumDodelitve;

        fixNadzornik();
    }

    protected void fixNadzornik() {
        if (nadzornik.contains("rešuje"))
            nadzornik = nadzornik.substring(0, nadzornik.indexOf("rešuje")).trim();
    
        if (nadzornikiTypos.containsKey(nadzornik))
            nadzornik = nadzornikiTypos.get(nadzornik);
    }

    public String getZavezanec() {
        return this.zavezanec;
    }

    public String getOpis() {
        return this.opis;
    }

    public String getPrijavitelj() {
        return this.prijavitelj;
    }

    public String getNadzornik() {
        return this.nadzornik;    
    }

    public Date getDatumDodelitve() {
        return this.datumDodelitve;
    }

    public String toString() {
        return String.format("[Case #%s] dodeljen=%s zavezanec='%s' nadzornik='%s' opis='%s'", 
            (caseNumber == null ? "?" : caseNumber),
            DF.format(datumDodelitve),
            zavezanec,
            nadzornik,
            opis
        );
    }
}
