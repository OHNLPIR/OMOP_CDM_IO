package edu.mayo.bsi.semistructuredir.cdm.elasticsearch.impl;

import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.join.query.HasChildQueryBuilder;
import org.elasticsearch.join.query.HasParentQueryBuilder;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

/**
 * Generates an elasticsearch query based on an input collection of CDM objects in JSON form
 */
public class CDMQueryGenerator {

    private List<JSONObject> models;
    private List<Integer> personIDs;
    private ScoreMode scoreMode;

    public CDMQueryGenerator() {
        this.models = new LinkedList<>();
        this.personIDs = new LinkedList<>();
        this.scoreMode = ScoreMode.Total;
    }

    public CDMQueryGenerator addCDMObjects(JSONObject... objects) {
        models.addAll(Arrays.asList(objects));
        return this;
    }

    public CDMQueryGenerator addFilteringPersons(Integer... personIDs) {
        this.personIDs.addAll(Arrays.asList(personIDs));
        return this;
    }

    public CDMQueryGenerator setScoreMode(ScoreMode mode) {
        this.scoreMode = mode;
        return this;
    }

    // TODO: support adding encounter limitations etc

    public QueryBuilder build() {
        // Build child
        BoolQueryBuilder document = QueryBuilders.boolQuery();
        for (JSONObject cdm : models) {
            String type = cdm.optString("model_type");
            if (type == null || type.length() == 0) {
                continue; // Should never happen
            }
            BoolQueryBuilder elementQuery = QueryBuilders.boolQuery();
            for (Map.Entry<String, Object> e : cdm.toMap().entrySet()) {
                if (e.getValue().toString().isEmpty()) {
                    continue;
                }
                if (!(e.getValue() instanceof JSONArray) && !e.getValue().toString().equals("[]")) {
                    elementQuery.should(QueryBuilders.matchQuery(e.getKey(), e.getValue()));
                }
                // TODO add support for nested
            }
            document.should(new HasChildQueryBuilder(type, elementQuery, scoreMode));
        }
        if (personIDs.size() > 0) {
            BoolQueryBuilder root = QueryBuilders.boolQuery();
            if (personIDs.size() == 1) {
                root.must(QueryBuilders.termQuery("person_id", personIDs.get(0)));
            } else {
                BoolQueryBuilder persons = QueryBuilders.boolQuery();
                for (int id : personIDs) {
                    persons.should(QueryBuilders.termQuery("person_id", id));
                }
                root.must(persons);
            }
            document.must(new HasParentQueryBuilder("Encounter", new HasParentQueryBuilder("Person", root, false), false));
        }
        return document;
    }
}
