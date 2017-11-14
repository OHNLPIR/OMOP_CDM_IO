package edu.mayo.omopindexer.model;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Map;

/**
 * Model for a Date in the OMOP CDM Model
 * Drugs include prescription and over-the-counter medicines, vaccines, and large-molecule biologic therapies.
 */
public class CDMDrugExposure implements CDMModel {
    /**
     * A mapping of drug exposures to their vocabulary type
     */
    private final Map<String, String> mention;
    /**
     * An array of {@link edu.mayo.omopindexer.model.CDMDate} representing this drug's relevant administration dates
     */
    private final CDMDate[] date;
    /**
     * A quantity associated with this drug exposure
     */
    private final Double quantity;
    /**
     * A unit associated with this drug exposure quantity
     */
    private final String unit;
    /**
     * The effective drug dose of this exposure
     */
    private final String effectiveDrugDose;

    /** Included for reflection compatibility: do not use, do not remove */
    private CDMDrugExposure() {
        this(null, null, null, null, null);
    }

    public CDMDrugExposure(Map<String, String> mention, Double quantity, String unit, String effectiveDrugDose, CDMDate... dates) {
        this.mention = mention;
        this.date = dates;
        this.quantity = quantity;
        this.unit = unit;
        this.effectiveDrugDose = effectiveDrugDose;
    }

    /**
     * @return A mapping of drug exposures to their vocabulary type
     */
    public Map<String, String> getMentionMapping() {
        return mention;
    }

    /**
     * @return An array of {@link edu.mayo.omopindexer.model.CDMDate} representing this drug's relevant administration dates
     */
    public CDMDate[] getDates() {
        return date;
    }

    /**
     * @return The quantity associated with this drug exposure
     */
    public Double getQuantity() {
        return quantity;
    }

    /**
     * @return The unit associated with this quantity measurement
     */
    public String getUnit() {
        return unit;
    }

    /**
     * @return The effective drug dose for this exposure
     */
    public String getEffectiveDrugDose() {
        return effectiveDrugDose;
    }

    public String getModelTypeName() {
        return "DrugExposure";
    }

    public JSONObject getAsJSON() {
        JSONObject ret = new JSONObject();
        if (mention != null) {
            for (Map.Entry<String, String> e : mention.entrySet()) {
                ret.put("drug_exposure_" + e.getKey(), e.getValue());
            }
        }
        ret.put("quantity", quantity);
        ret.put("effectiveDrugDose", effectiveDrugDose == null ? "" : effectiveDrugDose);
        ret.put("unit", unit == null ? "" : unit);
        JSONArray dateArray = new JSONArray();
        for (CDMDate date : getDates()) {
            dateArray.put(date.getAsJSON());
        }
        ret.put("date", dateArray);
        ret.put("model_type", "DrugExposure");
        return ret;
    }

    @Override
    public JSONObject getJSONMapping() {
        JSONObject ret = new JSONObject();
        ret.put("quantity", constructTypeObject("float"));
        ret.put("effectiveDrugDose", constructTypeObject("string"));
        ret.put("unit", constructTypeObject("string"));
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