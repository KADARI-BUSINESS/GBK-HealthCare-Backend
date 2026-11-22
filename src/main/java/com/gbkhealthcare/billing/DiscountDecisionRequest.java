package com.gbkhealthcare.billing;
import jakarta.validation.constraints.NotNull;
public record DiscountDecisionRequest(@NotNull Boolean approved){}
