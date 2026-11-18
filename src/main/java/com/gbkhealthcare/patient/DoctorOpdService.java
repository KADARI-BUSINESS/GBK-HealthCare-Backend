package com.gbkhealthcare.patient;

import com.gbkhealthcare.auth.Doctor;
import com.gbkhealthcare.auth.DoctorRepository;
import com.gbkhealthcare.auth.StaffAccount;
import com.gbkhealthcare.auth.StaffAccountRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class DoctorOpdService {
    private static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Kolkata");
    private static final int HISTORY_LIMIT = 20;
    private final DoctorRepository doctors;
    private final DoctorQueueEntryRepository queue;
    private final PatientVisitRepository visits;
    private final NursingIntakeRecordRepository intakes;
    private final StaffAccountRepository staff;
    private final PatientHistoryAccessEventRepository historyAudit;
    private final DoctorOpdAuditEventRepository audit;
    private final ConsultationAddendumRepository addenda;
    private final PatientFollowUpRepository followUps;
    private final PatientFollowUpAuditEventRepository followUpAudit;

    public DoctorOpdService(DoctorRepository doctors, DoctorQueueEntryRepository queue, PatientVisitRepository visits,
            NursingIntakeRecordRepository intakes, StaffAccountRepository staff,
            PatientHistoryAccessEventRepository historyAudit, DoctorOpdAuditEventRepository audit,
            ConsultationAddendumRepository addenda, PatientFollowUpRepository followUps,
            PatientFollowUpAuditEventRepository followUpAudit) {
        this.doctors = doctors; this.queue = queue; this.visits = visits; this.intakes = intakes;
        this.staff = staff; this.historyAudit = historyAudit; this.audit = audit; this.addenda = addenda;
        this.followUps = followUps; this.followUpAudit = followUpAudit;
    }

    @Transactional(readOnly = true)
    public List<DoctorOpdQueueItemResponse> queue(Jwt jwt) {
        Doctor doctor = requireDoctor(jwt); LocalDateTime now = now();
        return queue.findActiveForDoctor(doctor.getId()).stream().map(q -> DoctorOpdQueueItemResponse.from(q, now)).toList();
    }

    @Transactional
    public DoctorOpdQueueItemResponse claim(Jwt jwt, Long queueId) {
        Doctor doctor = requireDoctor(jwt); DoctorQueueEntry entry = lock(queueId, doctor.getId()); LocalDateTime at = now();
        transition(() -> entry.claim(at)); entry.getPatientVisit().setEncounterStatus("DOCTOR_CLAIMED"); saveAudit(doctor, entry, "QUEUE_CLAIMED", at);
        return DoctorOpdQueueItemResponse.from(queue.save(entry), at);
    }

    @Transactional
    public DoctorOpdQueueItemResponse start(Jwt jwt, Long queueId) {
        Doctor doctor = requireDoctor(jwt); DoctorQueueEntry entry = lock(queueId, doctor.getId()); LocalDateTime at = now();
        PatientVisit visit = entry.getPatientVisit();
        if (visit.getSourceFollowUp() != null) {
            PatientFollowUp followUp = followUps.findByIdForUpdate(visit.getSourceFollowUp().getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Linked follow-up is unavailable."));
            String previous = followUp.getStatus();
            try { followUp.markInProgress(at); }
            catch (IllegalStateException ex) { throw conflict(ex.getMessage()); }
            followUps.save(followUp);
            followUpAudit.save(new PatientFollowUpAuditEvent(followUp.getId(), previous, "IN_PROGRESS",
                    "CONSULTATION_STARTED", "DOCTOR", doctor.getId(), null, visit.getId(), at));
        }
        transition(() -> entry.start(at)); entry.getPatientVisit().setEncounterStatus("IN_CONSULTATION");
        entry.getPatientVisit().setConsultationStatus("DRAFT"); saveAudit(doctor, entry, "CONSULTATION_STARTED", at);
        return DoctorOpdQueueItemResponse.from(queue.save(entry), at);
    }

    @Transactional
    public DoctorOpdQueueItemResponse hold(Jwt jwt, Long queueId) {
        Doctor doctor = requireDoctor(jwt); DoctorQueueEntry entry = lock(queueId, doctor.getId()); LocalDateTime at = now();
        transition(() -> entry.hold(at)); entry.getPatientVisit().setEncounterStatus("ON_HOLD");
        saveAudit(doctor, entry, "CONSULTATION_ON_HOLD", at);
        return DoctorOpdQueueItemResponse.from(queue.save(entry), at);
    }

    @Transactional
    public DoctorOpdQueueItemResponse resume(Jwt jwt, Long queueId) {
        Doctor doctor = requireDoctor(jwt); DoctorQueueEntry entry = lock(queueId, doctor.getId()); LocalDateTime at = now();
        transition(entry::resume); entry.getPatientVisit().setEncounterStatus("IN_CONSULTATION");
        saveAudit(doctor, entry, "CONSULTATION_RESUMED", at);
        return DoctorOpdQueueItemResponse.from(queue.save(entry), at);
    }

    @Transactional(readOnly = true)
    public DoctorNursingIntakeResponse nursingIntake(Jwt jwt, Long queueId) {
        Doctor doctor = requireDoctor(jwt); DoctorQueueEntry entry = assigned(queueId, doctor.getId());
        NursingIntakeRecord record = intakes.findTopByQueueEntry_IdAndRecordStateOrderByRevisionDesc(queueId, "HANDED_OFF")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Signed nursing handoff not found."));
        StaffAccount nurse = staff.findById(record.getNurseStaffId()).orElse(null);
        return new DoctorNursingIntakeResponse(NursingIntakeResponse.from(record), record.getNurseStaffId(),
                nurse == null ? "Staff account unavailable" : nurse.getFullName(), record.getSignedAt());
    }

    @Transactional
    public List<RelevantHistoryItemResponse> relevantHistory(Jwt jwt, Long queueId) {
        Doctor doctor = requireDoctor(jwt); DoctorQueueEntry entry = assigned(queueId, doctor.getId());
        PatientVisit current = entry.getPatientVisit();
        List<PatientVisit> records = visits.findRelevantClinicalHistory(current.getPatient().getId(), current.getId(),
                current.getCreatedAt(), PageRequest.of(0, HISTORY_LIMIT));
        LocalDateTime at = now();
        if (records.isEmpty()) {
            historyAudit.save(new PatientHistoryAccessEvent(doctor.getId(), current.getPatient().getId(), current.getId(), "PATIENT_HISTORY_EMPTY", null, at));
        } else {
            records.forEach(record -> historyAudit.save(new PatientHistoryAccessEvent(doctor.getId(), current.getPatient().getId(),
                    current.getId(), "PATIENT_VISIT_CLINICAL_HISTORY", record.getId(), at)));
        }
        return records.stream().map(RelevantHistoryItemResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public DoctorConsultationResponse consultation(Jwt jwt, Long queueId) {
        Doctor doctor = requireDoctor(jwt); return DoctorConsultationResponse.from(assigned(queueId, doctor.getId()));
    }

    @Transactional
    public DoctorConsultationResponse saveDraft(Jwt jwt, Long queueId, ConsultationDraftRequest request) {
        Doctor doctor = requireDoctor(jwt); DoctorQueueEntry entry = lock(queueId, doctor.getId());
        if (!List.of("IN_CONSULTATION", "ON_HOLD").contains(entry.getStatus())) throw conflict("Start the consultation before saving a draft.");
        PatientVisit visit = entry.getPatientVisit();
        if ("SIGNED".equals(visit.getConsultationStatus())) throw conflict("Signed consultation records are immutable. Use an addendum for corrections.");
        visit.setChiefComplaints(clean(request.chiefComplaints())); visit.setPresentIllness(clean(request.presentIllness()));
        visit.setGeneralExamination(clean(request.generalExamination())); visit.setHeartExamination(clean(request.heartExamination()));
        visit.setLungsExamination(clean(request.lungsExamination())); visit.setAbdomenExamination(clean(request.abdomenExamination()));
        visit.setRectalExamination(clean(request.rectalExamination())); visit.setDiagnosis(clean(request.diagnosis()));
        visit.setAssessment(clean(request.assessment())); visit.setTreatmentPlan(clean(request.treatmentPlan()));
        visit.setDoctorNotes(clean(request.doctorNotes())); visit.setConsultationStatus("DRAFT");
        saveAudit(doctor, entry, "CONSULTATION_DRAFT_SAVED", now());
        return DoctorConsultationResponse.from(entry);
    }

    @Transactional
    public DoctorConsultationResponse sign(Jwt jwt, Long queueId) {
        Doctor doctor = requireDoctor(jwt); DoctorQueueEntry entry = lock(queueId, doctor.getId());
        PatientVisit visit = entry.getPatientVisit();
        if (!"IN_CONSULTATION".equals(entry.getStatus())) throw conflict("Only an active consultation can be signed.");
        if (visit.getDiagnosis() == null || visit.getDiagnosis().isBlank()) throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "A diagnosis is required before signing.");
        LocalDateTime at = now(); transition(() -> entry.sign(at));
        visit.setConsultationStatus("SIGNED"); visit.setConsultationSignedAt(at);
        visit.setEncounterStatus("CONSULTATION_SIGNED"); visit.setCheckoutStatus("PENDING");
        if (visit.getSourceFollowUp() != null) {
            PatientFollowUp followUp = followUps.findByIdForUpdate(visit.getSourceFollowUp().getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Linked follow-up is unavailable."));
            String previous = followUp.getStatus();
            try { followUp.markCompleted(at); }
            catch (IllegalStateException ex) { throw conflict(ex.getMessage()); }
            followUps.save(followUp);
            followUpAudit.save(new PatientFollowUpAuditEvent(followUp.getId(), previous, "COMPLETED",
                    "CONSULTATION_SIGNED", "DOCTOR", doctor.getId(), null, visit.getId(), at));
        }
        saveAudit(doctor, entry, "CONSULTATION_SIGNED", at); queue.save(entry);
        return DoctorConsultationResponse.from(entry);
    }

    @Transactional(readOnly = true)
    public List<ConsultationAddendumResponse> addenda(Jwt jwt, Long queueId) {
        Doctor doctor = requireDoctor(jwt); DoctorQueueEntry entry = assigned(queueId, doctor.getId());
        return addenda.findAllByPatientVisit_IdOrderByCreatedAtAscIdAsc(entry.getPatientVisit().getId())
                .stream().map(ConsultationAddendumResponse::from).toList();
    }

    @Transactional
    public ConsultationAddendumResponse addendum(Jwt jwt, Long queueId, ConsultationAddendumRequest request) {
        Doctor doctor = requireDoctor(jwt); DoctorQueueEntry entry = lock(queueId, doctor.getId());
        if (!"CONSULTATION_SIGNED".equals(entry.getStatus())) throw conflict("Addenda are only allowed after consultation signing.");
        LocalDateTime at = now();
        ConsultationAddendum saved = addenda.save(new ConsultationAddendum(entry.getPatientVisit(), doctor.getId(), request.text().trim(), at));
        saveAudit(doctor, entry, "CONSULTATION_ADDENDUM_ADDED", at);
        return ConsultationAddendumResponse.from(saved);
    }

    private Doctor requireDoctor(Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Doctor authentication is required.");
        Long id = null;
        try {
            Object claim = jwt.getClaims().get("doctorId");
            if (claim instanceof Number number) id = new BigDecimal(number.toString()).longValueExact();
        } catch (RuntimeException ignored) { /* invalid subject claims are rejected below */ }
        if (id == null || id < 1) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "A valid Doctor identity is required.");
        return doctors.findById(id).filter(Doctor::isEnabled)
                .filter(d -> d.getApprovalStatus() == Doctor.ApprovalStatus.APPROVED)
                .filter(d -> d.getEmail().equalsIgnoreCase(jwt.getSubject()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Active approved Doctor account not found."));
    }

    private DoctorQueueEntry assigned(Long queueId, Long doctorId) {
        return queue.findAssigned(queueId, doctorId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assigned Doctor encounter not found."));
    }
    private DoctorQueueEntry lock(Long queueId, Long doctorId) {
        return queue.findAssignedForUpdate(queueId, doctorId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assigned Doctor encounter not found."));
    }
    private void saveAudit(Doctor doctor, DoctorQueueEntry entry, String action, LocalDateTime at) {
        audit.save(new DoctorOpdAuditEvent(doctor.getId(), entry.getPatientVisit().getId(), entry.getId(), action, at));
    }
    private static void transition(Runnable action) {
        try { action.run(); } catch (IllegalStateException ex) { throw conflict(ex.getMessage()); }
    }
    private static String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private static LocalDateTime now() { return LocalDateTime.now(CLINIC_ZONE); }
    private static ResponseStatusException conflict(String message) { return new ResponseStatusException(HttpStatus.CONFLICT, message); }
}
