package com.gbkhealthcare.auth;

import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class DoctorAuthService {

    private static final Logger log = LoggerFactory.getLogger(DoctorAuthService.class);

    private final DoctorRepository doctorRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final RefreshSessionManager refreshSessionService;
    private final long expirationSeconds;

    public DoctorAuthService(
            DoctorRepository doctorRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService,
            RefreshSessionManager refreshSessionService,
            @Value("${app.security.jwt-expiration-seconds:900}") long expirationSeconds) {

        this.doctorRepository = doctorRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.refreshSessionService = refreshSessionService;
        this.expirationSeconds = expirationSeconds;
    }

    public LoginResult login(DoctorLoginRequest request, String correlationId) {
        long loginStartedAt = System.nanoTime();
        log.info("login_diagnostic correlationId={} stage=service_entry elapsedMs=0 outcome=success",
                correlationId);
        boolean loginSucceeded = false;
        try {
            String email = request.email().trim();

            long lookupStartedAt = System.nanoTime();
            Doctor doctor;
            try {
                java.util.Optional<Doctor> doctorResult = doctorRepository.findByEmailIgnoreCase(email);
                doctor = doctorResult
                        .filter(Doctor::isEnabled)
                        .orElseThrow(() -> new BadCredentialsException(
                                "Invalid email or password."));
                log.info("login_diagnostic correlationId={} stage=doctor_repository_lookup elapsedMs={} outcome=success",
                        correlationId, elapsedMillis(lookupStartedAt));
            } catch (RuntimeException exception) {
                log.warn("login_diagnostic correlationId={} stage=doctor_repository_lookup elapsedMs={} outcome=failure exceptionType={}",
                        correlationId, elapsedMillis(lookupStartedAt), exception.getClass().getSimpleName());
                throw exception;
            }

            long passwordStartedAt = System.nanoTime();
            try {
                if (!passwordEncoder.matches(request.password(), doctor.getPasswordHash())) {
                    log.warn("login_diagnostic correlationId={} stage=bcrypt_password_match elapsedMs={} outcome=failure exceptionType={}",
                            correlationId, elapsedMillis(passwordStartedAt), BadCredentialsException.class.getSimpleName());
                    throw new BadCredentialsException("Invalid email or password.");
                }
                log.info("login_diagnostic correlationId={} stage=bcrypt_password_match elapsedMs={} outcome=success",
                        correlationId, elapsedMillis(passwordStartedAt));
            } catch (RuntimeException exception) {
                if (!(exception instanceof BadCredentialsException)) {
                    log.warn("login_diagnostic correlationId={} stage=bcrypt_password_match elapsedMs={} outcome=failure exceptionType={}",
                            correlationId, elapsedMillis(passwordStartedAt), exception.getClass().getSimpleName());
                }
                throw exception;
            }

            long jwtStartedAt = System.nanoTime();
            String token;
            RefreshSessionManager.IssuedSession refreshSession;
            try {
                token = jwtTokenService.generateToken(doctor);
                refreshSession = refreshSessionService.create(doctor.getId());
                log.info("login_diagnostic correlationId={} stage=jwt_generation elapsedMs={} outcome=success",
                        correlationId, elapsedMillis(jwtStartedAt));
            } catch (RuntimeException exception) {
                log.error("login_diagnostic correlationId={} stage=jwt_generation elapsedMs={} outcome=failure exceptionType={}",
                        correlationId, elapsedMillis(jwtStartedAt), exception.getClass().getSimpleName());
                throw exception;
            }

            loginSucceeded = true;
            return new LoginResult(token, "Bearer", expirationSeconds, doctor.getEmail(), refreshSession.refreshToken(), refreshSession.expiresAt());
        } finally {
            log.info("login_diagnostic correlationId={} stage=service_completion elapsedMs={} outcome={}",
                    correlationId, elapsedMillis(loginStartedAt), loginSucceeded ? "success" : "failure");
        }
    }

    private static long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }

    public record LoginResult(
            String accessToken,
            String tokenType,
            long expiresInSeconds,
            String doctorEmail,
            String refreshToken,
            java.time.Instant refreshExpiresAt) {
    }
}
