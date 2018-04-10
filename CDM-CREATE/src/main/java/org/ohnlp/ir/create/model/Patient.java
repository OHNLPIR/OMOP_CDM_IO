package org.ohnlp.ir.create.model;

import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * Represents a patient
 */
@Component
public class Patient {
    private String id;
    private String gender;
    private String ethnicity;
    private String race;
    private String city;
    private Date dob;

    public Patient() {};

    public Patient(String id, String gender, String ethnicity, String race, String city, Date dob) {
        this.id = id;
        this.gender = gender;
        this.ethnicity = ethnicity;
        this.race = race;
        this.city = city;
        this.dob = dob;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getEthnicity() {
        return ethnicity;
    }

    public void setEthnicity(String ethnicity) {
        this.ethnicity = ethnicity;
    }

    public String getRace() {
        return race;
    }

    public void setRace(String race) {
        this.race = race;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public Date getDob() {
        return dob;
    }

    public void setDob(Date dob) {
        this.dob = dob;
    }
}
