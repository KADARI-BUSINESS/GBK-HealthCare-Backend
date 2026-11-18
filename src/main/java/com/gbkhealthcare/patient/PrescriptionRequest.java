package com.gbkhealthcare.patient;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record PrescriptionRequest(@Size(max=5000) String notes,
        @NotEmpty @Size(max=30) List<@NotNull @Valid PrescriptionItemRequest> items,
        Long supersedesPrescriptionId) {}
