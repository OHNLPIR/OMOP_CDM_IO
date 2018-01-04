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
     * An ordered array of result hits
     */
    private List<DocumentHit> hits;

    /**
     * A set of patient to documenthit[] mappings
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

    public List<DocumentHit> getHits() {
        return hits;
    }

    public void setHits(List<DocumentHit> hits) {
        this.hits = hits;
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

    public void calculatePatientHits() {
        Map<Patient, List<DocumentHit>> patientToHitsMap = new HashMap<>();
        for (DocumentHit doc : hits) {
            patientToHitsMap.computeIfAbsent(doc.getPatient(), (k) -> new LinkedList<>()).add(doc);
        }
        this.patientHits = new ArrayList<>(patientToHitsMap.size());
        for (Map.Entry<Patient, List<DocumentHit>> e : patientToHitsMap.entrySet()) {
            PatientHit hit = new PatientHit();
            List<DocumentHit> unsortedHits = e.getValue();
            ArrayList<DocumentHit> docHits = new ArrayList<>(unsortedHits.size());
            double totalScore = 0;
            for (DocumentHit doc : unsortedHits) {
                totalScore += doc.getScore();
                docHits.add(doc);
            }
            docHits.sort((d1, d2) -> Double.compare(d2.getScore(),d1.getScore()));
            hit.setDocs(docHits);
            hit.setPatient(e.getKey());
            hit.setScore(totalScore);
            this.patientHits.add(hit);
        }
        this.patientHits.sort((h1, h2) -> {
            int curr = Double.compare(h2.score, h1.score);
            if (curr != 0) {
                return curr;
            } else {
                return h2.docs.size() - h1.docs.size();
            }
        });
    }
}

