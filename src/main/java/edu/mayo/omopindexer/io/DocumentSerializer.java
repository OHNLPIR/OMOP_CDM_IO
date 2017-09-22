package edu.mayo.omopindexer.io;

import edu.mayo.omopindexer.model.CDMModel;
import org.json.JSONObject;

import java.util.*;

/** Transforms a document that contains a certain set of models into a JSON indexable by ElasticSearch */
public class DocumentSerializer {
    /** The section name of this document */
    private final String sectionName;
    /** The section ID of this document */
    private final String sectionID;
    /** The document header of this document **/
    private final String documentHeader;
    /** The ID of the Document */
    private String documentID;
    /** The raw text of the Document */
    private String documentText;
    /** A mapping by type of OMOP CDM models associated with this document */
    private Map<String, List<CDMModel>> models;

    public DocumentSerializer(String documentID, String documentText, String documentHeader, String sectionName, String sectionID, CDMModel... docModels) {
        this.documentID = documentID;
        this.documentText = documentText;
        this.documentHeader = documentHeader;
        this.sectionName = sectionName;
        this.sectionID = sectionID;
        this.models = new HashMap<>();
        for (CDMModel model : docModels) {
            if (!this.models.containsKey(model.getModelTypeName())) {
                this.models.put(model.getModelTypeName(), new LinkedList<>());
            }
            this.models.get(model.getModelTypeName()).add(model);
        }
    }

    /** @return A que of JSON Objects, first element will always be the parent document and subsequent JSONs its
     * associated model instances */
    public Deque<JSONObject> toElasticSearchIndexableJSONs() {
        // Construct the parent document to index
        JSONObject parent = new JSONObject();
        parent.put("DocumentID", documentID);
        parent.put("RawText", documentText);
        parent.put("Header", documentHeader);
        parent.put("Section_Name", sectionName);
        parent.put("Section_ID", Integer.valueOf(sectionID));
        // Construct a list of children models to also index and associate
        LinkedList<JSONObject> ret = new LinkedList<JSONObject>();
        for (Map.Entry<String, List<CDMModel>> e : models.entrySet()) {
            for (CDMModel m : e.getValue()) {
                JSONObject child = m.getAsJSON();
                child.put("type", m.getModelTypeName());
                ret.add(child);
            }
        }
        // Add the root document at the head of the que
        ret.addFirst(parent);
        return ret;
    }
}
