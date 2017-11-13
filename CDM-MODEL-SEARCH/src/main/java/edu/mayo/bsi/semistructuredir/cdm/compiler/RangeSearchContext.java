package edu.mayo.bsi.semistructuredir.cdm.compiler;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;

public class RangeSearchContext implements SearchContext {

    private SearchContext[] elements;
    private boolean includeLeft;
    private boolean includeRight;

    public RangeSearchContext(boolean includeLeft, boolean includeRight, SearchContext... elements) {
        this.elements = elements;
        this.includeLeft = includeLeft;
        this.includeRight = includeRight;
    }

    public QueryBuilder toQuery(String field) {
        RangeQueryBuilder builder = QueryBuilders.rangeQuery(field);
        if (!((elements[0] == null || elements[0] instanceof SimpleSearchContext) && (elements[1] == null || elements[1] instanceof SimpleSearchContext))) {
            throw new UnsupportedOperationException("Complex non-term ranges are currently unsupported");
        }
        if (includeLeft) {
            builder.gte(elements[0] == null ? null : ((SimpleSearchContext)elements[0]).getToken());
        } else {
            builder.gt(elements[0] == null ? null : ((SimpleSearchContext)elements[0]).getToken());
        }
        if (includeRight) {
            builder.lte(elements[1] == null ? null : ((SimpleSearchContext)elements[1]).getToken());
        } else {
            builder.lt(elements[1] == null ? null : ((SimpleSearchContext)elements[1]).getToken());
        }
        return builder;
    }
}
