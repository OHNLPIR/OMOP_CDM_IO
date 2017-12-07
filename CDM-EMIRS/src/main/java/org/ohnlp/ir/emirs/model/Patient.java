package org.ohnlp.ir.emirs.model;

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
}
