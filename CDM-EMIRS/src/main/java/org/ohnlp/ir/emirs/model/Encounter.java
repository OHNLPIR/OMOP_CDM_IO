package org.ohnlp.ir.emirs.model;

import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * Represents a patient encounter
 */
@Component
public class Encounter {
    /**
     * Patient's age at encounter
     */
    private long encounterAge;
    /**
     * The date of the encounter
     */
    private Date encounterDate;

    public long getEncounterAge() {
        return encounterAge;
    }

    public void setEncounterAge(long encounterAge) {
        this.encounterAge = encounterAge;
    }

    public Date getEncounterDate() {
        return encounterDate;
    }

    public void setEncounterDate(Date encounterDate) {
        this.encounterDate = encounterDate;
    }
}
