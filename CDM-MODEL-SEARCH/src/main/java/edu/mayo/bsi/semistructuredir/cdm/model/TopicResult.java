package edu.mayo.bsi.semistructuredir.cdm.model;

import java.util.List;

public class TopicResult {
    String topicID;
    String topicDesc;
    List<TopicResultEntry> results;

    public TopicResult(String topicID, String topicDesc, List<TopicResultEntry> results) {
        this.topicID = topicID;
        this.topicDesc = topicDesc;
        this.results = results;
    }

    public String getTopicID() {
        return topicID;
    }

    public void setTopicID(String topicID) {
        this.topicID = topicID;
    }

    public String getTopicDesc() {
        return topicDesc;
    }

    public void setTopicDesc(String topicDesc) {
        this.topicDesc = topicDesc;
    }

    public List<TopicResultEntry> getResults() {
        return results;
    }

    public void setResults(List<TopicResultEntry> results) {
        this.results = results;
    }
}
