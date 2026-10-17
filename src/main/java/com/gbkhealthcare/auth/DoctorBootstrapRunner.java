package com.gbkhealthcare.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DoctorBootstrapRunner {

    @Bean
    CommandLineRunner bootstrapDoctor(
            DoctorRepository doctorRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.security.bootstrap-doctor.enabled:false}") boolean bootstrapEnabled,
            @Value("${DOCTOR_BOOTSTRAP_NAME:}") String doctorName,
            @Value("${DOCTOR_BOOTSTRAP_EMAIL:}") String doctorEmail,
            @Value("${DOCTOR_BOOTSTRAP_PASSWORD:}") String doctorPassword) {

        return args -> {
            // Disabled by default. No public signup endpoint is needed.
            if (!bootstrapEnabled) {
                return;
            }

            if (doctorName == null || doctorName.isBlank()
                    || doctorEmail == null || doctorEmail.isBlank()
                    || doctorPassword == null
                    || doctorPassword.length() < 12) {

                throw new IllegalStateException(
                        "Bootstrap requires a doctor name, email, "
                                + "and a password of at least 12 characters.");
            }

            String normalizedEmail = doctorEmail.trim().toLowerCase(java.util.Locale.ROOT);

            if (doctorRepository.existsByEmailIgnoreCase(normalizedEmail)) {
                System.out.println(
                        "Bootstrap doctor already exists; no changes made.");
                return;
            }

            Doctor doctor = new Doctor();
            doctor.setFullName(doctorName.trim());
            doctor.setEmail(normalizedEmail);
            doctor.setPasswordHash(
                    passwordEncoder.encode(doctorPassword));
            doctor.setEnabled(true);

            doctorRepository.save(doctor);

            System.out.println(
                    "Initial doctor account created successfully.");
        };
    }
}