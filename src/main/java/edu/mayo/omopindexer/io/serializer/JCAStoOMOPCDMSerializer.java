package edu.mayo.omopindexer.io.serializer;

import edu.mayo.omopindexer.indexing.ElasticSearchIndexer;
import edu.mayo.omopindexer.model.*;
import org.apache.ctakes.typesystem.type.refsem.Date;
import org.apache.ctakes.typesystem.type.refsem.UmlsConcept;
import org.apache.ctakes.typesystem.type.structured.DocumentID;
import org.apache.ctakes.typesystem.type.syntax.Chunk;
import org.apache.ctakes.typesystem.type.textsem.DiseaseDisorderMention;
import org.apache.ctakes.typesystem.type.textsem.MeasurementAnnotation;
import org.apache.ctakes.typesystem.type.textsem.MedicationMention;
import org.apache.ctakes.typesystem.type.textsem.SignSymptomMention;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.resource.ResourceInitializationException;

import java.text.DecimalFormat;
import java.util.*;

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
        // Create indexes of covered/covering classes for performance reasons when we need to associate multiple mentions
        // with one another
        Map<MedicationMention, Collection<Chunk>> medicationToChunk =
                JCasUtil.indexCovering(jCas, MedicationMention.class, Chunk.class);
        Map<Chunk, Collection<MeasurementAnnotation>> chunkToMeasurement = JCasUtil.indexCovered(jCas, Chunk.class, MeasurementAnnotation.class);
        // Condition Occurrences
        // - Disease and Disorder
        for (DiseaseDisorderMention mention : JCasUtil.select(jCas, DiseaseDisorderMention.class)) {
            String mentionText = appendUmlsConcepts(mention.getCoveredText(), mention.getOntologyConceptArr());
            CDMDate date = null; // TODO: Handle Date
            generatedModels.add(new CDMConditionOccurrence(mentionText, date));
        }

        // - Sign and Symptom
        for (SignSymptomMention mention : JCasUtil.select(jCas, SignSymptomMention.class)) {
            String mentionText = appendUmlsConcepts(mention.getCoveredText(), mention.getOntologyConceptArr());
            CDMDate date = null; // TODO: Handle Date
            generatedModels.add(new CDMConditionOccurrence(mentionText, date));
        }


        // Drug Exposures
        // - Medication TODO: Ask Sijia about pulling from MedXN?
        for (MedicationMention mention : JCasUtil.select(jCas, MedicationMention.class)) {
            String mentionText = appendUmlsConcepts(mention.getCoveredText(), mention.getOntologyConceptArr());
            // Try to find associated dosage information (measurement in same chunk)
            Set<MeasurementAnnotation> foundMeasurements = new HashSet<>();
            for (Chunk c : medicationToChunk.get(mention)) { // Guaranteed not to be null; mention will always be in a chunk
                foundMeasurements.addAll(chunkToMeasurement.getOrDefault(c, new LinkedList<>()));
            }
            String effectiveDrugDose = null;
            // TODO: reevaluate implementation of below
            if (foundMeasurements.size() > 0) {
                MeasurementAnnotation currMax = null;
                for (MeasurementAnnotation m : foundMeasurements) { // Find largest covering annotation
                    if (currMax == null || (m.getBegin() < currMax.getBegin() && m.getEnd() > currMax.getEnd()
                            && (m.getEnd() - m.getBegin()) >= (currMax.getEnd() - currMax.getBegin()))) {
                        currMax = m;
                    }
                }
                effectiveDrugDose = currMax.getCoveredText(); //TODO is this quantity or effective?
            }
            generatedModels.add(new CDMDrugExposure(mentionText, null, null, null, effectiveDrugDose)); // TODO
        }

        // Measurement
        for (MeasurementAnnotation mention : JCasUtil.select(jCas, MeasurementAnnotation.class)) {
            String mentionText = mention.getCoveredText();
            // Do some basic preprocessing
            // - Detects when unit and numerics are combined into the same word
            mentionText = mentionText.replaceAll("([0-9.,\\-]+)([a-zA-Z])", "$1 $2");
            // - Remove thousands grouping. The character being removed depends on whoever is running the program (i.e. in france would be ., in US would be ,
            mentionText = mentionText.replace(((DecimalFormat) DecimalFormat.getInstance(Locale.getDefault())).getDecimalFormatSymbols().getGroupingSeparator() + "", "");
            // Split ranges into multiple measurement annotations
            String[] parse = mentionText.split(" ");// TODO something more...thorough
            String value = parse[0];
            String unit = parse.length > 1 ? " " + parse[1] : "";
            String[] rangeCheck = value.split("([-/])"); // TODO preprocess six/seven/etc word form into numeric
            if (rangeCheck.length > 1) {
                for (String rangeItem : rangeCheck) {
                    Double dValue;
                    if (!rangeItem.matches("[.0-9]+")) {
                        dValue = null;
                    } else {
                        dValue = Double.parseDouble(rangeItem);
                    }
                    generatedModels.add(new CDMMeasurement(rangeItem + unit, null, null, dValue));
                }
            } else {
                Double dValue;
                if (!value.matches("[.0-9]+")) {
                    dValue = null;
                } else {
                    dValue = Double.parseDouble(value);
                }
                generatedModels.add(new CDMMeasurement(mentionText, null, null, dValue));
            }
        }
        // Person TODO MAPPINGS

        // Send to ElasticSearch
        // - Serialize
        DocumentSerializer serializer = new DocumentSerializer(id, text, generatedModels.toArray(new CDMModel[0]));
        ElasticSearchIndexer.indexSerialized(serializer);
    }

    /**
     * Appends UMLS concepts to the mention string to enable elasticsearch lookups: format string {cui} {tui}
     * {vocab}{code} {vocab2}{code2}
     * @param mentionText The text mention to expand
     * @param ontologyArray The ontology array containing UMLS concepts
     * @return An expanded string containing umls concepts
     */
    private String appendUmlsConcepts(String mentionText, FSArray ontologyArray) {
        String ret = mentionText;
        if (ontologyArray != null && ontologyArray.size() > 0) {
            StringBuilder sB = new StringBuilder(mentionText);
            boolean flag = false;
            for (FeatureStructure fs : ontologyArray.toArray()) {
                if (fs instanceof UmlsConcept) {
                    UmlsConcept concept = (UmlsConcept) fs;
                    if (!flag) {
                        sB.append(" ").append(concept.getCui()).append(" ").append(concept.getTui());
                        flag = true;
                    }
                    sB.append(" ").append(concept.getPreferredText()).append(" ").append(concept.getCodingScheme()).append("").append(concept.getCode());
                }
            }
            if (flag) {
                ret = sB.toString();
            }
        }
        return ret;
    }

    public static AnalysisEngineDescription createAnnotatorDescription() throws ResourceInitializationException {
        return AnalysisEngineFactory.createEngineDescription(JCAStoOMOPCDMSerializer.class);
    }

    /**
     * @param date A cTAKES Date Annotation to convert
     * @return Converts cTAKES {@link org.apache.ctakes.typesystem.type.refsem.Date date} annotations to
     * programmatically usable java {@link java.util.Date dates}
     * @see org.apache.ctakes.typesystem.type.refsem.Date
     */
    private java.util.Date cTAKESDateToJavaDate(Date date) {
        String yearString = date.getYear().replaceAll("[^0-9a-zA-Z]", "");
        String monthString = date.getMonth().replaceAll("[^0-9a-zA-Z]", "");
        String dayString = date.getDay().replaceAll("[^0-9a-zA-Z]", "");
        int year = Integer.valueOf(yearString); // TODO: validation
        int month;
        if (monthString.matches("[0-9]+")) {
            month = Integer.valueOf(monthString);
        } else {
            month = 1; //TODO
        }
        int day = Integer.valueOf(dayString);
        Calendar c = Calendar.getInstance();
        c.setTimeZone(TimeZone.getTimeZone("UTC"));
        c.set(year, month - 1, day); // Use numeric values for now
        return c.getTime();
    }

    private CDMDate generateDateModel(Date date1, Date date2, Object duration, CDMDate.CDMDate_Subject subject) {
        return new CDMDate(cTAKESDateToJavaDate(date1), cTAKESDateToJavaDate(date2), CDMDate.CDMDate_Type.COMPOSITE, subject, duration);
    }


}
