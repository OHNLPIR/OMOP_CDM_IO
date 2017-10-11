package edu.mayo.omopindexer.vocabs;

import org.sqlite.SQLiteConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedList;

/**
 * This class supplies functionality for UMLS to Source Vocabulary Conversions using a dictionary lookup generated from
 * a UMLS installation. <br>
 * Results from conversion with {@link #getSourceCodesForVocab(UMLSSourceVocabulary, String)} can then be further
 * manipulated with the appropriate [VOCABNAME]Utils class (e.g. {@link SNOMEDCTUtils})<br>
 * <br>
 * This converter is not thread-safe, therefore a new instance should be created for each process using {@link #newConverter()}
 */
public class UMLSToSourceVocabularyConverter {

    private final PreparedStatement getSourceCodingPS;

    private UMLSToSourceVocabularyConverter() {
        String connURL = "jdbc:sqlite:" + System.getProperty("vocab.src.dir") + "UMLS/UMLS.sqlite";
        try {
            SQLiteConfig config = new SQLiteConfig();
            config.setReadOnly(true);
            System.out.print("Importing UMLS Vocabulary Mappings...");
            Connection ohdsiDBConn = DriverManager.getConnection(connURL, config.toProperties());
            System.out.println("Done");
            getSourceCodingPS = ohdsiDBConn.prepareStatement("SELECT CODE,STR FROM CONCEPT_MAPPINGS WHERE CUI=? AND SAB=? AND LAT=?");
        } catch (SQLException e) {
            throw new RuntimeException("Could not instantiate the UMLS to Source Vocabulary Converter", e);
        }
    }

    /**
     * Creates a new {@link UMLSToSourceVocabularyConverter} instance, multiple calls may be needed if objects are to be used
     * on different threads.
     * @return A new {@link UMLSToSourceVocabularyConverter} instance
     */
    public static UMLSToSourceVocabularyConverter newConverter() {
        return new UMLSToSourceVocabularyConverter();
    }

    /**
     * Retrieves equivalent codes within the various UMLS source vocabularies corresponding to a given UMLS concept
     *
     * @param vocab   The vocabulary to retrieve
     * @param umlsCUI The umls CUI to retrieve equivalent codes within the supplied source vocabulary for
     * @return A collection of source vocabulary codes corresponding to the UMLS CUI supplied, can be empty
     */
    public Collection<String> getSourceCodesForVocab(UMLSSourceVocabulary vocab, String umlsCUI) throws SQLException {
        Collection<String> ret = new LinkedList<>();
        getSourceCodingPS.setString(1, umlsCUI);
        getSourceCodingPS.setString(2, vocab.name());
        getSourceCodingPS.setString(3, "ENG");
        return ret;
    }

    public enum UMLSSourceVocabulary {
        /** SNOMED Clinical Terms - United States */
        SNOMEDCT_US
    }

}
