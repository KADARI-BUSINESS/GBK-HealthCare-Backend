package com.gbkhealthcare.auth;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth/doctor")
public class DoctorAuthController {
    private static final Logger log = LoggerFactory.getLogger(DoctorAuthController.class);
    private static final String REFRESH_COOKIE = "doctor_refresh";
    private static final String CSRF_COOKIE = "doctor_csrf";
    private final DoctorAuthService doctorAuthService;
    private final RefreshSessionManager refreshSessions;
    private final String cookieDomain;
    private final long refreshLifetimeSeconds;
    private final long accessLifetimeSeconds;
    private final String[] allowedOrigins;
    private final boolean secureCookies;
    private final String cookieSameSite;

    public DoctorAuthController(DoctorAuthService doctorAuthService, RefreshSessionManager refreshSessions,
            @Value("${app.security.cookie-domain:}") String cookieDomain,
            @Value("${app.security.refresh-expiration-seconds:2592000}") long refreshLifetimeSeconds,
            @Value("${app.security.jwt-expiration-seconds:900}") long accessLifetimeSeconds,
            @Value("${app.security.cookies-secure:true}") boolean secureCookies,
            @Value("${app.security.cookie-same-site:None}") String cookieSameSite,
            @Value("${app.security.allowed-origins:http://localhost:5173,https://main.d2jr9663g6l11x.amplifyapp.com}") String allowedOrigins) {
        this.doctorAuthService = doctorAuthService;
        this.refreshSessions = refreshSessions;
        this.cookieDomain = cookieDomain;
        this.refreshLifetimeSeconds = refreshLifetimeSeconds;
        this.accessLifetimeSeconds = accessLifetimeSeconds;
        this.secureCookies = secureCookies;
        this.cookieSameSite = cookieSameSite;
        this.allowedOrigins = allowedOrigins.split(",");
    }

    @PostMapping("/login")
    public ResponseEntity<AccessTokenResponse> login(
            @Valid @RequestBody DoctorLoginRequest request,
            @RequestHeader(name = "Origin", required = false) String origin) {
        requireTrustedOrigin(origin);
        String correlationId = UUID.randomUUID().toString();
        long startedAt = System.nanoTime();
        log.info("login_diagnostic correlationId={} stage=controller_entry elapsedMs=0 outcome=success", correlationId);
        try {
            DoctorAuthService.LoginResult result = doctorAuthService.login(request, correlationId);
            String csrfToken = newCsrfToken();
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, refreshCookie(result.refreshToken(), refreshLifetimeSeconds, true).toString())
                    .header(HttpHeaders.SET_COOKIE, csrfCookie(csrfToken, refreshLifetimeSeconds).toString())
                    .body(new AccessTokenResponse(result.accessToken(), result.tokenType(), result.expiresInSeconds(), result.doctorEmail(), csrfToken));
        } catch (BadCredentialsException exception) {
            log.warn("login_diagnostic correlationId={} stage=login_completion elapsedMs={} outcome=failure exceptionType={}",
                    correlationId, elapsedMillis(startedAt), exception.getClass().getSimpleName());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password.");
        } catch (RuntimeException exception) {
            log.error("login_diagnostic correlationId={} stage=login_completion elapsedMs={} outcome=failure exceptionType={}",
                    correlationId, elapsedMillis(startedAt), exception.getClass().getSimpleName());
            throw exception;
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<AccessTokenResponse> refresh(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken,
            @CookieValue(name = CSRF_COOKIE, required = false) String csrfCookie,
            @RequestHeader(name = "X-XSRF-TOKEN", required = false) String csrfHeader,
            @RequestHeader(name = "Origin", required = false) String origin) {
        requireCsrf(csrfCookie, csrfHeader, origin);
        RefreshSessionManager.IssuedSession session;
        try {
            session = refreshSessions.rotate(refreshToken);
        } catch (ResponseStatusException exception) {
            if (exception.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .header(HttpHeaders.SET_COOKIE, refreshCookie("", 0, true).toString())
                        .header(HttpHeaders.SET_COOKIE, csrfCookie("", 0).toString())
                        .build();
            }
            throw exception;
        }
        String nextCsrf = newCsrfToken();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie(session.refreshToken(), refreshLifetimeSeconds, true).toString())
                .header(HttpHeaders.SET_COOKIE, csrfCookie(nextCsrf, refreshLifetimeSeconds).toString())
                .body(new AccessTokenResponse(session.accessToken(), "Bearer", accessLifetimeSeconds, session.doctorEmail(), nextCsrf));
    }

    @GetMapping("/csrf")
    public ResponseEntity<CsrfTokenResponse> csrf(
            @CookieValue(name = CSRF_COOKIE, required = false) String existingToken,
            @RequestHeader(name = "Origin", required = false) String origin) {
        requireTrustedOrigin(origin);
        String token = existingToken == null || existingToken.isBlank() ? newCsrfToken() : existingToken;
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, csrfCookie(token, refreshLifetimeSeconds).toString())
                .body(new CsrfTokenResponse(token));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken,
            @CookieValue(name = CSRF_COOKIE, required = false) String csrfCookie,
            @RequestHeader(name = "X-XSRF-TOKEN", required = false) String csrfHeader,
            @RequestHeader(name = "Origin", required = false) String origin) {
        requireCsrf(csrfCookie, csrfHeader, origin);
        refreshSessions.revoke(refreshToken);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshCookie("", 0, true).toString())
                .header(HttpHeaders.SET_COOKIE, csrfCookie("", 0).toString())
                .build();
    }

    private void requireCsrf(String cookie, String header, String origin) {
        requireTrustedOrigin(origin);
        if (cookie == null || header == null || !java.security.MessageDigest.isEqual(
                cookie.getBytes(java.nio.charset.StandardCharsets.UTF_8), header.getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "CSRF validation failed.");
        }
    }

    private void requireTrustedOrigin(String origin) {
        if (origin == null || java.util.Arrays.stream(allowedOrigins).map(String::trim).noneMatch(origin::equals)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Request origin is not allowed.");
        }
    }

    private ResponseCookie refreshCookie(String value, long maxAge, boolean httpOnly) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(REFRESH_COOKIE, value)
                .httpOnly(httpOnly).secure(secureCookies).sameSite(cookieSameSite).path("/api/auth/doctor")
                .maxAge(Duration.ofSeconds(maxAge));
        if (!cookieDomain.isBlank()) builder.domain(cookieDomain);
        return builder.build();
    }

    private ResponseCookie csrfCookie(String value, long maxAge) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(CSRF_COOKIE, value)
                .httpOnly(false).secure(secureCookies).sameSite(cookieSameSite).path("/api/auth/doctor")
                .maxAge(Duration.ofSeconds(maxAge));
        if (!cookieDomain.isBlank()) builder.domain(cookieDomain);
        return builder.build();
    }

    private static String newCsrfToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static long elapsedMillis(long startedAt) { return (System.nanoTime() - startedAt) / 1_000_000; }
    public record AccessTokenResponse(String accessToken, String tokenType, long expiresInSeconds, String doctorEmail, String csrfToken) {}
    public record CsrfTokenResponse(String csrfToken) {}
}
