package com.gbkhealthcare.patient;

import com.gbkhealthcare.auth.StaffAccessService;
import com.gbkhealthcare.auth.StaffAccount;
import com.gbkhealthcare.auth.StaffRole;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/reception")
public class ReceptionController {
    private final StaffAccessService access;
    private final ReceptionService reception;

    public ReceptionController(StaffAccessService access, ReceptionService reception) {
        this.access = access;
        this.reception = reception;
    }

    @GetMapping("/patients")
    public List<ReceptionPatientSummary> searchPatients(@AuthenticationPrincipal Jwt jwt,
            @RequestParam String query) {
        requireReception(jwt);
        return reception.searchPatients(query);
    }

    @GetMapping("/doctors")
    public List<ReceptionDoctorOption> doctors(@AuthenticationPrincipal Jwt jwt) {
        requireReception(jwt);
        return reception.availableDoctors();
    }

    @GetMapping("/patients/{patientId}/scheduled-follow-ups")
    public List<ReceptionFollowUpOption> followUps(@AuthenticationPrincipal Jwt jwt,
            @PathVariable Long patientId) {
        requireReception(jwt);
        return reception.scheduledFollowUps(patientId);
    }

    @GetMapping("/queue-summary")
    public ReceptionQueueSummary queueSummary(@AuthenticationPrincipal Jwt jwt) {
        requireReception(jwt);
        return reception.queueSummary();
    }

    @GetMapping("/follow-ups")
    public List<FollowUpLifecycleResponse> followUps(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String status) {
        requireReception(jwt);
        return reception.followUpsForReception(status);
    }

    @GetMapping("/follow-ups/{followUpId}/audit")
    public List<PatientFollowUpAuditEvent> followUpAudit(@AuthenticationPrincipal Jwt jwt,
            @PathVariable Long followUpId) {
        requireReception(jwt);
        return reception.followUpAudit(followUpId);
    }

    @PutMapping("/follow-ups/{followUpId}/cancel")
    public FollowUpLifecycleResponse cancelFollowUp(@AuthenticationPrincipal Jwt jwt,
            @PathVariable Long followUpId, @Valid @RequestBody FollowUpReasonRequest request) {
        StaffAccount actor = requireReception(jwt);
        return reception.cancelFollowUp(followUpId, actor, request.reason());
    }

    @PutMapping("/follow-ups/{followUpId}/missed")
    public FollowUpLifecycleResponse missedFollowUp(@AuthenticationPrincipal Jwt jwt,
            @PathVariable Long followUpId, @Valid @RequestBody FollowUpReasonRequest request) {
        StaffAccount actor = requireReception(jwt);
        return reception.markFollowUpMissed(followUpId, actor, request.reason());
    }

    @PostMapping("/check-ins")
    @ResponseStatus(HttpStatus.CREATED)
    public ReceptionCheckInResponse checkIn(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ReceptionCheckInRequest request) {
        StaffAccount actor = requireReception(jwt);
        return reception.checkIn(request, actor);
    }

    private StaffAccount requireReception(Jwt jwt) {
        return access.requireRole(jwt, StaffRole.RECEPTION);
    }
}
