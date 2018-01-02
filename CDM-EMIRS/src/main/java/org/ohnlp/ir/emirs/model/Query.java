package org.ohnlp.ir.emirs.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import edu.mayo.bsi.semistructuredir.cdm.elasticsearch.QueryGeneratorFactory;
import edu.mayo.bsi.semistructuredir.cdm.elasticsearch.impl.CDMQueryGenerator;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.search.MatchQuery;
import org.elasticsearch.join.query.HasParentQueryBuilder;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@JsonIgnoreProperties({"patientIDFilter", "patientIDFilterQuery"})
public class Query {
    private Collection<Clause> structured;
    private Collection<JsonNode> cdmQuery;
    private String unstructured;
    private String jsonSrc;
    private ArrayList<Integer> patientIDFilter;

    public Collection<Clause> getStructured() {
        return structured;
    }

    public void setStructured(Collection<Clause> structured) {
        this.structured = structured;
    }

    public String getUnstructured() {
        return unstructured;
    }

    public void setUnstructured(String unstructured) {
        this.unstructured = unstructured;
    }

    public String getJsonSrc() {
        return jsonSrc;
    }

    public void setJsonSrc(String jsonSrc) {
        this.jsonSrc = jsonSrc;
    }

    public QueryBuilder toESQuery() {
        BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();
        QueryBuilder textQuery = QueryGeneratorFactory.newTextQuery().rawTextQuery("RawText", getUnstructured()).build();
        queryBuilder.should(textQuery);
        if (cdmQuery != null && cdmQuery.size() > 0) {
            CDMQueryGenerator cdmQuery = QueryGeneratorFactory.newCDMQuery();
            for (JsonNode node : getCdmQuery()) {
                cdmQuery.addCDMObjects(new JSONObject(node.toString()));
            }
            queryBuilder.should(cdmQuery.build());
        }

        if (patientIDFilter != null) {
            QueryBuilder query;
            if (patientIDFilter.size() == 0) {
                // Hacky way to always fail for all filter by adding a has parent check that will always fail (person with encounter parent)
                query = new HasParentQueryBuilder("Encounter", QueryBuilders.matchAllQuery(), false);
            } else {
                query = QueryBuilders.termsQuery("person_id", patientIDFilter);
            }
            queryBuilder.filter(new HasParentQueryBuilder(
                    "Encounter",
                    new HasParentQueryBuilder(
                            "Person",
                            query,
                            false),
                    false)
            );
        }
        if (structured != null && structured.size() > 0) {
            BoolQueryBuilder temp = QueryBuilders.boolQuery();
            temp.should(queryBuilder);
            temp.should(QueryGeneratorFactory.newStructuredQuery().setStructuredQuery(getStructuredAsSSQ(false)).build());
        }
        setJsonSrc(queryBuilder.toString());
        return queryBuilder;
    }

    public QueryBuilder getPatientIDFilterQuery() {
        String ssqQuery = getStructuredAsSSQ(true);
        return QueryGeneratorFactory.newStructuredQuery().setStructuredQuery(ssqQuery).build();
    }

    public void setPatientIDFilter(ArrayList<Integer> patientIDs) {
        this.patientIDFilter = patientIDs;
    }

    public Collection<JsonNode> getCdmQuery() {
        return cdmQuery;
    }

    public void setCdmQuery(Collection<JsonNode> cdmQuery) {
        this.cdmQuery = cdmQuery;
    }

    public String getStructuredAsSSQ(boolean filtering) {
        Map<String, List<Clause>> map = new HashMap<>();
        for (Clause clause : structured) {
            map.computeIfAbsent(clause.getRecordType(), (k) -> new LinkedList<>()).add(clause);
        }
        StringBuilder out = new StringBuilder();
        for (Map.Entry<String, List<Clause>> e : map.entrySet()) {
            StringBuilder nextType = new StringBuilder();
            nextType.append("type: ").append(e.getKey()).append("\n");
            int appendedClauses = 0;
            for (Clause c : e.getValue()) {
                if (!filtering || c.getType().equalsIgnoreCase("Must") || c.getType().equalsIgnoreCase("Must Not")) {
                    nextType.append(c.getAsSSQ()).append("\n");
                    appendedClauses++;
                }
            }
            if (appendedClauses > 0) {
                out.append(nextType); // No guarantee that it won't only have should/should not clauses
            }
        }
        return out.toString();
    }
}
