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
    private DoctorActivityService activityService;
    private PatientVisitService service;

    @BeforeEach
    void setUp() {
        visitRepository = mock(PatientVisitRepository.class);
        patientRepository = mock(PatientRepository.class);
        activityService = mock(DoctorActivityService.class);
        service = new PatientVisitService(visitRepository, patientRepository, activityService);
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

    @Test
    void todayVisitQueryUsesRequestedIndiaDateAndDoctorScope() {
        java.time.LocalDate today = java.time.LocalDate.of(2026, 11, 13);
        Object[] row = {10L, 5L, "Patient One", today, "Doctor One", "Cough"};
        when(visitRepository.findVisitSummariesForDoctorOnDate(DOCTOR_A_ID, today))
                .thenReturn(java.util.Collections.singletonList(row));
        var results = service.getVisitSummariesForDoctorOnDate(DOCTOR_A_ID, today);
        assertEquals(1, results.size());
        assertEquals("Patient One", results.get(0).patientName());
        assertEquals("Cough", results.get(0).chiefComplaints());
        verify(visitRepository).findVisitSummariesForDoctorOnDate(DOCTOR_A_ID, today);
    }

    @Test
    void todayVisitQueryReturnsEmptyWhenNoVisitsExist() {
        java.time.LocalDate today = java.time.LocalDate.of(2026, 11, 13);
        when(visitRepository.findVisitSummariesForDoctorOnDate(DOCTOR_A_ID, today))
                .thenReturn(List.of());
        assertEquals(List.of(), service.getVisitSummariesForDoctorOnDate(DOCTOR_A_ID, today));
    }

    @Test
    void indiaTodayUsesKolkataMidnightBoundary() {
        assertEquals(java.time.LocalDate.of(2026, 11, 13),
                PatientVisitController.todayInIndia(java.time.Instant.parse("2026-11-12T18:30:00Z")));
        assertEquals(java.time.LocalDate.of(2026, 11, 12),
                PatientVisitController.todayInIndia(java.time.Instant.parse("2026-11-12T18:29:59Z")));
    }

    @Test
    void explicitVisitCreationRecordsDoctorActivity() {
        Patient patient = patientOwnedBy(DOCTOR_A_ID);
        when(patientRepository.findByIdForUpdate(PATIENT_ID)).thenReturn(Optional.of(patient));
        when(visitRepository.findMaxVisitNumberByPatientId(PATIENT_ID)).thenReturn(0);
        when(visitRepository.save(any(PatientVisit.class))).thenAnswer(invocation -> {
            PatientVisit saved = invocation.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        PatientVisit saved = service.createVisitForDoctor(PATIENT_ID, DOCTOR_A_ID, new PatientVisit());

        assertEquals(10L, saved.getId());
        verify(activityService).recordActivity(DOCTOR_A_ID,
                DoctorActivity.ActionType.VISIT_RECORDED,
                DoctorActivity.EntityType.VISIT, 10L);
    }

    @Test
    void failedVisitCreationDoesNotRecordActivity() {
        when(patientRepository.findByIdForUpdate(PATIENT_ID)).thenReturn(Optional.empty());
        assertThrows(PatientNotFoundException.class,
                () -> service.createVisitForDoctor(PATIENT_ID, DOCTOR_A_ID, new PatientVisit()));
        verify(activityService, never()).recordActivity(anyLong(), any(), any(), anyLong());
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


