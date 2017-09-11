package edu.mayo.omopindexer.model;

import jdk.nashorn.internal.runtime.JSONFunctions;
import org.json.JSONObject;

import java.util.Date;

/**
 * Model for a Date in the OMOP CDM Model
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

    public CDMDate(Date date, Date date2, CDMDate_Type type, CDMDate_Subject subject) {
        this.date = date;
        this.date2 = date2;
        this.type = type;
        this.subject = subject;
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
     * @return The date(s) denoted by this date object, in long-long format (milliseconds since 1/1/1970 00:00:00 GMT)
     * if {@link edu.mayo.omopindexer.model.CDMDate.CDMDate_Type}
     * indicates single dates, or if missing one or both dates, will replace the relevant long with the string "UNKNOWN"
     * @throws RuntimeException if no date type is supplied or if type handling is not present in this method
     */
    public String getStandardizedDate() {
        if (getType() == null) {
            throw new RuntimeException("Date type not supplied");
        }
        switch (getType()) {
            case START:
                if (date != null) return date.getTime() + "-UNKNOWN";
                else return "UNKNOWN-UNKNOWN";
            case END:
                if (date != null) return "UNKNOWN-" + date.getTime();
                else return "UNKNOWN-UNKNOWN";
            case PERIOD:
            case DURATION:
                String firstDate = date == null ? "UNKNOWN" : date.getTime() + "";
                String secondDate = date2 == null ? "UNKNOWN" : date2.getTime() + "";
                return firstDate + "-" + secondDate;
            default:
                throw new RuntimeException("Type handling for " + type.getTypeName() + " not present!");
        }
    }

    public String getName() {
        return "Date";
    }

    public JSONObject getAsJSON() {
        JSONObject ret = new JSONObject();
        ret.put("date", getStandardizedDate());
        ret.put("type", getType().getTypeName());
        ret.put("subject", getSubject().getTypeName());
        if (getType().equals(CDMDate_Type.DURATION)) {
            if (date == null || date2 == null) {
                ret.put("duration", "UNKNOWN");
            } else {
                ret.put("duration", date2.getTime() - date.getTime()); // Stores the difference as milliseconds
            }
        }
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
