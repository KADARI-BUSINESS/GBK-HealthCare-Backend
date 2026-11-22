package com.gbkhealthcare.auth;

import jakarta.validation.constraints.NotNull;

public record DoctorStatusRequest(
        @NotNull(message = "enabled is required")
        Boolean enabled) {
}
