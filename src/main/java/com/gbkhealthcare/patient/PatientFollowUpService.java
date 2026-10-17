
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

    public PatientFollowUpService(
            PatientFollowUpRepository followUpRepository,
            PatientVisitRepository visitRepository,
            PatientRepository patientRepository) {

        this.followUpRepository = followUpRepository;
        this.visitRepository = visitRepository;
        this.patientRepository = patientRepository;
    }

    @Transactional
    public PatientFollowUp scheduleFollowUp(
            Long visitId,
            LocalDate followUpDate,
            String followUpNotes) {

        if (followUpDate == null) {
            throw new IllegalArgumentException(
                    "Follow-up date is required.");
        }

        PatientVisit visit = visitRepository.findById(visitId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Visit with ID " + visitId + " was not found."));

        PatientFollowUp followUp = new PatientFollowUp();
        followUp.setVisit(visit);
        followUp.setFollowUpDate(followUpDate);
        followUp.setFollowUpNotes(followUpNotes);
        followUp.setStatus("SCHEDULED");

        return followUpRepository.save(followUp);
    }

    @Transactional(readOnly = true)
    public List<PatientFollowUp> getFollowUpsByVisit(Long visitId) {

        if (!visitRepository.existsById(visitId)) {
            throw new IllegalArgumentException(
                    "Visit with ID " + visitId + " was not found.");
        }

        return followUpRepository.findByVisitIdWithDetails(visitId);
    }

    @Transactional(readOnly = true)
    public List<PatientFollowUp> getFollowUpsByPatient(Long patientId) {

        if (!patientRepository.existsById(patientId)) {
            throw new IllegalArgumentException(
                    "Patient with ID " + patientId + " was not found.");
        }

        return followUpRepository.findByPatientIdWithDetails(patientId);
    }

    @Transactional(readOnly = true)
    public List<PatientFollowUp> getUpcomingFollowUps() {

        return followUpRepository.findUpcomingWithDetails(
                "SCHEDULED",
                LocalDate.now());
    }

    @Transactional
    public PatientFollowUp rescheduleFollowUp(
            Long followUpId,
            LocalDate newDate,
            String notes) {

        if (newDate == null) {
            throw new IllegalArgumentException(
                    "Follow-up date is required.");
        }

        PatientFollowUp followUp = findFollowUp(followUpId);

        if ("CANCELLED".equals(followUp.getStatus())) {
            throw new IllegalStateException(
                    "A cancelled follow-up cannot be rescheduled.");
        }

        if ("COMPLETED".equals(followUp.getStatus())) {
            throw new IllegalStateException(
                    "A completed follow-up cannot be rescheduled.");
        }

        followUp.setFollowUpDate(newDate);
        followUp.setFollowUpNotes(notes);

        return followUpRepository.save(followUp);
    }

    @Transactional
    public PatientFollowUp completeFollowUp(Long followUpId) {

        PatientFollowUp followUp = findFollowUp(followUpId);

        if ("CANCELLED".equals(followUp.getStatus())) {
            throw new IllegalStateException(
                    "A cancelled follow-up cannot be completed.");
        }

        followUp.setStatus("COMPLETED");
        followUp.setCompletedAt(LocalDateTime.now());

        return followUpRepository.save(followUp);
    }

    @Transactional
    public PatientFollowUp cancelFollowUp(Long followUpId) {

        PatientFollowUp followUp = findFollowUp(followUpId);

        if ("COMPLETED".equals(followUp.getStatus())) {
            throw new IllegalStateException(
                    "A completed follow-up cannot be cancelled.");
        }

        followUp.setStatus("CANCELLED");

        return followUpRepository.save(followUp);
    }

    private PatientFollowUp findFollowUp(Long followUpId) {

        return followUpRepository.findByIdWithDetails(followUpId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Follow-up with ID " + followUpId
                                + " was not found."));
    }
}
