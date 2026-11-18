package com.gbkhealthcare.auth;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;

@RestController
@RequestMapping("/api/auth/staff")
public class StaffAuthController {
    private static final String REFRESH_COOKIE = "staff_refresh";
    private static final String CSRF_COOKIE = "staff_csrf";
    private final StaffAuthService auth;
    private final StaffRefreshSessionService sessions;
    private final String cookieDomain, sameSite;
    private final long refreshLifetime;
    private final boolean secureCookies;
    private final String[] allowedOrigins;

    public StaffAuthController(StaffAuthService auth, StaffRefreshSessionService sessions,
            @Value("${app.security.cookie-domain:}") String cookieDomain,
            @Value("${app.security.staff-refresh-expiration-seconds:2592000}") long refreshLifetime,
            @Value("${app.security.cookies-secure:true}") boolean secureCookies,
            @Value("${app.security.cookie-same-site:None}") String sameSite,
            @Value("${app.security.allowed-origins:http://localhost:5173}") String allowedOrigins) {
        this.auth = auth; this.sessions = sessions; this.cookieDomain = cookieDomain; this.refreshLifetime = refreshLifetime;
        this.secureCookies = secureCookies; this.sameSite = sameSite; this.allowedOrigins = allowedOrigins.split(",");
    }

    @PostMapping("/login")
    public ResponseEntity<StaffAccessTokenResponse> login(@Valid @RequestBody StaffLoginRequest request,
            @RequestHeader(name = "Origin", required = false) String origin) {
        requireTrustedOrigin(origin);
        try {
            StaffAuthService.LoginResult result = auth.login(request);
            String csrf = newCsrfToken();
            return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, refreshCookie(result.session().refreshToken(), refreshLifetime).toString())
                    .header(HttpHeaders.SET_COOKIE, csrfCookie(csrf, refreshLifetime).toString())
                    .body(new StaffAccessTokenResponse(result.accessToken(), "Bearer", result.expiresInSeconds(), result.email(), result.roles(), csrf));
        } catch (BadCredentialsException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password.");
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<StaffAccessTokenResponse> refresh(@CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken,
            @CookieValue(name = CSRF_COOKIE, required = false) String csrfCookie,
            @RequestHeader(name = "X-XSRF-TOKEN", required = false) String csrfHeader,
            @RequestHeader(name = "Origin", required = false) String origin) {
        requireCsrf(csrfCookie, csrfHeader, origin);
        StaffRefreshSessionService.IssuedSession session;
        try { session = sessions.rotate(refreshToken); }
        catch (ResponseStatusException exception) {
            if (exception.getStatusCode() == HttpStatus.UNAUTHORIZED) return clearCookiesForAccess(HttpStatus.UNAUTHORIZED);
            throw exception;
        }
        String csrf = newCsrfToken();
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, refreshCookie(session.refreshToken(), refreshLifetime).toString())
                .header(HttpHeaders.SET_COOKIE, csrfCookie(csrf, refreshLifetime).toString())
                .body(new StaffAccessTokenResponse(session.accessToken(), "Bearer", auth.getAccessLifetime(), session.email(), null, csrf));
    }

    @GetMapping("/csrf")
    public ResponseEntity<CsrfTokenResponse> csrf(@CookieValue(name = CSRF_COOKIE, required = false) String existing,
            @RequestHeader(name = "Origin", required = false) String origin) {
        requireTrustedOrigin(origin);
        String token = existing == null || existing.isBlank() ? newCsrfToken() : existing;
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, csrfCookie(token, refreshLifetime).toString()).body(new CsrfTokenResponse(token));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken,
            @CookieValue(name = CSRF_COOKIE, required = false) String csrfCookie,
            @RequestHeader(name = "X-XSRF-TOKEN", required = false) String csrfHeader,
            @RequestHeader(name = "Origin", required = false) String origin) {
        requireCsrf(csrfCookie, csrfHeader, origin); sessions.revoke(refreshToken); return clearCookies(HttpStatus.NO_CONTENT);
    }

    private ResponseEntity<Void> clearCookies(HttpStatus status) {
        return ResponseEntity.status(status).header(HttpHeaders.SET_COOKIE, refreshCookie("", 0).toString())
                .header(HttpHeaders.SET_COOKIE, csrfCookie("", 0).toString()).build();
    }
    private ResponseEntity<StaffAccessTokenResponse> clearCookiesForAccess(HttpStatus status) {
        return ResponseEntity.<StaffAccessTokenResponse>status(status).header(HttpHeaders.SET_COOKIE, refreshCookie("", 0).toString())
                .header(HttpHeaders.SET_COOKIE, csrfCookie("", 0).toString()).build();
    }
    private void requireCsrf(String cookie, String header, String origin) {
        requireTrustedOrigin(origin);
        if (cookie == null || header == null || !MessageDigest.isEqual(cookie.getBytes(StandardCharsets.UTF_8), header.getBytes(StandardCharsets.UTF_8)))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "CSRF validation failed.");
    }
    private void requireTrustedOrigin(String origin) {
        if (origin == null || Arrays.stream(allowedOrigins).map(String::trim).noneMatch(origin::equals))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Request origin is not allowed.");
    }
    private ResponseCookie refreshCookie(String value, long age) { return cookie(REFRESH_COOKIE, value, true, age); }
    private ResponseCookie csrfCookie(String value, long age) { return cookie(CSRF_COOKIE, value, false, age); }
    private ResponseCookie cookie(String name, String value, boolean httpOnly, long age) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value).httpOnly(httpOnly).secure(secureCookies)
                .sameSite(sameSite).path("/api/auth/staff").maxAge(Duration.ofSeconds(age));
        if (!cookieDomain.isBlank()) builder.domain(cookieDomain);
        return builder.build();
    }
    private static String newCsrfToken() { byte[] bytes = new byte[32]; new SecureRandom().nextBytes(bytes); return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); }
    public record StaffAccessTokenResponse(String accessToken, String tokenType, long expiresInSeconds, String email, java.util.Set<StaffRole> roles, String csrfToken) {}
    public record CsrfTokenResponse(String csrfToken) {}
}
