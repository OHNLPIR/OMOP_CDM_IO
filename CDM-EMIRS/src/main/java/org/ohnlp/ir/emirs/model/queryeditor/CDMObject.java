package org.ohnlp.ir.emirs.model.queryeditor;

import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Defines a CDM Object (A document can contain multiple of these)
 */
@Component
public class CDMObject {
    private String type;
    private Map<String, FieldTerm> terms;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, FieldTerm> getTerms() {
        return terms;
    }

    public void setTerms(Map<String, FieldTerm> terms) {
        this.terms = terms;
    }
}
