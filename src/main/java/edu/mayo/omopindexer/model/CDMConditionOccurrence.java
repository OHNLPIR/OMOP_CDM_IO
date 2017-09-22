package edu.mayo.omopindexer.model;

import org.json.JSONObject;

import java.util.Map;

/**
 * Model for a Condition Occurrence in the OMOP CDM:
 * Conditions are records of a Person suggesting the presence of a disease or medical condition stated as a diagnosis,
 * a sign or a symptom, which is either observed by a Provider or reported by the patient.
 * Conditions are recorded in different sources and levels of standardization
 */
public class CDMConditionOccurrence implements CDMModel {
    /** The actual textual mention of this condition or occurrence */
    private final String mention;

    /**
     *  A {@link edu.mayo.omopindexer.model.CDMDate} representation of this occurrence's date information with the
     *  appropriate {@link edu.mayo.omopindexer.model.CDMDate.CDMDate_Type} depending on available information
     */
    private final CDMDate date;

    /** Included for reflection compatibility: do not use, do not remove */
    private CDMConditionOccurrence() {this(null, null);}

    public CDMConditionOccurrence(String occurrenceMention, CDMDate date) {
        this.mention = occurrenceMention;
        this.date = date;
    }

    /**
     * @return The {@link #date} associated with this occurrence
     */
    public CDMDate getDate() {
        return date;
    }

    /**
     * @return The textual information associated with this occurrence
     */
    public String getOccurrenceText() {
        return mention;
    }


    public String getModelTypeName() {
        return "ConditionOccurrence";
    }

    public JSONObject getAsJSON() {
        JSONObject ret = new JSONObject();
        ret.put("condition_occurrence", mention);
        // TODO date
        return ret;
    }

    @Override
    public JSONObject getJSONMapping() {
        JSONObject ret = new JSONObject();
        ret.put("condition_occurrence", constructTypeObject("string"));
        ret.put("date", constructNestedDateTypeObject());
        return ret;
    }

    private JSONObject constructTypeObject(String type) {
        JSONObject ret = new JSONObject();
        ret.put("type", type);
        return ret;
    }

    private JSONObject constructNestedDateTypeObject() {
        JSONObject ret = new JSONObject();
        ret.put("type", "nested");
        JSONObject properties = new JSONObject();
        for (Map.Entry<String, Object> e : CDMDate.getJSONMappingStatic().toMap().entrySet()) {
            properties.put(e.getKey(), e.getValue());
        }
        ret.put("properties", properties);
        return ret;
    }
}
