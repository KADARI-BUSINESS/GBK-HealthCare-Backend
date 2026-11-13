
package com.gbkhealthcare.patient;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PatientFollowUpService {

    private final PatientFollowUpRepository followUpRepository;
    private final PatientVisitRepository visitRepository;
    private final PatientRepository patientRepository;
    private final DoctorActivityService activityService;

    public PatientFollowUpService(
            PatientFollowUpRepository followUpRepository,
            PatientVisitRepository visitRepository,
            PatientRepository patientRepository,
            DoctorActivityService activityService) {

        this.followUpRepository = followUpRepository;
        this.visitRepository = visitRepository;
        this.patientRepository = patientRepository;
        this.activityService = activityService;
    }

    @Transactional(readOnly = true)
    public List<BulkFollowUpRow> getAllFollowUpsForDoctor(Long doctorAccountId) {
        return followUpRepository.findAllWithDetailsForDoctor(doctorAccountId).stream()
                .map(row -> new BulkFollowUpRow(
                        (Long) row[2], (String) row[3], (Long) row[0], (Long) row[1],
                        (LocalDate) row[4], (LocalDate) row[5], (String) row[6],
                        (String) row[7], (LocalDateTime) row[8], (LocalDateTime) row[9]))
                .toList();
    }

    public record BulkFollowUpRow(Long patientId, String patientName, Long followUpId,
            Long visitId, LocalDate followUpDate, LocalDate previousVisitDate,
            String followUpNotes, String status, LocalDateTime createdAt,
            LocalDateTime completedAt) {}
    @Transactional
    public PatientFollowUp scheduleFollowUp(
            Long visitId,
            Long doctorAccountId,
            LocalDate followUpDate,
            String followUpNotes) {

        PatientVisit visit = visitRepository
                .findByIdAndPatient_DoctorAccount_Id(visitId, doctorAccountId)
                .orElseThrow(FollowUpResourceNotFoundException::new);

        if (followUpDate == null) {
            throw new IllegalArgumentException(
                    "Follow-up date is required.");
        }

        PatientFollowUp followUp = new PatientFollowUp();
        followUp.setVisit(visit);
        followUp.setFollowUpDate(followUpDate);
        followUp.setFollowUpNotes(followUpNotes);
        followUp.setStatus("SCHEDULED");

        PatientFollowUp savedFollowUp = followUpRepository.save(followUp);
        activityService.recordActivity(doctorAccountId,
                DoctorActivity.ActionType.FOLLOW_UP_SCHEDULED,
                DoctorActivity.EntityType.FOLLOW_UP, savedFollowUp.getId());
        return savedFollowUp;
    }

    @Transactional(readOnly = true)
    public List<PatientFollowUp> getFollowUpsByVisit(
            Long visitId,
            Long doctorAccountId) {

        if (visitRepository.findByIdAndPatient_DoctorAccount_Id(
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

        if (patientRepository.findByIdAndDoctorAccount_Id(
                patientId, doctorAccountId).isEmpty()) {
            throw new FollowUpResourceNotFoundException();
        }

        return followUpRepository
                .findByPatientIdAndDoctorAccountIdWithDetails(patientId, doctorAccountId);
    }

    @Transactional(readOnly = true)
    public List<PatientFollowUp> getUpcomingFollowUps(Long doctorAccountId) {

        return followUpRepository.findUpcomingWithDetailsForDoctor(
                "SCHEDULED",
                LocalDate.now(),
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

        PatientFollowUp followUp = findFollowUp(followUpId, doctorAccountId);

        if ("CANCELLED".equals(followUp.getStatus())) {
            throw new IllegalStateException(
                    "A cancelled follow-up cannot be rescheduled.");
        }

        if ("COMPLETED".equals(followUp.getStatus())) {
            throw new IllegalStateException(
                    "A completed follow-up cannot be rescheduled.");
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

        PatientFollowUp followUp = findFollowUp(followUpId, doctorAccountId);

        if ("CANCELLED".equals(followUp.getStatus())) {
            throw new IllegalStateException(
                    "A cancelled follow-up cannot be completed.");
        }

        if ("COMPLETED".equals(followUp.getStatus())) {
            return followUp;
        }

        followUp.setStatus("COMPLETED");
        followUp.setCompletedAt(LocalDateTime.now());
        PatientFollowUp savedFollowUp = followUpRepository.save(followUp);
        activityService.recordActivity(doctorAccountId,
                DoctorActivity.ActionType.FOLLOW_UP_COMPLETED,
                DoctorActivity.EntityType.FOLLOW_UP, savedFollowUp.getId());
        return savedFollowUp;
    }

    @Transactional
    public PatientFollowUp cancelFollowUp(Long followUpId, Long doctorAccountId) {

        PatientFollowUp followUp = findFollowUp(followUpId, doctorAccountId);

        if ("COMPLETED".equals(followUp.getStatus())) {
            throw new IllegalStateException(
                    "A completed follow-up cannot be cancelled.");
        }

        if ("CANCELLED".equals(followUp.getStatus())) {
            return followUp;
        }

        followUp.setStatus("CANCELLED");
        PatientFollowUp savedFollowUp = followUpRepository.save(followUp);
        activityService.recordActivity(doctorAccountId,
                DoctorActivity.ActionType.FOLLOW_UP_CANCELLED,
                DoctorActivity.EntityType.FOLLOW_UP, savedFollowUp.getId());
        return savedFollowUp;
    }

    private PatientFollowUp findFollowUp(Long followUpId, Long doctorAccountId) {

        return followUpRepository
                .findByIdAndDoctorAccountIdWithDetails(followUpId, doctorAccountId)
                .orElseThrow(FollowUpResourceNotFoundException::new);
    }
}





