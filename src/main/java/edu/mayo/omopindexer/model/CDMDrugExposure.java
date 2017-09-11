package edu.mayo.omopindexer.model;

import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Model for a Date in the OMOP CDM Model
 * Drugs include prescription and over-the-counter medicines, vaccines, and large-molecule biologic therapies.
 */
public class CDMDrugExposure implements CDMModel {
    /** The textual mention of the drug exposure event */
    private final String mention;
    /** A {@link edu.mayo.omopindexer.model.CDMDate} representation of this drug's relevant administration dates */
    private final CDMDate date;
    /** A quantity associated with this drug exposure */
    private final Integer quantity;
    /** The effective drug dose of this exposure */
    private final BigDecimal effectiveDrugDose;

    private CDMDrugExposure() {this(null, null, null, null);}

    public CDMDrugExposure(String mention, CDMDate date, Integer quantity, BigDecimal effectiveDrugDose) {
        this.mention = mention;
        this.date = date;
        this.quantity = quantity;
        this.effectiveDrugDose = effectiveDrugDose;
    }

    /** @return The textual mention of this drug exposure event */
    public String getMention() {
        return mention;
    }

    /**
     * @return A {@link edu.mayo.omopindexer.model.CDMDate} representation of this drug's relevant administration dates
     */
    public CDMDate getDate() {
        return date;
    }

    /** @return The quantity associated with this drug exposure */
    public Integer getQuantity() {
        return quantity;
    }

    /** @return The effective drug dose for this exposure */
    public BigDecimal getEffectiveDrugDose() {
        return effectiveDrugDose;
    }

    public String getName() {
        return "DrugExposure";
    }

    public JSONObject getAsJSON() {
        JSONObject ret = new JSONObject();
        if (mention != null) ret.put("drug_exposure", mention);
        if (quantity != null) ret.put("quantity", quantity);
        if (date != null) {
            for (Map.Entry<String, Object> e : date.getAsJSON().toMap().entrySet()) {
                ret.put(e.getKey(), e.getValue());
            }
        }
        return ret;
    }
}
