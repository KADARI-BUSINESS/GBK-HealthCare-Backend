package com.gbkhealthcare.billing;
import jakarta.validation.Valid;import org.springframework.http.HttpStatus;import org.springframework.security.core.annotation.AuthenticationPrincipal;import org.springframework.security.oauth2.jwt.Jwt;import org.springframework.web.bind.annotation.*;import java.util.List;
@RestController @RequestMapping("/api/admin/billing")
public class AdminBillingController {
 private final BillingService billing; public AdminBillingController(BillingService billing){this.billing=billing;}
 @GetMapping("/services") public List<BillableServiceResponse> services(@AuthenticationPrincipal Jwt jwt){return billing.serviceCatalog(jwt,true);}
 @PostMapping("/services") @ResponseStatus(HttpStatus.CREATED) public BillableServiceResponse createService(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody BillableServiceRequest request){return billing.createService(jwt,request);}
 @PostMapping("/services/{id}/prices") @ResponseStatus(HttpStatus.CREATED) public BillableServiceResponse price(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id,@Valid @RequestBody ServicePriceRequest request){return billing.addPrice(jwt,id,request);}
 @PutMapping("/services/{id}") public BillableServiceResponse updateService(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id,@Valid @RequestBody BillableServiceRequest request){return billing.updateService(jwt,id,request);}
 @GetMapping("/services/{id}/price-history") public List<ServicePriceResponse> priceHistory(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id){return billing.priceHistory(jwt,id);}
 @PutMapping("/services/{id}/status") public BillableServiceResponse serviceStatus(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id,@Valid @RequestBody ServiceStatusRequest request){return billing.setServiceActive(jwt,id,request);}
 @GetMapping("/invoices") public List<InvoiceResponse> invoices(@AuthenticationPrincipal Jwt jwt){return billing.adminInvoices(jwt);}
 @PutMapping("/invoices/{id}/discount-decision") public InvoiceResponse discountDecision(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id,@Valid @RequestBody DiscountDecisionRequest request){return billing.approveDiscount(jwt,id,request.approved());}
}
