package com.gbkhealthcare.patient;
import jakarta.validation.constraints.*;
public record InvestigationOrderRequest(@NotNull InvestigationOrder.Type type,@NotBlank @Size(max=180) String name,@Size(max=10000) String indication,@NotNull InvestigationOrder.Priority priority){}
