package edu.mayo.bsi.semistructuredir.cdm.elasticsearch.impl;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

public class RawTextQuery implements TextQueryGenerator.TextQuery{
    private final String text;
    private final String field;

    public RawTextQuery(String field, String text) {
        this.field = field;
        this.text = text;
    }

    @Override
    public TextQueryGenerator.TextQuery setOption(String opt, Object value) {
        return this;
    }

    public QueryBuilder build() {
        return QueryBuilders.matchQuery(field, text);
    }
}
