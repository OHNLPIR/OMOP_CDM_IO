package edu.mayo.bsi.semistructuredir.cdm.compiler;

import org.elasticsearch.index.query.*;

import java.util.Collection;

/**
 * Construct prior to filters
 */
public class BoostingClauseHandler {
    Collection<QueryBuilder> positiveBoost;
    Collection<QueryBuilder> negativeBoost;

    public QueryBuilder toQuery() {
        BoolQueryBuilder base = QueryBuilders.boolQuery();
        for (QueryBuilder context : positiveBoost) {
            base.should(context);
        }
        BoolQueryBuilder neg = QueryBuilders.boolQuery();
        for (QueryBuilder context : negativeBoost) {
            neg.should(context);
        }
        return QueryBuilders.boostingQuery(base, neg).negativeBoost(0.2f);
    }
}
