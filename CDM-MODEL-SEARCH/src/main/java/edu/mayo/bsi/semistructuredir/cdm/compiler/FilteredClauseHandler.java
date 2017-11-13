package edu.mayo.bsi.semistructuredir.cdm.compiler;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

/**
 * Call after creating boosting query
 */
public class FilteredClauseHandler {
    FilterOption option;
    QueryBuilder filter;
    BoostingClauseHandler toFilter;

    public QueryBuilder toQuery(String field) {
        switch (option) {
            case REQUIRED:
                return QueryBuilders.boolQuery().should(toFilter.toQuery()).filter(filter);
            case NEGATE:
                return QueryBuilders.boolQuery().should(toFilter.toQuery()).mustNot(filter);
            default:
                break;
        }
        throw new IllegalArgumentException("Error in compiler: invalid filtering option"); // Not allowed
    }
}
