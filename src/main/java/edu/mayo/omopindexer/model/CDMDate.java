package edu.mayo.omopindexer.model;

import org.json.JSONObject;

import java.util.Date;

/**
 * Model for a Date in the OMOP CDM Model TODO: rethink structure of this!
 */
public class CDMDate implements CDMModel {
    /**
     * The date itself, or the first of two if {@link edu.mayo.omopindexer.model.CDMDate.CDMDate_Type} indicates multiple dates,
     * internal use only, use {@link #getStandardizedDate()} instead to retrieve
     **/
    private final Date date;
    /**
     * The second date if {@link edu.mayo.omopindexer.model.CDMDate.CDMDate_Type} indicates multiple dates, otherwise null
     * internal use only, use {@link #getStandardizedDate()} instead to retrieve
     **/
    private final Date date2;
    /**
     * The {@link edu.mayo.omopindexer.model.CDMDate.CDMDate_Type} of the date(s)
     */
    private final CDMDate_Type type;
    /**
     * The {@link edu.mayo.omopindexer.model.CDMDate.CDMDate_Subject} of the date(s)
     */
    private final CDMDate_Subject subject;

    /**
     * A special case field for string durations that cannot be stored as a timestamp (either duration in milliseconds
     * or a string representation)
     */
    private Object duration;

    private CDMDate() {
        this(null, null, null, null, null);
    }

    public CDMDate(Date date, Date date2, CDMDate_Type type, CDMDate_Subject subject, Object durationText) {
        this.date = date;
        this.date2 = date2;
        this.type = type;
        this.subject = subject;
        this.duration = durationText;
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

    /**
     * @deprecated Unused since adoption of COMPOSITE type
     * @return The date(s) denoted by this date object, in long-long format (milliseconds since 1/1/1970 00:00:00 GMT)
     * if {@link edu.mayo.omopindexer.model.CDMDate.CDMDate_Type}
     * indicates single dates, or if missing one or both dates, will replace the relevant long with the string "UNKNOWN"
     * If a duration string is also present, it will be appended to te end
     * @throws RuntimeException if no date type is supplied or if type handling is not present in this method
     */
    public String getStandardizedDate() {
        if (getType() == null) {
            throw new RuntimeException("Date type not supplied");
        }
        switch (getType()) {
            case START:
                if (date != null) {
                    return date.getTime() + "-UNKNOWN";
                } else {
                    return "UNKNOWN-UNKNOWN";
                }
            case END:
                if (date != null) {
                    return "UNKNOWN-" + date.getTime();
                } else {
                    return "UNKNOWN-UNKNOWN";
                }
            case PERIOD:
            case DURATION:
                String firstDate = date == null ? "UNKNOWN" : date.getTime() + "";
                String secondDate = date2 == null ? "UNKNOWN" : date2.getTime() + "";
                return firstDate + "-" + secondDate;
            default:
                throw new RuntimeException("Type handling for " + type.getTypeName() + " not present!");
        }
    }

    public String getModelTypeName() {
        return "Date";
    }

    public JSONObject getAsJSON() { //TODO update this
        JSONObject ret = new JSONObject();
        ret.put("date1", date == null ? null : date.getTime());
        ret.put("date2", date2 == null ? null : date2.getTime());
        ret.put("type", getType().getTypeName());
        ret.put("subject", subject == null ? null : getSubject().getTypeName());
        if (duration != null && duration instanceof Long) {
            ret.put("durationQuant", duration);
        } else {
            if (date == null || date2 == null) {
                ret.put("durationQuant", (Integer)null);
            } else {
                ret.put("durationQuant", date2.getTime() - date.getTime()); // Stores the difference as milliseconds
            }
            if (duration != null) {
                ret.put("duration", duration);
            }
        }
        return ret;
    }

    @Override
    public JSONObject getJSONMapping() {
        JSONObject ret = new JSONObject();
        ret.put("begin_date", constructTypeObject("date"));
        ret.put("end_date", constructTypeObject("date"));
        ret.put("type", constructTypeObject("string"));
        ret.put("subject", constructTypeObject("string"));
        ret.put("timestamp", constructTypeObject("string"));
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
     * <b>Any modifications to this enumerations must also be reflected in {@link CDMDate#getStandardizedDate()}!</b>
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
