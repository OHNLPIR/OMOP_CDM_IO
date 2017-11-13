package edu.mayo.bsi.semistructuredir.cdm.compiler;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

public class SimpleSearchContext implements SearchContext {

    private String token;

    public SimpleSearchContext(String token) {
        this.token = token;
    }

    public QueryBuilder toQuery(String field) {
        return QueryBuilders.matchQuery(field, token);
    }

    public String getToken() {
        return token;
    }
}
