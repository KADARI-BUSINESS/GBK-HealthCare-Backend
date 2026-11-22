package com.gbkhealthcare.patient;

import com.gbkhealthcare.auth.Doctor;
import com.gbkhealthcare.auth.DoctorRepository;
import com.gbkhealthcare.auth.StaffAccount;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class ReceptionService {
    private static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Kolkata");
    private static final int SEARCH_LIMIT = 25;

    private final PatientRepository patients;
    private final PatientVisitRepository visits;
    private final PatientFollowUpRepository followUps;
    private final DoctorRepository doctors;
    private final NursingQueueEntryRepository nursingQueue;
    private final PatientFollowUpAuditEventRepository followUpAudit;

    public ReceptionService(PatientRepository patients, PatientVisitRepository visits,
            PatientFollowUpRepository followUps, DoctorRepository doctors,
            NursingQueueEntryRepository nursingQueue, PatientFollowUpAuditEventRepository followUpAudit) {
        this.patients = patients;
        this.visits = visits;
        this.followUps = followUps;
        this.doctors = doctors;
        this.nursingQueue = nursingQueue;
        this.followUpAudit = followUpAudit;
    }

    @Transactional(readOnly = true)
    public List<ReceptionPatientSummary> searchPatients(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank() || rawQuery.trim().length() > 120) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Search must contain 1 to 120 characters.");
        }
        String query = rawQuery.trim();
        Long patientId = null;
        if (query.matches("[0-9]{1,18}")) {
            try { patientId = Long.valueOf(query); } catch (NumberFormatException ignored) { /* name/mobile search still applies */ }
        }
        return patients.searchForReception(patientId, query, PageRequest.of(0, SEARCH_LIMIT)).stream()
                .map(ReceptionPatientSummary::from).toList();
    }

    @Transactional(readOnly = true)
    public List<ReceptionDoctorOption> availableDoctors() {
        return doctors.findAllByApprovalStatusAndEnabledTrueOrderByFullNameAsc(Doctor.ApprovalStatus.APPROVED)
                .stream().map(ReceptionDoctorOption::from).toList();
    }

    @Transactional(readOnly = true)
    public List<ReceptionFollowUpOption> scheduledFollowUps(Long patientId) {
        if (patientId == null || patientId < 1 || !patients.existsById(patientId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found.");
        }
        return followUps.findUnlinkedScheduledForPatient(patientId).stream()
                .map(ReceptionFollowUpOption::from).toList();
    }

    @Transactional(readOnly = true)
    public List<FollowUpLifecycleResponse> followUpsForReception(String status) {
        if (status != null && !List.of("SCHEDULED", "CHECKED_IN", "IN_PROGRESS", "COMPLETED", "CANCELLED", "MISSED").contains(status))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported follow-up status.");
        if ("CHECKED_IN".equals(status)) {
            return followUps.findAllForReception("SCHEDULED").stream().filter(f -> f.getLinkedVisit() != null)
                    .map(FollowUpLifecycleResponse::from).toList();
        }
        if (status == null) return followUps.findAllForReception(null).stream().map(FollowUpLifecycleResponse::from).toList();
        return followUps.findAllForReception(status).stream().map(FollowUpLifecycleResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<PatientFollowUpAuditEvent> followUpAudit(Long followUpId) {
        if (!followUps.existsById(followUpId)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Follow-up not found.");
        return followUpAudit.findAllByFollowUpIdOrderByOccurredAtAscIdAsc(followUpId);
    }

    @Transactional
    public FollowUpLifecycleResponse cancelFollowUp(Long followUpId, StaffAccount actor, String reason) {
        return changeReceptionFollowUp(followUpId, actor, reason, false);
    }

    @Transactional
    public FollowUpLifecycleResponse markFollowUpMissed(Long followUpId, StaffAccount actor, String reason) {
        return changeReceptionFollowUp(followUpId, actor, reason, true);
    }

    private FollowUpLifecycleResponse changeReceptionFollowUp(Long followUpId, StaffAccount actor, String reason, boolean missed) {
        if (actor == null || actor.getId() == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Active Reception staff identity is required.");
        if (reason == null || reason.isBlank() || reason.trim().length() > 1000)
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "A reason of 1 to 1000 characters is required.");
        PatientFollowUp f = followUps.findByIdForUpdate(followUpId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Follow-up not found."));
        if (f.getLinkedVisit() != null) throw new ResponseStatusException(HttpStatus.CONFLICT, "A checked-in follow-up cannot be cancelled or marked missed.");
        if (missed && !f.getFollowUpDate().isBefore(LocalDate.now(CLINIC_ZONE)))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only a follow-up dated before today can be marked missed.");
        String previous = f.getStatus(); LocalDateTime at = LocalDateTime.now(CLINIC_ZONE);
        try {
            if (missed) f.markMissed("STAFF", actor.getId(), reason.trim(), at);
            else f.markCancelled("STAFF", actor.getId(), reason.trim(), at);
        } catch (IllegalStateException ex) { throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage()); }
        f = followUps.save(f);
        followUpAudit.save(new PatientFollowUpAuditEvent(f.getId(), previous, f.getStatus(), missed ? "MISSED" : "CANCELLED",
                "STAFF", actor.getId(), reason.trim(), null, at));
        return FollowUpLifecycleResponse.from(f);
    }

    @Transactional(readOnly = true)
    public ReceptionQueueSummary queueSummary() {
        LocalDate today = LocalDate.now(CLINIC_ZONE);
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();
        return new ReceptionQueueSummary(nursingQueue.countTodayWaiting(start, end),
                nursingQueue.countTodayEmergencyWaiting(start, end));
    }

    @Transactional
    public ReceptionCheckInResponse checkIn(ReceptionCheckInRequest request, StaffAccount actor) {
        if (request == null || actor == null || actor.getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Active Reception staff identity is required.");
        }
        String requestKey = normalizeRequestKey(request.requestKey());
        PatientVisit previous = visits.findByCheckInRequestKey(requestKey).orElse(null);
        if (previous != null) {
            if (!sameRequest(previous, request)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "This check-in request key was already used for another request.");
            }
            NursingQueueEntry priorQueue = nursingQueue.findByVisit_Id(previous.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "The prior check-in is incomplete; contact an administrator."));
            return ReceptionCheckInResponse.from(previous, priorQueue);
        }

        Doctor assignedDoctor = doctors.findById(request.assignedDoctorId())
                .filter(Doctor::isEnabled)
                .filter(doctor -> doctor.getApprovalStatus() == Doctor.ApprovalStatus.APPROVED)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Select an approved and enabled Doctor."));

        Patient patient;
        boolean newPatient = request.newPatient() != null;
        if (newPatient) {
            patient = request.newPatient().toPatient();
            // Legacy display field is populated for compatibility; the ownership relation is deliberately untouched.
            patient.setDoctor(assignedDoctor.getFullName());
            patient = patients.saveAndFlush(patient);
        } else {
            patient = patients.findByIdForUpdate(request.patientId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found."));
        }

        PatientFollowUp sourceFollowUp = null;
        if (request.encounterType() == EncounterType.FOLLOW_UP) {
            sourceFollowUp = followUps.findByIdForReceptionCheckIn(request.followUpId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Scheduled follow-up not found."));
            if (!Objects.equals(sourceFollowUp.getVisit().getPatient().getId(), patient.getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The selected follow-up belongs to a different patient.");
            }
            if (!"SCHEDULED".equals(sourceFollowUp.getStatus())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Only a scheduled follow-up can be checked in.");
            }
            if (visits.findBySourceFollowUp_Id(sourceFollowUp.getId()).isPresent()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "This follow-up has already been checked in.");
            }
            sourceFollowUp.setCheckedInAt(LocalDateTime.now(CLINIC_ZONE));
            followUps.save(sourceFollowUp);
        }

        LocalDate visitDate = newPatient ? patient.getVisitDate() : LocalDate.now(CLINIC_ZONE);
        Integer currentMax = visits.findMaxVisitNumberByPatientId(patient.getId());
        PatientVisit visit = new PatientVisit();
        visit.setPatient(patient);
        visit.setVisitNumber((currentMax == null ? 0 : currentMax) + 1);
        visit.setVisitDate(visitDate);
        visit.setDoctor(assignedDoctor.getFullName());
        visit.setEncounterType(request.encounterType());
        visit.setAssignedDoctor(assignedDoctor);
        visit.setCheckedInByStaffId(actor.getId());
        visit.setEmergencyPriority(request.emergencyPriority());
        visit.setEmergencyNote(request.emergencyPriority() ? clean(request.emergencyNote()) : null);
        visit.setSourceFollowUp(sourceFollowUp);
        visit.setCheckInRequestKey(requestKey);
        visit.setCreatedAt(LocalDateTime.now(CLINIC_ZONE));

        if (newPatient) copyInitialClinicalValues(request.newPatient(), visit);
        try {
            visits.saveAndFlush(visit);
            if (sourceFollowUp != null) {
                followUps.save(sourceFollowUp);
                followUpAudit.save(new PatientFollowUpAuditEvent(sourceFollowUp.getId(), "SCHEDULED", "SCHEDULED",
                        "CHECKED_IN", "STAFF", actor.getId(), null, visit.getId(), sourceFollowUp.getCheckedInAt()));
            }
            NursingQueueEntry queueEntry = nursingQueue.saveAndFlush(new NursingQueueEntry(visit));
            return ReceptionCheckInResponse.from(visit, queueEntry);
        } catch (DataIntegrityViolationException exception) {
            // Unique request key/follow-up/queue constraints are the final concurrent-request guard.
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This attendance was already checked in or conflicts with another active check-in.");
        }
    }

    private static void copyInitialClinicalValues(PatientController.PatientRequest request, PatientVisit visit) {
        visit.setChiefComplaints(blankToNull(request.chiefComplaints));
        visit.setPresentIllness(blankToNull(request.presentIllness));
        visit.setGeneralExamination(blankToNull(request.generalExamination));
        visit.setHeartExamination(blankToNull(request.heartExamination));
        visit.setLungsExamination(blankToNull(request.lungsExamination));
        visit.setAbdomenExamination(blankToNull(request.abdomenExamination));
        visit.setRectalExamination(blankToNull(request.rectalExamination));
        visit.setTemperature(request.temperature);
        visit.setPulseRate(request.pulseRate);
        visit.setSpo2(request.spo2);
        visit.setBpSystolic(request.bpSystolic);
        visit.setBpDiastolic(request.bpDiastolic);
    }

    private static boolean sameRequest(PatientVisit visit, ReceptionCheckInRequest request) {
        if (visit.getEncounterType() != request.encounterType()
                || !Objects.equals(visit.getAssignedDoctor().getId(), request.assignedDoctorId())
                || visit.isEmergencyPriority() != request.emergencyPriority()
                || !Objects.equals(visit.getEmergencyNote(), request.emergencyPriority() ? clean(request.emergencyNote()) : null)
                || !Objects.equals(visit.getSourceFollowUp() == null ? null : visit.getSourceFollowUp().getId(), request.followUpId())) return false;
        if (request.patientId() != null) return Objects.equals(visit.getPatient().getId(), request.patientId());
        PatientController.PatientRequest candidate = request.newPatient();
        return candidate != null
                && visit.getPatient().getFullName().equals(candidate.fullName.trim())
                && visit.getPatient().getPhone().equals(candidate.phone.trim())
                && visit.getPatient().getAge().equals(candidate.age)
                && visit.getPatient().getGender().equals(candidate.gender);
    }

    private static String normalizeRequestKey(String value) {
        if (value == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A check-in request key is required.");
        try { return java.util.UUID.fromString(value).toString().toLowerCase(Locale.ROOT); }
        catch (IllegalArgumentException exception) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A valid UUID request key is required."); }
    }

    private static String clean(String value) { return blankToNull(value); }
    private static String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
