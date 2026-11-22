package com.gbkhealthcare.patient;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record PrescriptionItemRequest(@NotBlank @Size(max=180) String medicineName,
        @NotBlank @Size(max=120) String dosage, @NotBlank @Size(max=80) String route,
        @NotBlank @Size(max=120) String frequency, @NotBlank @Size(max=80) String duration,
        @NotNull @DecimalMin("0.01") @Digits(integer=5, fraction=2) BigDecimal quantity,
        @Size(max=4000) String instructions, @NotNull LocalDate startDate) {}
