package org.ohnlp.ir.emirs.model;

import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class QueryResult {
    /**
     * The query associated with this result
     */
    private Query query;

    /**
     * A pagination view of current document results
     */
    private Pagination documentPager;

    /**
     * A set of patient to document pagination mappings
     */
    private List<PatientHit> patientHits;

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

    public Pagination getDocPager() {
        return documentPager;
    }

    public void setDocPager(Pagination hits) {
        this.documentPager = hits;
    }

    public List<Patient> getPatients() {
        return patients;
    }

    public void setPatients(List<Patient> patients) {
        this.patients = patients;
    }


    public List<PatientHit> getPatientHits() {
        return patientHits;
    }

    public void setPatientHits(List<PatientHit> hits) {
        this.patientHits = hits;
    }
}

