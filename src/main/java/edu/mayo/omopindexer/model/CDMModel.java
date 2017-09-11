package edu.mayo.omopindexer.model;

import org.json.JSONObject;

/** CDM Models can be serialized and deserialized to/from JSON format*/
public interface CDMModel {
    /** @return The name of this model */
    String getName();
    /** @return the CDMModel in JSON format*/
    JSONObject getAsJSON();
}
