package edu.mayo.omopindexer.model;

import org.json.JSONObject;

/** CDM Models can be serialized and deserialized to/from JSON format*/
public interface CDMModel {
    /** @return The name of this model */
    String getModelTypeName();
    /** @return the CDMModel in JSON format*/
    JSONObject getAsJSON();
    /** @return A mapping of fields for elasticsearch for this particular model */
    JSONObject getJSONMapping();
    /** An enumeration of model class file names (non-fully-qualified): <b>add new model files here or they will not be
     *  registered into the elasticsearch index! </b>
     */
    enum Types { // Dates are intentionally excluded due to special case handling
        CDMConditionOccurrence,
        CDMDrugExposure,
        CDMMeasurement,
        CDMPerson,
        CDMUnstructuredObservation
    }
}
