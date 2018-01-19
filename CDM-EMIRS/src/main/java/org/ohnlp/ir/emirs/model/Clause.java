package org.ohnlp.ir.emirs.model;

import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class Clause {
    private String type;
    private String recordType;
    private String field;
    private String content;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getRecordType() {
        return recordType;
    }

    public void setRecordType(String recordType) {
        this.recordType = recordType;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    /**
     * Converts this Clause POJO to its equivalent version in the SSQ query language
     *
     * @return The equivalent version of this clause in the SSQ query language
     */
    public String getAsSSQ() {
        String filter;
        switch (this.type) {
            case "Must":
                filter = "+";
                break;
            case "Should":
                filter = "";
                break;
            case "Must Not":
                filter = "-";
                break;
            case "Should Not":
                filter = "~";
                break;
            default:
                filter = "";
                break;
        }
        return "    " + filter + " " + field + ": " + ((content.startsWith("[") || content.startsWith("R[") ||
                (content.startsWith("(") || content.startsWith("R(")))
                ? content : "\"" + content + "\"");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Clause clause = (Clause) o;
        return Objects.equals(type, clause.type) &&
                Objects.equals(recordType, clause.recordType) &&
                Objects.equals(field, clause.field) &&
                Objects.equals(content, clause.content);
    }

    @Override
    public int hashCode() {

        return Objects.hash(type, recordType, field, content);
    }
}
