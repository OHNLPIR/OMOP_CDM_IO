package edu.mayo.omopindexer.model;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Map;

/**
 * Model for a Condition Occurrence in the OMOP CDM:
 * Conditions are records of a Person suggesting the presence of a disease or medical condition stated as a diagnosis,
 * a sign or a symptom, which is either observed by a Provider or reported by the patient.
 * Conditions are recorded in different sources and levels of standardization
 */
public class CDMConditionOccurrence implements CDMModel {
    /** A mapping of condition occurrences to their vocabulary types */
    private final Map<String, String> mention;

    /**
     *  An array of {@link edu.mayo.omopindexer.model.CDMDate} representations for this occurrence's date information
     *  with the appropriate {@link edu.mayo.omopindexer.model.CDMDate.CDMDate_Type} for each
     */
    private final CDMDate[] date;

    /** Included for reflection compatibility: do not use, do not remove */
    private CDMConditionOccurrence() {this(null, null);}

    public CDMConditionOccurrence(Map<String, String> occurrenceMentions, CDMDate... dates) {
        this.mention = occurrenceMentions;
        this.date = dates;
    }

    /**
     * @return An array of {@link CDMDate} associated with this occurrence
     */
    public CDMDate[] getDates() {
        return date;
    }

    /**
     * @return A mapping of condition occurrences to their vocabulary types
     */
    public Map<String, String> getOccurrence() {
        return mention;
    }


    public String getModelTypeName() {
        return "ConditionOccurrence";
    }

    public JSONObject getAsJSON() {
        JSONObject ret = new JSONObject();
        if (mention != null) {
            for (Map.Entry<String, String> e : mention.entrySet()) {
                ret.put("condition_occurrence_" + e.getKey(), e.getValue());
            }
        }
        JSONArray dateArray = new JSONArray();
        for (CDMDate date : getDates()) {
            dateArray.put(date.getAsJSON());
        }
        ret.put("date", dateArray);
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
