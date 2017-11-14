package edu.mayo.bsi.semistructuredir.cdm.model;

public class TopicResultEntry {
    String result;
    String documentText;

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

    public TopicResultEntry(String docID, String documentText) {

        this.result = docID;
        this.documentText = documentText;
    }
}
