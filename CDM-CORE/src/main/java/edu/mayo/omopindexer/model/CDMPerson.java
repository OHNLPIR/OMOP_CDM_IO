package edu.mayo.omopindexer.model;

import edu.mayo.bsi.nlp.vts.SNOMEDCT;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Model for a Person in the OMOP CDM Model: The Person contains demographic attributes for the cohort.
 */
public class CDMPerson implements CDMModel {
    /**
     * An unique identifier used for identification purposes
     */
    private String personID;

    /**
     * The gender of this person
     */
    private CDMPerson_GENDER gender;

    /**
     * The race of this person
     */
    private CDMPerson_RACE race;

    /**
     * Used to dynamically generate a race if one was not supplied directly from structured data
     */
    private Map<CDMPerson_RACE, AtomicInteger> raceElectionMap;

    /**
     * A location ID denoting this person's location
     */
    private Long locationId;

    /**
     * The person's age expressed as milliseconds since the epoch
     */
    private Long dateOfBirth;

    /**
     * An array of age limits for <b>exclusion</b>
     * Example: “Older than 47” would exclude “Lower”
     */
    private CDMPerson_AGE_LIMITS[] exclusions;

    /**
     * Internal volatile versioning tracker, do not use.
     */
    private long version = 0;

    /**
     * Included for reflection compatibility: do not use, do not remove
     */
    private CDMPerson() {
        this(null, null, null, null, null);
    }

    public CDMPerson(String personID, CDMPerson_GENDER gender, CDMPerson_RACE race, Long locationId, Long dateOfBirth, CDMPerson_AGE_LIMITS... exclusions) {
        this.personID = personID;
        this.gender = gender;
        this.race = race;
        this.locationId = locationId;
        this.dateOfBirth = dateOfBirth;
        this.exclusions = exclusions;
        this.raceElectionMap = new HashMap<>();
        for (CDMPerson_RACE e : CDMPerson_RACE.values()) {
            raceElectionMap.put(e, new AtomicInteger(0));
        }
    }

    /**
     * Used internally for mapping generation, do not use
     **/
    public static CDMPerson generateEmpty() {
        return new CDMPerson();
    }

    /**
     * @return This person's {@link #gender}
     */
    public CDMPerson_GENDER getGender() {
        return gender;
    }

    /**
     * @return This person's {@link #race};
     */
    public CDMPerson_RACE getRace() {
        return race;
    }

    /**
     * @return This person's {@link #locationId}
     */
    public long getLocationId() {
        return locationId;
    }

    /**
     * @return Any exclusions to this person's age denotation
     * @see #exclusions
     */
    public CDMPerson_AGE_LIMITS[] getExclusions() {
        return exclusions;
    }

    public void electEthnicity(CDMPerson_RACE race) {
        raceElectionMap.get(race).incrementAndGet();
    }

    public String getModelTypeName() {
        return "Person";
    }

    public JSONObject getAsJSON() {
        JSONObject ret = new JSONObject();
        if (personID != null) ret.put("person_id", personID);
        else ret.put("person_id", "");
        if (gender != null) ret.put("gender", gender.name());
        else ret.put("gender", "");
        if (race != null) {
            ret.put("race", race.getFullyQualifiedName());
        } else {
            CDMPerson_RACE voted = null;
            int max = 0;
            for (Map.Entry<CDMPerson_RACE, AtomicInteger> e : raceElectionMap.entrySet()) {
                int curr = e.getValue().get();
                if (curr > max) {
                    voted = e.getKey();
                    max = curr;
                }
            }
            if (voted != null) {
                ret.put("race", voted.getFullyQualifiedName());
            } else {
                ret.put("race", "");
            }
        }
        ret.put("locationid", locationId);
        if (dateOfBirth != null) ret.put("date_of_birth", dateOfBirth);
        else ret.put("date_of_birth", (Long) null);
        if (exclusions != null && exclusions.length > 0) {
            StringBuilder sB = new StringBuilder();
            boolean flag = false;
            for (CDMPerson_AGE_LIMITS exclusion : exclusions) {
                if (flag) {
                    sB.append(" ");
                } else {
                    flag = true;
                }
                sB.append(exclusion.name());
            }
            ret.put("limits", sB.toString());
        } else {
            ret.put("limits", "");
        }
        ret.put("model_type", "Person");
        return ret;
    }

    @Override
    public JSONObject getJSONMapping() {
        JSONObject ret = new JSONObject();
        ret.put("person_id", constructTypeObject("string"));
        ret.put("gender", constructTypeObject("string"));
        ret.put("race", constructTypeObject("string"));
        ret.put("locationid", constructTypeObject("long"));
        ret.put("date_of_birth", constructTypeObject("date"));
        ret.put("limits", constructTypeObject("string"));
        return ret;
    }


    private JSONObject constructTypeObject(String type) {
        JSONObject ret = new JSONObject();
        ret.put("type", type);
        return ret;
    }

    /**
     * @return This person's age expressed as milliseconds since the epoch
     * @see #dateOfBirth
     */
    public Long getDateOfBirth() {
        return dateOfBirth;
    }

    /**
     * Sets date of birth
     *
     * @param dateOfBirth The person's age expressed as milliseconds since the epoch
     * @see #dateOfBirth
     */
    public void setDateOfBirth(long dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    /**
     * An enumeration of genders
     **/
    public enum CDMPerson_GENDER {
        MALE,
        FEMALE,
        UNKNOWN
    }

    /**
     * An enumeration of age limits for <b>exclusion</b>
     */
    public enum CDMPerson_AGE_LIMITS {
        UPPER,
        LOWER,
        OTHER
    }

    /**
     * An enumeration of ethnicities
     */
    public enum CDMPerson_RACE {
        /**
         * American Indian or Alaska Native
         */
        AIAN("American Indian or Alaska Native"),
        /**
         * Asian
         */
        ASIAN("Asian"),
        /**
         * Black or African American
         */
        BAA("Black or African American"),
        /**
         * Hispanic or Latino
         */
        HL("Hispanic or Latino"),
        /**
         * Native Hawaiian or Other Pacific Islander
         */
        NHPI("Native Hawaiian or Other Pacific Islander"),
        /**
         * White
         */
        WHITE("White");

        private String fullyQualifiedName;

        CDMPerson_RACE(String s) {
            fullyQualifiedName = s;
        }

        /**
         * @return the fully qualified name of this enumeration, e.g. HL -> Hispanic or Latino
         */
        public String getFullyQualifiedName() {
            return fullyQualifiedName;
        }

        public static CDMPerson_RACE fromSNOMEDCTCode(String code) {
            if (SNOMEDCT.isChild(code, "415229000")) { // By SNOMED Racial Group
                if (SNOMEDCT.isChild(code, "413490006")) {
                    return AIAN;
                }
                if (SNOMEDCT.isChild(code, "413581001")) { // Asian subtypes
                    return SNOMEDCT.isChild(code, "413582008") ? ASIAN : NHPI;
                }
                if (SNOMEDCT.isChild(code, "413464008")) {
                    return BAA;
                }
                if (SNOMEDCT.isChild(code, "414408004")) {
                    return HL;
                }
                if (SNOMEDCT.isChild(code, "413773004")) {
                    return WHITE;
                }
                return null;
            } else if (SNOMEDCT.isChild(code, "372148003")) { // By SNOMED Ethnic Group TODO double check validity of mappings
                if (SNOMEDCT.isChild(code, "66920001")) { // Amerind
                    return AIAN;
                }
                if (SNOMEDCT.isChild(code, "315280000") // Asian - Ethnic Group
                        || SNOMEDCT.isChild(code, "108342005") // South Asian and/or Australian Aborigine
                        || SNOMEDCT.isChild(code, "186044009") // South East Asian
                        || SNOMEDCT.isChild(code, "24812003")) { // Mongol
                    return ASIAN;
                }
                if (SNOMEDCT.isChild(code, "414661004") // Melanesian
                        || SNOMEDCT.isChild(code, "18575005") // Oceanian
                        || SNOMEDCT.isChild(code, "186039002") // New Zealand Maori
                        || SNOMEDCT.isChild(code, "186040000") // Cook Island Maori
                        || SNOMEDCT.isChild(code, "186039002") // Niuean
                        || SNOMEDCT.isChild(code, "186042008") // Tokelauan
                        ) {
                    return NHPI;
                }
                if (SNOMEDCT.isChild(code, "413465009") // Afro-caribbean
                        || SNOMEDCT.isChild(code, "413466005") // Afro-Caucasian
                        || SNOMEDCT.isChild(code, "315240009")) { // Black - Ethnic Group
                    return BAA;
                }
                if (SNOMEDCT.isChild(code, "315239007") && !SNOMEDCT.isChild(code, "186019001")) { // Mixed ethnic but not other
                    return BAA;
                }
                if (SNOMEDCT.isChild(code, "28409002") // Spaniard
                        || SNOMEDCT.isChild(code, "80208004") // Portuguese
                        ) {
                    return HL; // No option for native south american?
                }
                return WHITE; // Remainder are all various european ethnicities TODO check mappings here
            } else {
                return null;
            }
        }
    }

    /**
     * Used internally to preserve most up-to-date information in spite of asynchronous indexing
     *
     * @return The version of this person model object (increments with every access)
     */
    public long getVersion() {
        return ++version;
    }
}
