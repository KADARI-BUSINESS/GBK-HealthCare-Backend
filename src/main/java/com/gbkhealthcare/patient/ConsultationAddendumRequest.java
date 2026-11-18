package com.gbkhealthcare.patient;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConsultationAddendumRequest(@NotBlank @Size(max = 20000) String text) {}
