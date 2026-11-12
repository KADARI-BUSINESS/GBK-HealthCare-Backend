package com.gbkhealthcare.patient;

import java.time.LocalDate;

/** Projection containing only the fields included in the patient CSV export. */
public interface PatientCsvRow {
    Long getId();
    String getFullName();
    Integer getAge();
    String getGender();
    LocalDate getVisitDate();
    String getPhone();
    String getEmail();
    String getDoctor();
    String getChiefComplaints();
}
