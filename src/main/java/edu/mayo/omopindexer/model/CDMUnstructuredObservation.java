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

    /** Included for reflection compatibility: do not use, do not remove */
    private CDMUnstructuredObservation() {this(null);}

    public CDMUnstructuredObservation(String mention) {
        this.mention = mention;
    }

    /** @return The textual contents of this unstructured observation */
    public String getMention() {
        return mention;
    }

    public String getName() {
        return "UnstructuredObservation";
    }

    public JSONObject getAsJSON() {
        JSONObject ret = new JSONObject();
        if (mention != null) ret.put("observation", mention);
        return ret;
    }
}
