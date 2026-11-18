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
@RequestMapping("/api/nursing")
public class NursingController {
    private final StaffAccessService access;
    private final NursingService nursing;
    public NursingController(StaffAccessService access, NursingService nursing) { this.access = access; this.nursing = nursing; }
    @GetMapping("/queue") public List<NursingQueueItemResponse> queue(@AuthenticationPrincipal Jwt jwt) { requireNurse(jwt); return nursing.activeQueue(); }
    @PostMapping("/queue/{queueId}/claim") public NursingQueueItemResponse claim(@AuthenticationPrincipal Jwt jwt, @PathVariable Long queueId) { return nursing.claim(queueId, requireNurse(jwt)); }
    @GetMapping("/queue/{queueId}/intakes") public List<NursingIntakeResponse> history(@AuthenticationPrincipal Jwt jwt, @PathVariable Long queueId) { return nursing.intakeHistory(queueId, requireNurse(jwt)); }
    @PostMapping("/queue/{queueId}/intakes") @ResponseStatus(HttpStatus.CREATED)
    public NursingIntakeResponse save(@AuthenticationPrincipal Jwt jwt, @PathVariable Long queueId, @Valid @RequestBody NursingIntakeRequest request) { return nursing.saveIntake(queueId, requireNurse(jwt), request); }
    @GetMapping("/queue/{queueId}/audit") public List<NursingAuditEventResponse> audit(@AuthenticationPrincipal Jwt jwt, @PathVariable Long queueId) { return nursing.auditHistory(queueId, requireNurse(jwt)); }
    @PostMapping("/queue/{queueId}/handoff") public NursingHandoffResponse handoff(@AuthenticationPrincipal Jwt jwt, @PathVariable Long queueId) { return nursing.handoff(queueId, requireNurse(jwt)); }
    private StaffAccount requireNurse(Jwt jwt) { return access.requireRole(jwt, StaffRole.NURSE); }
}
