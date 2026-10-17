
package com.gbkhealthcare.patient;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api")
public class PatientFollowUpController {

    private final PatientFollowUpService followUpService;

    public PatientFollowUpController(
            PatientFollowUpService followUpService) {
        this.followUpService = followUpService;
    }

    // 1. Schedule a follow-up for an existing visit
    @PostMapping("/visits/{visitId}/follow-ups")
    @ResponseStatus(HttpStatus.CREATED)
    public FollowUpResponse scheduleFollowUp(
            @PathVariable Long visitId,
            @Valid @RequestBody FollowUpRequest request) {

        PatientFollowUp followUp = followUpService.scheduleFollowUp(
                visitId,
                request.followUpDate(),
                request.followUpNotes());

        return FollowUpResponse.from(followUp);
    }

    // 2. Get follow-ups linked to a particular visit
    @GetMapping("/visits/{visitId}/follow-ups")
    public List<FollowUpResponse> getFollowUpsByVisit(
            @PathVariable Long visitId) {

        return followUpService.getFollowUpsByVisit(visitId)
                .stream()
                .map(FollowUpResponse::from)
                .toList();
    }

    // 3. Get all follow-ups for a patient
    @GetMapping("/patients/{patientId}/follow-ups")
    public List<FollowUpResponse> getFollowUpsByPatient(
            @PathVariable Long patientId) {

        return followUpService.getFollowUpsByPatient(patientId)
                .stream()
                .map(FollowUpResponse::from)
                .toList();
    }

    // 4. Get upcoming scheduled follow-ups
    @GetMapping("/follow-ups/upcoming")
    public List<FollowUpResponse> getUpcomingFollowUps() {

        return followUpService.getUpcomingFollowUps()
                .stream()
                .map(FollowUpResponse::from)
                .toList();
    }

    // 5. Reschedule a follow-up
    @PutMapping("/follow-ups/{followUpId}")
    public FollowUpResponse rescheduleFollowUp(
            @PathVariable Long followUpId,
            @Valid @RequestBody FollowUpRequest request) {

        PatientFollowUp followUp = followUpService.rescheduleFollowUp(
                followUpId,
                request.followUpDate(),
                request.followUpNotes());

        return FollowUpResponse.from(followUp);
    }

    // 6. Mark a follow-up as completed
    @PutMapping("/follow-ups/{followUpId}/complete")
    public FollowUpResponse completeFollowUp(
            @PathVariable Long followUpId) {

        return FollowUpResponse.from(
                followUpService.completeFollowUp(followUpId));
    }

    // 7. Cancel a follow-up
    @PutMapping("/follow-ups/{followUpId}/cancel")
    public FollowUpResponse cancelFollowUp(
            @PathVariable Long followUpId) {

        return FollowUpResponse.from(
                followUpService.cancelFollowUp(followUpId));
    }

    // Request DTO
    public record FollowUpRequest(
            @NotNull(message = "Follow-up date is required") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate followUpDate,

            @Size(max = 2000, message = "Follow-up notes cannot exceed 2000 characters") String followUpNotes) {
    }

    // Response DTO: avoids returning JPA entities directly
    public record FollowUpResponse(
            Long id,
            Long visitId,
            Long patientId,
            LocalDate followUpDate,
            String followUpNotes,
            String status,
            LocalDateTime createdAt,
            LocalDateTime completedAt) {
        public static FollowUpResponse from(
                PatientFollowUp followUp) {

            PatientVisit visit = followUp.getVisit();

            return new FollowUpResponse(
                    followUp.getId(),
                    visit.getId(),
                    visit.getPatient().getId(),
                    followUp.getFollowUpDate(),
                    followUp.getFollowUpNotes(),
                    followUp.getStatus(),
                    followUp.getCreatedAt(),
                    followUp.getCompletedAt());
        }
    }
}
