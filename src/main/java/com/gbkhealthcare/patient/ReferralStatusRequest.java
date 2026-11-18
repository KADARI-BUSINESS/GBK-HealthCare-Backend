package com.gbkhealthcare.patient;
import jakarta.validation.constraints.NotNull;
public record ReferralStatusRequest(@NotNull ReferralOrder.Status status){}
