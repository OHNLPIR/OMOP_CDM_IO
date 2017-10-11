package edu.mayo.omopindexer.casengines;

import edu.mayo.omopindexer.indexing.PersonStaging;
import edu.mayo.omopindexer.model.CDMPerson;
import edu.mayo.omopindexer.model.CDMPerson.CDMPerson_ETHNICITY;
import edu.mayo.omopindexer.types.BioBankCNHeader;
import edu.mayo.omopindexer.vocabs.SNOMEDCTUtils;
import edu.mayo.omopindexer.vocabs.UMLSToSourceVocabularyConverter;
import org.apache.ctakes.dictionary.lookup2.ae.DefaultJCasTermAnnotator;
import org.apache.ctakes.typesystem.type.refsem.UmlsConcept;
import org.apache.ctakes.typesystem.type.textsem.AnatomicalSiteMention;
import org.apache.ctakes.typesystem.type.textsem.EntityMention;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EthnicityAndRaceExtractor extends JCasAnnotator_ImplBase {

    private Map<String, CDMPerson_ETHNICITY> cuiToEthnicityMap;
    private UMLSToSourceVocabularyConverter converterInstance;

    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
        super.initialize(context);
        cuiToEthnicityMap = new HashMap<>();
        // Initialize Values - instance values are presumed threadsafe (one pipeline instance per thread)
        converterInstance = UMLSToSourceVocabularyConverter.newConverter();
    }

    public static AnalysisEngineDescription buildPipeline() throws ResourceInitializationException {
        AggregateBuilder ret = new AggregateBuilder();
        ret.add(AnalysisEngineFactory.createEngineDescription(DefaultJCasTermAnnotator.class, "DictionaryDescriptor", "dictionaries/ethnicitiesandraces.xml"));
        ret.add(AnalysisEngineFactory.createEngineDescription(EthnicityAndRaceExtractor.class));
        return ret.createAggregateDescription();
    }

    @Override
    public void process(JCas jCas) throws AnalysisEngineProcessException {
        String documentHeader = JCasUtil.selectSingle(jCas, BioBankCNHeader.class).getValue();
        // - Capture patient ID
        Pattern pIDPattern = Pattern.compile("PATIENT_ID:([^\\|]+)");
        Matcher pIDMatcher = pIDPattern.matcher(documentHeader);
        String personID = null;
        if (pIDMatcher.find()) {
            personID = pIDMatcher.group(1);
        }
        if (personID == null) {
            return; // No associated person to associate this to
        }
        CDMPerson person = PersonStaging.get(personID);
        if (person == null) {
            return; // No associated person to associate this to
        }
        for (EntityMention e : JCasUtil.select(jCas, EntityMention.class)) {
            if (e instanceof AnatomicalSiteMention) {
                continue; // From cTAKES NER
            }
            if (e.getOntologyConceptArr() != null) {
                for (FeatureStructure fs : e.getOntologyConceptArr().toArray()) {
                    if (fs instanceof UmlsConcept) {
                        UmlsConcept umlsConcept = (UmlsConcept)fs;
                        try {
                            for (String snomedCode : converterInstance.getSourceCodesForVocab(
                                    UMLSToSourceVocabularyConverter.UMLSSourceVocabulary.SNOMEDCT_US, umlsConcept.getCui())) {
                                CDMPerson_ETHNICITY ethnicity = CDMPerson_ETHNICITY.fromSNOMEDCTCode(snomedCode);
                                if (ethnicity != null) {

                                }
                            }
                        } catch (SQLException e1) {
                            e1.printStackTrace();
                        }
                    }
                }
            }

            // Remove unrecognized entity so that we can reduce future work (only EntityMentions in index at the moment should be from ethnicityandraces extraction
            e.removeFromIndexes();
        }
    }
}
