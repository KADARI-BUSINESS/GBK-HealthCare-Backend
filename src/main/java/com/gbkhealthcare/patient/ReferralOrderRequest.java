package com.gbkhealthcare.patient;
import jakarta.validation.constraints.*;
public record ReferralOrderRequest(@NotBlank @Size(max=160) String specialty,@Size(max=200) String providerOrFacility,@NotBlank @Size(max=10000) String clinicalReason,@NotNull ReferralOrder.Priority priority,@Size(max=10000) String instructions){}
