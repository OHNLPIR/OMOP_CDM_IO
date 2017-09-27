package edu.mayo.omopindexer.model;

import org.json.JSONObject;

import java.util.Date;

/**
 * Model for a Date in the OMOP CDM Model TODO: rethink structure of this!
 */
public class CDMDate implements CDMModel {
    /**
     * The date itself represented by the model
     **/
    private final Date date;

    /**
     * The {@link edu.mayo.omopindexer.model.CDMDate.CDMDate_Type} of the date(s)
     */
    private final CDMDate_Type type;
    /**
     * The {@link edu.mayo.omopindexer.model.CDMDate.CDMDate_Subject} of the date(s)
     */
    private final CDMDate_Subject subject;

    /**
     * Used for durations and periods, stored as milliseconds
     */
    private Long duration;

    private CDMDate() {
        this(null, null, null, null);
    }

    public CDMDate(Date date, CDMDate_Type type, CDMDate_Subject subject, Long duration) {
        this.date = date;
        this.type = type;
        this.subject = subject;
        this.duration = duration;
    }

    /**
     * @return this CDMDate's {@link #type}
     */
    public CDMDate_Type getType() {
        return type;
    }

    /**
     * @return this CDMDate's {@link #subject}
     */
    public CDMDate_Subject getSubject() {
        return subject;
    }

    public String getModelTypeName() {
        return "Date";
    }

    public JSONObject getAsJSON() {
        JSONObject ret = new JSONObject();
        ret.put("date", date == null ? null : date.getTime());
        ret.put("type", getType().getTypeName());
        ret.put("subject", subject == null ? "" : getSubject().getTypeName());
        ret.put("timestamp", duration);
        return ret;
    }

    @Override
    public JSONObject getJSONMapping() {
        JSONObject ret = new JSONObject();
        ret.put("date", constructTypeObject("date"));
        ret.put("type", constructTypeObject("string"));
        ret.put("subject", constructTypeObject("string"));
        ret.put("timestamp", constructTypeObject("long"));
        return ret;
    }

    // Included for convenience
    public static JSONObject getJSONMappingStatic() {
        return new CDMDate().getJSONMapping();
    }

    private JSONObject constructTypeObject(String type) {
        JSONObject ret = new JSONObject();
        ret.put("type", type);
        return ret;
    }

    /**
     * Enumeration for use with {@link edu.mayo.omopindexer.model.CDMDate} denoting the subject of the date represented
     */
    public enum CDMDate_Subject {
        DRUG("DRUG"),
        CONDITION("CONDITION");

        private String typeName;

        CDMDate_Subject(String typeName) {
            this.typeName = typeName;
        }

        public String getTypeName() {
            return typeName;
        }
    }

    /**
     * Enumeration for use with {@link edu.mayo.omopindexer.model.CDMDate} denoting the type of date represented
     */
    public enum CDMDate_Type {
        START("START"),
        END("END"),
        PERIOD("PERIOD"),
        DURATION("DURATION");

        private String typeName;

        CDMDate_Type(String typeName) {
            this.typeName = typeName;
        }

        public String getTypeName() {
            return typeName;
        }
    }


}
