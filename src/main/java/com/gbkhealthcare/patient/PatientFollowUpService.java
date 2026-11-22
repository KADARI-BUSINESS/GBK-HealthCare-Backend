
package com.gbkhealthcare.patient;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class PatientFollowUpService {
    private static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Kolkata");

    private final PatientFollowUpRepository followUpRepository;
    private final PatientVisitRepository visitRepository;
    private final DoctorActivityService activityService;
    private final PatientFollowUpAuditEventRepository lifecycleAudit;

    public PatientFollowUpService(
            PatientFollowUpRepository followUpRepository,
            PatientVisitRepository visitRepository,
            DoctorActivityService activityService,
            PatientFollowUpAuditEventRepository lifecycleAudit) {

        this.followUpRepository = followUpRepository;
        this.visitRepository = visitRepository;
        this.activityService = activityService;
        this.lifecycleAudit = lifecycleAudit;
    }

    @Transactional(readOnly = true)
    public List<BulkFollowUpRow> getAllFollowUpsForDoctor(Long doctorAccountId) {
        return followUpRepository.findAllWithDetailsForDoctor(doctorAccountId).stream()
                .map(row -> new BulkFollowUpRow(
                        (Long) row[2], (String) row[3], (Long) row[0], (Long) row[1],
                        (LocalDate) row[4], (LocalDate) row[5], (String) row[6],
                        (String) row[7], (LocalDateTime) row[8], (LocalDateTime) row[9], (Long) row[10]))
                .toList();
    }

    public record BulkFollowUpRow(Long patientId, String patientName, Long followUpId,
            Long visitId, LocalDate followUpDate, LocalDate previousVisitDate,
            String followUpNotes, String status, LocalDateTime createdAt,
            LocalDateTime completedAt, Long checkedInVisitId) {}
    @Transactional
    public PatientFollowUp scheduleFollowUp(
            Long visitId,
            Long doctorAccountId,
            LocalDate followUpDate,
            String followUpNotes) {

        PatientVisit visit = visitRepository
                .findForDoctorFollowUp(visitId, doctorAccountId)
                .orElseThrow(FollowUpResourceNotFoundException::new);

        if (followUpDate == null) {
            throw new IllegalArgumentException(
                    "Follow-up date is required.");
        }

        PatientFollowUp followUp = new PatientFollowUp();
        LocalDateTime at = now();
        followUp.setVisit(visit);
        followUp.setCreatedAt(at);
        followUp.setFollowUpDate(followUpDate);
        followUp.setFollowUpNotes(followUpNotes);
        followUp.setStatus("SCHEDULED");

        PatientFollowUp savedFollowUp = followUpRepository.save(followUp);
        lifecycleAudit.save(new PatientFollowUpAuditEvent(savedFollowUp.getId(), null, "SCHEDULED",
                "SCHEDULED", "DOCTOR", doctorAccountId, null, visitId, at));
        activityService.recordActivity(doctorAccountId,
                DoctorActivity.ActionType.FOLLOW_UP_SCHEDULED,
                DoctorActivity.EntityType.FOLLOW_UP, savedFollowUp.getId());
        return savedFollowUp;
    }

    @Transactional(readOnly = true)
    public List<PatientFollowUp> getFollowUpsByVisit(
            Long visitId,
            Long doctorAccountId) {

        if (visitRepository.findForDoctorFollowUp(
                visitId, doctorAccountId).isEmpty()) {
            throw new FollowUpResourceNotFoundException();
        }

        return followUpRepository
                .findByVisitIdAndDoctorAccountIdWithDetails(visitId, doctorAccountId);
    }

    @Transactional(readOnly = true)
    public List<PatientFollowUp> getFollowUpsByPatient(
            Long patientId,
            Long doctorAccountId) {

        if (!visitRepository.existsPatientVisitForDoctor(patientId, doctorAccountId)) {
            throw new FollowUpResourceNotFoundException();
        }

        return followUpRepository
                .findByPatientIdAndDoctorAccountIdWithDetails(patientId, doctorAccountId);
    }

    @Transactional(readOnly = true)
    public List<PatientFollowUp> getUpcomingFollowUps(Long doctorAccountId) {

        return followUpRepository.findUpcomingWithDetailsForDoctor(
                "SCHEDULED",
                LocalDate.now(CLINIC_ZONE),
                doctorAccountId);
    }

    @Transactional
    public PatientFollowUp rescheduleFollowUp(
            Long followUpId,
            Long doctorAccountId,
            LocalDate newDate,
            String notes) {

        if (newDate == null) {
            throw new IllegalArgumentException(
                    "Follow-up date is required.");
        }

        PatientFollowUp followUp = findFollowUpForUpdate(followUpId, doctorAccountId);
        if (followUp.getLinkedVisit() != null) throw new IllegalStateException("A checked-in follow-up cannot be rescheduled.");

        if (!"SCHEDULED".equals(followUp.getStatus())) {
            throw new IllegalStateException(
                    "Only a scheduled follow-up can be rescheduled.");
        }

        boolean dateChanged = !newDate.equals(followUp.getFollowUpDate());
        followUp.setFollowUpDate(newDate);
        followUp.setFollowUpNotes(notes);
        PatientFollowUp savedFollowUp = followUpRepository.save(followUp);
        if (dateChanged) {
            activityService.recordActivity(doctorAccountId,
                    DoctorActivity.ActionType.FOLLOW_UP_RESCHEDULED,
                    DoctorActivity.EntityType.FOLLOW_UP, savedFollowUp.getId());
        }
        return savedFollowUp;
    }

    @Transactional
    public PatientFollowUp completeFollowUp(Long followUpId, Long doctorAccountId) {
        PatientFollowUp followUp = findFollowUpForUpdate(followUpId, doctorAccountId);
        if ("COMPLETED".equals(followUp.getStatus())) return followUp;
        PatientVisit linked = followUp.getLinkedVisit();
        if (linked == null || linked.getAssignedDoctor() == null
                || !doctorAccountId.equals(linked.getAssignedDoctor().getId())
                || !"SIGNED".equals(linked.getConsultationStatus()) || linked.getConsultationSignedAt() == null)
            throw new IllegalStateException("A follow-up is completed only when its assigned Doctor signs the linked consultation.");
        String previous = followUp.getStatus();
        followUp.markCompleted(linked.getConsultationSignedAt());
        PatientFollowUp savedFollowUp = followUpRepository.save(followUp);
        lifecycleAudit.save(new PatientFollowUpAuditEvent(savedFollowUp.getId(), previous, "COMPLETED",
                "CONSULTATION_SIGNED", "DOCTOR", doctorAccountId, null, linked.getId(), savedFollowUp.getCompletedAt()));
        activityService.recordActivity(doctorAccountId,
                DoctorActivity.ActionType.FOLLOW_UP_COMPLETED,
                DoctorActivity.EntityType.FOLLOW_UP, savedFollowUp.getId());
        return savedFollowUp;
    }

    @Transactional
    public PatientFollowUp cancelFollowUp(Long followUpId, Long doctorAccountId, String reason) {
        if (reason == null || reason.isBlank() || reason.trim().length() > 1000)
            throw new IllegalArgumentException("A cancellation reason of 1 to 1000 characters is required.");
        PatientFollowUp followUp = findFollowUpForUpdate(followUpId, doctorAccountId);
        if ("CANCELLED".equals(followUp.getStatus())) return followUp;
        if (followUp.getLinkedVisit() != null) throw new IllegalStateException("A checked-in follow-up cannot be cancelled.");
        String previous = followUp.getStatus();
        followUp.markCancelled("DOCTOR", doctorAccountId, reason.trim(), now());
        PatientFollowUp savedFollowUp = followUpRepository.save(followUp);
        lifecycleAudit.save(new PatientFollowUpAuditEvent(savedFollowUp.getId(), previous, "CANCELLED",
                "CANCELLED", "DOCTOR", doctorAccountId, reason.trim(), null, savedFollowUp.getCancelledAt()));
        activityService.recordActivity(doctorAccountId,
                DoctorActivity.ActionType.FOLLOW_UP_CANCELLED,
                DoctorActivity.EntityType.FOLLOW_UP, savedFollowUp.getId());
        return savedFollowUp;
    }

    private PatientFollowUp findFollowUpForUpdate(Long followUpId, Long doctorAccountId) {
        PatientFollowUp followUp = followUpRepository.findByIdForUpdate(followUpId)
                .orElseThrow(FollowUpResourceNotFoundException::new);
        boolean legacyOwned = followUp.getVisit().getPatient().getDoctorAccount() != null
                && doctorAccountId.equals(followUp.getVisit().getPatient().getDoctorAccount().getId());
        boolean originalEncounterAssigned = followUp.getVisit().getAssignedDoctor() != null
                && doctorAccountId.equals(followUp.getVisit().getAssignedDoctor().getId());
        PatientVisit linked = followUp.getLinkedVisit();
        boolean encounterAssigned = linked != null && linked.getAssignedDoctor() != null
                && doctorAccountId.equals(linked.getAssignedDoctor().getId());
        if (!legacyOwned && !originalEncounterAssigned && !encounterAssigned) throw new FollowUpResourceNotFoundException();
        return followUp;
    }

    private static LocalDateTime now() { return LocalDateTime.now(CLINIC_ZONE); }
}





