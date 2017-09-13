package edu.mayo.omopindexer.model;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Model for a Person in the OMOP CDM Model: The Person contains demographic attributes for the cohort.
 */
public class CDMPerson implements CDMModel {

    /** The gender of this person */
    private CDMPerson_GENDER gender;

    /** The ethnicity of this person */
    private CDMPerson_ETHNICITY ethnicity;

    /** A location ID denoting this person's location */
    private Long locationId;

    /** An integer value of this person's age */
    private Integer age;

    /** Any further text associated with a person's age (e.g. weeks, months, years, etc) */
    private String age_text;

    /**
     * An array of age limits for <b>exclusion</b>
     * Example: “Older than 47” would exclude “Lower”
     */
    private CDMPerson_AGE_LIMITS[] exclusions;

    /** Included for reflection compatibility: do not use, do not remove */
    private CDMPerson() {this(null, null, null, null, null);}

    public CDMPerson(CDMPerson_GENDER gender, CDMPerson_ETHNICITY ethnicity, Long locationId, Integer age, String age_text, CDMPerson_AGE_LIMITS... exclusions) {
        this.gender = gender;
        this.ethnicity = ethnicity;
        this.locationId = locationId;
        this.age = age;
        this.age_text = age_text;
        this.exclusions = exclusions;
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
     * @return This person's {@link #age}
     */
    public int getAge() {
        return age;
    }

    /**
     * @see #age_text
     * @return Any associated text with this person's age
     */
    public String getAge_text() {
        return age_text;
    }

    /**
     * @see #exclusions
     * @return Any exclusions to this person's age denotation
     */
    public CDMPerson_AGE_LIMITS[] getExclusions() {
        return exclusions;
    }

    public String getModelTypeName() {
        return "Person";
    }

    public JSONObject getAsJSON() {
        JSONObject ret = new JSONObject();
        if (gender != null) ret.put("gender", gender.name());
        if (ethnicity != null) ret.put("ethnicity", ethnicity.fullyQualifiedName);
        if (locationId != null) ret.put("locationID", locationId);
        if (age != null) ret.put("age", age);
        if (age_text != null) ret.put("age_text", age_text);
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
        }
        return ret;
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
    }
}
