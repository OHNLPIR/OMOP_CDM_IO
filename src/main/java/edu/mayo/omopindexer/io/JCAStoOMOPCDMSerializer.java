package edu.mayo.omopindexer.io;

import edu.mayo.omopindexer.RegexpStatements;
import edu.mayo.omopindexer.indexing.ElasticSearchIndexer;
import edu.mayo.omopindexer.model.*;
import edu.mayo.omopindexer.types.BioBankCNHeader;
import org.apache.ctakes.typesystem.type.refsem.Date;
import org.apache.ctakes.typesystem.type.refsem.UmlsConcept;
import org.apache.ctakes.typesystem.type.structured.DocumentID;
import org.apache.ctakes.typesystem.type.syntax.Chunk;
import org.apache.ctakes.typesystem.type.textsem.*;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.impl.XmiCasSerializer;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.XMLSerializer;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        // Used for dosage identification
        Map<MedicationMention, Collection<Chunk>> medicationToChunk =
                JCasUtil.indexCovering(jCas, MedicationMention.class, Chunk.class);
        Map<Chunk, Collection<MeasurementAnnotation>> chunkToMeasurement = JCasUtil.indexCovered(jCas, Chunk.class, MeasurementAnnotation.class);
        // TODO will probably have to split as one of the intermediate annotators removes chunks breaking measurement detection
        // Used for temporal identification
        Map<DiseaseDisorderMention, Collection<Sentence>> diseaseToSentence = JCasUtil.indexCovering(jCas, DiseaseDisorderMention.class, Sentence.class);
        Map<SignSymptomMention, Collection<Sentence>> signSymptomToSentence = JCasUtil.indexCovering(jCas, SignSymptomMention.class, Sentence.class);
        Map<MedicationMention, Collection<Sentence>> medicationToSentence = JCasUtil.indexCovering(jCas, MedicationMention.class, Sentence.class);
        Map<Sentence, Collection<TimeMention>> sentenceToTime = JCasUtil.indexCovered(jCas, Sentence.class, TimeMention.class);



        // Condition Occurrences
        // - Disease and Disorder
        for (DiseaseDisorderMention mention : JCasUtil.select(jCas, DiseaseDisorderMention.class)) {
            // - Handle Text
            String mentionText = appendUmlsConcepts(mention.getCoveredText(), mention.getOntologyConceptArr());
            // - Handle date
            List<CDMDate> dates = new LinkedList<>();
            CDMDate date = null;
            for (Sentence s : diseaseToSentence.get(mention)) {
                for (TimeMention t : sentenceToTime.get(s)) {
                    String timeText = t.getCoveredText();
                    dates.addAll(generateDateModels(timeText, CDMDate.CDMDate_Subject.CONDITION));
                }
            }
            if (dates.size() > 1) {
                date = condenseDateModels(dates);
            } else if (dates.size() == 1) {
                date = dates.get(0);
            }
            generatedModels.add(new CDMConditionOccurrence(mentionText, date));
        }

        // - Sign and Symptom
        for (SignSymptomMention mention : JCasUtil.select(jCas, SignSymptomMention.class)) {
            // - Handle Text
            String mentionText = appendUmlsConcepts(mention.getCoveredText(), mention.getOntologyConceptArr());
            // - Handle date
            List<CDMDate> dates = new LinkedList<>();
            CDMDate date = null;
            for (Sentence s : signSymptomToSentence.get(mention)) {
                for (TimeMention t : sentenceToTime.get(s)) {
                    String timeText = t.getCoveredText();
                    dates.addAll(generateDateModels(timeText, CDMDate.CDMDate_Subject.CONDITION));
                }
            }
            if (dates.size() > 1) {
                date = condenseDateModels(dates);
            } else if (dates.size() == 1) {
                date = dates.get(0);
            }
            generatedModels.add(new CDMConditionOccurrence(mentionText, date));
        }


        // Drug Exposures
        // - Medication TODO: Ask Sijia about pulling from MedXN?
        for (MedicationMention mention : JCasUtil.select(jCas, MedicationMention.class)) {
            String mentionText = appendUmlsConcepts(mention.getCoveredText(), mention.getOntologyConceptArr());
            // - Try to find associated dosage information (measurement in same chunk)
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
            // - Handle Date
            List<CDMDate> dates = new LinkedList<>();
            CDMDate date = null;
            for (Sentence s : medicationToSentence.get(mention)) {
                for (TimeMention t : sentenceToTime.get(s)) {
                    String timeText = t.getCoveredText();
                    dates.addAll(generateDateModels(timeText, CDMDate.CDMDate_Subject.DRUG));
                }
            }
            if (dates.size() > 1) {
                date = condenseDateModels(dates);
            } else if (dates.size() == 1) {
                date = dates.get(0);
            }
            generatedModels.add(new CDMDrugExposure(mentionText, date, null, null, effectiveDrugDose)); // TODO
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
        // Pull obtainable information from headers
        // - Capture birthday
        BioBankCNHeader header = JCasUtil.selectSingle(jCas, BioBankCNHeader.class);
        Pattern birthdayPattern = Pattern.compile("birth_date:([0-9]{8})");
        Matcher birthdayMatcher = birthdayPattern.matcher(header.getValue());
        java.util.Date birthday = null;
        if (birthdayMatcher.find()) {
            try {
                SimpleDateFormat dF = new SimpleDateFormat("yyyyMMdd");
                dF.setTimeZone(TimeZone.getTimeZone("GMT"));
                birthday = dF.parse(birthdayMatcher.group(1));
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        // - Capture patient ID
        Pattern pIDPattern = Pattern.compile("PATIENT_ID:([^\\|]+)");
        Matcher pIDMatcher = pIDPattern.matcher(header.getValue());
        String patientID = null;
        if (pIDMatcher.find()) {
            patientID = pIDMatcher.group(1);
        }
        // TODO actually use this information somehow (i.e. by pulling from structured data
        // Send to ElasticSearch
        // - Serialize
        DocumentSerializer serializer = new DocumentSerializer(id, text, generatedModels.toArray(new CDMModel[0]));
        ElasticSearchIndexer.indexSerialized(serializer);
        XmiCasSerializer out = new XmiCasSerializer(jCas.getTypeSystem());
        try {
            XMLSerializer xml = new XMLSerializer(new FileOutputStream(new File("out", id + ".xmi")), false);
            out.serialize(jCas.getCas(), xml.getContentHandler());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }
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

    private Collection<CDMDate> generateDateModels(String dateString, CDMDate.CDMDate_Subject subject) {
        // Try period matching first
        Collection<CDMDate> ret = new LinkedList<>();
        Pattern freqMatcher = Pattern.compile(RegexpStatements.FREQPERIOD, Pattern.CASE_INSENSITIVE);
        Matcher m = freqMatcher.matcher(dateString);
        if (m.find()) {
            // see regex string documentation
            String freq1 = m.group(1);
            String freq2 = m.group(2);
            String freq3 = m.group(3); // -ly special case
            String freq4 = m.group(4);
            String period1 = m.group(6);
            String rangeIndicator = m.group(7);
            String period2 = m.group(8);
            String periodUnit = m.group(9);
            String periodLy = m.group(10);
            // Some cleanup
            if (freq1 == null && freq4 != null) { // -ly special case
                freq1 = "1";
            }
            if (freq1 == null && freq3 != null) { // -ly special case
                freq1 = freq3;
            }
            if (rangeIndicator == null) {
                period1 = (period1 == null ? "" : period1) + (period2 == null ? "" : period2);
            }
            if (periodLy != null && period1.length() == 0) {
                period1 = "1"; // Has a ly
                periodUnit = periodLy;
            }
            if (period1 != null) {
            }
        }
        return ret;
    }

    private CDMDate generateDateModel(Date date1, Date date2, Object duration, CDMDate.CDMDate_Subject subject) {
        return null; //TODO
    }

    private CDMDate condenseDateModels(List<CDMDate> dates) {
        return null;
    }



}
