package com.gbkhealthcare.patient;

import com.gbkhealthcare.auth.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DoctorOpdServiceTest {
    private DoctorRepository doctors; private DoctorQueueEntryRepository queue; private PatientVisitRepository visits;
    private NursingIntakeRecordRepository intakes; private StaffAccountRepository staff;
    private PatientHistoryAccessEventRepository historyAudit; private DoctorOpdAuditEventRepository audit;
    private ConsultationAddendumRepository addenda; private DoctorOpdService service; private Doctor doctor; private Jwt jwt;
    private PatientFollowUpRepository followUps; private PatientFollowUpAuditEventRepository followUpAudit;

    @BeforeEach void setUp() {
        doctors = mock(DoctorRepository.class); queue = mock(DoctorQueueEntryRepository.class); visits = mock(PatientVisitRepository.class);
        intakes = mock(NursingIntakeRecordRepository.class); staff = mock(StaffAccountRepository.class);
        historyAudit = mock(PatientHistoryAccessEventRepository.class); audit = mock(DoctorOpdAuditEventRepository.class);
        addenda = mock(ConsultationAddendumRepository.class); followUps = mock(PatientFollowUpRepository.class); followUpAudit = mock(PatientFollowUpAuditEventRepository.class);
        service = new DoctorOpdService(doctors, queue, visits, intakes, staff, historyAudit, audit, addenda, followUps, followUpAudit);
        doctor = new Doctor(); doctor.setId(41L); doctor.setEmail("doctor@example.test"); doctor.setEnabled(true); doctor.setApprovalStatus(Doctor.ApprovalStatus.APPROVED);
        jwt = mock(Jwt.class); when(jwt.getSubject()).thenReturn("doctor@example.test"); when(jwt.getClaims()).thenReturn(Map.of("doctorId", 41L));
        when(doctors.findById(41L)).thenReturn(Optional.of(doctor));
    }

    @Test void queueIsScopedToTheAuthenticatedDoctor() {
        when(queue.findActiveForDoctor(41L)).thenReturn(List.of());
        assertTrue(service.queue(jwt).isEmpty()); verify(queue).findActiveForDoctor(41L);
        verify(queue, never()).findActiveForDoctor(42L);
    }

    @Test void unassignedDoctorCannotClaimEncounter() {
        when(queue.findAssignedForUpdate(7L, 41L)).thenReturn(Optional.empty());
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.claim(jwt, 7L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(queue, never()).save(any());
    }

    @Test void inactiveOrUnapprovedDoctorCannotUseOpdService() {
        doctor.setEnabled(false);
        assertThrows(ResponseStatusException.class, () -> service.queue(jwt));
        doctor.setEnabled(true); doctor.setApprovalStatus(Doctor.ApprovalStatus.REJECTED);
        assertThrows(ResponseStatusException.class, () -> service.queue(jwt));
    }

    @Test void doctorQueueLifecycleTransitionsAreStrict() {
        DoctorQueueEntry entry = new DoctorQueueEntry(mock(NursingQueueEntry.class), mock(PatientVisit.class), doctor, LocalDateTime.now());
        assertThrows(IllegalStateException.class, () -> entry.start(LocalDateTime.now()));
        entry.claim(LocalDateTime.now()); entry.start(LocalDateTime.now()); entry.hold(LocalDateTime.now()); entry.resume();
        assertEquals("IN_CONSULTATION", entry.getStatus());
        entry.sign(LocalDateTime.now()); assertEquals("CONSULTATION_SIGNED", entry.getStatus());
        assertThrows(IllegalStateException.class, () -> entry.resume());
    }

    @Test void claimUsesLockedAssignedQueueAndConcurrentSecondClaimConflicts() {
        DoctorQueueEntry entry = realQueueEntry();
        when(queue.findAssignedForUpdate(7L, 41L)).thenReturn(Optional.of(entry)); when(queue.save(entry)).thenReturn(entry);
        assertEquals("CLAIMED", service.claim(jwt, 7L).status());
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.claim(jwt, 7L));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode()); verify(queue, times(2)).findAssignedForUpdate(7L, 41L);
        verify(audit).save(argThat(event -> event.getActionType().equals("QUEUE_CLAIMED") && event.getDoctorId().equals(41L)));
    }

    @Test void historicalReadsAreEncounterScopedCappedAndAuditedPerRecord() {
        DoctorQueueEntry entry = queueEntry(); PatientVisit current = entry.getPatientVisit(); Patient patient = current.getPatient();
        PatientVisit historical = mock(PatientVisit.class); when(historical.getId()).thenReturn(98L);
        when(queue.findAssigned(7L, 41L)).thenReturn(Optional.of(entry)); when(visits.findRelevantClinicalHistory(eq(55L), eq(70L), any(), any(PageRequest.class))).thenReturn(List.of(historical));
        assertEquals(1, service.relevantHistory(jwt, 7L).size());
        verify(visits).findRelevantClinicalHistory(eq(55L), eq(70L), any(), eq(PageRequest.of(0, 20)));
        verify(historyAudit).save(argThat(event -> event.getDoctorId().equals(41L) && event.getPatientId().equals(55L)
                && event.getEncounterId().equals(70L) && event.getAccessedResourceId().equals(98L)));
        verify(queue).findAssigned(7L, 41L);
    }

    @Test void historyAccessWithNoRelevantRecordsStillCreatesAccessAudit() {
        DoctorQueueEntry entry = queueEntry(); when(queue.findAssigned(7L, 41L)).thenReturn(Optional.of(entry));
        when(visits.findRelevantClinicalHistory(eq(55L), eq(70L), any(), any(PageRequest.class))).thenReturn(List.of());
        assertTrue(service.relevantHistory(jwt, 7L).isEmpty());
        verify(historyAudit).save(argThat(event -> event.getAccessedResource().equals("PATIENT_HISTORY_EMPTY") && event.getAccessedResourceId() == null));
    }

    @Test void signedNursingIntakeIsReturnedWithNurseAndHandoffMetadata() {
        DoctorQueueEntry entry = queueEntry(); when(queue.findAssigned(7L, 41L)).thenReturn(Optional.of(entry));
        NursingQueueEntry nq = mock(NursingQueueEntry.class); when(nq.getId()).thenReturn(8L);
        NursingIntakeRecord record = new NursingIntakeRecord(nq, 12L, 2,
                new NursingIntakeRequest(120, 80, 72, null, 16, 98, null, null, "Penicillin", "None", "Stable"),
                null, "HANDED_OFF", LocalDateTime.now());
        when(intakes.findTopByQueueEntry_IdAndRecordStateOrderByRevisionDesc(7L, "HANDED_OFF")).thenReturn(Optional.of(record));
        StaffAccount nurse = mock(StaffAccount.class); when(nurse.getFullName()).thenReturn("Nurse One"); when(staff.findById(12L)).thenReturn(Optional.of(nurse));
        DoctorNursingIntakeResponse response = service.nursingIntake(jwt, 7L);
        assertEquals("Nurse One", response.nurseName()); assertEquals("Penicillin", response.intake().allergies()); assertNotNull(response.handoffAt());
    }

    @Test void followUpMovesToInProgressOnlyWhenAssignedDoctorStartsConsultation() {
        DoctorQueueEntry entry = queueEntry(); PatientVisit visit = entry.getPatientVisit();
        PatientFollowUp followUp = new PatientFollowUp(); followUp.setId(91L); followUp.setStatus("SCHEDULED");
        when(visit.getSourceFollowUp()).thenReturn(followUp);
        when(queue.findAssignedForUpdate(7L, 41L)).thenReturn(Optional.of(entry)); when(queue.save(entry)).thenReturn(entry);
        when(followUps.findByIdForUpdate(91L)).thenReturn(Optional.of(followUp)); when(followUps.save(followUp)).thenReturn(followUp);
        service.start(jwt, 7L);
        assertEquals("IN_PROGRESS", followUp.getStatus()); assertNotNull(followUp.getStartedAt());
        verify(followUpAudit).save(argThat(event -> event.getActionType().equals("CONSULTATION_STARTED")
                && event.getPreviousStatus().equals("SCHEDULED") && event.getNewStatus().equals("IN_PROGRESS")
                && event.getActorId().equals(41L)));
    }

    @Test void followUpCompletesInTheSameSuccessfulConsultationSigningTransaction() {
        DoctorQueueEntry entry = queueEntry(); PatientVisit visit = entry.getPatientVisit();
        when(entry.getStatus()).thenReturn("IN_CONSULTATION"); when(visit.getDiagnosis()).thenReturn("Diagnosis");
        PatientFollowUp followUp = new PatientFollowUp(); followUp.setId(92L); followUp.setStatus("IN_PROGRESS");
        when(visit.getSourceFollowUp()).thenReturn(followUp);
        when(queue.findAssignedForUpdate(7L, 41L)).thenReturn(Optional.of(entry)); when(queue.save(entry)).thenReturn(entry);
        when(followUps.findByIdForUpdate(92L)).thenReturn(Optional.of(followUp)); when(followUps.save(followUp)).thenReturn(followUp);
        service.sign(jwt, 7L);
        assertEquals("COMPLETED", followUp.getStatus()); assertNotNull(followUp.getCompletedAt());
        verify(followUpAudit).save(argThat(event -> event.getActionType().equals("CONSULTATION_SIGNED")
                && event.getPreviousStatus().equals("IN_PROGRESS") && event.getNewStatus().equals("COMPLETED")
                && event.getActorId().equals(41L)));
    }

    @Test void followUpCannotBeCompletedBeforeConsultationHasStarted() {
        PatientFollowUp followUp = new PatientFollowUp(); followUp.setId(93L); followUp.setStatus("SCHEDULED");
        assertThrows(IllegalStateException.class, () -> followUp.markCompleted(LocalDateTime.now()));
        assertEquals("SCHEDULED", followUp.getStatus());
    }

    @Test void consultationDraftUsesExistingVisitFieldsAndSigningMakesItImmutable() {
        DoctorQueueEntry entry = queueEntry(); when(entry.getStatus()).thenReturn("IN_CONSULTATION", "IN_CONSULTATION", "CONSULTATION_SIGNED");
        PatientVisit visit = entry.getPatientVisit(); when(visit.getConsultationStatus()).thenReturn(null, "SIGNED"); when(visit.getDiagnosis()).thenReturn("Hypertension");
        when(queue.findAssignedForUpdate(7L, 41L)).thenReturn(Optional.of(entry)); when(queue.save(entry)).thenReturn(entry);
        ConsultationDraftRequest draft = new ConsultationDraftRequest("Headache", "Two days", "Normal", null, null, null, null,
                "Hypertension", "Stable", "Review", "Follow-up advised");
        service.saveDraft(jwt, 7L, draft);
        service.sign(jwt, 7L);
        verify(visit).setConsultationStatus("SIGNED"); verify(visit).setConsultationSignedAt(any());
        verify(visit).setEncounterStatus("CONSULTATION_SIGNED"); verify(visit).setCheckoutStatus("PENDING");
        assertThrows(ResponseStatusException.class, () -> service.saveDraft(jwt, 7L, draft));
        verify(audit).save(argThat(event -> event.getActionType().equals("CONSULTATION_SIGNED")));
    }

    @Test void signedConsultationCorrectionsAreAppendOnlyAddenda() {
        DoctorQueueEntry entry = queueEntry(); when(entry.getStatus()).thenReturn("CONSULTATION_SIGNED");
        when(queue.findAssignedForUpdate(7L, 41L)).thenReturn(Optional.of(entry));
        when(addenda.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        ConsultationAddendumResponse saved = service.addendum(jwt, 7L, new ConsultationAddendumRequest("Clarified medication instructions."));
        assertEquals("Clarified medication instructions.", saved.text());
        verify(addenda).save(any(ConsultationAddendum.class));
        verify(audit).save(argThat(event -> event.getActionType().equals("CONSULTATION_ADDENDUM_ADDED")));
    }

    private DoctorQueueEntry queueEntry() {
        DoctorQueueEntry entry = mock(DoctorQueueEntry.class); when(entry.getId()).thenReturn(7L); when(entry.getCreatedAt()).thenReturn(LocalDateTime.now().minusMinutes(6));
        when(entry.getStatus()).thenReturn("WAITING");
        PatientVisit visit = mock(PatientVisit.class); Patient patient = mock(Patient.class);
        when(entry.getPatientVisit()).thenReturn(visit); when(visit.getId()).thenReturn(70L); when(visit.getPatient()).thenReturn(patient);
        when(visit.getCreatedAt()).thenReturn(LocalDateTime.now()); when(patient.getId()).thenReturn(55L); when(patient.getFullName()).thenReturn("Patient");
        when(patient.getAge()).thenReturn(30); when(patient.getGender()).thenReturn("Other"); when(patient.getPhone()).thenReturn("9000000000"); return entry;
    }

    private DoctorQueueEntry realQueueEntry() {
        PatientVisit visit = mock(PatientVisit.class); Patient patient = mock(Patient.class);
        when(visit.getId()).thenReturn(70L); when(visit.getPatient()).thenReturn(patient); when(visit.getEmergencyNote()).thenReturn(null);
        when(patient.getId()).thenReturn(55L); when(patient.getFullName()).thenReturn("Patient"); when(patient.getAge()).thenReturn(30);
        when(patient.getGender()).thenReturn("Other"); when(patient.getPhone()).thenReturn("9000000000");
        return new DoctorQueueEntry(mock(NursingQueueEntry.class), visit, doctor, LocalDateTime.now().minusMinutes(6));
    }
}
