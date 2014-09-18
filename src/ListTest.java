import java.io.*;
import java.util.*;
import org.apache.commons.io.*;
import org.apache.commons.io.filefilter.*;

public class ListTest {
    public static void main(String[] args) {
        Collection<File> docs = FileUtils.listFiles(
            new File("test"),
            new IOFileFilter() {
                public boolean accept(File file) {
                    return accept(file.getParentFile(), file.getName());
                }
                
                public boolean accept(File dir, String name) {
                    String ext = FilenameUtils.getExtension(name);
                    if (! ("doc".equalsIgnoreCase(ext) || "docx".equalsIgnoreCase(ext)) )
                        return false;
                    if (FilenameUtils.getBaseName(name).toLowerCase().endsWith(".merged"))
                        return false;

                    return true;
                }
            },
            TrueFileFilter.INSTANCE
        );

        for (File f : docs)
            System.out.println(f);

    }
}
