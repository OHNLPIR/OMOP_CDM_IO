package org.ohnlp.ir.emirs.model;

import org.springframework.stereotype.Component;

/**
 * A returned search result from a query
 */
@Component
public class QueryHit {
    private Patient patient;
    private Encounter encounter;
    private Document doc;
    private double score;

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public Encounter getEncounter() {
        return encounter;
    }

    public void setEncounter(Encounter encounter) {
        this.encounter = encounter;
    }

    public Document getDoc() {
        return doc;
    }

    public void setDoc(Document doc) {
        this.doc = doc;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }
}
