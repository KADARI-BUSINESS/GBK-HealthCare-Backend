package com.gbkhealthcare.billing;
import com.gbkhealthcare.auth.StaffAccessService;import com.gbkhealthcare.auth.StaffRole;import jakarta.validation.Valid;import org.springframework.http.HttpStatus;import org.springframework.security.core.annotation.AuthenticationPrincipal;import org.springframework.security.oauth2.jwt.Jwt;import org.springframework.web.bind.annotation.*;import java.util.List;
@RestController @RequestMapping("/api/billing")
public class BillingController {
 private final BillingService billing; private final StaffAccessService access;
 public BillingController(BillingService billing,StaffAccessService access){this.billing=billing;this.access=access;}
 @GetMapping("/services") public List<BillableServiceResponse> services(@AuthenticationPrincipal Jwt jwt){return billing.serviceCatalog(jwt,false);}
 @GetMapping("/outstanding") public List<InvoiceResponse> outstanding(@AuthenticationPrincipal Jwt jwt){return billing.outstanding(jwt);}
 @GetMapping("/checkout-queue") public List<CheckoutQueueItemResponse> checkoutQueue(@AuthenticationPrincipal Jwt jwt){return billing.checkoutQueue(jwt);}
 @GetMapping("/encounters/{visitId}/invoices") public List<InvoiceResponse> history(@AuthenticationPrincipal Jwt jwt,@PathVariable Long visitId){return billing.encounterHistory(jwt,visitId);}
 @PostMapping("/encounters/{visitId}/invoices") @ResponseStatus(HttpStatus.CREATED) public InvoiceResponse create(@AuthenticationPrincipal Jwt jwt,@PathVariable Long visitId,@Valid @RequestBody InvoiceCreateRequest request){return billing.createInvoice(jwt,visitId,request);}
 @PostMapping("/invoices/{id}/issue") public InvoiceResponse issue(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id){return billing.issue(jwt,id);}
 @PostMapping("/invoices/{id}/void") public InvoiceResponse voidDraft(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id){return billing.voidDraft(jwt,id);}
 @PostMapping("/invoices/{id}/complete-checkout") public InvoiceResponse completeCheckout(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id){return billing.completeCheckout(jwt,id);}
 @PostMapping("/invoices/{id}/payments") @ResponseStatus(HttpStatus.CREATED) public InvoiceResponse payment(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id,@Valid @RequestBody PaymentRequest request){return billing.recordPayment(jwt,id,request);}
 @PostMapping("/invoices/{id}/payments/{paymentId}/refunds") @ResponseStatus(HttpStatus.CREATED) public RefundResponse refund(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id,@PathVariable Long paymentId,@Valid @RequestBody RefundRequest request){return billing.refund(jwt,id,paymentId,request);}
 @PostMapping("/invoices/{id}/adjustments") public InvoiceResponse adjustment(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id,@Valid @RequestBody AdjustmentRequest request){return billing.adjust(jwt,id,request);}
 @GetMapping("/invoices/{id}/audit") public List<BillingAuditEvent> audit(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id){return billing.audit(jwt,id);}
}
