package edu.mayo.omopindexer.indexing;

import edu.mayo.omopindexer.model.CDMModel;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Transforms a document that contains a certain set of models into a JSON indexable by ElasticSearch */
public class CDMToJSONSerializer {
    /** The section name of this document */
    private final String sectionName;
    /** The section ID of this document */
    private final String sectionID;
    /** The document header of this document **/
    private final String documentHeader;
    /** The ID of the Document */
    private String documentID;
    /** The raw text of the Document */
    private String documentText;
    /** A mapping by type of OMOP CDM models associated with this document */
    private Map<String, List<CDMModel>> models;
    /** The encounter ID associated with this document */
    private String encounterID;
    /** The patient ID associated with this document */
    private String personID;
    /** A queue of constructed JSON Objects for indexing */
    private LinkedList<JSONObject> docQueue;


    public CDMToJSONSerializer(String documentID, String documentText, String documentHeader, String sectionName, String sectionID, CDMModel... docModels) {
        this.documentID = documentID;
        this.documentText = documentText;
        this.documentHeader = documentHeader;
        this.sectionName = sectionName;
        this.sectionID = sectionID;
        this.models = new HashMap<>();
        this.docQueue = new LinkedList<>();
        for (CDMModel model : docModels) {
            if (!this.models.containsKey(model.getModelTypeName())) {
                this.models.put(model.getModelTypeName(), new LinkedList<>());
            }
            this.models.get(model.getModelTypeName()).add(model);
        }
        // Construct an encounter ID for this document
        // Pull obtainable information from headers TODO maybe want to put this somewhere else (e.g. a "StructuredDemographicInformationExtractor") as this only works on biobank CN
        // - Capture birthday
        Pattern birthdayPattern = Pattern.compile("birth_date:([0-9]{8})");
        Matcher birthdayMatcher = birthdayPattern.matcher(documentHeader);
        Date birthday = null;
        if (birthdayMatcher.find()) {
            try {
                SimpleDateFormat dF = new SimpleDateFormat("yyyyMMdd");
                dF.setTimeZone(TimeZone.getTimeZone("GMT"));
                birthday = dF.parse(birthdayMatcher.group(1));
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        // - Capture encounter date to calculate person age at time of encounter
        Pattern activityDateTimePattern = Pattern.compile("ACTIVITY_DTM:([^\\|]+)");
        Matcher activityDTMMatcher = activityDateTimePattern.matcher(documentHeader);
        /* The date of the encounter associated with this document */
        Date activityDTM = null;
        if (activityDTMMatcher.find()) {
            try {
                SimpleDateFormat dF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
                dF.setTimeZone(TimeZone.getTimeZone("GMT"));
                activityDTM = dF.parse(activityDTMMatcher.group(1));
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        // - Capture patient ID
        Pattern pIDPattern = Pattern.compile("PATIENT_ID:([^\\|]+)");
        Matcher pIDMatcher = pIDPattern.matcher(documentHeader);
        personID = null;
        if (pIDMatcher.find()) {
            personID = pIDMatcher.group(1);
        }
        encounterID = personID + ":" +
                (activityDTM == null ? "" : activityDTM.getTime() + "") + ":" +
                (birthday == null ? "" : birthday.getTime() + "");
    }

    /** @return A queue of JSON Objects, first element will always be the parent document and subsequent JSONs its
     * associated model instances */
    public Deque<JSONObject> toElasticSearchIndexableJSONs() {
        // Construct the parent document to index
        JSONObject document = new JSONObject();
        document.put("DocumentID", documentID);
        document.put("RawText", documentText);
        document.put("Header", documentHeader);
        document.put("Section_Name", sectionName);
        document.put("Section_ID", Integer.valueOf(sectionID));
        document.put("Encounter_ID", encounterID);
        document.put("Person_ID", personID);
        // Construct a list of children models to also index and associate
        for (Map.Entry<String, List<CDMModel>> e : models.entrySet()) {
            for (CDMModel m : e.getValue()) {
                JSONObject child = m.getAsJSON();
                child.put("type", m.getModelTypeName());
                docQueue.add(child);
            }
        }
        // Add the root document at the head of the que
        docQueue.addFirst(document);
        return docQueue;
    }
}
