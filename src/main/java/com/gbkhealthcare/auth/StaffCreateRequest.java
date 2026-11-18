package com.gbkhealthcare.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record StaffCreateRequest(
        @NotBlank @Size(max = 120) String fullName,
        @NotBlank @Email @Size(max = 160) String email,
        @Pattern(regexp = "^$|[6-9][0-9]{9}", message = "Mobile number must be a valid 10-digit number.") String mobileNumber,
        @NotBlank @Size(min = 12, max = 72) String temporaryPassword,
        @NotNull @Size(min = 1, max = 3) Set<StaffRole> roles) {}
