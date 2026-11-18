package com.gbkhealthcare.patient;

import com.gbkhealthcare.auth.Doctor;
import com.gbkhealthcare.auth.StaffAccount;
import com.gbkhealthcare.auth.StaffAccessService;
import com.gbkhealthcare.auth.StaffRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import jakarta.validation.Validation;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NursingServiceTest {
    private NursingQueueEntryRepository queue;
    private NursingIntakeRecordRepository intakes;
    private NursingIntakeAuditEventRepository audit;
    private DoctorQueueEntryRepository doctorQueue;
    private NursingService service;
    private StaffAccount nurse;

    @BeforeEach void setUp() {
        queue = mock(NursingQueueEntryRepository.class); intakes = mock(NursingIntakeRecordRepository.class);
        audit = mock(NursingIntakeAuditEventRepository.class); doctorQueue = mock(DoctorQueueEntryRepository.class);
        service = new NursingService(queue, intakes, audit, doctorQueue); nurse = mock(StaffAccount.class); when(nurse.getId()).thenReturn(12L);
    }

    @Test void queueRetrievalUsesSharedEmergencyOrderedRepositoryQuery() {
        when(queue.findActiveQueue()).thenReturn(List.of());
        assertTrue(service.activeQueue().isEmpty());
        verify(queue).findActiveQueue();
    }

    @Test void claimUsesLockedRowAndPreventsSecondClaim() {
        NursingQueueEntry entry = responseQueueEntry();
        when(entry.getStatus()).thenReturn("WAITING", "CLAIMED", "CLAIMED");
        when(queue.findByIdForUpdate(9L)).thenReturn(Optional.of(entry));
        assertEquals("CLAIMED", service.claim(9L, nurse).status());
        assertThrows(ResponseStatusException.class, () -> service.claim(9L, nurse));
        verify(queue, times(2)).findByIdForUpdate(9L);
        verify(queue).save(entry);
        verify(audit).save(argThat(event -> event.getActionType().equals("CLAIMED") && event.getActorStaffId().equals(12L)));
    }

    @Test void bmiIsCalculatedFromHeightAndWeightAndUnavailableValuesStayNull() {
        NursingIntakeRequest request = request(new BigDecimal("170"), new BigDecimal("70"));
        assertEquals(new BigDecimal("24.22"), NursingService.calculateBmi(request));
        assertNull(NursingService.calculateBmi(request(null, null)));
        assertNull(NursingService.calculateBmi(request(new BigDecimal("170"), null)));
    }

    @Test void clinicalValuesAreValidatedWhileUnavailableMeasurementsMayRemainNull() {
        var validator = Validation.buildDefaultValidatorFactory().getValidator();
        assertTrue(validator.validate(request(null, null)).isEmpty());
        NursingIntakeRequest invalid = new NursingIntakeRequest(70, 90, 300, new BigDecimal("50"), 2, 120,
                new BigDecimal("10"), new BigDecimal("700"), "", "", "");
        assertFalse(validator.validate(invalid).isEmpty());
    }

    @Test void oxygenSaturationMustBeAtLeastOneAndBloodPressureMustBeConsistent() {
        var validator = Validation.buildDefaultValidatorFactory().getValidator();
        NursingIntakeRequest zeroOxygen = new NursingIntakeRequest(120, 80, 70, new BigDecimal("36.5"), 16, 0,
                null, null, "", "", "");
        var oxygenViolations = validator.validate(zeroOxygen);
        assertEquals(1, oxygenViolations.size());
        assertEquals("oxygenSaturation", oxygenViolations.iterator().next().getPropertyPath().toString());
        assertTrue(validator.validate(new NursingIntakeRequest(120, 80, 70, new BigDecimal("36.5"), 16, 1,
                null, null, "", "", "")).isEmpty());

        NursingIntakeRequest inconsistentPressure = new NursingIntakeRequest(80, 80, 70, new BigDecimal("36.5"), 16, 98,
                null, null, "", "", "");
        var pressureViolations = validator.validate(inconsistentPressure);
        assertEquals(1, pressureViolations.size());
        assertEquals("bloodPressureConsistent", pressureViolations.iterator().next().getPropertyPath().toString());
    }

    @Test void controllerRequiresActiveNurseRole() {
        StaffAccessService access = mock(StaffAccessService.class);
        NursingController controller = new NursingController(access, service);
        org.springframework.security.oauth2.jwt.Jwt jwt = mock(org.springframework.security.oauth2.jwt.Jwt.class);
        when(access.requireRole(jwt, StaffRole.NURSE)).thenReturn(nurse);
        when(queue.findActiveQueue()).thenReturn(List.of());
        assertTrue(controller.queue(jwt).isEmpty());
        verify(access).requireRole(jwt, StaffRole.NURSE);
        when(access.requireRole(jwt, StaffRole.NURSE)).thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN));
        assertThrows(ResponseStatusException.class, () -> controller.queue(jwt));
    }

    @Test void saveIntakePersistsRevisionAndAuditWithoutChangingVisit() {
        NursingQueueEntry entry = claimedQueue();
        when(queue.findByIdForUpdate(9L)).thenReturn(Optional.of(entry));
        when(intakes.findMaxRevision(9L)).thenReturn(2);
        when(intakes.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        NursingIntakeResponse saved = service.saveIntake(9L, nurse, request(new BigDecimal("170"), new BigDecimal("70")));
        assertEquals(3, saved.revision()); assertEquals("DRAFT", saved.recordState()); assertEquals(new BigDecimal("24.22"), saved.bmi());
        verify(intakes).save(any(NursingIntakeRecord.class)); verify(audit).save(argThat(event -> event.getActionType().equals("INTAKE_SAVED")));
        verifyNoInteractions(doctorQueue);
    }

    @Test void handoffCreatesSignedSnapshotDoctorQueueAndAuditAtomically() {
        NursingQueueEntry entry = claimedQueue(); PatientVisit visit = mock(PatientVisit.class); Doctor doctor = mock(Doctor.class);
        when(entry.getVisit()).thenReturn(visit); when(visit.getAssignedDoctor()).thenReturn(doctor); when(visit.getId()).thenReturn(31L); when(doctor.getId()).thenReturn(4L);
        when(queue.findByIdForUpdate(9L)).thenReturn(Optional.of(entry));
        NursingIntakeRecord latest = mock(NursingIntakeRecord.class);
        when(intakes.findTopByQueueEntry_IdOrderByRevisionDesc(9L)).thenReturn(Optional.of(latest));
        when(intakes.findMaxRevision(9L)).thenReturn(1);
        when(intakes.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(doctorQueue.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        NursingHandoffResponse response = service.handoff(9L, nurse);
        assertEquals(31L, response.patientVisitId()); assertEquals(4L, response.assignedDoctorId()); assertEquals("WAITING", response.status());
        verify(queue).save(entry); verify(doctorQueue).save(any(DoctorQueueEntry.class));
        verify(audit).save(argThat(event -> event.getActionType().equals("HANDED_OFF")));
    }

    @Test void anotherNurseCannotCompleteClaimedIntake() {
        NursingQueueEntry entry = claimedQueue(); when(entry.getClaimedByStaffId()).thenReturn(88L);
        when(queue.findByIdForUpdate(9L)).thenReturn(Optional.of(entry));
        assertThrows(ResponseStatusException.class, () -> service.saveIntake(9L, nurse, request(null, null)));
        verifyNoInteractions(intakes);
    }

    @Test void auditHistoryIsReadableByTheClaimingNurseAndContainsOnlyAuditMetadata() {
        NursingQueueEntry entry = claimedQueue(); when(queue.findById(9L)).thenReturn(Optional.of(entry));
        NursingIntakeAuditEvent event = new NursingIntakeAuditEvent(9L, 33L, 12L, "INTAKE_SAVED", 1, LocalDateTime.now());
        when(audit.findAllByQueueEntryIdOrderByOccurredAtAscIdAsc(9L)).thenReturn(List.of(event));
        List<NursingAuditEventResponse> events = service.auditHistory(9L, nurse);
        assertEquals(1, events.size()); assertEquals("INTAKE_SAVED", events.get(0).actionType());
        assertEquals(12L, events.get(0).actorStaffId());
        verify(audit).findAllByQueueEntryIdOrderByOccurredAtAscIdAsc(9L);
    }

    @Test void concurrencyAndAtomicityAreBackedByPessimisticLockAndTransactions() throws Exception {
        var lock = NursingQueueEntryRepository.class.getMethod("findByIdForUpdate", Long.class).getAnnotation(org.springframework.data.jpa.repository.Lock.class);
        assertNotNull(lock); assertEquals(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE, lock.value());
        assertNotNull(NursingService.class.getMethod("claim", Long.class, StaffAccount.class).getAnnotation(org.springframework.transaction.annotation.Transactional.class));
        assertNotNull(NursingService.class.getMethod("saveIntake", Long.class, StaffAccount.class, NursingIntakeRequest.class).getAnnotation(org.springframework.transaction.annotation.Transactional.class));
        assertNotNull(NursingService.class.getMethod("handoff", Long.class, StaffAccount.class).getAnnotation(org.springframework.transaction.annotation.Transactional.class));
    }

    private NursingQueueEntry claimedQueue() {
        NursingQueueEntry entry = mock(NursingQueueEntry.class); when(entry.getId()).thenReturn(9L);
        when(entry.getStatus()).thenReturn("CLAIMED"); when(entry.getClaimedByStaffId()).thenReturn(12L);
        PatientVisit visit = mock(PatientVisit.class); when(entry.getVisit()).thenReturn(visit); return entry;
    }
    private NursingQueueEntry responseQueueEntry() {
        NursingQueueEntry entry = claimedQueue(); PatientVisit visit = mock(PatientVisit.class); Patient patient = mock(Patient.class); Doctor doctor = mock(Doctor.class);
        when(entry.getVisit()).thenReturn(visit); when(entry.getCreatedAt()).thenReturn(LocalDateTime.now().minusMinutes(2));
        when(visit.getId()).thenReturn(31L); when(visit.getPatient()).thenReturn(patient); when(visit.getAssignedDoctor()).thenReturn(doctor);
        when(patient.getId()).thenReturn(54L); when(patient.getFullName()).thenReturn("Test Patient"); when(patient.getAge()).thenReturn(42); when(patient.getGender()).thenReturn("Other");
        when(patient.getPhone()).thenReturn("9000000000");
        when(doctor.getId()).thenReturn(4L); when(doctor.getFullName()).thenReturn("Assigned Doctor"); return entry;
    }
    private static NursingIntakeRequest request(BigDecimal height, BigDecimal weight) {
        return new NursingIntakeRequest(120, 80, 70, new BigDecimal("36.5"), 16, 98, height, weight, "", "", "");
    }
}
