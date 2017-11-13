package edu.mayo.bsi.semistructuredir.cdm.compiler;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import java.util.Collection;

public class ArraySearchContext implements SearchContext {

    private final int minCnt;
    private Collection<SearchContext> arrayElements;

    public ArraySearchContext(Collection<SearchContext> contexts, int minCnt) {
        this.arrayElements = contexts;
        this.minCnt = minCnt;
    }

    public QueryBuilder toQuery(String field) {
        BoolQueryBuilder query = QueryBuilders.boolQuery();
        for (SearchContext context : arrayElements) {
            query.should(context.toQuery(field));
        }
        query.minimumShouldMatch(minCnt);
        return query;
    }
}
