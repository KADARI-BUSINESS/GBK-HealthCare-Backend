package com.gbkhealthcare.patient;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/doctor-opd")
public class DoctorOpdController {
    private final DoctorOpdService opd;
    private final DoctorClinicalOrderService orders;
    public DoctorOpdController(DoctorOpdService opd, DoctorClinicalOrderService orders) { this.opd = opd; this.orders = orders; }
    @GetMapping("/queue") public List<DoctorOpdQueueItemResponse> queue(@AuthenticationPrincipal Jwt jwt) { return opd.queue(jwt); }
    @PostMapping("/queue/{queueId}/claim") public DoctorOpdQueueItemResponse claim(@AuthenticationPrincipal Jwt jwt, @PathVariable Long queueId) { return opd.claim(jwt, queueId); }
    @PostMapping("/queue/{queueId}/start") public DoctorOpdQueueItemResponse start(@AuthenticationPrincipal Jwt jwt, @PathVariable Long queueId) { return opd.start(jwt, queueId); }
    @PostMapping("/queue/{queueId}/hold") public DoctorOpdQueueItemResponse hold(@AuthenticationPrincipal Jwt jwt, @PathVariable Long queueId) { return opd.hold(jwt, queueId); }
    @PostMapping("/queue/{queueId}/resume") public DoctorOpdQueueItemResponse resume(@AuthenticationPrincipal Jwt jwt, @PathVariable Long queueId) { return opd.resume(jwt, queueId); }
    @GetMapping("/queue/{queueId}/nursing-intake") public DoctorNursingIntakeResponse nursingIntake(@AuthenticationPrincipal Jwt jwt, @PathVariable Long queueId) { return opd.nursingIntake(jwt, queueId); }
    @GetMapping("/queue/{queueId}/history") public List<RelevantHistoryItemResponse> history(@AuthenticationPrincipal Jwt jwt, @PathVariable Long queueId) { return opd.relevantHistory(jwt, queueId); }
    @GetMapping("/queue/{queueId}/consultation") public DoctorConsultationResponse consultation(@AuthenticationPrincipal Jwt jwt, @PathVariable Long queueId) { return opd.consultation(jwt, queueId); }
    @PutMapping("/queue/{queueId}/consultation") public DoctorConsultationResponse draft(@AuthenticationPrincipal Jwt jwt, @PathVariable Long queueId, @Valid @RequestBody ConsultationDraftRequest request) { return opd.saveDraft(jwt, queueId, request); }
    @PostMapping("/queue/{queueId}/sign") public DoctorConsultationResponse sign(@AuthenticationPrincipal Jwt jwt, @PathVariable Long queueId) { return opd.sign(jwt, queueId); }
    @GetMapping("/queue/{queueId}/addenda") public List<ConsultationAddendumResponse> addenda(@AuthenticationPrincipal Jwt jwt, @PathVariable Long queueId) { return opd.addenda(jwt, queueId); }
    @PostMapping("/queue/{queueId}/addenda") @ResponseStatus(HttpStatus.CREATED)
    public ConsultationAddendumResponse addendum(@AuthenticationPrincipal Jwt jwt, @PathVariable Long queueId, @Valid @RequestBody ConsultationAddendumRequest request) { return opd.addendum(jwt, queueId, request); }
    @GetMapping("/queue/{queueId}/prescriptions") public List<PrescriptionResponse> prescriptions(@AuthenticationPrincipal Jwt jwt,@PathVariable Long queueId){return orders.prescriptions(jwt,queueId);}
    @PostMapping("/queue/{queueId}/prescriptions") @ResponseStatus(HttpStatus.CREATED) public PrescriptionResponse createPrescription(@AuthenticationPrincipal Jwt jwt,@PathVariable Long queueId,@Valid @RequestBody PrescriptionRequest request){return orders.createPrescription(jwt,queueId,request);}
    @PutMapping("/queue/{queueId}/prescriptions/{prescriptionId}") public PrescriptionResponse updatePrescription(@AuthenticationPrincipal Jwt jwt,@PathVariable Long queueId,@PathVariable Long prescriptionId,@Valid @RequestBody PrescriptionRequest request){return orders.updatePrescription(jwt,queueId,prescriptionId,request);}
    @PostMapping("/queue/{queueId}/prescriptions/{prescriptionId}/sign") public PrescriptionResponse signPrescription(@AuthenticationPrincipal Jwt jwt,@PathVariable Long queueId,@PathVariable Long prescriptionId){return orders.signPrescription(jwt,queueId,prescriptionId);}
    @GetMapping("/queue/{queueId}/investigation-orders") public List<InvestigationOrderResponse> investigations(@AuthenticationPrincipal Jwt jwt,@PathVariable Long queueId){return orders.investigations(jwt,queueId);}
    @PostMapping("/queue/{queueId}/investigation-orders") @ResponseStatus(HttpStatus.CREATED) public InvestigationOrderResponse createInvestigation(@AuthenticationPrincipal Jwt jwt,@PathVariable Long queueId,@Valid @RequestBody InvestigationOrderRequest request){return orders.createInvestigation(jwt,queueId,request);}
    @PutMapping("/queue/{queueId}/investigation-orders/{orderId}/status") public InvestigationOrderResponse investigationStatus(@AuthenticationPrincipal Jwt jwt,@PathVariable Long queueId,@PathVariable Long orderId,@Valid @RequestBody InvestigationStatusRequest request){return orders.investigationStatus(jwt,queueId,orderId,request);}
    @GetMapping("/queue/{queueId}/referrals") public List<ReferralOrderResponse> referrals(@AuthenticationPrincipal Jwt jwt,@PathVariable Long queueId){return orders.referrals(jwt,queueId);}
    @PostMapping("/queue/{queueId}/referrals") @ResponseStatus(HttpStatus.CREATED) public ReferralOrderResponse createReferral(@AuthenticationPrincipal Jwt jwt,@PathVariable Long queueId,@Valid @RequestBody ReferralOrderRequest request){return orders.createReferral(jwt,queueId,request);}
    @PutMapping("/queue/{queueId}/referrals/{referralId}/status") public ReferralOrderResponse referralStatus(@AuthenticationPrincipal Jwt jwt,@PathVariable Long queueId,@PathVariable Long referralId,@Valid @RequestBody ReferralStatusRequest request){return orders.referralStatus(jwt,queueId,referralId,request);}
}
