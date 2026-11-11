package com.gbkhealthcare.patient;

import com.gbkhealthcare.auth.Doctor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PatientFollowUpServiceTest {

    private static final Long DOCTOR_A_ID = 1L;
    private static final Long DOCTOR_B_ID = 2L;
    private static final Long VISIT_ID = 10L;
    private static final Long FOLLOW_UP_ID = 20L;

    private PatientFollowUpRepository followUpRepository;
    private PatientVisitRepository visitRepository;
    private PatientRepository patientRepository;
    private PatientFollowUpService service;

    @BeforeEach
    void setUp() {
        followUpRepository = mock(PatientFollowUpRepository.class);
        visitRepository = mock(PatientVisitRepository.class);
        patientRepository = mock(PatientRepository.class);
        service = new PatientFollowUpService(
                followUpRepository, visitRepository, patientRepository);
    }

    @Test
    void doctorCanReadOwnFollowUpsForVisit() {
        when(visitRepository.findByIdAndPatient_DoctorAccount_Id(
                VISIT_ID, DOCTOR_A_ID)).thenReturn(Optional.of(visit(DOCTOR_A_ID)));
        PatientFollowUp followUp = followUp();
        when(followUpRepository.findByVisitIdAndDoctorAccountIdWithDetails(
                VISIT_ID, DOCTOR_A_ID)).thenReturn(java.util.List.of(followUp));

        assertEquals(java.util.List.of(followUp),
                service.getFollowUpsByVisit(VISIT_ID, DOCTOR_A_ID));
    }

    @Test
    void doctorCannotReadAnotherDoctorsFollowUp() {
        when(patientRepository.findByIdAndDoctorAccount_Id(5L, DOCTOR_A_ID))
                .thenReturn(Optional.empty());

        assertThrows(FollowUpResourceNotFoundException.class,
                () -> service.getFollowUpsByPatient(5L, DOCTOR_A_ID));
        verify(followUpRepository, never())
                .findByPatientIdAndDoctorAccountIdWithDetails(5L, DOCTOR_A_ID);
    }

    @Test
    void doctorCannotUpdateAnotherDoctorsFollowUp() {
        when(followUpRepository.findByIdAndDoctorAccountIdWithDetails(
                FOLLOW_UP_ID, DOCTOR_A_ID)).thenReturn(Optional.empty());

        assertThrows(FollowUpResourceNotFoundException.class,
                () -> service.rescheduleFollowUp(
                        FOLLOW_UP_ID, DOCTOR_A_ID, LocalDate.now(), null));
        verify(followUpRepository, never()).save(any());
    }

    @Test
    void doctorCannotCompleteAnotherDoctorsFollowUp() {
        when(followUpRepository.findByIdAndDoctorAccountIdWithDetails(
                FOLLOW_UP_ID, DOCTOR_A_ID)).thenReturn(Optional.empty());

        assertThrows(FollowUpResourceNotFoundException.class,
                () -> service.completeFollowUp(FOLLOW_UP_ID, DOCTOR_A_ID));
        verify(followUpRepository, never()).save(any());
    }

    @Test
    void doctorCannotCancelAnotherDoctorsFollowUp() {
        when(followUpRepository.findByIdAndDoctorAccountIdWithDetails(
                FOLLOW_UP_ID, DOCTOR_A_ID)).thenReturn(Optional.empty());

        assertThrows(FollowUpResourceNotFoundException.class,
                () -> service.cancelFollowUp(FOLLOW_UP_ID, DOCTOR_A_ID));
        verify(followUpRepository, never()).save(any());
    }

    @Test
    void doctorCannotCreateFollowUpForAnotherDoctorsVisit() {
        when(visitRepository.findByIdAndPatient_DoctorAccount_Id(
                VISIT_ID, DOCTOR_A_ID)).thenReturn(Optional.empty());

        assertThrows(FollowUpResourceNotFoundException.class,
                () -> service.scheduleFollowUp(
                        VISIT_ID, DOCTOR_A_ID, LocalDate.now(), null));
        verify(followUpRepository, never()).save(any());
    }

    @Test
    void visitQueryUsesAuthenticatedDoctorOwnershipScope() {
        PatientVisit visit = visit(DOCTOR_B_ID);
        when(visitRepository.findByIdAndPatient_DoctorAccount_Id(
                VISIT_ID, DOCTOR_A_ID)).thenReturn(Optional.empty());

        assertThrows(FollowUpResourceNotFoundException.class,
                () -> service.getFollowUpsByVisit(VISIT_ID, DOCTOR_A_ID));
        verify(followUpRepository, never())
                .findByVisitIdAndDoctorAccountIdWithDetails(VISIT_ID, DOCTOR_A_ID);
    }

    private static PatientFollowUp followUp() {
        PatientFollowUp followUp = new PatientFollowUp();
        followUp.setId(FOLLOW_UP_ID);
        followUp.setVisit(visit(DOCTOR_A_ID));
        followUp.setFollowUpDate(LocalDate.of(2026, 11, 20));
        followUp.setStatus("SCHEDULED");
        return followUp;
    }

    private static PatientVisit visit(Long doctorId) {
        Doctor doctor = new Doctor();
        doctor.setId(doctorId);
        Patient patient = new Patient();
        patient.setId(5L);
        patient.setDoctorAccount(doctor);
        PatientVisit visit = new PatientVisit();
        visit.setId(VISIT_ID);
        visit.setPatient(patient);
        return visit;
    }
}
