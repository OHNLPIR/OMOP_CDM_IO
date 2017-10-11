package edu.mayo.omopindexer.model;

import edu.mayo.omopindexer.vocabs.SNOMEDCTUtils;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Model for a Person in the OMOP CDM Model: The Person contains demographic attributes for the cohort.
 */
public class CDMPerson implements CDMModel {
    /** An unique identifier used for identification purposes */
    private String personID;

    /** The gender of this person */
    private CDMPerson_GENDER gender;

    /** The ethnicity of this person */
    private CDMPerson_ETHNICITY ethnicity;

    /** Used to dynamically generate an ethnicity if one was not supplied directly from structured data */
    private Map<CDMPerson_ETHNICITY, AtomicInteger> ethnicityElectionMap;

    /** A location ID denoting this person's location */
    private Long locationId;

    /** The person's age expressed as milliseconds since the epoch*/
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

    /** Included for reflection compatibility: do not use, do not remove */
    private CDMPerson() {this(null, null, null, null, null);}

    public CDMPerson(String personID, CDMPerson_GENDER gender, CDMPerson_ETHNICITY ethnicity, Long locationId, Long dateOfBirth, CDMPerson_AGE_LIMITS... exclusions) {
        this.personID = personID;
        this.gender = gender;
        this.ethnicity = ethnicity;
        this.locationId = locationId;
        this.dateOfBirth = dateOfBirth;
        this.exclusions = exclusions;
        this.ethnicityElectionMap = new HashMap<>();
        for (CDMPerson_ETHNICITY e : CDMPerson_ETHNICITY.values()) {
            ethnicityElectionMap.put(e, new AtomicInteger(0));
        }
    }

    /** Used internally for mapping generation, do not use **/
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
     * @return This person's {@link #ethnicity};
     */
    public CDMPerson_ETHNICITY getEthnicity() {
        return ethnicity;
    }

    /**
     * @return This person's {@link #locationId}
     */
    public long getLocationId() {
        return locationId;
    }

    /**
     * @see #exclusions
     * @return Any exclusions to this person's age denotation
     */
    public CDMPerson_AGE_LIMITS[] getExclusions() {
        return exclusions;
    }

    public void electEthnicity(CDMPerson_ETHNICITY ethnicity) {
        ethnicityElectionMap.get(ethnicity).incrementAndGet();
    }

    public String getModelTypeName() {
        return "Person";
    }

    public JSONObject getAsJSON() {
        JSONObject ret = new JSONObject();
        if (personID != null) ret.put("person_id", personID); else ret.put("person_id", "");
        if (gender != null) ret.put("gender", gender.name()); else ret.put("gender", "");
        if (ethnicity != null) {
            ret.put("ethnicity", ethnicity.getFullyQualifiedName());
        } else {
            CDMPerson_ETHNICITY voted = null;
            int max = 0;
            for (Map.Entry<CDMPerson_ETHNICITY, AtomicInteger> e : ethnicityElectionMap.entrySet()) {
                int curr = e.getValue().get();
                if (curr > max) {
                    voted = e.getKey();
                    max = curr;
                }
            }
            if (voted != null) {
                ret.put("ethnicity", voted.getFullyQualifiedName());
            } else {
                ret.put("ethnicity", "");
            }
        }
        ret.put("locationid", locationId);
        if (dateOfBirth != null) ret.put("date_of_birth", dateOfBirth); else ret.put("date_of_birth", (Long)null);
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
        return ret;
    }

    @Override
    public JSONObject getJSONMapping() {
        JSONObject ret = new JSONObject();
        ret.put("person_id", constructTypeObject("string"));
        ret.put("gender", constructTypeObject("string"));
        ret.put("ethnicity", constructTypeObject("string"));
        ret.put("locationid", constructTypeObject("long"));
        ret.put("date_of_birth", constructTypeObject("long"));
        ret.put("limits", constructTypeObject("string"));
        return ret;
    }


    private JSONObject constructTypeObject(String type) {
        JSONObject ret = new JSONObject();
        ret.put("type", type);
        return ret;
    }

    /**
     * @see #dateOfBirth
     * @return This person's age expressed as milliseconds since the epoch
     */
    public Long getDateOfBirth() {
        return dateOfBirth;
    }

    /** An enumeration of genders **/
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

    /** An enumeration of ethnicities */
    public enum CDMPerson_ETHNICITY {
        /** American Indian or Alaska Native */
        AIAN("American Indian or Alaska Native"),
        /** Asian */
        ASIAN("Asian"),
        /** Black or African American */
        BAA("Black or African American"),
        /** Hispanic or Latino */
        HL("Hispanic or Latino"),
        /** Native Hawaiian or Other Pacific Islander */
        NHPI("Native Hawaiian or Other Pacific Islander"),
        /** White */
        WHITE("White");

        private String fullyQualifiedName;

        CDMPerson_ETHNICITY(String s) {
            fullyQualifiedName = s;
        }

        /** @return the fully qualified name of this enumeration, e.g. HL -> Hispanic or Latino */
        public String getFullyQualifiedName() {
            return fullyQualifiedName;
        }

        public static CDMPerson_ETHNICITY fromSNOMEDCTCode(String code) {
            if (SNOMEDCTUtils.isChild(code, "415229000")) { // SNOMED Racial Group
                if (SNOMEDCTUtils.isChild(code, "413490006")) {
                    return AIAN;
                }
                if (SNOMEDCTUtils.isChild(code, "413581001")) { // Asian subtypes
                    return SNOMEDCTUtils.isChild(code, "413582008") ? ASIAN : NHPI;
                }
                if (SNOMEDCTUtils.isChild(code, "413464008")) {
                    return BAA;
                }
                if (SNOMEDCTUtils.isChild(code, "414408004")) {
                    return HL;
                }
                if (SNOMEDCTUtils.isChild(code, "413773004")) {
                    return WHITE;
                }
                return null;
            } else {
                return null;
            }
        }
    }

    /**
     * Used internally to preserve most up-to-date information in spite of asynchronous indexing
     * @return The version of this person model object (increments with every access)
     */
    public long getVersion() {
        return ++version;
    }
}
