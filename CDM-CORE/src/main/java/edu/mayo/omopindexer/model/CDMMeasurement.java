package edu.mayo.omopindexer.model;

import org.json.JSONObject;

import java.math.BigDecimal;

/**
 * Model for a Measurement in the OMOP CDM Model
 * The MEASUREMENT table contains records of Measurement, i.e. structured values (numerical or categorical) obtained
 * through systematic and standardized examination or testing of a Person or Person's sample.
 * The MEASUREMENT table contains both orders and results of such Measurements as laboratory tests, vital signs,
 * quantitative findings from pathology reports, etc.
 */
public class CDMMeasurement implements CDMModel {
    /**
     * A unique identifier for the measurement
     */
    private final Long measurementUID;
    /**
     * The concept id of the operator applying to this measurement, e.g. ids for <, ≤, =, ≥, >
     */
    private final Long operator_concept_id;
    /**
     * A numeric value for the measurement, expressed as a non floating-point (for precision) decimal
     */
    private final Double value;
    /**
     * The string value that this measurement pertains to
     */
    private final String textRef;
    /**
     * NLP Positional Information
     */
    private final int begin;
    /**
     * NLP Positional Information
     */
    private final int end;

    /**
     * Included for reflection compatibility: do not use, do not remove
     */
    private CDMMeasurement() {
        this(0, 0, null, null, null, null);
    }

    public CDMMeasurement(int begin, int end, String textRef, Long measurementUID, Long operator_concept_id, Double value) {
        this.textRef = textRef;
        this.measurementUID = measurementUID;
        this.operator_concept_id = operator_concept_id;
        this.value = value;
        this.begin = begin;
        this.end = end;
    }

    /**
     * @return The unique identifier of this measurement
     */
    public Long getMeasurementUID() {
        return measurementUID;
    }

    /**
     * @return The concept ID for this measurement's operator
     */
    public Long getOperatorConceptId() {
        return operator_concept_id;
    }

    /**
     * @return the numeric value of this measurement
     */
    public Double getValue() {
        return value;
    }

    /**
     * @return The raw text of the measurement in question
     */
    public String getTextRef() {
        return textRef;
    }

    public String getModelTypeName() {
        return "Measurement";
    }

    public JSONObject getAsJSON() {
        JSONObject ret = new JSONObject();
        ret.put("measurement", textRef == null ? "" : textRef);
        ret.put("measurementid", measurementUID);
        ret.put("operator_concept_id", operator_concept_id);
        if (value != null) ret.put("value", value); else ret.put("value", (Double)null);
        ret.put("model_type", "Measurement");
        ret.put("begin", begin);
        ret.put("end", end);
        return ret;
    }

    @Override
    public JSONObject getJSONMapping() {
        JSONObject ret = new JSONObject();
        ret.put("measurement", constructTypeObject("string"));
        ret.put("measurementid", constructTypeObject("long"));
        ret.put("operator_concept_id", constructTypeObject("integer"));
        ret.put("value", constructTypeObject("float"));
        ret.put("begin", constructTypeObject("long"));
        ret.put("end", constructTypeObject("long"));
        return ret;
    }

    private JSONObject constructTypeObject(String type) {
        JSONObject ret = new JSONObject();
        ret.put("type", type);
        return ret;
    }
}
