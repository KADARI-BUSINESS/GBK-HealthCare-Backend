package com.gbkhealthcare.patient;

import com.gbkhealthcare.auth.Doctor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PatientServiceActivityTest {
    @Test
    void registrationRecordsOwnerOnceWithoutLoggingItsAutomaticallyCreatedVisit() {
        PatientRepository patients = mock(PatientRepository.class);
        PatientVisitRepository visits = mock(PatientVisitRepository.class);
        DoctorActivityService activities = mock(DoctorActivityService.class);
        Doctor doctor = new Doctor();
        doctor.setId(31L);
        doctor.setFullName("Doctor A");
        Patient patient = new Patient();
        patient.setDoctorAccount(doctor);
        patient.setDoctor("Doctor A");
        patient.setFullName("Patient A");
        patient.setVisitDate(java.time.LocalDate.of(2026, 11, 13));
        when(patients.save(patient)).thenAnswer(invocation -> {
            patient.setId(77L);
            return patient;
        });
        when(visits.save(any(PatientVisit.class))).thenAnswer(invocation -> invocation.getArgument(0));
        PatientService service = new PatientService(patients, visits, activities);

        Patient result = service.create(patient);

        assertAll(
                () -> assertEquals(77L, result.getId()),
                () -> verify(activities).recordActivity(31L,
                        DoctorActivity.ActionType.PATIENT_REGISTERED,
                        DoctorActivity.EntityType.PATIENT, 77L),
                () -> verify(visits).save(any(PatientVisit.class)),
                () -> verify(activities, never()).recordActivity(31L,
                        DoctorActivity.ActionType.VISIT_RECORDED,
                        DoctorActivity.EntityType.VISIT, 77L));
    }
}
