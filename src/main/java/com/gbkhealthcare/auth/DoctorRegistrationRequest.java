
package com.gbkhealthcare.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DoctorRegistrationRequest(

        @NotBlank(message = "Full name is required")
        @Size(max = 120, message = "Full name is too long")
        String fullName,

        @NotBlank(message = "Email is required")
        @Email(message = "Enter a valid email address")
        @Size(max = 160, message = "Email is too long")
        String email,

        @NotBlank(message = "Mobile number is required")
        @Pattern(regexp = "^[6-9][0-9]{9}$", message = "Enter a valid 10-digit Indian mobile number")
        String mobileNumber,

        @NotBlank(message = "Qualification is required")
        @Size(max = 120, message = "Qualification is too long")
        String qualification,

        @NotBlank(message = "Specialization is required")
        @Size(max = 120, message = "Specialization is too long")
        String specialization,

        @Size(max = 100, message = "Medical registration number is too long")
        String medicalRegistrationNumber,

        @NotBlank(message = "Password is required")
        @Size(min = 12, max = 128,
                message = "Password must contain between 12 and 128 characters")
        String password

) {
}
