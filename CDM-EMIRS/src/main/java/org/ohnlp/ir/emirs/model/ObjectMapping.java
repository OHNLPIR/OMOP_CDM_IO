package org.ohnlp.ir.emirs.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.mashape.unirest.http.JsonNode;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@JsonIgnoreProperties(ignoreUnknown = true)
public class ObjectMapping {
    private String parent;

    @JsonProperty(value="_parent")
    public void unpackFromNestedObject(Map<String, String> obj) {
        parent = obj.getOrDefault("type", "ROOT");
    }


    @JsonProperty(value="properties")
    private Map<String, Map<String, Object>> mappings;

    public Map<String, Map<String, Object>> getMappings() {
        return mappings;
    }

    public void setMappings(Map<String, Map<String, Object>> mappings) {
        this.mappings = mappings;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }
}
