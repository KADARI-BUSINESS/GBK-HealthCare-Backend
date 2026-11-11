package com.gbkhealthcare.patient;

import com.gbkhealthcare.auth.Doctor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class PatientVisitServiceTest {

    private static final Long DOCTOR_A_ID = 1L;
    private static final Long PATIENT_ID = 5L;

    private PatientVisitRepository visitRepository;
    private PatientRepository patientRepository;
    private PatientVisitService service;

    @BeforeEach
    void setUp() {
        visitRepository = mock(PatientVisitRepository.class);
        patientRepository = mock(PatientRepository.class);
        service = new PatientVisitService(visitRepository, patientRepository);
    }

    @Test
    void doctorCanReadVisitsForOwnedPatient() {
        Patient patient = patientOwnedBy(DOCTOR_A_ID);
        PatientVisit visit = new PatientVisit();
        visit.setId(10L);
        visit.setPatient(patient);
        when(patientRepository.findByIdAndDoctorAccount_Id(PATIENT_ID, DOCTOR_A_ID))
                .thenReturn(Optional.of(patient));
        when(visitRepository
                .findByPatient_IdAndPatient_DoctorAccount_IdOrderByVisitDateDesc(
                        PATIENT_ID, DOCTOR_A_ID))
                .thenReturn(List.of(visit));

        assertEquals(List.of(visit),
                service.getVisitsByPatientIdForDoctor(PATIENT_ID, DOCTOR_A_ID));
        verify(visitRepository)
                .findByPatient_IdAndPatient_DoctorAccount_IdOrderByVisitDateDesc(
                        PATIENT_ID, DOCTOR_A_ID);
    }

    @Test
    void doctorCannotReadAnotherDoctorsPatientVisits() {
        when(patientRepository.findByIdAndDoctorAccount_Id(PATIENT_ID, DOCTOR_A_ID))
                .thenReturn(Optional.empty());

        assertThrows(PatientNotFoundException.class,
                () -> service.getVisitsByPatientIdForDoctor(PATIENT_ID, DOCTOR_A_ID));
        verify(visitRepository, never())
                .findByPatient_IdAndPatient_DoctorAccount_IdOrderByVisitDateDesc(
                        anyLong(), anyLong());
    }

    @Test
    void nonexistentPatientReturnsNotFoundWithoutQueryingVisits() {
        when(patientRepository.findByIdAndDoctorAccount_Id(999L, DOCTOR_A_ID))
                .thenReturn(Optional.empty());

        assertThrows(PatientNotFoundException.class,
                () -> service.getVisitsByPatientIdForDoctor(999L, DOCTOR_A_ID));
        verify(visitRepository, never())
                .findByPatient_IdAndPatient_DoctorAccount_IdOrderByVisitDateDesc(
                        anyLong(), anyLong());
    }

    private static Patient patientOwnedBy(Long doctorId) {
        Doctor doctor = new Doctor();
        doctor.setId(doctorId);
        Patient patient = new Patient();
        patient.setId(PATIENT_ID);
        patient.setDoctorAccount(doctor);
        return patient;
    }
}
