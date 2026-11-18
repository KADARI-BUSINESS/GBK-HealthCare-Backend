package com.gbkhealthcare.patient;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record NursingIntakeRequest(
        @Min(40) @Max(300) Integer bpSystolic,
        @Min(20) @Max(200) Integer bpDiastolic,
        @Min(20) @Max(250) Integer pulseRate,
        @DecimalMin("25.0") @DecimalMax("45.0") BigDecimal temperatureC,
        @Min(4) @Max(80) Integer respiratoryRate,
        @Min(1) @Max(100) Integer oxygenSaturation,
        @DecimalMin("20.0") @DecimalMax("250.0") BigDecimal heightCm,
        @DecimalMin("0.5") @DecimalMax("500.0") BigDecimal weightKg,
        @Size(max = 10000) String allergies,
        @Size(max = 10000) String medicationReconciliation,
        @Size(max = 10000) String nursingNotes) {
    @AssertTrue(message = "Systolic blood pressure must be greater than diastolic blood pressure.")
    public boolean isBloodPressureConsistent() {
        return bpSystolic == null || bpDiastolic == null || bpSystolic > bpDiastolic;
    }
}
