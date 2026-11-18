package com.gbkhealthcare.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminLoginRequest(
        @NotBlank @Email @Size(max = 160) String email,
        @NotBlank @Size(max = 128) String password) {}
