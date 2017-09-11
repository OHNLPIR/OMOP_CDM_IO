package edu.mayo.omopindexer.io.serializer;

import edu.mayo.omopindexer.model.CDMModel;
import org.json.JSONObject;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/** Transforms a document that contains a certain set of models into a JSON indexable by ElasticSearch */
public class DocumentSerializer {
    /** The ID of the Document */
    private String documentID;
    /** The raw text of the Document */
    private String documentText;
    /** A mapping by type of OMOP CDM models associated with this document */
    private Map<String, List<CDMModel>> models;

    /** @return A que of JSON Objects, first element will always be the parent document and subsequent JSONs its
     * associated model instances */
    public Deque<JSONObject> toElasticSearchIndexableJSONs() {
        // Construct the parent document to index
        JSONObject parent = new JSONObject();
        parent.put("DocumentID", documentID);
        parent.put("RawText", documentText);
        // Construct a list of children models to also index and associate
        LinkedList<JSONObject> ret = new LinkedList<JSONObject>();
        for (Map.Entry<String, List<CDMModel>> e : models.entrySet()) {
            for (CDMModel m : e.getValue()) {
                JSONObject child = m.getAsJSON();
                child.put("type", m.getName());
                ret.add(child);
            }
        }
        // Add the root document at the head of the que
        ret.addFirst(parent);
        return ret;
    }
}
