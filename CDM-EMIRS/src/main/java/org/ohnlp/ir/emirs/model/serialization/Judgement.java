package org.ohnlp.ir.emirs.model.serialization;

public class Judgement {
    private String document;
    private Integer relevance;
    private Boolean isDocJudgement;

    public String getDocument() {
        return document;
    }

    public void setDocument(String document) {
        this.document = document;
    }

    public Integer getRelevance() {
        return relevance;
    }

    public void setRelevance(Integer relevance) {
        this.relevance = relevance;
    }

    public Boolean getDocJudgement() {
        return isDocJudgement;
    }

    public void setDocJudgement(Boolean docJudgement) {
        isDocJudgement = docJudgement;
    }
}
