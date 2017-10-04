package edu.mayo.omopindexer.model;

import org.json.JSONObject;

public class GeneratedEncounter implements CDMModel {

    /**
     * The date of the encounter, expressed as milliseconds since the Epoch
     */
    private final Long dateOfEncounter;
    /**
     * The patient's age at the time of the encounter
     */
    private final Long ageAtEncounter;
    /**
     * The encounter's location
     */
    private final String encounterLocation;
    /**
     * Used internally for ElasticSearch aggregation purposes. Is equivalent to patient_id:encounter_date_millis
     */
    private final String encounterID;


    public GeneratedEncounter(String encounterID, long dateOfEncounter, long ageAtEncounter, String encounterLocation) {
        this.encounterID = encounterID;
        this.dateOfEncounter = dateOfEncounter;
        this.ageAtEncounter = ageAtEncounter;
        this.encounterLocation = encounterLocation;
    }

    @Override
    public String getModelTypeName() {
        return "Encounter";
    }

    @Override
    public JSONObject getAsJSON() {
        JSONObject ret = new JSONObject();
        ret.put("encounter_id", encounterID);
        ret.put("encounter_date", dateOfEncounter);
        ret.put("encounter_age", ageAtEncounter);
        ret.put("encounter_location", encounterLocation);
        return ret;
    }

    @Override
    public JSONObject getJSONMapping() {
        JSONObject ret = new JSONObject();
        ret.put("encounter_id", constructTypeObject("string"));
        ret.put("encounter_date", constructTypeObject("date"));
        ret.put("encounter_age", constructTypeObject("long"));
        ret.put("encounter_location", constructTypeObject("string"));
        return ret;
    }

    private JSONObject constructTypeObject(String type) {
        JSONObject ret = new JSONObject();
        ret.put("type", type);
        return ret;
    }
}
