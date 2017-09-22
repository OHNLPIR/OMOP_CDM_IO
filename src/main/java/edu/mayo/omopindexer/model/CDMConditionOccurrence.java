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
        if (mention != null) ret.put("condition_occurrence", mention);
        if (date != null) {
            for (Map.Entry<String, Object> e : date.getAsJSON().toMap().entrySet()) {
                ret.put(e.getKey(), e.getValue());
            }
        }
        return ret;
    }

    @Override
    public JSONObject getJSONMapping() {
        JSONObject ret = new JSONObject();
        JSONObject conditionOccurrenceType = new JSONObject();
        conditionOccurrenceType.put("type", "string");
        ret.put("condition_occurrence", conditionOccurrenceType);
        return ret;
    }
}
