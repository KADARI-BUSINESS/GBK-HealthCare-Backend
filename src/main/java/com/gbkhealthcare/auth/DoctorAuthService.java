package com.gbkhealthcare.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class DoctorAuthService {

    private final DoctorRepository doctorRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final long expirationSeconds;

    public DoctorAuthService(
            DoctorRepository doctorRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService,
            @Value("${app.security.jwt-expiration-seconds:3600}") long expirationSeconds) {

        this.doctorRepository = doctorRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.expirationSeconds = expirationSeconds;
    }

    public LoginResult login(DoctorLoginRequest request) {
        String email = request.email().trim();

        Doctor doctor = doctorRepository
                .findByEmailIgnoreCase(email)
                .filter(Doctor::isEnabled)
                .orElseThrow(() -> new BadCredentialsException(
                        "Invalid email or password."));

        if (!passwordEncoder.matches(
                request.password(),
                doctor.getPasswordHash())) {

            throw new BadCredentialsException(
                    "Invalid email or password.");
        }

        String token = jwtTokenService.generateToken(doctor);

        return new LoginResult(
                token,
                "Bearer",
                expirationSeconds);
    }

    public record LoginResult(
            String accessToken,
            String tokenType,
            long expiresInSeconds) {
    }
}