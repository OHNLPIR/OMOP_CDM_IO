package edu.mayo.omopindexer.model;

import org.json.JSONObject;

/** CDM Models can be serialized and deserialized to/from JSON format*/
public interface CDMModel {
    /** @return The name of this model */
    String getName();
    /** @return the CDMModel in JSON format*/
    JSONObject getAsJSON();

    /** An enumeration of model class file names (non-fully-qualified): <b>add new model files here or they will not be
     *  registered into the elasticsearch index! </b>
     */
    enum Types {
        CDMConditionOccurrence,
        CDMDate,
        CDMDrugExposure,
        CDMMeasurement,
        CDMPerson,
        CDMUnstructuredObservation
    }
}
