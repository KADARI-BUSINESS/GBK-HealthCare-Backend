package com.gbkhealthcare.patient;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FollowUpReasonRequest(@NotBlank @Size(max = 1000) String reason) {}
