package com.gbkhealthcare.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DoctorLoginRequest(

        @NotBlank(message = "Email is required") @Email(message = "Enter a valid email address") @Size(max = 160, message = "Email is too long") String email,

        @NotBlank(message = "Password is required") @Size(max = 128, message = "Password is too long") String password

) {
}