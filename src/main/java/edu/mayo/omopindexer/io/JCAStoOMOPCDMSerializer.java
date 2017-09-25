package edu.mayo.omopindexer.io;

import edu.mayo.omopindexer.RegexpStatements;
import edu.mayo.omopindexer.indexing.ElasticSearchIndexer;
import edu.mayo.omopindexer.model.*;
import edu.mayo.omopindexer.types.BioBankCNHeader;
import edu.mayo.omopindexer.types.BioBankCNSectionHeader;
import jdk.nashorn.internal.runtime.regexp.joni.Regex;
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
        // Used for dosage identification TODO is supposed to be chunks, but chunks get removed by temporal annotator. Use sentence instead, evaluate performance impact later
        Map<MedicationMention, Collection<Sentence>> medicationToChunk =
                JCasUtil.indexCovering(jCas, MedicationMention.class, Sentence.class);
        Map<Sentence, Collection<MeasurementAnnotation>> chunkToMeasurement = JCasUtil.indexCovered(jCas, Sentence.class, MeasurementAnnotation.class);
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
            List<String> dateMentions = new LinkedList<>();
            for (Sentence s : diseaseToSentence.get(mention)) {
                for (TimeMention t : sentenceToTime.get(s)) {
                    String timeText = t.getCoveredText();
                    dateMentions.add(timeText);
                }
            }
            generatedModels.add(new CDMConditionOccurrence(mentionText, generateDateModels(dateMentions, CDMDate.CDMDate_Subject.CONDITION).toArray(new CDMDate[0])));
        }

        // - Sign and Symptom
        for (SignSymptomMention mention : JCasUtil.select(jCas, SignSymptomMention.class)) {
            // - Handle Text
            String mentionText = appendUmlsConcepts(mention.getCoveredText(), mention.getOntologyConceptArr());
            // - Handle date
            List<String> dateMentions = new LinkedList<>();
            for (Sentence s : signSymptomToSentence.get(mention)) {
                for (TimeMention t : sentenceToTime.get(s)) {
                    String timeText = t.getCoveredText();
                    dateMentions.add(timeText);
                }
            }
            generatedModels.add(new CDMConditionOccurrence(mentionText, generateDateModels(dateMentions, CDMDate.CDMDate_Subject.CONDITION).toArray(new CDMDate[0])));
        }


        // Drug Exposures
        // - Medication TODO: Ask Sijia about pulling from MedXN?
        for (MedicationMention mention : JCasUtil.select(jCas, MedicationMention.class)) {
            String mentionText = appendUmlsConcepts(mention.getCoveredText(), mention.getOntologyConceptArr());
            // - Try to find associated dosage information (measurement in same chunk)
            Set<MeasurementAnnotation> foundMeasurements = new HashSet<>();
            for (Sentence c : medicationToChunk.get(mention)) { // Guaranteed not to be null; mention will always be in a chunk
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
            List<String> dateMentions = new LinkedList<>();
            for (Sentence s : medicationToSentence.get(mention)) {
                for (TimeMention t : sentenceToTime.get(s)) {
                    String timeText = t.getCoveredText();
                    dateMentions.add(timeText);
                }
            }
            generatedModels.add(new CDMDrugExposure(mentionText, null, null, effectiveDrugDose, generateDateModels(dateMentions, CDMDate.CDMDate_Subject.DRUG).toArray(new CDMDate[0]))); // TODO
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
        // - Pull Metadata
        String headerText = header.getValue();
        BioBankCNSectionHeader section = JCasUtil.selectSingle(jCas, BioBankCNSectionHeader.class);
        String sectionName = section.getSectionName();
        String sectionID = section.getSectionID();
        // - Serialize
        DocumentSerializer serializer = new DocumentSerializer(id, text, headerText, sectionName, sectionID, generatedModels.toArray(new CDMModel[0]));
        ElasticSearchIndexer.indexSerialized(serializer);
        XmiCasSerializer out = new XmiCasSerializer(jCas.getTypeSystem());
        try {
            File f = new File("out");
            if (!f.exists()) {
                f.mkdirs();
            }
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
     *
     * @param mentionText   The text mention to expand
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
     * Generates date models from Stirng
     *
     * @param dateStrings A collection of Strings to parse
     * @param subject     The {@link edu.mayo.omopindexer.model.CDMDate.CDMDate_Subject} that these date strings pertain to
     * @return A collection of generated models
     */
    private Collection<CDMDate> generateDateModels(Collection<String> dateStrings, CDMDate.CDMDate_Subject subject) { // TODO implement non-frequencies
        LinkedList<CDMDate> ret = new LinkedList<>();
        for (String s : dateStrings) {
            Matcher m = RegexpStatements.FREQPERIOD.matcher(s);
            if (m.find()) {
                // see regex string documentation
                String freq1 = m.group(1);
                String freq2 = m.group(2);
                String freq3 = m.group(3); // -ly special case
                String freq4 = m.group(4); //  every (other) special case
                String everyOther = m.group(5); // - contains other
                String period1 = m.group(7);
                String rangeIndicator = m.group(8);
                String period2 = m.group(9);
                String periodUnit = m.group(10);
                String periodLy = m.group(11);
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
                // - Do nothing with frequencies for now (not represented in date information
                if (freq1 != null) {
                }
                if (freq2 != null) {
                }
                boolean periodModify = (freq4 != null && freq4.toLowerCase().contains("other")) || everyOther != null;
                if (period1 != null && period1.length() > 0 && periodUnit != null) {
                    period1 = normalizeNumber(s);
                    if (period1 == null) continue;
                    if (periodModify) period1 = 2 * java.lang.Integer.valueOf(period1) + "";
                    String input = transformUnitOfTime(periodUnit);
                    if (input == null) continue;
                    ret.add(new CDMDate(null, null, CDMDate.CDMDate_Type.PERIOD, subject,
                            input == null ? period1 + " " + periodUnit : "RP" + input.replace("%d", period1)));
                } else if (periodUnit != null) { // Empty period/period not specified but unit present, assume 1 or 2
                    String input = transformUnitOfTime(periodUnit);
                    if (input == null) continue;
                    ret.add(new CDMDate(null, null, CDMDate.CDMDate_Type.PERIOD, subject, input == null ?
                            (periodModify ? "2" : "1") + " " + periodUnit : "RP" + input.replace("%d", periodModify ? "2" : "1")));
                }
            }

        }
        return ret;
    }

    /**
     * Converts various accepted inputs into ISO8601 standard
     *
     * @param input The input untransformed string
     * @return A Unit of Time formatted string
     */
    public static String transformUnitOfTime(String input) {
        input = input.toLowerCase();
        if (input.endsWith("day") && input.length() > 3) { // Mon-Sun
            return "wk";
        }
        if (input.length() > 1) {
            switch (input.substring(0, 2)) {
                case "ho":
                    return "T%dH";
                case "da":
                    return "%dD";
                case "mo":
                    return "%dM";
                case "mi":
                    return "T%dM";
                case "ye":
                    return "%dY";
                case "we":
                    return "%dW";
                case "se":
                    return "T%ds";
            }
        } else {
            switch (input) {
                case "d":
                    return "%dD";
                case "m":
                    return "T%dM";
                case "y":
                    return "%dY";
                case "s":
                    return "T%dS";
                case "h":
                    return "T%dH";
                case "w":
                    return "%dW";
            }
        }
        return null;
    }

    /**
     * Converts a string text input (e.g. "one", "two") into a number string (e.g. "1", "2")
     *
     * @param input text form of number
     * @return numeric string version of input
     **/
    private String normalizeNumber(String input) {
        try {
            // Process numeric
            input = input.replaceAll(",", ""); // Remove grouping separators
            if (input.matches("[0-9]+")) { // Direct parse
                return Integer.valueOf(input) + "";
            }
            if (input.matches("[0-9]+(.[0-9]+)?")) {
                return Double.valueOf(input) + "";
            }
            // Process text
            int sum = 0;
            String[] split = input.toLowerCase().split("[ -]");
            // Arrive at final number via summation
            for (String s : split) {
                if (s.equalsIgnoreCase("and")) {
                    continue;
                }
                if (s.equalsIgnoreCase("twenty")) { // Annoying special case
                    sum += 20;
                    continue;
                }
                String prefix = s.substring(0, 3);

                int temp = 0;
                switch (prefix) {
                    case "zer":
                        temp += 0;
                        break;
                    case "one":
                    case "onc":
                        temp += 1;
                        break;
                    case "two":
                    case "twi":
                        temp += 2;
                        break;
                    case "thr":
                    case "thi":
                        temp += 3;
                        break;
                    case "fou":
                        temp += 4;
                        break;
                    case "fiv":
                    case "fif":
                        temp += 5;
                        break;
                    case "six":
                        temp += 6;
                        break;
                    case "sev":
                        temp += 7;
                        break;
                    case "eig":
                        temp += 8;
                        break;
                    case "nin":
                        temp += 9;
                        break;
                    case "ten":
                        temp += 10;
                        break;
                    case "ele":
                        temp += 11;
                        break;
                    case "twe":
                        temp += 12;
                        break;
                }

                if (s.endsWith("teen")) temp += 10;
                if (s.endsWith("ty")) temp *= 10;
                if (temp != -1) sum += temp;
                if (s.equals("hundred")) {
                    if (sum == 0) sum += 100;
                    else sum *= 100;
                }
                if (s.equals("thousand")) {
                    if (sum == 0) sum += 1000;
                    else sum *= 1000;
                }
                if (s.equals("million")) {
                    if (sum == 0) sum += 1000000;
                    else sum *= 1000000;
                }
            }
            return sum + "";
        } catch (IndexOutOfBoundsException e) { // Input wasn't a number/was an unsupported number
            return null; // Fail to null first
        }
    }

}
