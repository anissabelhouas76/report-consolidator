package dz.ab.reportconsolidator.config;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.springframework.context.annotation.Configuration;
@Configuration
public class WordSecurityConfiguration {

    public WordSecurityConfiguration() {

        // Une ressource interne du DOCX :
        // maximum ~20 Mo
        ZipSecureFile.setMaxEntrySize(20L * 1024 * 1024);

        // Quantité de texte/XML :
        // maximum ~10 Mo
        ZipSecureFile.setMaxTextSize(10L * 1024 * 1024);

        // Maximum 1000 éléments dans le DOCX
        ZipSecureFile.setMaxFileCount(1000);
    }
}