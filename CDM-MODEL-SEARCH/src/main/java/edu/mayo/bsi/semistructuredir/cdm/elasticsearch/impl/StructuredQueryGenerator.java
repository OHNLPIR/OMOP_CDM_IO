package edu.mayo.bsi.semistructuredir.cdm.elasticsearch.impl;

import edu.mayo.bsi.semistructuredir.cdm.compiler.Parser;
import org.elasticsearch.index.query.QueryBuilder;

public class StructuredQueryGenerator {

    private String ssqQuery;

    public StructuredQueryGenerator setStructuredQuery(String query) {
        this.ssqQuery = query;
        return this;
    }

    public QueryBuilder build() {
        if (ssqQuery == null) {
            throw new IllegalArgumentException("A structured query must be set before it is built!");
        }
        return Parser.generateQueryFromString(ssqQuery);
    }
}
