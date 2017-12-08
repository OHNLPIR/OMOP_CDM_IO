package org.ohnlp.ir.emirs.model;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class QueryResult {
    /**
     * The query associated with this result
     */
    private Query query;
    /**
     * An ordered array of result hits
     */
    private List<QueryHit> hits;

    /**
     * The patients associated with this result
     */
    private List<Patient> patients;

    public Query getQuery() {
        return query;
    }

    public void setQuery(Query query) {
        this.query = query;
    }

    public List<QueryHit> getHits() {
        return hits;
    }

    public void setHits(List<QueryHit> hits) {
        this.hits = hits;
    }

    public List<Patient> getPatients() {
        return patients;
    }

    public void setPatients(List<Patient> patients) {
        this.patients = patients;
    }
}

