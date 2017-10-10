package edu.mayo.omopindexer.model;

import org.json.JSONObject;

public class GeneratedEncounter implements CDMModel {

    /**
     * The date of the encounter, expressed as milliseconds since the Epoch
     */
    private Long dateOfEncounter;
    /**
     * The patient's age at the time of the encounter
     */
    private Long ageAtEncounter;
    /**
     * The encounter's location
     */
    private String encounterLocation;
    /**
     * Used internally for ElasticSearch aggregation purposes. Is equivalent to person_id:encounter_date_millis:person_dob_millis
     */
    private final String encounterID;

    /**
     * The ID of the person participating in this encounter
     */
    private final String personID;


    public GeneratedEncounter(String personID, String encounterID, Long encounterDate, Long personDOB) {
        this(personID, encounterID, encounterDate, null, null);
        if (encounterDate != null && personDOB != null) {
            ageAtEncounter = encounterDate - personDOB;
        }
    }

    public GeneratedEncounter(String personID, String encounterID, Long dateOfEncounter, Long ageAtEncounter, String encounterLocation) {
        this.personID = personID;
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
        ret.put("person_id", personID);
        ret.put("encounter_id", encounterID);
        ret.put("encounter_date", dateOfEncounter);
        ret.put("encounter_age", ageAtEncounter);
        ret.put("encounter_location", encounterLocation);
        return ret;
    }

    @Override
    public JSONObject getJSONMapping() {
        JSONObject ret = new JSONObject();
        ret.put("person_id", constructTypeObject("string"));
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

    /**
     * @return The date of the encounter, expressed as milliseconds since the Epoch
     */
    public Long getDateOfEncounter() {
        return dateOfEncounter;
    }

    /**
     * @param dateOfEncounter The date of the encounter to set, expressed as milliseconds since the Epoch
     */
    public void setDateOfEncounter(Long dateOfEncounter) {
        this.dateOfEncounter = dateOfEncounter;
    }

    /**
     * @return The person's age at the time of the encounter
     */
    public Long getAgeAtEncounter() {
        return ageAtEncounter;
    }

    /**
     * @param ageAtEncounter The person's age at the time of the encounter to set
     */
    public void setAgeAtEncounter(Long ageAtEncounter) {
        this.ageAtEncounter = ageAtEncounter;
    }

    /**
     * @return The encounter's location
     */
    public String getEncounterLocation() {
        return encounterLocation;
    }

    /**
     * @param encounterLocation The encounter's location to set
     */
    public void setEncounterLocation(String encounterLocation) {
        this.encounterLocation = encounterLocation;
    }

    /**
     * @return The ID of this encounter: equivalent to person_id:encounter_date_millis:person_dob_millis
     */
    public String getEncounterID() {
        return encounterID;
    }

    /**
     * @return The ID of the person participating in this encounter
     */
    public String getPersonID() {
        return personID;
    }

    /** Used internally for mapping generation, do not use **/
    public static GeneratedEncounter generateEmpty() {
        return new GeneratedEncounter(null, null, null, null);
    }
}
