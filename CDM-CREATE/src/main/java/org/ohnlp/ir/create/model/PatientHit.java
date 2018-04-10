package org.ohnlp.ir.create.model;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PatientHit {
    Patient patient;
    List<DocumentHit> docs;
    double score;

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public List<DocumentHit> getDocs() {
        return docs;
    }

    public void setDocs(List<DocumentHit> docs) {
        this.docs = docs;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }
}
