package org.ohnlp.ir.create.model.serialization;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Collection;

public class SerializationModel {
    private String unstructured;
    private JsonNode structured;
    private JsonNode cdm;
    private Collection<Judgement> hits;

    public String getUnstructured() {
        return unstructured;
    }

    public void setUnstructured(String unstructured) {
        this.unstructured = unstructured;
    }

    public JsonNode getStructured() {
        return structured;
    }

    public void setStructured(JsonNode structured) {
        this.structured = structured;
    }

    public JsonNode getCdm() {
        return cdm;
    }

    public void setCdm(JsonNode cdm) {
        this.cdm = cdm;
    }

    public Collection<Judgement> getHits() {
        return hits;
    }

    public void setHits(Collection<Judgement> hits) {
        this.hits = hits;
    }
}
