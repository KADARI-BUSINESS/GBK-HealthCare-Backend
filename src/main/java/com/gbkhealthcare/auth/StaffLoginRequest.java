package com.gbkhealthcare.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StaffLoginRequest(@NotBlank @Email @Size(max = 160) String email,
        @NotBlank @Size(max = 72) String password) {}
