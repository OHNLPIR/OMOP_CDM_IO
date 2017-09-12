package edu.mayo.omopindexer.io.serializer;

import edu.mayo.omopindexer.indexing.ElasticSearchIndexer;
import edu.mayo.omopindexer.model.CDMConditionOccurrence;
import edu.mayo.omopindexer.model.CDMDate;
import edu.mayo.omopindexer.model.CDMDrugExposure;
import edu.mayo.omopindexer.model.CDMModel;
import org.apache.ctakes.typesystem.type.refsem.*;
import org.apache.ctakes.typesystem.type.relation.TemporalRelation;
import org.apache.ctakes.typesystem.type.structured.DocumentID;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.util.Collection;
import java.util.LinkedList;

/**
 * Converts incoming JCAS structures with assumed cTAKES type system into the OMOP CDM data model,
 * then sends to ElasticSearch for indexing
 */
public class JCAStoOMOPCDMSerializer extends JCasAnnotator_ImplBase {
    @Override
    public void process(JCas jCas) throws AnalysisEngineProcessException {
        // Initialize storage for models
        Collection<CDMModel> generatedModels = new LinkedList<>();
        // Retrieve Metadata Information
        String text = jCas.getDocumentText();
        String id = JCasUtil.selectSingle(jCas, DocumentID.class).getDocumentID();
        // Condition Occurrences
        // - Disease and Disorder
        for (DiseaseDisorder mention : JCasUtil.select(jCas, DiseaseDisorder.class)) {
            String mentionText = mention.getSubject();
            TemporalRelation durRel = mention.getDuration();
            CDMDate date;
            if (durRel != null) {
                Time t1 = (Time) durRel.getArg1();
                Time t2 = (Time) durRel.getArg2();
                date = generateDateModel(t1, t2);
            } else {
                date = generateDateModel(mention.getStartTime(), mention.getEndTime());
            }
            generatedModels.add(new CDMConditionOccurrence(mentionText, date));
        }
        // - Sign and Symptom
        for (SignSymptom mention : JCasUtil.select(jCas, SignSymptom.class)) {
            String mentionText = mention.getSubject();
            TemporalRelation durRel = mention.getDuration();
            CDMDate date;
            if (durRel != null) {
                Time t1 = (Time) durRel.getArg1();
                Time t2 = (Time) durRel.getArg2();
                date = generateDateModel(t1, t2);
            } else {
                date = generateDateModel(mention.getStartTime(), mention.getEndTime());
            }
            generatedModels.add(new CDMConditionOccurrence(mentionText, date));
        }


        // Drug Exposures
        // - Medication TODO
        for (Medication mention : JCasUtil.select(jCas, Medication.class)) {
            MedicationStrength strength = mention.getMedicationStrength();
            MedicationDosage dosage = mention.getMedicationDosage();
            MedicationDuration duration = mention.getMedicationDuration();
            Date startDate = mention.getStartDate();
            Date endDate = mention.getEndDate();
            CDMDate date = generateDateModel(startDate, endDate, duration == null ? null : duration.getValue(),
                    CDMDate.CDMDate_Subject.DRUG);
            String dosageValue = dosage.getValue();
            Double quant = Double.valueOf(strength.getNumber());
            String quantUnit = strength.getUnit();
            generatedModels.add(new CDMDrugExposure(mention.getSubject(), date, quant, quantUnit, dosageValue));
        }

        // Send to ElasticSearch
        // - Serialize
        DocumentSerializer serializer = new DocumentSerializer(id, text, generatedModels.toArray(new CDMModel[0]));
        ElasticSearchIndexer.indexSerialized(serializer);
    }

    public static AnalysisEngineDescription createAnnotatorDescription() throws ResourceInitializationException {
        return AnalysisEngineFactory.createEngineDescription(JCAStoOMOPCDMSerializer.class);
    }

    /**
     * @param date A cTAKES Date Annotation
     * @return Converts cTAKES {@link org.apache.ctakes.typesystem.type.refsem.Date date} annotations to
     * programmatically usable java {@link java.util.Date dates}
     * @see org.apache.ctakes.typesystem.type.refsem.Date
     */
    private java.util.Date cTAKESDateToJavaDate(Date date) {
        String yearString = date.getYear();
        String monthString = date.getMonth();
        String dayString = date.getDay();
    }

    private CDMDate generateDateModel(Date date1, Date date2, String durationString, CDMDate.CDMDate_Subject subject) {
        return new CDMDate(cTAKESDateToJavaDate(date1), cTAKESDateToJavaDate(date2), CDMDate.CDMDate_Type.COMPOSITE, subject, durationString);
    }


}
