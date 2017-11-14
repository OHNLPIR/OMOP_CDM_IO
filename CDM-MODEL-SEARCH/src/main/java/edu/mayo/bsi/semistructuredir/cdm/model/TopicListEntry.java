package edu.mayo.bsi.semistructuredir.cdm.model;

public class TopicListEntry {
    private String name;
    private String description;

    public TopicListEntry() {}

    public TopicListEntry(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
