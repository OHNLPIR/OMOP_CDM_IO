package org.ohnlp.ir.emirs.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class MappingDefinition {
    private IndexDefinition index;

    public IndexDefinition getIndex() {
        return index;
    }

    public void setIndex(IndexDefinition index) {
        this.index = index;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class IndexDefinition {
        @JsonProperty(value="mappings")
        private Map<String, ObjectMapping> definitions;

        public Map<String, ObjectMapping> getDefinitions() {
            return definitions;
        }

        public void setDefinitions(Map<String, ObjectMapping> definitions) {
            this.definitions = definitions;
        }

    }
}
