package com.gbkhealthcare.billing;
import jakarta.validation.constraints.NotNull;
public record ServiceStatusRequest(@NotNull Boolean active){}
