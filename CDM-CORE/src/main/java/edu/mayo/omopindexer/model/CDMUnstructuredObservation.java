package edu.mayo.omopindexer.model;

import org.json.JSONObject;

/**
 * Model for an unstructured observation in the OMOP CDM Model
 * Any data that cannot be represented by any other domains, such as social and lifestyle facts, medical history,
 * family history, etc. is recorded here.
 */
public class CDMUnstructuredObservation implements CDMModel {
    /** The textual contents of this unstructured observation */
    private final String mention;
    /**
     * NLP Positional Information
     */
    private final int begin;
    /**
     * NLP Positional Information
     */
    private final int end;

    /** Included for reflection compatibility: do not use, do not remove */
    private CDMUnstructuredObservation() {this(0, 0, null);}

    public CDMUnstructuredObservation(int begin, int end, String mention) {
        this.begin = begin;
        this.end = end;
        this.mention = mention;
    }

    /** @return The textual contents of this unstructured observation */
    public String getMention() {
        return mention;
    }

    public String getModelTypeName() {
        return "UnstructuredObservation";
    }

    public JSONObject getAsJSON() {
        JSONObject ret = new JSONObject();
        ret.put("observation", mention);
        ret.put("model_type", "Unstructured Observation");
        return ret;
    }

    @Override
    public JSONObject getJSONMapping() {
        JSONObject ret = new JSONObject();
        ret.put("observation", constructTypeObject("string"));
        return ret;
    }

    private JSONObject constructTypeObject(String type) {
        JSONObject ret = new JSONObject();
        ret.put("type", type);
        return ret;
    }
}
