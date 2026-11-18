package com.gbkhealthcare.patient;

import com.gbkhealthcare.auth.Doctor;
import com.gbkhealthcare.auth.DoctorRepository;
import com.gbkhealthcare.auth.StaffAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ReceptionServiceTest {
    private final PatientRepository patients = mock(PatientRepository.class);
    private final PatientVisitRepository visits = mock(PatientVisitRepository.class);
    private final PatientFollowUpRepository followUps = mock(PatientFollowUpRepository.class);
    private final DoctorRepository doctors = mock(DoctorRepository.class);
    private final NursingQueueEntryRepository queue = mock(NursingQueueEntryRepository.class);
    private final PatientFollowUpAuditEventRepository followUpAudit = mock(PatientFollowUpAuditEventRepository.class);
    private final ReceptionService service = new ReceptionService(patients, visits, followUps, doctors, queue, followUpAudit);
    private final StaffAccount actor = mock(StaffAccount.class);
    private Doctor doctor;

    @BeforeEach void setUp() {
        when(actor.getId()).thenReturn(33L);
        doctor = new Doctor(); doctor.setId(7L); doctor.setFullName("Approved Doctor");
    }

    @Test void patientSearchSupportsNumericIdAndCapsResults() {
        Patient patient = patient(5L);
        when(patients.searchForReception(eq(5L), eq("5"), any(PageRequest.class))).thenReturn(List.of(patient));
        assertEquals(5L, service.searchPatients(" 5 ").get(0).patientId());
        verify(patients).searchForReception(eq(5L), eq("5"), eq(PageRequest.of(0, 25)));
    }

    @Test void newPatientIsRegisteredAndCheckedInWithoutChangingDoctorOwnership() {
        when(visits.findByCheckInRequestKey("00000000-0000-4000-8000-000000000001")).thenReturn(Optional.empty());
        when(doctors.findById(7L)).thenReturn(Optional.of(doctor));
        when(patients.saveAndFlush(any(Patient.class))).thenAnswer(invocation -> {
            Patient saved = invocation.getArgument(0); saved.setId(12L); return saved;
        });
        when(visits.findMaxVisitNumberByPatientId(12L)).thenReturn(0);
        when(visits.saveAndFlush(any(PatientVisit.class))).thenAnswer(invocation -> {
            PatientVisit saved = invocation.getArgument(0); saved.setId(52L); return saved;
        });
        when(queue.saveAndFlush(any(NursingQueueEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReceptionCheckInResponse result = service.checkIn(newPatientRequest(), actor);
        assertEquals(12L, result.patientId()); assertEquals(52L, result.visitId());
        assertEquals(1, result.visitNumber()); assertEquals("Approved Doctor", result.assignedDoctorName());
        assertEquals("WAITING", result.nursingQueueStatus());
        var patientCaptor = org.mockito.ArgumentCaptor.forClass(Patient.class);
        verify(patients).saveAndFlush(patientCaptor.capture());
        assertNull(patientCaptor.getValue().getDoctorAccount(), "Reception must not assign legacy Patient.doctorAccount ownership");
        assertEquals("Approved Doctor", patientCaptor.getValue().getDoctor());
        verify(queue).saveAndFlush(any(NursingQueueEntry.class));
    }

    @Test void existingPatientGetsOneVisitAndOneNursingEntry() {
        Patient patient = patient(12L);
        when(visits.findByCheckInRequestKey("00000000-0000-4000-8000-000000000002")).thenReturn(Optional.empty());
        when(doctors.findById(7L)).thenReturn(Optional.of(doctor));
        when(patients.findByIdForUpdate(12L)).thenReturn(Optional.of(patient));
        when(visits.findMaxVisitNumberByPatientId(12L)).thenReturn(2);
        when(visits.saveAndFlush(any(PatientVisit.class))).thenAnswer(invocation -> {
            PatientVisit saved = invocation.getArgument(0); saved.setId(53L); return saved;
        });
        when(queue.saveAndFlush(any(NursingQueueEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ReceptionCheckInResponse result = service.checkIn(existingRequest(12L, "00000000-0000-4000-8000-000000000002"), actor);
        assertEquals(3, result.visitNumber());
        verify(patients, never()).saveAndFlush(any());
        verify(visits, times(1)).saveAndFlush(any(PatientVisit.class));
        verify(queue, times(1)).saveAndFlush(any(NursingQueueEntry.class));
    }

    @Test void rejectsDisabledOrUnapprovedDoctorBeforeWritingPatientData() {
        Doctor disabled = new Doctor(); disabled.setId(8L); disabled.setEnabled(false);
        Doctor rejected = new Doctor(); rejected.setId(9L); rejected.setApprovalStatus(Doctor.ApprovalStatus.REJECTED);
        when(visits.findByCheckInRequestKey(anyString())).thenReturn(Optional.empty());
        when(doctors.findById(7L)).thenReturn(Optional.of(disabled), Optional.of(rejected));
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.checkIn(newPatientRequest(), actor));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, error.getStatusCode());
        ReceptionCheckInRequest rejectedDoctorRequest = new ReceptionCheckInRequest(EncounterType.NEW, 7L,
                "00000000-0000-4000-8000-000000000009", null, newPatientRequest().newPatient(), null, false, null);
        ResponseStatusException rejectedError = assertThrows(ResponseStatusException.class,
                () -> service.checkIn(rejectedDoctorRequest, actor));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, rejectedError.getStatusCode());
        verifyNoInteractions(patients, followUps, queue);
        verify(visits, never()).saveAndFlush(any());
    }

    @Test void followUpCheckInLinksScheduledSourceAndCreatesNursingEntry() {
        Patient patient = patient(12L);
        PatientVisit originalVisit = new PatientVisit(); originalVisit.setPatient(patient);
        PatientFollowUp followUp = new PatientFollowUp(); followUp.setId(91L); followUp.setVisit(originalVisit); followUp.setStatus("SCHEDULED");
        when(visits.findByCheckInRequestKey(anyString())).thenReturn(Optional.empty());
        when(doctors.findById(7L)).thenReturn(Optional.of(doctor));
        when(patients.findByIdForUpdate(12L)).thenReturn(Optional.of(patient));
        when(followUps.findByIdForReceptionCheckIn(91L)).thenReturn(Optional.of(followUp));
        when(visits.findBySourceFollowUp_Id(91L)).thenReturn(Optional.empty());
        when(visits.findMaxVisitNumberByPatientId(12L)).thenReturn(1);
        when(visits.saveAndFlush(any(PatientVisit.class))).thenAnswer(invocation -> {
            PatientVisit saved = invocation.getArgument(0); saved.setId(54L); return saved;
        });
        when(queue.saveAndFlush(any(NursingQueueEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ReceptionCheckInRequest request = new ReceptionCheckInRequest(EncounterType.FOLLOW_UP, 7L,
                "00000000-0000-4000-8000-000000000003", 12L, null, 91L, false, null);
        ReceptionCheckInResponse result = service.checkIn(request, actor);
        assertEquals(91L, result.sourceFollowUpId()); assertEquals(EncounterType.FOLLOW_UP, result.encounterType());
        assertEquals("SCHEDULED", followUp.getStatus(), "Reception check-in must not alter clinical follow-up status.");
        assertNotNull(followUp.getCheckedInAt());
        var captor = org.mockito.ArgumentCaptor.forClass(PatientVisit.class);
        verify(visits).saveAndFlush(captor.capture()); assertSame(followUp, captor.getValue().getSourceFollowUp());
        verify(followUpAudit).save(argThat(event -> event.getActionType().equals("CHECKED_IN")
                && event.getPreviousStatus().equals("SCHEDULED") && event.getNewStatus().equals("SCHEDULED")
                && event.getActorId().equals(33L) && event.getEncounterId().equals(54L)));
    }

    @Test void replayedRequestReturnsOriginalCheckInWithoutCreatingAnotherVisit() {
        Patient patient = patient(12L);
        PatientVisit prior = new PatientVisit(); prior.setId(52L); prior.setPatient(patient); prior.setAssignedDoctor(doctor);
        prior.setEncounterType(EncounterType.NEW); prior.setEmergencyPriority(false); prior.setCheckInRequestKey("00000000-0000-4000-8000-000000000004");
        NursingQueueEntry queueEntry = new NursingQueueEntry(prior);
        when(visits.findByCheckInRequestKey(prior.getCheckInRequestKey())).thenReturn(Optional.of(prior));
        when(queue.findByVisit_Id(52L)).thenReturn(Optional.of(queueEntry));
        ReceptionCheckInResponse result = service.checkIn(existingRequest(12L, prior.getCheckInRequestKey()), actor);
        assertEquals(52L, result.visitId()); verify(visits, never()).saveAndFlush(any()); verify(queue, never()).saveAndFlush(any());
    }

    @Test void receptionistMissedAndCancellationChangesRequireReasonAndRecordActorAudit() {
        PatientFollowUp followUp = new PatientFollowUp(); followUp.setId(94L); followUp.setVisit(new PatientVisit());
        followUp.getVisit().setPatient(patient(12L)); followUp.setFollowUpDate(LocalDate.now().minusDays(1));
        when(followUps.findByIdForUpdate(94L)).thenReturn(Optional.of(followUp)); when(followUps.save(followUp)).thenReturn(followUp);
        var missed = service.markFollowUpMissed(94L, actor, "Patient did not arrive");
        assertEquals("MISSED", missed.status()); assertEquals("Patient did not arrive", followUp.getMissedReason());
        verify(followUpAudit).save(argThat(event -> event.getActionType().equals("MISSED")
                && event.getPreviousStatus().equals("SCHEDULED") && event.getActorId().equals(33L)));
    }

    @Test void checkedInFollowUpCannotBeCancelledOrMarkedMissed() {
        PatientVisit linked = new PatientVisit(); linked.setId(80L);
        PatientFollowUp followUp = new PatientFollowUp(); followUp.setId(95L); followUp.setVisit(new PatientVisit());
        followUp.setLinkedVisit(linked); when(followUps.findByIdForUpdate(95L)).thenReturn(Optional.of(followUp));
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> service.cancelFollowUp(95L, actor, "Patient request"));
        assertEquals("SCHEDULED", followUp.getStatus()); verify(followUps, never()).save(any());
    }

    @Test void queueFailureTriggersTransactionalRollback() {
        when(visits.findByCheckInRequestKey(anyString())).thenReturn(Optional.empty());
        when(doctors.findById(7L)).thenReturn(Optional.of(doctor));
        when(patients.saveAndFlush(any(Patient.class))).thenAnswer(invocation -> {
            Patient saved = invocation.getArgument(0); saved.setId(12L); return saved;
        });
        when(visits.findMaxVisitNumberByPatientId(12L)).thenReturn(1);
        when(visits.saveAndFlush(any(PatientVisit.class))).thenAnswer(invocation -> {
            PatientVisit saved = invocation.getArgument(0); saved.setId(55L); return saved;
        });
        when(queue.saveAndFlush(any(NursingQueueEntry.class))).thenThrow(new IllegalStateException("queue store failure"));
        RecordingTransactionManager transactionManager = new RecordingTransactionManager();
        ProxyFactory proxyFactory = new ProxyFactory(service);
        proxyFactory.setProxyTargetClass(true);
        proxyFactory.addAdvice(new TransactionInterceptor(transactionManager, new AnnotationTransactionAttributeSource()));
        ReceptionService transactionalService = (ReceptionService) proxyFactory.getProxy();
        assertThrows(IllegalStateException.class,
                () -> transactionalService.checkIn(newPatientRequest(), actor));
        verify(patients).saveAndFlush(any(Patient.class));
        verify(visits).saveAndFlush(any(PatientVisit.class)); verify(queue).saveAndFlush(any(NursingQueueEntry.class));
        assertEquals(1, transactionManager.rollbacks);
        assertEquals(0, transactionManager.commits);
    }

    private ReceptionCheckInRequest newPatientRequest() {
        PatientController.PatientRequest patient = new PatientController.PatientRequest();
        patient.fullName = "New Test Patient"; patient.age = 32; patient.gender = "Other";
        patient.phone = "9876543210"; patient.visitDate = LocalDate.of(2026, 11, 17);
        return new ReceptionCheckInRequest(EncounterType.NEW, 7L,
                "00000000-0000-4000-8000-000000000001", null, patient, null, true, "Urgent symptoms");
    }

    private ReceptionCheckInRequest existingRequest(Long patientId, String requestKey) {
        return new ReceptionCheckInRequest(EncounterType.NEW, 7L, requestKey, patientId, null, null, false, null);
    }

    private Patient patient(Long id) {
        Patient patient = new Patient(); patient.setId(id); patient.setFullName("Existing Patient"); patient.setAge(40);
        patient.setGender("Female"); patient.setPhone("9876543210"); patient.setVisitDate(LocalDate.of(2026, 1, 1));
        patient.setDoctor("Legacy Doctor"); return patient;
    }

    private static final class RecordingTransactionManager extends AbstractPlatformTransactionManager {
        private int rollbacks;
        private int commits;
        @Override protected Object doGetTransaction() { return new Object(); }
        @Override protected void doBegin(Object transaction, TransactionDefinition definition) {}
        @Override protected void doCommit(DefaultTransactionStatus status) { commits++; }
        @Override protected void doRollback(DefaultTransactionStatus status) { rollbacks++; }
    }
}
