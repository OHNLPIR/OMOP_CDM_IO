package edu.mayo.bsi.semistructuredir.cdm.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.util.LinkedList;
import java.util.List;

@JacksonXmlRootElement(localName = "topic_descriptions")
public class TopicDescriptionModel {
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "topic")
    private List<Topic> topics;

    public List<Topic> getTopics() {
        return topics;
    }

    public void setTopics(List<Topic> topic) {
        this.topics = topic;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Topic {
        @JacksonXmlProperty(localName = "id")
        int id;
        @JacksonXmlProperty(localName = "A")
        String longDescription;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getLongDescription() {
            return longDescription;
        }

        public void setLongDescription(String longDescription) {
            this.longDescription = longDescription;
        }
    }

    public String serializePretty() {
        List<String> builder = new LinkedList<>();
        for (Topic t : topics) {
            builder.add(t.getId() + ": " + t.getLongDescription());
        }
        return String.join("\n", builder);
    }
}
