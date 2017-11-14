package edu.mayo.bsi.semistructuredir.cdm.model;

public class TopicResultEntry {
    String result;
    double score;
    String documentText;

    public TopicResultEntry(String docID, String documentText, double score) {
        this.result = docID;
        this.documentText = documentText;
        this.score = score;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getDocumentText() {
        return documentText;
    }

    public void setDocumentText(String documentText) {
        this.documentText = documentText;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

}
