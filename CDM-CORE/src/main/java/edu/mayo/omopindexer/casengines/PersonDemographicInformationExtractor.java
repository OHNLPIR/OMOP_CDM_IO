package edu.mayo.omopindexer.casengines;

import edu.mayo.bsi.nlp.vts.UMLS;
import edu.mayo.omopindexer.indexing.PersonStaging;
import edu.mayo.omopindexer.model.CDMPerson;
import edu.mayo.omopindexer.model.CDMPerson.CDMPerson_RACE;
import edu.mayo.omopindexer.types.ClinicalDocumentMetadata;
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

public class PersonDemographicInformationExtractor extends JCasAnnotator_ImplBase {

    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
        super.initialize(context);
        // Initialize Values - instance values are presumed threadsafe (one pipeline instance per thread)
    }

    public static AnalysisEngineDescription buildPipeline() throws ResourceInitializationException {
        AggregateBuilder ret = new AggregateBuilder();
        ret.add(AnalysisEngineFactory.createEngineDescription(DefaultJCasTermAnnotator.class, "DictionaryDescriptor", "/dictionaries/ethnicitiesandraces.xml"));
        ret.add(AnalysisEngineFactory.createEngineDescription(PersonDemographicInformationExtractor.class));
        return ret.createAggregateDescription();
    }

    @Override
    public void process(JCas jCas) throws AnalysisEngineProcessException {
        String personID = JCasUtil.selectSingle(jCas, ClinicalDocumentMetadata.class).getPatientId();
        if (personID == null) {
            return; // No associated person to associate this to
        }
        CDMPerson person = PersonStaging.get(personID);
        if (person == null) {
            return; // No associated person to associate this to
        }
        // Date of Birth
        String patientDOB = JCasUtil.selectSingle(jCas, ClinicalDocumentMetadata.class).getPatientDOB();
        try {
            long dobSinceEpoch = Long.valueOf(patientDOB);
            person.setDateOfBirth(dobSinceEpoch);
        } catch (NumberFormatException ignored) {} // DOB in invalid format, cannot be stored in person
        // Ethnicity Election
        for (EntityMention e : JCasUtil.select(jCas, EntityMention.class)) {
            if (e instanceof AnatomicalSiteMention) {
                continue; // From cTAKES NER
            }
            if (e.getOntologyConceptArr() != null) {
                for (FeatureStructure fs : e.getOntologyConceptArr().toArray()) {
                    if (fs instanceof UmlsConcept) {
                        UmlsConcept umlsConcept = (UmlsConcept)fs;
                        try {
                            for (String snomedCode : UMLS.getSourceCodesForVocab(
                                    UMLS.UMLSSourceVocabulary.SNOMEDCT_US, umlsConcept.getCui())) {
                                CDMPerson_RACE ethnicity = CDMPerson_RACE.fromSNOMEDCTCode(snomedCode);
                                if (ethnicity != null) {
                                    person.electEthnicity(ethnicity);
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
