package com.gbkhealthcare.auth;

import jakarta.validation.constraints.NotNull;

public record StaffStatusRequest(@NotNull Boolean enabled) {}
