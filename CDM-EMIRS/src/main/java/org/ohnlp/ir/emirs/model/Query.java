package org.ohnlp.ir.emirs.model;

import com.fasterxml.jackson.databind.JsonNode;
import edu.mayo.bsi.semistructuredir.cdm.elasticsearch.QueryGeneratorFactory;
import edu.mayo.bsi.semistructuredir.cdm.elasticsearch.impl.CDMQueryGenerator;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class Query {
    private Collection<Clause> structured;
    private Collection<JsonNode> cdmQuery;
    private String unstructured;
    private String jsonSrc;

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
        setJsonSrc(queryBuilder.toString());
        return queryBuilder;
    }

    public Collection<JsonNode> getCdmQuery() {
        return cdmQuery;
    }

    public void setCdmQuery(Collection<JsonNode> cdmQuery) {
        this.cdmQuery = cdmQuery;
    }

    public String getStructuredAsSSQ() {
        Map<String, List<Clause>> map = new HashMap<>();
        for (Clause clause : structured) {
            map.computeIfAbsent(clause.getRecordType(), (k) -> new LinkedList<>()).add(clause);
        }
        StringBuilder out = new StringBuilder();
        for (Map.Entry<String, List<Clause>> e : map.entrySet()) {
            out.append("type: ").append(e.getKey()).append("\n");
            for (Clause c : e.getValue()) {
                out.append(c.getAsSSQ()).append("\n");
            }
        }
        return out.toString();
    }
}
