package org.ohnlp.ir.emirs.model;

import com.fasterxml.jackson.databind.JsonNode;
import edu.mayo.bsi.semistructuredir.cdm.elasticsearch.QueryGeneratorFactory;
import edu.mayo.bsi.semistructuredir.cdm.elasticsearch.impl.CDMQueryGenerator;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.json.JSONObject;
import org.ohnlp.ir.emirs.Properties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;

@Component
public class Query {
    private String structured;
    private Collection<JsonNode> cdmQuery;
    private String unstructured;
    private String jsonSrc;

    public String getStructured() {
        return structured;
    }

    public void setStructured(String structured) {
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
}
