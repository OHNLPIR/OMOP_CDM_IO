package org.ohnlp.ir.emirs.model.queryeditor;

import org.springframework.stereotype.Component;

/**
 * Denotes a field-term search pair
 */
@Component
public class FieldTerm {
    private String field;
    private String term;

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getTerm() {
        return term;
    }

    public void setTerm(String term) {
        this.term = term;
    }
}
