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
    /** A unique identifier for the measurement */
    private final Long measurementUID;
    /** The concept id of the operator applying to this measurement, e.g. ids for <, ≤, =, ≥, > */
    private final Long operator_concept_id;
    /** A numeric value for the measurement, expressed as a non floating-point (for precision) decimal*/
    private final BigDecimal value;

    public CDMMeasurement(Long measurementUID, Long operator_concept_id, BigDecimal value) {
        this.measurementUID = measurementUID;
        this.operator_concept_id = operator_concept_id;
        this.value = value;
    }

    /** @return The unique identifier of this measurement */
    public Long getMeasurementUID() {
        return measurementUID;
    }

    /** @return The concept ID for this measurement's operator */
    public Long getOperatorConceptId() {
        return operator_concept_id;
    }

    /** @return the numeric value of this measurement */
    public BigDecimal getValue() {
        return value;
    }

    public String getName() {
        return "Measurement";
    }

    public JSONObject getAsJSON() {
        JSONObject ret = new JSONObject();
        if (measurementUID != null) ret.put("measurementid", measurementUID);
        if (operator_concept_id != null) ret.put("operator_concept_id", operator_concept_id);
        if (value != null) ret.put("value", value.toString());
        return ret;
    }
}
