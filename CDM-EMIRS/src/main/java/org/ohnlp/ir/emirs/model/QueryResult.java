package org.ohnlp.ir.emirs.model;

import org.springframework.stereotype.Component;

@Component
public class QueryResult {
    /**
     * The query associated with this result
     */
    private Query query;
    /**
     * An ordered array of result hits
     */
    private QueryHit[] hits;

    public Query getQuery() {
        return query;
    }

    public void setQuery(Query query) {
        this.query = query;
    }

    public QueryHit[] getHits() {
        return hits;
    }

    public void setHits(QueryHit[] hits) {
        this.hits = hits;
    }
}
