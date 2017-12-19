package org.ohnlp.ir.emirs.model;

import edu.mayo.bsi.semistructuredir.cdm.elasticsearch.QueryGeneratorFactory;
import org.elasticsearch.index.query.QueryBuilder;
import org.ohnlp.ir.emirs.Properties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@EnableConfigurationProperties(Properties.class)
public class Query {
    private String structured;
    private String cdmQuery;
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
        QueryBuilder textQuery = QueryGeneratorFactory.newTextQuery().rawTextQuery("RawText", getUnstructured()).build();
        return textQuery; //TODO
    }

    public String getCdmQuery() {
        return cdmQuery;
    }

    public void setCdmQuery(String cdmQuery) {
        this.cdmQuery = cdmQuery;
    }
}
