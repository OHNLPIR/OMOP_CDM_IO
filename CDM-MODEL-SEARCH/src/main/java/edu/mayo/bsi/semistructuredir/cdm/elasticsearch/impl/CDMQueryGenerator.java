package edu.mayo.bsi.semistructuredir.cdm.elasticsearch.impl;

import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
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
    private long encounterAgeLow;
    private long encounterAgeHigh;
    private boolean encounterAgeLowInclusive;
    private boolean encounterAgeHighInclusive;
    private boolean encounterAgeDirty;
    private Date encounterDateLow;
    private Date encounterDateHigh;
    private boolean encounterDateLowInclusive;
    private boolean encounterDateHighInclusive;
    private boolean encounterDateDirty;

    public CDMQueryGenerator() {
        this.models = new LinkedList<>();
        this.personIDs = new LinkedList<>();
        this.scoreMode = ScoreMode.Avg;
        this.encounterAgeHigh = -1;
        this.encounterAgeLow = -1;
        this.encounterAgeHighInclusive = false;
        this.encounterAgeLowInclusive = false;
        this.encounterAgeDirty = false;
        this.encounterDateHigh = null;
        this.encounterDateLow = null;
        this.encounterDateHighInclusive = false;
        this.encounterDateLowInclusive = false;
        this.encounterDateDirty = false;
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

    public CDMQueryGenerator setEncounterAgeLow(long age, boolean inclusive) {
        this.encounterAgeLow = age;
        this.encounterAgeLowInclusive = inclusive;
        this.encounterAgeDirty = this.encounterAgeHigh > 0 && this.encounterAgeLow > 0;
        return this;
    }

    public CDMQueryGenerator setEncounterAgeHigh(long age, boolean inclusive) {
        this.encounterAgeHigh = age;
        this.encounterAgeHighInclusive = inclusive;
        this.encounterAgeDirty = this.encounterAgeHigh > 0 && this.encounterAgeLow > 0;
        return this;
    }


    public CDMQueryGenerator setEncounterDateLow(Date date, boolean inclusive) {
        this.encounterDateLow = date;
        this.encounterDateLowInclusive = inclusive;
        this.encounterDateDirty = this.encounterDateHigh != null && this.encounterDateLow != null;
        return this;
    }

    public CDMQueryGenerator setEncounterDateHigh(Date date, boolean inclusive) {
        this.encounterDateHigh = date;
        this.encounterDateHighInclusive = inclusive;
        this.encounterDateDirty = this.encounterDateHigh != null && this.encounterDateLow != null;
        return this;
    }

    // TODO: some of this stuff (mainly revolving around document parents) should probably be moved as separate functionality

    public QueryBuilder build() {
        // Build child
        BoolQueryBuilder document = QueryBuilders.boolQuery();
        for (JSONObject cdm : models) {
            String type = cdm.optString("model_type");
            if (type == null || type.length() == 0) {
                continue; // Should never happen
            }
            BoolQueryBuilder elementQuery = QueryBuilders.boolQuery();
            for (Object key : cdm.keySet()) {
                Object value = cdm.get(key.toString());
                if (value.toString().isEmpty()) {
                    continue;
                }
                if (!(value instanceof JSONArray) && !value.toString().equals("[]")) {
                    elementQuery.should(QueryBuilders.matchQuery(key.toString(), value));
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
            QueryBuilder encounterFilterQuery = new HasParentQueryBuilder("Person", root, false); // Default is to just check for a person that matches criteria
            if (this.encounterAgeDirty) { // If we do have encounter age limits, we want to add to encounter filter
                RangeQueryBuilder encounterAgeQuery = QueryBuilders.rangeQuery("encounter_age");
                if (this.encounterAgeLow > 0) { // Can't have a negative age
                    encounterAgeQuery.from(this.encounterAgeLow, this.encounterAgeLowInclusive);
                } else {
                    encounterAgeQuery.from(null);
                }
                if (this.encounterAgeHigh > 0) { // Can't have a negative age
                    encounterAgeQuery.to(this.encounterAgeHigh, this.encounterAgeHighInclusive);
                } else {
                    encounterAgeQuery.to(null);
                }
                encounterFilterQuery = QueryBuilders.boolQuery().must(encounterFilterQuery).must(encounterAgeQuery);
            }
            if (this.encounterDateDirty) {
                RangeQueryBuilder encounterDateQuery = QueryBuilders.rangeQuery("encounter_date");
                if (this.encounterDateLow != null) {
                    encounterDateQuery.from(this.encounterDateLow, this.encounterDateLowInclusive);
                } else {
                    encounterDateQuery.from(null);
                }
                if (this.encounterDateHigh != null) {
                    encounterDateQuery.to(this.encounterDateHigh, this.encounterDateHighInclusive);
                } else {
                    encounterDateQuery.to(null);
                }
                encounterFilterQuery =  // If filter is already bool add clause otherwise construct a new bool query with 2 clauses
                        encounterFilterQuery instanceof BoolQueryBuilder ?
                                ((BoolQueryBuilder) encounterFilterQuery).must(encounterDateQuery) :
                                QueryBuilders.boolQuery().must(encounterFilterQuery).must(encounterDateQuery);
            }
            document.must(new HasParentQueryBuilder("Encounter", encounterFilterQuery, false));
        }
        return document;
    }
}
