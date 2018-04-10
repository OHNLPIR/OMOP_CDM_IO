package org.ohnlp.ir.create.model.serialization;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@JsonIgnoreProperties
public class SerializationRequest {
    private String username;
    private String queryName;
    private JsonNode structured;
    private String unstructured;
    private JsonNode cdm;
    private Map<String, Integer> docJudgements;
    private Map<String, Integer> patientJudgements;


    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getQueryName() {
        return queryName;
    }

    public void setQueryName(String queryName) {
        this.queryName = queryName;
    }

    public JsonNode getStructured() {
        return structured;
    }

    public void setStructured(JsonNode structured) {
        this.structured = structured;
    }

    public String getUnstructured() {
        return unstructured;
    }

    public void setUnstructured(String unstructured) {
        this.unstructured = unstructured;
    }

    public JsonNode getCdm() {
        return cdm;
    }

    public void setCdm(JsonNode cdm) {
        this.cdm = cdm;
    }

    public Map<String, Integer> getDocJudgements() {
        return docJudgements;
    }

    public void setDocJudgements(Map<String, Integer> docJudgements) {
        this.docJudgements = docJudgements;
    }

    public Map<String, Integer> getPatientJudgements() {
        return patientJudgements;
    }

    public void setPatientJudgements(Map<String, Integer> patientJudgements) {
        this.patientJudgements = patientJudgements;
    }

}
