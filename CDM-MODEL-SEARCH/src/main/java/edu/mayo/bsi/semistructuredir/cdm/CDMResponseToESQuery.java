package edu.mayo.bsi.semistructuredir.cdm;

import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.join.query.HasChildQueryBuilder;
import org.elasticsearch.join.query.HasParentQueryBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;

public class CDMResponseToESQuery {

    public static void main(String... args) {
        String test = "[\"{\\\"observation\\\":\\\"i.\\\"}\",\"{\\\"drug_exposure_raw\\\":\\\"mesalamine\\\",\\\"drug_exposure_tui\\\":\\\"T121\\\",\\\"drug_exposure_SNOMEDCT_US_text\\\":\\\"mesalamine\\\",\\\"drug_exposure_RXNORM_code\\\":\\\"52582\\\",\\\"date\\\":[],\\\"drug_exposure_RXNORM_text\\\":\\\"mesalamine\\\",\\\"model_type\\\":\\\"DrugExposure\\\",\\\"drug_exposure_OHDSI_code\\\":\\\"4315038 4299159 968426\\\",\\\"effectiveDrugDose\\\":\\\"\\\",\\\"drug_exposure_cui\\\":\\\"C0127615\\\",\\\"unit\\\":\\\"\\\",\\\"drug_exposure_OHDSI_text\\\":\\\"Mesalazine mesalamine\\\",\\\"drug_exposure_SNOMEDCT_US_code\\\":\\\"86977007 387501005\\\"}\",\"{\\\"drug_exposure_raw\\\":\\\"olsalazine\\\",\\\"drug_exposure_tui\\\":\\\"T121\\\",\\\"drug_exposure_SNOMEDCT_US_text\\\":\\\"olsalazine\\\",\\\"drug_exposure_RXNORM_code\\\":\\\"32385\\\",\\\"date\\\":[],\\\"drug_exposure_RXNORM_text\\\":\\\"olsalazine\\\",\\\"model_type\\\":\\\"DrugExposure\\\",\\\"drug_exposure_OHDSI_code\\\":\\\"916282 4029208 4299843\\\",\\\"effectiveDrugDose\\\":\\\"\\\",\\\"drug_exposure_cui\\\":\\\"C0069454\\\",\\\"unit\\\":\\\"\\\",\\\"drug_exposure_OHDSI_text\\\":\\\"olsalazine Olsalazine\\\",\\\"drug_exposure_SNOMEDCT_US_code\\\":\\\"108673002 386890003\\\"}\",\"{\\\"condition_occurrence_OHDSI_text\\\":\\\"Ulcer Ulcerative\\\",\\\"date\\\":[],\\\"condition_occurrence_raw\\\":\\\"ulcerative\\\",\\\"condition_occurrence_tui\\\":\\\"T047\\\",\\\"condition_occurrence_SNOMEDCT_US_code\\\":\\\"56208002 255321001 429040005\\\",\\\"condition_occurrence_cui\\\":\\\"C0041582\\\",\\\"model_type\\\":\\\"ConditionOccurrence\\\",\\\"condition_occurrence_OHDSI_code\\\":\\\"4206914 4113274 4177703\\\",\\\"condition_occurrence_SNOMEDCT_US_text\\\":\\\"Ulcer\\\"}\",\"{\\\"condition_occurrence_OHDSI_text\\\":\\\"Disease\\\",\\\"date\\\":[],\\\"condition_occurrence_raw\\\":\\\"disease\\\",\\\"condition_occurrence_tui\\\":\\\"T047\\\",\\\"condition_occurrence_SNOMEDCT_US_code\\\":\\\"64572001\\\",\\\"condition_occurrence_cui\\\":\\\"C0012634\\\",\\\"model_type\\\":\\\"ConditionOccurrence\\\",\\\"condition_occurrence_OHDSI_code\\\":\\\"4274025\\\",\\\"condition_occurrence_SNOMEDCT_US_text\\\":\\\"Disease\\\"}\",\"{\\\"condition_occurrence_OHDSI_text\\\":\\\"Ulcerative colitis\\\",\\\"date\\\":[],\\\"condition_occurrence_raw\\\":\\\"ulcerative colitis\\\",\\\"condition_occurrence_tui\\\":\\\"T047\\\",\\\"condition_occurrence_SNOMEDCT_US_code\\\":\\\"64766004\\\",\\\"condition_occurrence_cui\\\":\\\"C0009324\\\",\\\"model_type\\\":\\\"ConditionOccurrence\\\",\\\"condition_occurrence_OHDSI_code\\\":\\\"81893\\\",\\\"condition_occurrence_SNOMEDCT_US_text\\\":\\\"Ulcerative Colitis\\\"}\",\"{\\\"condition_occurrence_OHDSI_text\\\":\\\"Colitis\\\",\\\"date\\\":[],\\\"condition_occurrence_raw\\\":\\\"colitis\\\",\\\"condition_occurrence_tui\\\":\\\"T047\\\",\\\"condition_occurrence_SNOMEDCT_US_code\\\":\\\"64226004\\\",\\\"condition_occurrence_cui\\\":\\\"C0009319\\\",\\\"model_type\\\":\\\"ConditionOccurrence\\\",\\\"condition_occurrence_OHDSI_code\\\":\\\"4272488\\\",\\\"condition_occurrence_SNOMEDCT_US_text\\\":\\\"Colitis\\\"}\",\"{\\\"drug_exposure_raw\\\":\\\"sulfasalazine\\\",\\\"drug_exposure_tui\\\":\\\"T109\\\",\\\"drug_exposure_SNOMEDCT_US_text\\\":\\\"Sulfasalazine\\\",\\\"drug_exposure_RXNORM_code\\\":\\\"9524\\\",\\\"date\\\":[],\\\"drug_exposure_RXNORM_text\\\":\\\"Sulfasalazine\\\",\\\"model_type\\\":\\\"DrugExposure\\\",\\\"drug_exposure_OHDSI_code\\\":\\\"964339 4164453 4299020\\\",\\\"effectiveDrugDose\\\":\\\"\\\",\\\"drug_exposure_cui\\\":\\\"C0036078\\\",\\\"unit\\\":\\\"\\\",\\\"drug_exposure_OHDSI_text\\\":\\\"Sulfasalazine\\\",\\\"drug_exposure_SNOMEDCT_US_code\\\":\\\"45844004 387248006\\\"}\"]";
        JSONArray arr = new JSONArray(new JSONTokener(test));
        LinkedList<JSONObject> objs = new LinkedList<>();
        for (Object o : arr) {
            objs.add(new JSONObject(new JSONTokener(o.toString())));
        }
        System.out.println("{\"query\":" + buildCDMQuery(objs, 2693843L).toString() + "}");

    }

    private static QueryBuilder buildCDMQuery(Collection<JSONObject> cdmModels, long... personID) {
        // Build child
        BoolQueryBuilder document = QueryBuilders.boolQuery();
        for (JSONObject cdm : cdmModels) {
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
                } else {
                    continue; // TODO add support for nested
                }
            }
            document.should(new HasChildQueryBuilder(type, elementQuery, ScoreMode.Total));
        }
        if (personID != null && personID.length > 0) {
            BoolQueryBuilder root = QueryBuilders.boolQuery();
            if (personID.length == 1) {
                root.must(QueryBuilders.termQuery("person_id", personID[0]));
            } else {
                BoolQueryBuilder persons = QueryBuilders.boolQuery();
                for (long id : personID) {
                    persons.should(QueryBuilders.termQuery("person_id", id));
                }
            }
            document.must(new HasParentQueryBuilder("Encounter", new HasParentQueryBuilder("Person", root, false), false));
        }
        return document;
    }
}
