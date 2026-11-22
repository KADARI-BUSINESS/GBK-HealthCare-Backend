package com.gbkhealthcare.patient;
import jakarta.validation.constraints.NotNull;
public record InvestigationStatusRequest(@NotNull InvestigationOrder.Status status){}
